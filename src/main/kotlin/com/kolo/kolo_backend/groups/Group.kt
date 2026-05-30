
package com.kolo.kolo_backend.groups

import com.kolo.kolo_backend.auth.User
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "groups")
class Group(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "description")
    var description: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    val createdBy: User,

    @Column(name = "paystack_virtual_account_number")
    var paystackVirtualAccountNumber: String? = null,

    @Column(name = "paystack_bank_name")
    var paystackBankName: String? = null,

    // Always in kobo
    @Column(name = "pool_balance")
    var poolBalance: Long = 0L,

    @Column(name = "total_loan_book")
    var totalLoanBook: Long = 0L,

    @Column(name = "max_members")
    var maxMembers: Int = 20,

    @Column(name = "is_premium")
    var isPremium: Boolean = false,

    @Column(name = "status")
    var status: String = "ACTIVE",

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
)