
package com.kolo.kolo_backend.groups.service

import com.kolo.kolo_backend.auth.User
import com.kolo.kolo_backend.groups.Group
import com.kolo.kolo_backend.groups.Invitation
import com.kolo.kolo_backend.groups.Membership
import com.kolo.kolo_backend.groups.dto.*
import com.kolo.kolo_backend.groups.repository.GroupRepository
import com.kolo.kolo_backend.groups.repository.InvitationRepository
import com.kolo.kolo_backend.groups.repository.MembershipRepository
import com.kolo.kolo_backend.shared.exception.AppException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val membershipRepository: MembershipRepository,
    private val invitationRepository: InvitationRepository
) {

    // Create a new group
    @Transactional
    fun createGroup(user: User, request: CreateGroupRequest): GroupResponse {
        val group = groupRepository.save(
            Group(
                name = request.name,
                description = request.description,
                createdBy = user,
                maxMembers = request.maxMembers
            )
        )

        // Calculate first due date
        val nextDueDate = when (request.frequency) {
            "WEEKLY" -> LocalDate.now().plusWeeks(1)
            "MONTHLY" -> LocalDate.now().plusMonths(1)
            else -> LocalDate.now().plusMonths(1)
        }

        // Admin membership with real contribution amount
        membershipRepository.save(
            Membership(
                user = user,
                group = group,
                contributionAmount = request.contributionAmountKobo,
                frequency = request.frequency,
                nextDueDate = nextDueDate,
                role = "ADMIN",
                status = "ACTIVE"
            )
        )

        return group.toResponse(1L)
    }

    // Get group details
    @Transactional(readOnly = true)
    fun getGroupDetail(user: User, groupId: UUID): GroupDetailResponse {
        val group = findGroupOrThrow(groupId)
        val members = membershipRepository.findAllByGroupIdAndStatus(groupId, "ACTIVE")
        val myMembership = membershipRepository.findByUserIdAndGroupId(user.id!!, groupId)

        return GroupDetailResponse(
            group = group.toResponse(members.size.toLong()),
            members = members.map { it.toResponse() },
            myMembership = myMembership?.toResponse()
        )
    }

    // Get all groups for current user
    @Transactional(readOnly = true)
    fun getMyGroups(user: User): List<GroupResponse> {
        return groupRepository.findAllByMemberId(user.id!!).map { group ->
            val memberCount = membershipRepository.countByGroupIdAndStatus(group.id!!, "ACTIVE")
            group.toResponse(memberCount)
        }
    }

    // Invite a member
    @Transactional
    fun inviteMember(user: User, groupId: UUID, request: InviteMemberRequest): InvitationResponse {
        val group = findGroupOrThrow(groupId)

        // Only admin can invite
        val membership = membershipRepository.findByUserIdAndGroupId(user.id!!, groupId)
            ?: throw AppException("You are not a member of this group", HttpStatus.FORBIDDEN)

        if (membership.role != "ADMIN") {
            throw AppException("Only group admin can invite members", HttpStatus.FORBIDDEN)
        }

        // Check member limit
        val currentCount = membershipRepository.countByGroupIdAndStatus(groupId, "ACTIVE")
        if (currentCount >= group.maxMembers) {
            throw AppException(
                "Group is full. Maximum ${group.maxMembers} members allowed.",
                HttpStatus.BAD_REQUEST
            )
        }

        val normalizedPhone = normalizePhone(request.phoneNumber)

        // Check if already a member
        // Check existing pending invitation
        val existingInvite = invitationRepository.findByPhoneNumberAndGroupIdAndStatus(
            normalizedPhone, groupId, "PENDING"
        )
        if (existingInvite != null) {
            throw AppException(
                "An invitation has already been sent to this number.",
                HttpStatus.BAD_REQUEST
            )
        }

        val token = UUID.randomUUID().toString().replace("-", "")

        val invitation = invitationRepository.save(
            Invitation(
                group = group,
                invitedBy = user,
                phoneNumber = normalizedPhone,
                token = token,
                expiresAt = LocalDateTime.now().plusDays(7)
            )
        )

        // TODO: Send SMS via Termii with invite link
        println("📨 Invitation for $normalizedPhone — Token: $token")

        return InvitationResponse(
            invitationToken = token,
            groupName = group.name,
            inviteLink = "https://kolo.app/join/$token",
            expiresAt = invitation.expiresAt.format(DateTimeFormatter.ISO_DATE_TIME)
        )
    }

    // Join group via invitation token
    @Transactional
    fun joinGroup(user: User, request: JoinGroupRequest): MembershipResponse {
        val invitation = invitationRepository.findByToken(request.invitationToken)
            ?: throw AppException("Invalid invitation link", HttpStatus.BAD_REQUEST)

        if (invitation.status != "PENDING") {
            throw AppException("This invitation has already been used", HttpStatus.BAD_REQUEST)
        }

        if (invitation.expiresAt.isBefore(LocalDateTime.now())) {
            invitation.status = "EXPIRED"
            invitationRepository.save(invitation)
            throw AppException("This invitation has expired", HttpStatus.BAD_REQUEST)
        }

        // Verify phone matches
        val normalizedPhone = normalizePhone(user.phoneNumber)
        if (invitation.phoneNumber != normalizedPhone) {
            throw AppException(
                "This invitation was sent to a different phone number",
                HttpStatus.FORBIDDEN
            )
        }

        val group = invitation.group

        // Check not already a member
        if (membershipRepository.existsByUserIdAndGroupId(user.id!!, group.id!!)) {
            throw AppException("You are already a member of this group", HttpStatus.BAD_REQUEST)
        }

        // Calculate first due date
        val nextDueDate = when (request.frequency) {
            "WEEKLY" -> LocalDate.now().plusWeeks(1)
            "MONTHLY" -> LocalDate.now().plusMonths(1)
            else -> LocalDate.now().plusMonths(1)
        }

        val membership = membershipRepository.save(
            Membership(
                user = user,
                group = group,
                contributionAmount = request.contributionAmountKobo,
                frequency = request.frequency,
                nextDueDate = nextDueDate,
                role = "MEMBER"
            )
        )

        // Mark invitation as accepted
        invitation.status = "ACCEPTED"
        invitationRepository.save(invitation)

        return membership.toResponse()
    }

    // Helper — find group or throw
    private fun findGroupOrThrow(groupId: UUID): Group {
        return groupRepository.findById(groupId).orElseThrow {
            AppException("Group not found", HttpStatus.NOT_FOUND)
        }
    }

    // Normalize Nigerian phone numbers
    private fun normalizePhone(phone: String): String {
        return when {
            phone.startsWith("+234") -> phone
            phone.startsWith("0") -> "+234${phone.substring(1)}"
            else -> "+234$phone"
        }
    }

    // Extension functions — entity to response
    private fun Group.toResponse(memberCount: Long) = GroupResponse(
        id = id.toString(),
        name = name,
        description = description,
        poolBalanceKobo = poolBalance,
        poolBalanceFormatted = formatKobo(poolBalance),
        totalMembers = memberCount,
        maxMembers = maxMembers,
        isPremium = isPremium,
        status = status,
        createdBy = createdBy.fullName ?: createdBy.phoneNumber,
        virtualAccountNumber = paystackVirtualAccountNumber,
        bankName = paystackBankName,
        createdAt = createdAt.format(DateTimeFormatter.ISO_DATE_TIME)
    )

    private fun Membership.toResponse() = MembershipResponse(
        id = id.toString(),
        userId = user.id.toString(),
        fullName = user.fullName,
        phoneNumber = user.phoneNumber,
        contributionAmountKobo = contributionAmount,
        contributionAmountFormatted = formatKobo(contributionAmount),
        frequency = frequency,
        savingsBalanceKobo = savingsBalance,
        savingsBalanceFormatted = formatKobo(savingsBalance),
        nextDueDate = nextDueDate?.toString(),
        role = role,
        status = status,
        joinedAt = joinedAt.format(DateTimeFormatter.ISO_DATE_TIME)
    )

    // Format kobo to naira string
    private fun formatKobo(kobo: Long): String {
        val naira = kobo / 100.0
        return "₦${String.format("%,.2f", naira)}"
    }
}