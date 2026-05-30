
package com.kolo.kolo_backend.fines.service

import com.kolo.kolo_backend.auth.User
import com.kolo.kolo_backend.contributions.dto.PaymentInitResponse
import com.kolo.kolo_backend.contributions.repository.TransactionRepository
import com.kolo.kolo_backend.fines.Fine
import com.kolo.kolo_backend.fines.dto.*
import com.kolo.kolo_backend.fines.repository.FineRepository
import com.kolo.kolo_backend.groups.Membership
import com.kolo.kolo_backend.groups.repository.GroupRepository
import com.kolo.kolo_backend.groups.repository.MembershipRepository
import com.kolo.kolo_backend.payments.service.PaystackService
import com.kolo.kolo_backend.contributions.Transaction
import com.kolo.kolo_backend.notifications.service.NotificationService
import com.kolo.kolo_backend.shared.exception.AppException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class FineService(
    private val fineRepository: FineRepository,
    private val membershipRepository: MembershipRepository,
    private val groupRepository: GroupRepository,
    private val transactionRepository: TransactionRepository,
    private val paystackService: PaystackService,
    private val notificationService: NotificationService
) {

    companion object {
        private const val PLATFORM_CUT_RATE = 0.30
        private const val POOL_CUT_RATE = 0.70
        private const val TIER_ONE_RATE = 0.05   // 5% — 4 to 7 days late
        private const val TIER_TWO_RATE = 0.10   // 10% — 8 to 14 days late
        private const val SUSPENSION_DAYS = 30   // Suspend after 30 days
    }

    // Called by the cron job every day at midnight
    @Transactional
    fun processDailyFines() {
        println("⏰ Fine engine running — ${LocalDate.now()}")

        val activeMemberships = membershipRepository
            .findAllByStatus("ACTIVE")
            .filter { it.nextDueDate != null }

        var finesIssued = 0
        var suspensions = 0

        activeMemberships.forEach { membership ->
            val daysLate = ChronoUnit.DAYS
                .between(membership.nextDueDate, LocalDate.now())
                .toInt()

            when {
                // Not late — skip
                daysLate <= 0 -> return@forEach

                // Grace period — first ever late, warn only
                daysLate <= 3 && !membership.gracePeriodUsed -> {
                    membership.gracePeriodUsed = true
                    membershipRepository.save(membership)
                    println("⚠ Grace period used — ${membership.user.phoneNumber}")
                }

                // Tier 1 fine — 4 to 7 days late
                daysLate in 4..7 -> {
                    issueFine(membership, daysLate, TIER_ONE_RATE)
                    finesIssued++
                }

                // Tier 2 fine — 8 to 14 days late
                daysLate in 8..14 -> {
                    issueFine(membership, daysLate, TIER_TWO_RATE)
                    finesIssued++
                }

                // Suspend — 30+ days late
                daysLate >= SUSPENSION_DAYS -> {
                    suspendMembership(membership)
                    suspensions++
                }
            }
        }

        println("✅ Fine engine complete — Fines: $finesIssued, Suspensions: $suspensions")
    }

    // Issue a fine for a membership
    private fun issueFine(
        membership: Membership,
        daysLate: Int,
        rate: Double
    ) {
        // Don't double-fine for same day
        val existingFines = fineRepository.findAllByMembershipIdAndStatus(
            membership.id!!, "PENDING"
        )

        val alreadyFinedToday = existingFines.any {
            it.createdAt.toLocalDate() == LocalDate.now()
        }

        if (alreadyFinedToday) return

        val fineAmount = (membership.contributionAmount * rate).toLong()
        val platformCut = (fineAmount * PLATFORM_CUT_RATE).toLong()
        val poolCut = (fineAmount * POOL_CUT_RATE).toLong()

        fineRepository.save(
            Fine(
                membership = membership,
                group = membership.group,
                user = membership.user,
                amount = fineAmount,
                reason = "Late contribution — $daysLate days overdue",
                daysLate = daysLate,
                platformCut = platformCut,
                poolCut = poolCut,
                status = "PENDING"
            )
        )

        notificationService.sendFineNotification(
            phoneNumber = membership.user.phoneNumber,
            amountFormatted = formatKobo(fineAmount),
            groupName = membership.group.name,
            daysLate = daysLate
        )

        // Drop trust score by 5 points per fine
        val user = membership.user
        user.trustScore = maxOf(0, user.trustScore - 5)

        println("💸 Fine issued — ${membership.user.phoneNumber}, " +
                "Amount: ${formatKobo(fineAmount)}, Days late: $daysLate")
    }

    // Suspend membership after 30+ days
    private fun suspendMembership(membership: Membership) {
        membership.status = "SUSPENDED"
        membership.updatedAt = LocalDateTime.now()
        membershipRepository.save(membership)

        notificationService.sendSuspensionNotice(
            phoneNumber = membership.user.phoneNumber,
            groupName = membership.group.name
        )

        println("🚫 Membership suspended — ${membership.user.phoneNumber}")
    }

    // Get my pending fines
    @Transactional(readOnly = true)
    fun getMyFines(user: User): FinesSummaryResponse {
        val fines = fineRepository.findAllByUserIdAndStatus(user.id!!, "PENDING")
        val totalPending = fineRepository.sumPendingFinesByUser(user.id!!)

        return FinesSummaryResponse(
            totalPendingKobo = totalPending,
            totalPendingFormatted = formatKobo(totalPending),
            fines = fines.map { it.toResponse() }
        )
    }

    // Initiate fine payment
    @Transactional
    fun initiateFinepayment(user: User, fineId: UUID): PaymentInitResponse {
        val fine = fineRepository.findById(fineId).orElseThrow {
            AppException("Fine not found", HttpStatus.NOT_FOUND)
        }

        if (fine.user.id != user.id) {
            throw AppException("Unauthorized", HttpStatus.FORBIDDEN)
        }

        if (fine.status != "PENDING") {
            throw AppException("Fine is already ${fine.status}", HttpStatus.BAD_REQUEST)
        }

        val reference = "KOLO-FINE-${UUID.randomUUID().toString().replace("-","").take(12).uppercase()}"

        // Create pending transaction
        transactionRepository.save(
            Transaction(
                user = user,
                group = fine.group,
                membership = fine.membership,
                type = "FINE",
                amount = fine.amount,
                paystackReference = reference,
                status = "PENDING",
                metadata = """{"fineId":"${fine.id}","type":"FINE"}"""
            )
        )

        // Initialize Paystack payment
        val paystackData = paystackService.initializePayment(
            email = "${user.phoneNumber}@kolo.app",
            amountKobo = fine.amount,
            reference = reference,
            metadata = mapOf(
                "fineId" to fine.id.toString(),
                "type" to "FINE"
            )
        )

        return PaymentInitResponse(
            authorizationUrl = paystackData.authorizationUrl,
            reference = reference,
            amountKobo = fine.amount,
            amountFormatted = formatKobo(fine.amount),
            message = "Complete fine payment at the authorization URL"
        )
    }

    // Verify fine payment
    @Transactional
    fun verifyFinePayment(user: User, reference: String): FineResponse {
        val transaction = transactionRepository.findByPaystackReference(reference)
            ?: throw AppException("Transaction not found", HttpStatus.NOT_FOUND)

        if (transaction.status == "SUCCESS") {
            val fineId = extractFineId(transaction.metadata)
            val fine = fineRepository.findById(fineId).orElseThrow {
                AppException("Fine not found", HttpStatus.NOT_FOUND)
            }
            return fine.toResponse()
        }

        // Verify with Paystack
        val paystackData = paystackService.verifyTransaction(reference)

        if (paystackData.status == "success") {
            // Mark transaction success
            transaction.status = "SUCCESS"
            transaction.paystackStatus = "success"
            transaction.updatedAt = LocalDateTime.now()
            transactionRepository.save(transaction)

            // Get fine ID from metadata
            val fineId = extractFineId(transaction.metadata)
            val fine = fineRepository.findById(fineId).orElseThrow {
                AppException("Fine not found", HttpStatus.NOT_FOUND)
            }

            // Mark fine paid
            fine.status = "PAID"
            fine.updatedAt = LocalDateTime.now()
            fineRepository.save(fine)

            notificationService.sendFinePaymentConfirmation(
                phoneNumber = user.phoneNumber,
                amountFormatted = formatKobo(fine.amount)
            )

            // Distribute fine — pool cut goes to group pool
            val group = fine.group
            group.poolBalance += fine.poolCut
            groupRepository.save(group)

            // Restore trust score
            val fineUser = fine.user
            fineUser.trustScore = minOf(100, fineUser.trustScore + 2)

            // Check if all fines paid — reactivate if suspended
            val remainingFines = fineRepository
                .findAllByUserIdAndStatus(user.id!!, "PENDING")

            if (remainingFines.isEmpty()) {
                val membership = fine.membership
                if (membership.status == "SUSPENDED") {
                    membership.status = "ACTIVE"
                    membership.updatedAt = LocalDateTime.now()
                    membershipRepository.save(membership)
                    println("✅ Membership reactivated — ${user.phoneNumber}")
                }
            }

            println("✅ Fine paid — ${user.phoneNumber}, Amount: ${formatKobo(fine.amount)}")
            return fine.toResponse()
        }

        throw AppException("Payment not confirmed by Paystack", HttpStatus.BAD_REQUEST)
    }

    // Get fines for a group (admin only)
    @Transactional(readOnly = true)
    fun getGroupFines(user: User, groupId: UUID): List<FineResponse> {
        val membership = membershipRepository.findByUserIdAndGroupId(user.id!!, groupId)
            ?: throw AppException("You are not a member of this group", HttpStatus.FORBIDDEN)

        if (membership.role != "ADMIN") {
            throw AppException("Only admin can view group fines", HttpStatus.FORBIDDEN)
        }

        return fineRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId)
            .map { it.toResponse() }
    }

    // Extract fine ID from transaction metadata
    private fun extractFineId(metadata: String?): UUID {
        val fineIdRegex = """"fineId":"([^"]+)"""".toRegex()
        val match = fineIdRegex.find(metadata ?: "")
            ?: throw AppException("Fine ID not found in transaction metadata", HttpStatus.INTERNAL_SERVER_ERROR)
        return UUID.fromString(match.groupValues[1])
    }

    private fun Fine.toResponse() = FineResponse(
        id = id.toString(),
        amountKobo = amount,
        amountFormatted = formatKobo(amount),
        reason = reason,
        daysLate = daysLate,
        status = status,
        groupName = group.name,
        createdAt = createdAt.format(DateTimeFormatter.ISO_DATE_TIME)
    )

    private fun formatKobo(kobo: Long): String {
        val naira = kobo / 100.0
        return "₦${String.format("%,.2f", naira)}"
    }
}