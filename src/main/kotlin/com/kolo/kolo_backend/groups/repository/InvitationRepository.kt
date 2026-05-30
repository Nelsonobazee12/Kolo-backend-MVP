
package com.kolo.kolo_backend.groups.repository

import com.kolo.kolo_backend.groups.Invitation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface InvitationRepository : JpaRepository<Invitation, UUID> {
    fun findByToken(token: String): Invitation?
    fun findByPhoneNumberAndGroupIdAndStatus(
        phoneNumber: String,
        groupId: UUID,
        status: String
    ): Invitation?
}