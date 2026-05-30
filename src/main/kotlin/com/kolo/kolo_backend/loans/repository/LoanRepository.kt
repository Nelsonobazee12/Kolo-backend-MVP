
package com.kolo.kolo_backend.loans.repository

import com.kolo.kolo_backend.loans.Loan
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LoanRepository : JpaRepository<Loan, UUID> {

    fun findAllByBorrowerIdOrderByCreatedAtDesc(borrowerId: UUID): List<Loan>

    fun findAllByGroupIdOrderByCreatedAtDesc(groupId: UUID): List<Loan>

    fun findAllByBorrowerIdAndStatus(borrowerId: UUID, status: String): List<Loan>

    fun existsByBorrowerIdAndStatusIn(
        borrowerId: UUID,
        statuses: List<String>
    ): Boolean

    @Query("""
        SELECT COALESCE(SUM(l.amountApproved), 0)
        FROM Loan l
        WHERE l.group.id = :groupId
        AND l.status IN ('ACTIVE', 'APPROVED')
    """)
    fun sumActiveLoansByGroup(groupId: UUID): Long
}