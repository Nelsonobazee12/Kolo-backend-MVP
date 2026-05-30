
package com.kolo.kolo_backend.groups

import com.kolo.kolo_backend.auth.User
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "memberships")
class Membership(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    val group: Group,

    // Always in kobo
    @Column(name = "contribution_amount", nullable = false)
    var contributionAmount: Long,

    @Column(name = "frequency", nullable = false)
    var frequency: String, // WEEKLY or MONTHLY

    @Column(name = "next_due_date")
    var nextDueDate: LocalDate? = null,

    // Always in kobo
    @Column(name = "savings_balance")
    var savingsBalance: Long = 0L,

    @Column(name = "role")
    var role: String = "MEMBER", // ADMIN or MEMBER

    @Column(name = "grace_period_used")
    var gracePeriodUsed: Boolean = false,

    @Column(name = "status")
    var status: String = "ACTIVE",

    @Column(name = "joined_at")
    val joinedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)