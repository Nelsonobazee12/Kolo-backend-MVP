package com.kolo.kolo_backend.contributions.repository

import com.kolo.kolo_backend.contributions.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TransactionRepository : JpaRepository<Transaction, UUID> {

    fun findByPaystackReference(reference: String): Transaction?

    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<Transaction>

    fun findAllByGroupIdOrderByCreatedAtDesc(groupId: UUID): List<Transaction>

    fun findAllByGroupIdAndTypeOrderByCreatedAtDesc(
        groupId: UUID,
        type: String
    ): List<Transaction>

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) 
        FROM Transaction t 
        WHERE t.group.id = :groupId 
        AND t.type = 'CONTRIBUTION' 
        AND t.status = 'SUCCESS'
    """)
    fun sumSuccessfulContributions(groupId: UUID): Long
}