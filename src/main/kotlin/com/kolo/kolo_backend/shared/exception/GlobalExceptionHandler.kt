
package com.kolo.kolo_backend.shared.exception

import com.kolo.kolo_backend.auth.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    // Handle our custom app exceptions
    @ExceptionHandler(AppException::class)
    fun handleAppException(ex: AppException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .status(ex.status)
            .body(ApiResponse(success = false, message = ex.message ?: "An error occurred"))
    }

    // Handle validation errors (@Valid failures)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException
    ): ResponseEntity<ApiResponse<Map<String, String>>> {
        val errors = ex.bindingResult.allErrors.associate { error ->
            val fieldName = (error as FieldError).field
            val message = error.defaultMessage ?: "Invalid value"
            fieldName to message
        }
        return ResponseEntity
            .badRequest()
            .body(ApiResponse(success = false, message = "Validation failed", data = errors))
    }

    // Catch everything else
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        println("Unhandled exception: ${ex.message}")
        return ResponseEntity
            .internalServerError()
            .body(ApiResponse(success = false, message = "Something went wrong. Please try again."))
    }
}