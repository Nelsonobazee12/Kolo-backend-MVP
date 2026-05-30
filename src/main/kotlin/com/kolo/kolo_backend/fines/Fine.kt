
package com.kolo.kolo_backend.fines

import com.kolo.kolo_backend.auth.User
import com.kolo.kolo_backend.groups.Group
import com.kolo.kolo_backend.groups.Membership
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "fines")
class Fine(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false)
    val membership: Membership,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    val group: Group,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    // Always in kobo
    @Column(name = "amount", nullable = false)
    val amount: Long,

    @Column(name = "reason")
    val reason: String? = null,

    @Column(name = "days_late")
    val daysLate: Int = 0,

    // 30% goes to platform
    @Column(name = "platform_cut")
    val platformCut: Long = 0L,

    // 70% goes back to pool
    @Column(name = "pool_cut")
    val poolCut: Long = 0L,

    // PENDING, PAID, WAIVED
    @Column(name = "status")
    var status: String = "PENDING",

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)