
package com.kolo.kolo_backend.groups

import com.kolo.kolo_backend.auth.User
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "invitations")
class Invitation(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    val group: Group,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    val invitedBy: User,

    @Column(name = "phone_number", nullable = false)
    val phoneNumber: String,

    @Column(name = "token", unique = true, nullable = false)
    val token: String,

    @Column(name = "status")
    var status: String = "PENDING", // PENDING, ACCEPTED, EXPIRED

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)