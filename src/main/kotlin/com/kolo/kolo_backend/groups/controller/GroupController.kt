
package com.kolo.kolo_backend.groups.controller

import com.kolo.kolo_backend.auth.User
import com.kolo.kolo_backend.auth.dto.ApiResponse
import com.kolo.kolo_backend.groups.dto.*
import com.kolo.kolo_backend.groups.service.GroupService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/groups")
class GroupController(
    private val groupService: GroupService
) {

    @PostMapping
    fun createGroup(
        @Valid @RequestBody request: CreateGroupRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<GroupResponse>> {
        val result = groupService.createGroup(user, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse(success = true, message = "Group created successfully", data = result)
        )
    }

    @GetMapping
    fun getMyGroups(
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<List<GroupResponse>>> {
        val result = groupService.getMyGroups(user)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Groups retrieved", data = result)
        )
    }

    @GetMapping("/{groupId}")
    fun getGroupDetail(
        @PathVariable groupId: UUID,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<GroupDetailResponse>> {
        val result = groupService.getGroupDetail(user, groupId)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Group retrieved", data = result)
        )
    }

    @PostMapping("/{groupId}/invite")
    fun inviteMember(
        @PathVariable groupId: UUID,
        @Valid @RequestBody request: InviteMemberRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<InvitationResponse>> {
        val result = groupService.inviteMember(user, groupId, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Invitation sent successfully", data = result)
        )
    }

    @PostMapping("/join")
    fun joinGroup(
        @Valid @RequestBody request: JoinGroupRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<MembershipResponse>> {
        val result = groupService.joinGroup(user, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Joined group successfully", data = result)
        )
    }
}