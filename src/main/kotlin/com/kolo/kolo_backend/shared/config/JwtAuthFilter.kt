
package com.kolo.kolo_backend.shared.config

import com.kolo.kolo_backend.auth.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(7)

        if (!jwtService.validateToken(token)) {
            filterChain.doFilter(request, response)
            return
        }

        // Temp tokens don't grant access to protected routes
        if (jwtService.getTokenType(token) == "temp") {
            filterChain.doFilter(request, response)
            return
        }

        if (jwtService.getTokenType(token) != "access") {
            filterChain.doFilter(request, response)
            return
        }

        val userId = jwtService.getUserIdFromToken(token)
        val user = userRepository.findById(userId).orElse(null)

        if (user != null && !user.isBlacklisted) {
            val authentication = UsernamePasswordAuthenticationToken(
                user,
                null,
                listOf(SimpleGrantedAuthority("ROLE_USER"))
            )
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }
}