
package com.kolo.kolo_backend.fines.dto

data class FineResponse(
    val id: String,
    val amountKobo: Long,
    val amountFormatted: String,
    val reason: String?,
    val daysLate: Int,
    val status: String,
    val groupName: String,
    val createdAt: String
)

data class PayFineRequest(
    val fineId: String
)

data class FinesSummaryResponse(
    val totalPendingKobo: Long,
    val totalPendingFormatted: String,
    val fines: List<FineResponse>
)