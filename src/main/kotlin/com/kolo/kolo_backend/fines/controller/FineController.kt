
package com.kolo.kolo_backend.fines.controller

import com.kolo.kolo_backend.auth.User
import com.kolo.kolo_backend.auth.dto.ApiResponse
import com.kolo.kolo_backend.contributions.dto.PaymentInitResponse
import com.kolo.kolo_backend.fines.dto.*
import com.kolo.kolo_backend.fines.service.FineService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/fines")
class FineController(
    private val fineService: FineService
) {

    @GetMapping("/my-fines")
    fun getMyFines(
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<FinesSummaryResponse>> {
        val result = fineService.getMyFines(user)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Fines retrieved", data = result)
        )
    }

    @PostMapping("/{fineId}/pay")
    fun payFine(
        @PathVariable fineId: UUID,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<PaymentInitResponse>> {
        val result = fineService.initiateFinepayment(user, fineId)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Fine payment initialized", data = result)
        )
    }

    @GetMapping("/verify/{reference}")
    fun verifyFinePayment(
        @PathVariable reference: String,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<FineResponse>> {
        val result = fineService.verifyFinePayment(user, reference)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Fine payment verified", data = result)
        )
    }

    @GetMapping("/group/{groupId}")
    fun getGroupFines(
        @PathVariable groupId: UUID,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<List<FineResponse>>> {
        val result = fineService.getGroupFines(user, groupId)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Group fines retrieved", data = result)
        )
    }

    // DEVELOPMENT ONLY — remove before production
    @PostMapping("/trigger-engine")
    fun triggerFineEngine(
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<String>> {
        fineService.processDailyFines()
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Fine engine triggered")
        )
    }
}