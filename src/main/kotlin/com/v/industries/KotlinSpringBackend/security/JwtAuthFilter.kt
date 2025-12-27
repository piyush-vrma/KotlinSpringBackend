package com.v.industries.KotlinSpringBackend.security

import com.v.industries.KotlinSpringBackend.security.JwtService.Companion.BEARER_PREFIX
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader(AUTH_HEADER_KEY)
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            if (jwtService.validateAccessToken(authHeader)) {
                val userId = jwtService.getUserIdFromToken(authHeader)
                val auth = UsernamePasswordAuthenticationToken(userId, null, emptyList())
                SecurityContextHolder.getContext().authentication = auth
            }
        }

        filterChain.doFilter(request, response)
    }

    companion object {
        private const val AUTH_HEADER_KEY = "Authorization"
    }
}