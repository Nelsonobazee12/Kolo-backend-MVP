
package com.kolo.kolo_backend.auth

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "phone_number", unique = true, nullable = false)
    val phoneNumber: String,

    @Column(name = "full_name")
    var fullName: String? = null,

    @Column(name = "pin_hash")
    var pinHash: String? = null,

    @Column(name = "bvn_hash")
    var bvnHash: String? = null,

    @Column(name = "bvn_verified")
    var bvnVerified: Boolean = false,

    @Column(name = "trust_score")
    var trustScore: Int = 50,

    @Column(name = "fine_count")
    var fineCount: Int = 0,

    @Column(name = "total_cycles_completed")
    var totalCyclesCompleted: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_tier")
    var kycTier: KycTier = KycTier.BRONZE,

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: AccountStatus = AccountStatus.ACTIVE,

    @Column(name = "is_blacklisted")
    var isBlacklisted: Boolean = false,

    @Column(name = "refresh_token_hash")
    var refreshTokenHash: String? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
)

enum class KycTier { BRONZE, SILVER, GOLD, PLATINUM }
enum class AccountStatus { ACTIVE, SUSPENDED, BLACKLISTED }