
package com.kolo.kolo_backend.groups.repository

import com.kolo.kolo_backend.groups.Membership
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MembershipRepository : JpaRepository<Membership, UUID> {
    fun findByUserIdAndGroupId(userId: UUID, groupId: UUID): Membership?
    fun findAllByGroupId(groupId: UUID): List<Membership>
    fun findAllByGroupIdAndStatus(groupId: UUID, status: String): List<Membership>
    fun existsByUserIdAndGroupId(userId: UUID, groupId: UUID): Boolean
    fun countByGroupIdAndStatus(groupId: UUID, status: String): Long
    fun findAllByStatus(status: String): List<Membership>
}