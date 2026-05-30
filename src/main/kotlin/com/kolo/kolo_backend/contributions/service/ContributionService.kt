package com.kolo.kolo_backend.contributions.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.kolo.kolo_backend.auth.User
import com.kolo.kolo_backend.contributions.Transaction
import com.kolo.kolo_backend.contributions.dto.*
import com.kolo.kolo_backend.contributions.repository.TransactionRepository
import com.kolo.kolo_backend.groups.repository.GroupRepository
import com.kolo.kolo_backend.groups.repository.MembershipRepository
import com.kolo.kolo_backend.payments.service.PaystackService
import com.kolo.kolo_backend.shared.exception.AppException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.collections.get

@Service
class ContributionService(
    private val transactionRepository: TransactionRepository,
    private val groupRepository: GroupRepository,
    private val membershipRepository: MembershipRepository,
    private val paystackService: PaystackService
) {

    // Initiate contribution payment
    @Transactional
    fun initiateContribution(
        user: User,
        request: InitiateContributionRequest
    ): PaymentInitResponse {
        val groupId = UUID.fromString(request.groupId)
        val group = groupRepository.findById(groupId).orElseThrow {
            AppException("Group not found", HttpStatus.NOT_FOUND)
        }

        val membership = membershipRepository.findByUserIdAndGroupId(user.id!!, groupId)
            ?: throw AppException("You are not a member of this group", HttpStatus.FORBIDDEN)

        if (membership.status != "ACTIVE") {
            throw AppException(
                "Your membership is suspended. Clear outstanding fines first.",
                HttpStatus.FORBIDDEN
            )
        }

        val amountKobo = membership.contributionAmount
        val reference = "KOLO-CONTRIB-${UUID.randomUUID().toString().replace("-", "").take(12).uppercase()}"

        // Create pending transaction
        transactionRepository.save(
            Transaction(
                user = user,
                group = group,
                membership = membership,
                type = "CONTRIBUTION",
                amount = amountKobo,
                paystackReference = reference,
                status = "PENDING",
                metadata = """{"groupId":"${group.id}","membershipId":"${membership.id}","userId":"${user.id}"}"""
            )
        )

        // Initialize Paystack payment
        val paystackData = paystackService.initializePayment(
            email = "${user.phoneNumber}@kolo.app",
            amountKobo = amountKobo,
            reference = reference,
            metadata = mapOf(
                "groupId" to group.id.toString(),
                "membershipId" to membership.id.toString(),
                "userId" to user.id.toString(),
                "type" to "CONTRIBUTION"
            )
        )

        return PaymentInitResponse(
            authorizationUrl = paystackData.authorizationUrl,
            reference = reference,
            amountKobo = amountKobo,
            amountFormatted = formatKobo(amountKobo),
            message = "Complete your payment at the authorization URL"
        )
    }

    // Handle Paystack webhook — called when payment is confirmed
    @Transactional
    fun handleWebhook(payload: String, signature: String) {
        // Verify the webhook is genuinely from Paystack
        if (!paystackService.verifyWebhookSignature(payload, signature)) {
            throw AppException("Invalid webhook signature", HttpStatus.UNAUTHORIZED)
        }

        // Parse the event
        val objectMapper = ObjectMapper()
        val event = objectMapper.readValue(payload, Map::class.java)
        val eventType = event["event"] as? String ?: return
        val data = event["data"] as? Map<*, *> ?: return

        when (eventType) {
            "charge.success" -> handleChargeSuccess(data)
            "transfer.success" -> handleTransferSuccess(data)
            "transfer.failed" -> handleTransferFailed(data)
            else -> println("Unhandled Paystack event: $eventType")
        }
    }

    // Payment confirmed — update balances
    private fun handleChargeSuccess(data: Map<*, *>) {
        val reference = data["reference"] as? String ?: return
        val amountKobo = (data["amount"] as? Number)?.toLong() ?: return
        val status = data["status"] as? String ?: return

        val transaction = transactionRepository.findByPaystackReference(reference) ?: run {
            println("⚠ Transaction not found for reference: $reference")
            return
        }

        if (transaction.status == "SUCCESS") {
            println("⚠ Transaction already processed: $reference")
            return
        }

        transaction.paystackStatus = status
        transaction.status = "SUCCESS"
        transaction.updatedAt = LocalDateTime.now()
        transactionRepository.save(transaction)

        when (transaction.type) {
            "CONTRIBUTION" -> processContributionSuccess(transaction, amountKobo)
            else -> println("Unhandled transaction type: ${transaction.type}")
        }
    }

    private fun processContributionSuccess(transaction: Transaction, amountKobo: Long) {
        val membership = transaction.membership ?: return
        val group = transaction.group

        // Update member savings balance
        membership.savingsBalance += amountKobo
        membership.gracePeriodUsed = false

        // Calculate next due date
        membership.nextDueDate = when (membership.frequency) {
            "WEEKLY" -> LocalDate.now().plusWeeks(1)
            "MONTHLY" -> LocalDate.now().plusMonths(1)
            else -> LocalDate.now().plusMonths(1)
        }
        membership.updatedAt = LocalDateTime.now()
        membershipRepository.save(membership)

        // Update group pool balance
        group.poolBalance += amountKobo
        group.updatedAt = LocalDateTime.now()
        groupRepository.save(group)

        println("✅ Contribution processed — Member: ${transaction.user.phoneNumber}, Amount: ${formatKobo(amountKobo)}, Group: ${group.name}")
    }

    private fun handleTransferSuccess(data: Map<*, *>) {
        val reference = data["reference"] as? String ?: return
        val transaction = transactionRepository.findByPaystackReference(reference) ?: return
        transaction.status = "SUCCESS"
        transaction.paystackStatus = "success"
        transaction.updatedAt = LocalDateTime.now()
        transactionRepository.save(transaction)
        println("✅ Transfer successful: $reference")
    }

    private fun handleTransferFailed(data: Map<*, *>) {
        val reference = data["reference"] as? String ?: return
        val transaction = transactionRepository.findByPaystackReference(reference) ?: return
        transaction.status = "FAILED"
        transaction.paystackStatus = "failed"
        transaction.updatedAt = LocalDateTime.now()
        transactionRepository.save(transaction)
        println("❌ Transfer failed: $reference")
    }

    // Get contribution history for a group
    @Transactional(readOnly = true)
    fun getGroupHistory(user: User, groupId: UUID): ContributionHistoryResponse {
        // Verify member
        membershipRepository.findByUserIdAndGroupId(user.id!!, groupId)
            ?: throw AppException("You are not a member of this group", HttpStatus.FORBIDDEN)

        val transactions = transactionRepository
            .findAllByGroupIdAndTypeOrderByCreatedAtDesc(groupId, "CONTRIBUTION")

        val totalContributed = transactionRepository.sumSuccessfulContributions(groupId)

        return ContributionHistoryResponse(
            transactions = transactions.map { it.toResponse() },
            totalContributedKobo = totalContributed,
            totalContributedFormatted = formatKobo(totalContributed)
        )
    }

    // Get personal transaction history
    @Transactional(readOnly = true)
    fun getMyHistory(user: User): List<TransactionResponse> {
        return transactionRepository
            .findAllByUserIdOrderByCreatedAtDesc(user.id!!)
            .map { it.toResponse() }
    }

    private fun Transaction.toResponse() = TransactionResponse(
        id = id.toString(),
        type = type,
        amountKobo = amount,
        amountFormatted = formatKobo(amount),
        status = status,
        reference = paystackReference,
        groupName = group.name,
        createdAt = createdAt.format(DateTimeFormatter.ISO_DATE_TIME)
    )

    private fun formatKobo(kobo: Long): String {
        val naira = kobo / 100.0
        return "₦${String.format("%,.2f", naira)}"
    }

    @Transactional
    fun verifyAndProcessPayment(reference: String, user: User): TransactionResponse {
        val transaction = transactionRepository.findByPaystackReference(reference)
            ?: throw AppException("Transaction not found", HttpStatus.NOT_FOUND)

        if (transaction.user.id != user.id) {
            throw AppException("Unauthorized", HttpStatus.FORBIDDEN)
        }

        if (transaction.status == "SUCCESS") {
            return transaction.toResponse()
        }

        // Verify with Paystack directly
        val paystackData = paystackService.verifyTransaction(reference)

        if (paystackData.status == "success") {
            transaction.status = "SUCCESS"
            transaction.paystackStatus = "success"
            transaction.updatedAt = LocalDateTime.now()
            transactionRepository.save(transaction)

            // Process the contribution
            processContributionSuccess(transaction, paystackData.amountKobo)
        }

        return transaction.toResponse()
    }
}