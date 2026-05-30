// src/main/kotlin/com/kolo/loans/Loan.kt
package com.kolo.kolo_backend.loans

import com.kolo.kolo_backend.auth.User
import com.kolo.kolo_backend.groups.Group
import com.kolo.kolo_backend.groups.Membership
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "loans")
class Loan(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    val borrower: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    val group: Group,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false)
    val membership: Membership,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guarantor_id")
    var guarantor: User? = null,

    // Always in kobo
    @Column(name = "amount_requested", nullable = false)
    val amountRequested: Long,

    @Column(name = "amount_approved")
    var amountApproved: Long? = null,

    @Column(name = "interest_rate", nullable = false)
    val interestRate: BigDecimal,

    @Column(name = "total_repayable")
    var totalRepayable: Long? = null,

    @Column(name = "amount_repaid")
    var amountRepaid: Long = 0L,

    @Column(name = "collateral_type")
    var collateralType: String? = null,

    @Column(name = "collateral_document_url")
    var collateralDocumentUrl: String? = null,

    // TIER1 or TIER2
    @Column(name = "tier", nullable = false)
    val tier: String,

    // PENDING, APPROVED, ACTIVE, REPAID, DEFAULTED, REJECTED
    @Column(name = "status")
    var status: String = "PENDING",

    @Column(name = "rejection_reason")
    var rejectionReason: String? = null,

    @Column(name = "due_date")
    var dueDate: LocalDate? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)