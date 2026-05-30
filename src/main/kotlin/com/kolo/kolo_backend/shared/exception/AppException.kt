// src/main/kotlin/com/kolo/shared/exception/AppException.kt
package com.kolo.kolo_backend.shared.exception

import org.springframework.http.HttpStatus

class AppException(
    message: String,
    val status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR
) : RuntimeException(message)