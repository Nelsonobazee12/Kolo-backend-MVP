package com.kolo.kolo_backend.payments.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.kolo.kolo_backend.shared.exception.AppException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.get

@Service
class PaystackService(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper
) {

    @Value("\${paystack.secret-key}")
    private lateinit var secretKey: String

    @Value("\${paystack.base-url}")
    private lateinit var baseUrl: String

    // Initialize a payment — returns checkout URL
    fun initializePayment(
        email: String,
        amountKobo: Long,
        reference: String,
        metadata: Map<String, Any> = emptyMap()
    ): PaystackInitData {
        val body = mapOf(
            "email" to email,
            "amount" to amountKobo,
            "reference" to reference,
            "metadata" to metadata,
            "callback_url" to "https://kolo.app/payment/callback"
        )

        val response = webClient.post()
            .uri("$baseUrl/transaction/initialize")
            .header("Authorization", "Bearer $secretKey")
            .header("Content-Type", "application/json")
            .bodyValue(body)
            .retrieve()
            .onStatus({ it.isError }) { res ->
                res.bodyToMono<String>().flatMap { errorBody ->
                    Mono.error(AppException(
                        "Paystack error: $errorBody",
                        HttpStatus.BAD_GATEWAY
                    ))
                }
            }
            .bodyToMono(Map::class.java)
            .block()
            ?: throw AppException("No response from Paystack", HttpStatus.BAD_GATEWAY)

        val data = response["data"] as? Map<*, *>
            ?: throw AppException("Invalid Paystack response", HttpStatus.BAD_GATEWAY)

        return PaystackInitData(
            authorizationUrl = data["authorization_url"] as String,
            accessCode = data["access_code"] as String,
            reference = data["reference"] as String
        )
    }

    // Verify a transaction by reference
    fun verifyTransaction(reference: String): PaystackVerifyData {
        val response = webClient.get()
            .uri("$baseUrl/transaction/verify/$reference")
            .header("Authorization", "Bearer $secretKey")
            .retrieve()
            .onStatus({ it.isError }) { res ->
                res.bodyToMono<String>().flatMap { errorBody ->
                    Mono.error(AppException(
                        "Paystack verification error: $errorBody",
                        HttpStatus.BAD_GATEWAY
                    ))
                }
            }
            .bodyToMono(Map::class.java)
            .block()
            ?: throw AppException("No response from Paystack", HttpStatus.BAD_GATEWAY)

        val data = response["data"] as? Map<*, *>
            ?: throw AppException("Invalid Paystack response", HttpStatus.BAD_GATEWAY)

        val customerMap = data["customer"] as? Map<*, *>
        val metadataMap = data["metadata"] as? Map<*, *>

        return PaystackVerifyData(
            reference = data["reference"] as String,
            status = data["status"] as String,
            amountKobo = (data["amount"] as Number).toLong(),
            email = customerMap?.get("email") as? String ?: "",
            metadata = metadataMap ?: emptyMap<String, Any>()
        )
    }

    // Create transfer recipient (for withdrawals)
    fun createTransferRecipient(
        accountName: String,
        accountNumber: String,
        bankCode: String
    ): String {
        val body = mapOf(
            "type" to "nuban",
            "name" to accountName,
            "account_number" to accountNumber,
            "bank_code" to bankCode,
            "currency" to "NGN"
        )

        val response = webClient.post()
            .uri("$baseUrl/transferrecipient")
            .header("Authorization", "Bearer $secretKey")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map::class.java)
            .block()
            ?: throw AppException("No response from Paystack", HttpStatus.BAD_GATEWAY)

        val data = response["data"] as? Map<*, *>
            ?: throw AppException("Invalid Paystack response", HttpStatus.BAD_GATEWAY)

        return data["recipient_code"] as String
    }

    // Initiate transfer (for withdrawals)
    fun initiateTransfer(
        amountKobo: Long,
        recipientCode: String,
        reason: String,
        reference: String
    ): String {
        val body = mapOf(
            "source" to "balance",
            "amount" to amountKobo,
            "recipient" to recipientCode,
            "reason" to reason,
            "reference" to reference
        )

        val response = webClient.post()
            .uri("$baseUrl/transfer")
            .header("Authorization", "Bearer $secretKey")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map::class.java)
            .block()
            ?: throw AppException("No response from Paystack", HttpStatus.BAD_GATEWAY)

        val data = response["data"] as? Map<*, *>
            ?: throw AppException("Invalid Paystack response", HttpStatus.BAD_GATEWAY)

        return data["transfer_code"] as String
    }

    // Verify webhook signature
    fun verifyWebhookSignature(payload: String, signature: String): Boolean {
        val hmac = Mac.getInstance("HmacSHA512")
        val secretKeySpec = SecretKeySpec(
            secretKey.toByteArray(),
            "HmacSHA512"
        )
        hmac.init(secretKeySpec)
        val computedSignature = hmac.doFinal(payload.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return computedSignature == signature
    }
}

// Paystack response data classes
data class PaystackInitData(
    val authorizationUrl: String,
    val accessCode: String,
    val reference: String
)

data class PaystackVerifyData(
    val reference: String,
    val status: String,
    val amountKobo: Long,
    val email: String,
    val metadata: Map<*, *>
)