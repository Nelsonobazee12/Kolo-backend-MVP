
package com.kolo.kolo_backend.payments.controller

import com.kolo.kolo_backend.contributions.service.ContributionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/payments")
class WebhookController(
    private val contributionService: ContributionService
) {

    @PostMapping("/webhook")
    fun handleWebhook(
        @RequestBody payload: String,
        @RequestHeader("x-paystack-signature") signature: String
    ): ResponseEntity<String> {
        contributionService.handleWebhook(payload, signature)
        return ResponseEntity.ok("OK")
    }
}