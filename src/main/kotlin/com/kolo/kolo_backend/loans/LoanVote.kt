
package com.kolo.kolo_backend.loans

import com.kolo.kolo_backend.auth.User
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "loan_votes")
class LoanVote(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    val loan: Loan,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    val voter: User,

    // APPROVE or REJECT
    @Column(name = "vote", nullable = false)
    val vote: String,

    @Column(name = "reason")
    val reason: String? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)