
package com.kolo.kolo_backend.contributions

import com.kolo.kolo_backend.auth.User
import com.kolo.kolo_backend.groups.Group
import com.kolo.kolo_backend.groups.Membership
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "transactions")
class Transaction(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    val group: Group,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id")
    val membership: Membership? = null,

    // CONTRIBUTION, WITHDRAWAL, LOAN_DISBURSEMENT,
    // LOAN_REPAYMENT, FINE
    @Column(name = "type", nullable = false)
    val type: String,

    // Always in kobo
    @Column(name = "amount", nullable = false)
    val amount: Long,

    @Column(name = "paystack_reference")
    var paystackReference: String? = null,

    @Column(name = "paystack_status")
    var paystackStatus: String? = null,

    // PENDING, SUCCESS, FAILED
    @Column(name = "status")
    var status: String = "PENDING",

    @Column(name = "metadata")
    var metadata: String? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)