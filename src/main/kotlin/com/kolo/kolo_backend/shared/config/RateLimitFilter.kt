
package com.kolo.kolo_backend.shared.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitFilter : OncePerRequestFilter() {

    // One bucket per IP address
    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val ip = getClientIp(request)
        val bucket = buckets.computeIfAbsent(ip) { createBucket(request.requestURI) }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response)
        } else {
            response.status = 429
            response.contentType = "application/json"
            response.writer.write("""
                {
                    "success": false,
                    "message": "Too many requests. Please slow down.",
                    "data": null
                }
            """.trimIndent())
        }
    }

    private fun createBucket(uri: String): Bucket {
        // Stricter limits for auth endpoints
        val limit = when {
            uri.contains("/api/auth/register") ||
                    uri.contains("/api/auth/verify-otp") ||
                    uri.contains("/api/auth/login") -> {
                // 5 requests per minute for auth
                Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)))
            }
            else -> {
                // 60 requests per minute for everything else
                Bandwidth.classic(60, Refill.greedy(60, Duration.ofMinutes(1)))
            }
        }
        return Bucket.builder().addLimit(limit).build()
    }

    private fun getClientIp(request: HttpServletRequest): String {
        return request.getHeader("X-Forwarded-For")?.split(",")?.first()?.trim()
            ?: request.getHeader("X-Real-IP")
            ?: request.remoteAddr
    }
}