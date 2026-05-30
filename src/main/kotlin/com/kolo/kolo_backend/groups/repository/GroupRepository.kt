
package com.kolo.kolo_backend.groups.repository

import com.kolo.kolo_backend.groups.Group
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GroupRepository : JpaRepository<Group, UUID> {

    @Query("""
        SELECT g FROM Group g 
        JOIN Membership m ON m.group.id = g.id 
        WHERE m.user.id = :userId 
        AND m.status = 'ACTIVE'
        AND g.deletedAt IS NULL
    """)
    fun findAllByMemberId(userId: UUID): List<Group>
}