
package com.kolo.kolo_backend.shared.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Configuration
class WebClientConfig {

    @Bean
    fun webClient(): WebClient {
        return WebClient.builder()
            .codecs { it.defaultCodecs().maxInMemorySize(2 * 1024 * 1024) }
            .filter(loggingFilter())
            .build()
    }

    private fun loggingFilter(): ExchangeFilterFunction {
        return ExchangeFilterFunction.ofRequestProcessor { request ->
            println("→ ${request.method()} ${request.url()}")
            Mono.just(request)
        }
    }
}