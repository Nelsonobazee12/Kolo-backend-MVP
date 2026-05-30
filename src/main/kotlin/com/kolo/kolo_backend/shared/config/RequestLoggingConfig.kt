
package com.kolo.kolo_backend.shared.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.CommonsRequestLoggingFilter

@Configuration
class RequestLoggingConfig {

    @Bean
    fun requestLoggingFilter(): CommonsRequestLoggingFilter {
        val filter = CommonsRequestLoggingFilter()
        filter.setIncludeQueryString(true)
        filter.setIncludePayload(false) // Don't log request body — contains passwords/PINs
        filter.setIncludeHeaders(false) // Don't log headers — contains JWT tokens
        filter.setIncludeClientInfo(true)
        filter.setMaxPayloadLength(1000)
        return filter
    }
}