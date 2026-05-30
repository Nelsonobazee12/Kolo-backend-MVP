
package com.kolo.kolo_backend.loans.repository

import com.kolo.kolo_backend.loans.LoanVote
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LoanVoteRepository : JpaRepository<LoanVote, UUID> {

    fun existsByLoanIdAndVoterId(loanId: UUID, voterId: UUID): Boolean

    fun findAllByLoanId(loanId: UUID): List<LoanVote>

    fun countByLoanIdAndVote(loanId: UUID, vote: String): Long
}