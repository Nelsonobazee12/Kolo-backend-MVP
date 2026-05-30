
package com.kolo.kolo_backend.notifications.providers

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class SmsProvider(
    private val webClient: WebClient
) {

    @Value("\${termii.api-key}")
    private lateinit var apiKey: String

    @Value("\${termii.base-url}")
    private lateinit var baseUrl: String

    fun sendSms(phoneNumber: String, message: String): Boolean {
        return try {
            // Termii expects number without + sign
            val formattedNumber = phoneNumber.removePrefix("+")

            webClient.post()
                .uri("$baseUrl/api/sms/send")
                .header("Content-Type", "application/json")
                .bodyValue(mapOf(
                    "to" to formattedNumber,
                    "from" to "N-Alert",  // Default Termii sender ID
                    "sms" to message,
                    "type" to "plain",
                    "api_key" to apiKey,
                    "channel" to "generic"
                ))
                .retrieve()
                .bodyToMono(Map::class.java)
                .block()

            println("✅ SMS sent to $formattedNumber")
            true
        } catch (e: Exception) {
            println("⚠ SMS failed to $phoneNumber — ${e.message}")
            false
        }
    }

    fun sendOtp(phoneNumber: String): String {
        val otp = (100000..999999).random().toString()
        val message = "Your Kolo verification code is $otp. " +
                "Valid for 10 minutes. Do not share this with anyone."
        sendSms(phoneNumber, message)
        return otp
    }
}