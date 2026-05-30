
package com.kolo.kolo_backend.groups.dto

import jakarta.validation.constraints.*

// REQUESTS

data class CreateGroupRequest(
    @field:NotBlank(message = "Group name is required")
    @field:Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    val name: String,

    @field:Size(max = 255, message = "Description too long")
    val description: String? = null,

    @field:Max(value = 100, message = "Maximum 100 members allowed")
    @field:Min(value = 2, message = "Minimum 2 members required")
    val maxMembers: Int = 20,

    @field:NotNull(message = "Contribution amount is required")
    @field:Min(value = 100000, message = "Minimum contribution is ₦1,000")
    val contributionAmountKobo: Long,

    @field:NotBlank(message = "Frequency is required")
    @field:Pattern(
        regexp = "^(WEEKLY|MONTHLY)$",
        message = "Frequency must be WEEKLY or MONTHLY"
    )
    val frequency: String
)

data class JoinGroupRequest(
    @field:NotBlank(message = "Invitation token is required")
    val invitationToken: String,

    @field:NotNull(message = "Contribution amount is required")
    @field:Min(value = 100000, message = "Minimum contribution is ₦1,000")
    val contributionAmountKobo: Long,

    @field:NotBlank(message = "Frequency is required")
    @field:Pattern(
        regexp = "^(WEEKLY|MONTHLY)$",
        message = "Frequency must be WEEKLY or MONTHLY"
    )
    val frequency: String
)

data class InviteMemberRequest(
    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(
        regexp = "^(\\+234|0)[789][01]\\d{8}$",
        message = "Enter a valid Nigerian phone number"
    )
    val phoneNumber: String
)

// RESPONSES

data class GroupResponse(
    val id: String,
    val name: String,
    val description: String?,
    val poolBalanceKobo: Long,
    val poolBalanceFormatted: String,
    val totalMembers: Long,
    val maxMembers: Int,
    val isPremium: Boolean,
    val status: String,
    val createdBy: String,
    val virtualAccountNumber: String?,
    val bankName: String?,
    val createdAt: String
)

data class MembershipResponse(
    val id: String,
    val userId: String,
    val fullName: String?,
    val phoneNumber: String,
    val contributionAmountKobo: Long,
    val contributionAmountFormatted: String,
    val frequency: String,
    val savingsBalanceKobo: Long,
    val savingsBalanceFormatted: String,
    val nextDueDate: String?,
    val role: String,
    val status: String,
    val joinedAt: String
)

data class GroupDetailResponse(
    val group: GroupResponse,
    val members: List<MembershipResponse>,
    val myMembership: MembershipResponse?
)

data class InvitationResponse(
    val invitationToken: String,
    val groupName: String,
    val inviteLink: String,
    val expiresAt: String
)