
package com.kolo.kolo_backend.fines.repository

import com.kolo.kolo_backend.fines.Fine
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FineRepository : JpaRepository<Fine, UUID> {

    fun findAllByUserIdAndStatus(userId: UUID, status: String): List<Fine>

    fun findAllByGroupIdOrderByCreatedAtDesc(groupId: UUID): List<Fine>

    fun findAllByMembershipIdAndStatus(membershipId: UUID, status: String): List<Fine>

    @Query("""
        SELECT COALESCE(SUM(f.amount), 0) 
        FROM Fine f 
        WHERE f.user.id = :userId 
        AND f.status = 'PENDING'
    """)
    fun sumPendingFinesByUser(userId: UUID): Long

    @Query("""
        SELECT COALESCE(SUM(f.amount), 0)
        FROM Fine f
        WHERE f.group.id = :groupId
        AND f.status = 'PAID'
    """)
    fun sumPaidFinesByGroup(groupId: UUID): Long
}