
package com.kolo.kolo_backend.loans.dto

import jakarta.validation.constraints.*

// REQUESTS

data class LoanRequest(
    @field:NotBlank(message = "Group ID is required")
    val groupId: String,

    @field:NotNull(message = "Amount is required")
    @field:Min(value = 100000, message = "Minimum loan is ₦1,000")
    val amountKobo: Long,

    @field:NotBlank(message = "Repayment period is required")
    @field:Pattern(
        regexp = "^(30|60|90)$",
        message = "Repayment period must be 30, 60, or 90 days"
    )
    val repaymentDays: String,

    // Required for Tier 2 only
    val collateralType: String? = null,
    val guarantorPhone: String? = null
)

data class LoanVoteRequest(
    @field:NotBlank(message = "Vote is required")
    @field:Pattern(
        regexp = "^(APPROVE|REJECT)$",
        message = "Vote must be APPROVE or REJECT"
    )
    val vote: String,

    val reason: String? = null
)

data class LoanRepaymentRequest(
    @field:NotBlank(message = "Loan ID is required")
    val loanId: String
)

// RESPONSES

data class LoanResponse(
    val id: String,
    val tier: String,
    val amountRequestedKobo: Long,
    val amountRequestedFormatted: String,
    val amountApprovedKobo: Long?,
    val amountApprovedFormatted: String?,
    val interestRate: String,
    val totalRepayableKobo: Long?,
    val totalRepayableFormatted: String?,
    val amountRepaidKobo: Long,
    val amountRepaidFormatted: String,
    val remainingKobo: Long?,
    val remainingFormatted: String?,
    val status: String,
    val tier1Eligible: Boolean,
    val dueDate: String?,
    val groupName: String,
    val rejectionReason: String?,
    val createdAt: String
)

data class LoanEligibilityResponse(
    val tier1EligibleKobo: Long,
    val tier1EligibleFormatted: String,
    val tier2EligibleKobo: Long,
    val tier2EligibleFormatted: String,
    val hasActiveLoan: Boolean,
    val trustScore: Int,
    val pendingFinesKobo: Long,
    val pendingFinesFormatted: String,
    val canBorrow: Boolean,
    val reason: String?
)

data class VoteResponse(
    val loanId: String,
    val vote: String,
    val totalApprove: Long,
    val totalReject: Long,
    val totalVoters: Long,
    val votingComplete: Boolean,
    val loanStatus: String
)