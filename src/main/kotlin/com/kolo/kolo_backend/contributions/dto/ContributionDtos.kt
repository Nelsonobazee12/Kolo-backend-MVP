
package com.kolo.kolo_backend.contributions.dto

import jakarta.validation.constraints.*

// REQUESTS

data class InitiateContributionRequest(
    @field:NotNull(message = "Group ID is required")
    val groupId: String,
)

data class WithdrawalRequest(
    @field:NotNull(message = "Group ID is required")
    val groupId: String,

    @field:NotNull(message = "Amount is required")
    @field:Min(value = 100000, message = "Minimum withdrawal is ₦1,000")
    val amountKobo: Long,

    @field:NotBlank(message = "Bank code is required")
    val bankCode: String,

    @field:NotBlank(message = "Account number is required")
    val accountNumber: String
)

// RESPONSES

data class PaymentInitResponse(
    val authorizationUrl: String,
    val reference: String,
    val amountKobo: Long,
    val amountFormatted: String,
    val message: String
)

data class TransactionResponse(
    val id: String,
    val type: String,
    val amountKobo: Long,
    val amountFormatted: String,
    val status: String,
    val reference: String?,
    val groupName: String,
    val createdAt: String
)

data class ContributionHistoryResponse(
    val transactions: List<TransactionResponse>,
    val totalContributedKobo: Long,
    val totalContributedFormatted: String
)