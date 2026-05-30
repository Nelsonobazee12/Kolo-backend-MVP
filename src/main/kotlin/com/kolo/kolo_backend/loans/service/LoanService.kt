
package com.kolo.kolo_backend.loans.service

import com.kolo.kolo_backend.auth.User
import com.kolo.kolo_backend.auth.repository.UserRepository
import com.kolo.kolo_backend.contributions.Transaction
import com.kolo.kolo_backend.contributions.dto.PaymentInitResponse
import com.kolo.kolo_backend.contributions.repository.TransactionRepository
import com.kolo.kolo_backend.fines.repository.FineRepository
import com.kolo.kolo_backend.groups.repository.GroupRepository
import com.kolo.kolo_backend.groups.repository.MembershipRepository
import com.kolo.kolo_backend.loans.Loan
import com.kolo.kolo_backend.loans.LoanVote
import com.kolo.kolo_backend.loans.dto.*
import com.kolo.kolo_backend.loans.repository.LoanRepository
import com.kolo.kolo_backend.loans.repository.LoanVoteRepository
import com.kolo.kolo_backend.payments.service.PaystackService
import com.kolo.kolo_backend.shared.exception.AppException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class LoanService(
    private val loanRepository: LoanRepository,
    private val loanVoteRepository: LoanVoteRepository,
    private val membershipRepository: MembershipRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val fineRepository: FineRepository,
    private val transactionRepository: TransactionRepository,
    private val paystackService: PaystackService
) {

    companion object {
        private val TIER1_INTEREST_RATE = BigDecimal("0.05")  // 5%
        private val TIER2_INTEREST_RATE = BigDecimal("0.10")  // 10%
        private const val PLATFORM_INTEREST_CUT = 0.30
        private const val VOTING_WINDOW_HOURS = 72L
        private const val MIN_TRUST_SCORE = 40
    }

    // Check loan eligibility
    @Transactional(readOnly = true)
    fun checkEligibility(user: User, groupId: UUID): LoanEligibilityResponse {
        val membership = membershipRepository.findByUserIdAndGroupId(user.id!!, groupId)
            ?: throw AppException("You are not a member of this group", HttpStatus.FORBIDDEN)

        val pendingFines = fineRepository.sumPendingFinesByUser(user.id!!)
        val hasActiveLoan = loanRepository.existsByBorrowerIdAndStatusIn(
            user.id!!, listOf("PENDING", "APPROVED", "ACTIVE")
        )

        // Tier 1 — up to 100% of savings balance
        val tier1Eligible = membership.savingsBalance

        // Tier 2 — up to 3x savings balance
        val tier2Eligible = membership.savingsBalance * 3

        // Determine if can borrow
        val canBorrow: Boolean
        val reason: String?

        when {
            hasActiveLoan -> {
                canBorrow = false
                reason = "You already have an active loan. Repay it before requesting another."
            }
            pendingFines > 0 -> {
                canBorrow = false
                reason = "You have pending fines of ${formatKobo(pendingFines)}. Clear them first."
            }
            membership.status != "ACTIVE" -> {
                canBorrow = false
                reason = "Your membership is ${membership.status}. Contact group admin."
            }
            user.trustScore < MIN_TRUST_SCORE -> {
                canBorrow = false
                reason = "Your trust score (${user.trustScore}) is too low. Minimum is $MIN_TRUST_SCORE."
            }
            tier1Eligible == 0L -> {
                canBorrow = false
                reason = "You have no savings balance yet. Make contributions first."
            }
            else -> {
                canBorrow = true
                reason = null
            }
        }

        return LoanEligibilityResponse(
            tier1EligibleKobo = tier1Eligible,
            tier1EligibleFormatted = formatKobo(tier1Eligible),
            tier2EligibleKobo = tier2Eligible,
            tier2EligibleFormatted = formatKobo(tier2Eligible),
            hasActiveLoan = hasActiveLoan,
            trustScore = user.trustScore,
            pendingFinesKobo = pendingFines,
            pendingFinesFormatted = formatKobo(pendingFines),
            canBorrow = canBorrow,
            reason = reason
        )
    }

    // Request a loan
    @Transactional
    fun requestLoan(user: User, request: LoanRequest): LoanResponse {
        val groupId = UUID.fromString(request.groupId)
        val group = groupRepository.findById(groupId).orElseThrow {
            AppException("Group not found", HttpStatus.NOT_FOUND)
        }

        val membership = membershipRepository.findByUserIdAndGroupId(user.id!!, groupId)
            ?: throw AppException("You are not a member of this group", HttpStatus.FORBIDDEN)

        // Run eligibility checks
        val eligibility = checkEligibility(user, groupId)
        if (!eligibility.canBorrow) {
            throw AppException(eligibility.reason ?: "Not eligible for a loan", HttpStatus.BAD_REQUEST)
        }

        // Check pool has enough liquidity
        val activeLoanBook = loanRepository.sumActiveLoansByGroup(groupId)
        val availableLiquidity = group.poolBalance - activeLoanBook

        if (request.amountKobo > availableLiquidity) {
            throw AppException(
                "Insufficient pool liquidity. Available: ${formatKobo(availableLiquidity)}",
                HttpStatus.BAD_REQUEST
            )
        }

        // Determine tier
        val tier = if (request.amountKobo <= membership.savingsBalance) "TIER1" else "TIER2"
        val interestRate = if (tier == "TIER1") TIER1_INTEREST_RATE else TIER2_INTEREST_RATE

        // Tier 2 validations
        if (tier == "TIER2") {
            if (request.amountKobo > membership.savingsBalance * 3) {
                throw AppException(
                    "Maximum Tier 2 loan is 3x your savings: ${formatKobo(membership.savingsBalance * 3)}",
                    HttpStatus.BAD_REQUEST
                )
            }
            if (request.collateralType == null) {
                throw AppException(
                    "Collateral type is required for Tier 2 loans",
                    HttpStatus.BAD_REQUEST
                )
            }
        }

        val totalRepayable = calculateTotalRepayable(request.amountKobo, interestRate)
        val dueDate = LocalDate.now().plusDays(request.repaymentDays.toLong())

        val loan = loanRepository.save(
            Loan(
                borrower = user,
                group = group,
                membership = membership,
                amountRequested = request.amountKobo,
                interestRate = interestRate,
                totalRepayable = totalRepayable,
                collateralType = request.collateralType,
                tier = tier,
                dueDate = dueDate,
                // Tier 1 auto-approved, Tier 2 goes to vote
                status = if (tier == "TIER1") "APPROVED" else "PENDING"
            )
        )

        // Tier 1 — disburse immediately
        if (tier == "TIER1") {
            disburseLoan(loan, user)
        }

        return loan.toResponse(membership.savingsBalance)
    }

    // Disburse loan — transfer money to borrower
    private fun disburseLoan(loan: Loan, user: User) {
        // Create transfer recipient
        // For now log — full bank transfer in production
        // requires Paystack transfer API with OTP verification
        println("💰 Disbursing ${formatKobo(loan.amountRequested)} to ${user.phoneNumber}")

        // Deduct from pool
        val group = loan.group
        group.poolBalance -= loan.amountRequested
        group.totalLoanBook += loan.amountRequested
        groupRepository.save(group)

        // Record transaction
        transactionRepository.save(
            Transaction(
                user = user,
                group = group,
                membership = loan.membership,
                type = "LOAN_DISBURSEMENT",
                amount = loan.amountRequested,
                status = "SUCCESS",
                metadata = """{"loanId":"${loan.id}","tier":"${loan.tier}"}"""
            )
        )

        loan.status = "ACTIVE"
        loan.amountApproved = loan.amountRequested
        loan.updatedAt = LocalDateTime.now()
        loanRepository.save(loan)

        println("✅ Loan disbursed — ${user.phoneNumber}, Amount: ${formatKobo(loan.amountRequested)}")
    }

    // Vote on Tier 2 loan
    @Transactional
    fun voteOnLoan(user: User, loanId: UUID, request: LoanVoteRequest): VoteResponse {
        val loan = loanRepository.findById(loanId).orElseThrow {
            AppException("Loan not found", HttpStatus.NOT_FOUND)
        }

        if (loan.status != "PENDING") {
            throw AppException("This loan is no longer open for voting", HttpStatus.BAD_REQUEST)
        }

        // Voter must be group member
        val membership = membershipRepository.findByUserIdAndGroupId(
            user.id!!, loan.group.id!!
        ) ?: throw AppException("You are not a member of this group", HttpStatus.FORBIDDEN)

        // Borrower cannot vote on own loan
        if (loan.borrower.id == user.id) {
            throw AppException("You cannot vote on your own loan request", HttpStatus.BAD_REQUEST)
        }

        // Check already voted
        if (loanVoteRepository.existsByLoanIdAndVoterId(loanId, user.id!!)) {
            throw AppException("You have already voted on this loan", HttpStatus.BAD_REQUEST)
        }

        // Record vote
        loanVoteRepository.save(
            LoanVote(
                loan = loan,
                voter = user,
                vote = request.vote,
                reason = request.reason
            )
        )

        // Tally votes
        val totalMembers = membershipRepository
            .countByGroupIdAndStatus(loan.group.id!!, "ACTIVE") - 1 // Exclude borrower

        val approveVotes = loanVoteRepository.countByLoanIdAndVote(loanId, "APPROVE")
        val rejectVotes = loanVoteRepository.countByLoanIdAndVote(loanId, "REJECT")
        val totalVotes = approveVotes + rejectVotes

        var votingComplete = false
        var loanStatus = loan.status

        // Majority approve
        if (approveVotes > totalMembers / 2) {
            loan.status = "APPROVED"
            loan.amountApproved = loan.amountRequested
            loanRepository.save(loan)
            disburseLoan(loan, loan.borrower)
            votingComplete = true
            loanStatus = "ACTIVE"
        }

        // Majority reject
        if (rejectVotes > totalMembers / 2) {
            loan.status = "REJECTED"
            loan.rejectionReason = "Rejected by group vote"
            loanRepository.save(loan)
            votingComplete = true
            loanStatus = "REJECTED"
        }

        return VoteResponse(
            loanId = loanId.toString(),
            vote = request.vote,
            totalApprove = approveVotes,
            totalReject = rejectVotes,
            totalVoters = totalMembers,
            votingComplete = votingComplete,
            loanStatus = loanStatus
        )
    }

    // Initiate loan repayment
    @Transactional
    fun initiateRepayment(user: User, request: LoanRepaymentRequest): PaymentInitResponse {
        val loanId = UUID.fromString(request.loanId)
        val loan = loanRepository.findById(loanId).orElseThrow {
            AppException("Loan not found", HttpStatus.NOT_FOUND)
        }

        if (loan.borrower.id != user.id) {
            throw AppException("Unauthorized", HttpStatus.FORBIDDEN)
        }

        if (loan.status != "ACTIVE") {
            throw AppException("Loan is not active", HttpStatus.BAD_REQUEST)
        }

        val remaining = (loan.totalRepayable ?: 0L) - loan.amountRepaid
        if (remaining <= 0) {
            throw AppException("Loan is already fully repaid", HttpStatus.BAD_REQUEST)
        }

        val reference = "KOLO-REPAY-${UUID.randomUUID().toString().replace("-","").take(12).uppercase()}"

        transactionRepository.save(
            Transaction(
                user = user,
                group = loan.group,
                membership = loan.membership,
                type = "LOAN_REPAYMENT",
                amount = remaining,
                paystackReference = reference,
                status = "PENDING",
                metadata = """{"loanId":"${loan.id}","type":"LOAN_REPAYMENT"}"""
            )
        )

        val paystackData = paystackService.initializePayment(
            email = "${user.phoneNumber}@kolo.app",
            amountKobo = remaining,
            reference = reference,
            metadata = mapOf(
                "loanId" to loan.id.toString(),
                "type" to "LOAN_REPAYMENT"
            )
        )

        return PaymentInitResponse(
            authorizationUrl = paystackData.authorizationUrl,
            reference = reference,
            amountKobo = remaining,
            amountFormatted = formatKobo(remaining),
            message = "Complete loan repayment at the authorization URL"
        )
    }

    // Verify loan repayment
    @Transactional
    fun verifyRepayment(user: User, reference: String): LoanResponse {
        val transaction = transactionRepository.findByPaystackReference(reference)
            ?: throw AppException("Transaction not found", HttpStatus.NOT_FOUND)

        if (transaction.status == "SUCCESS") {
            val loanId = extractLoanId(transaction.metadata)
            val loan = loanRepository.findById(loanId).orElseThrow {
                AppException("Loan not found", HttpStatus.NOT_FOUND)
            }
            val membership = loan.membership
            return loan.toResponse(membership.savingsBalance)
        }

        val paystackData = paystackService.verifyTransaction(reference)

        if (paystackData.status == "success") {
            transaction.status = "SUCCESS"
            transaction.paystackStatus = "success"
            transaction.updatedAt = LocalDateTime.now()
            transactionRepository.save(transaction)

            val loanId = extractLoanId(transaction.metadata)
            val loan = loanRepository.findById(loanId).orElseThrow {
                AppException("Loan not found", HttpStatus.NOT_FOUND)
            }

            // Update repayment
            loan.amountRepaid += paystackData.amountKobo
            loan.updatedAt = LocalDateTime.now()

            // Calculate interest distribution
            val interestEarned = (paystackData.amountKobo * loan.interestRate.toDouble()).toLong()
            val platformInterestCut = (interestEarned * PLATFORM_INTEREST_CUT).toLong()
            val poolInterestCut = interestEarned - platformInterestCut

            // Return principal + pool interest to pool
            val group = loan.group
            group.poolBalance += paystackData.amountKobo + poolInterestCut
            group.totalLoanBook -= paystackData.amountKobo

            // Check if fully repaid
            if (loan.amountRepaid >= (loan.totalRepayable ?: 0L)) {
                loan.status = "REPAID"

                // Boost trust score on full repayment
                val borrower = loan.borrower
                borrower.trustScore = minOf(100, borrower.trustScore + 10)
                userRepository.save(borrower)

                println("✅ Loan fully repaid — ${user.phoneNumber}")
            }

            loanRepository.save(loan)
            groupRepository.save(group)

            val membership = loan.membership
            return loan.toResponse(membership.savingsBalance)
        }

        throw AppException("Payment not confirmed by Paystack", HttpStatus.BAD_REQUEST)
    }

    // Get my active loans
    @Transactional(readOnly = true)
    fun getMyLoans(user: User): List<LoanResponse> {
        return loanRepository.findAllByBorrowerIdOrderByCreatedAtDesc(user.id!!)
            .map { loan ->
                val membership = loan.membership
                loan.toResponse(membership.savingsBalance)
            }
    }

    // Get group loan book (admin only)
    @Transactional(readOnly = true)
    fun getGroupLoanBook(user: User, groupId: UUID): List<LoanResponse> {
        val membership = membershipRepository.findByUserIdAndGroupId(user.id!!, groupId)
            ?: throw AppException("You are not a member of this group", HttpStatus.FORBIDDEN)

        if (membership.role != "ADMIN") {
            throw AppException("Only admin can view loan book", HttpStatus.FORBIDDEN)
        }

        return loanRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId)
            .map { loan -> loan.toResponse(membership.savingsBalance) }
    }

    // Calculate total repayable with interest
    private fun calculateTotalRepayable(amountKobo: Long, interestRate: BigDecimal): Long {
        val interest = BigDecimal(amountKobo) * interestRate
        return amountKobo + interest.toLong()
    }

    // Extract loan ID from transaction metadata
    private fun extractLoanId(metadata: String?): UUID {
        val regex = """"loanId":"([^"]+)"""".toRegex()
        val match = regex.find(metadata ?: "")
            ?: throw AppException("Loan ID not found in metadata", HttpStatus.INTERNAL_SERVER_ERROR)
        return UUID.fromString(match.groupValues[1])
    }

    private fun Loan.toResponse(savingsBalance: Long) = LoanResponse(
        id = id.toString(),
        tier = tier,
        amountRequestedKobo = amountRequested,
        amountRequestedFormatted = formatKobo(amountRequested),
        amountApprovedKobo = amountApproved,
        amountApprovedFormatted = amountApproved?.let { formatKobo(it) },
        interestRate = "${interestRate.multiply(BigDecimal("100"))}%",
        totalRepayableKobo = totalRepayable,
        totalRepayableFormatted = totalRepayable?.let { formatKobo(it) },
        amountRepaidKobo = amountRepaid,
        amountRepaidFormatted = formatKobo(amountRepaid),
        remainingKobo = totalRepayable?.minus(amountRepaid),
        remainingFormatted = totalRepayable?.minus(amountRepaid)?.let { formatKobo(it) },
        status = status,
        tier1Eligible = amountRequested <= savingsBalance,
        dueDate = dueDate?.toString(),
        groupName = group.name,
        rejectionReason = rejectionReason,
        createdAt = createdAt.format(DateTimeFormatter.ISO_DATE_TIME)
    )

    private fun formatKobo(kobo: Long): String {
        val naira = kobo / 100.0
        return "₦${String.format("%,.2f", naira)}"
    }
}