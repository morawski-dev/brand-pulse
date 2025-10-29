package com.morawski.dev.backend.security;

import com.morawski.dev.backend.exception.InvalidTokenException;
import com.morawski.dev.backend.exception.TokenExpiredException;
import com.morawski.dev.backend.util.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter for Spring Security.
 * Intercepts HTTP requests and validates JWT tokens from the Authorization header.
 *
 * Filter Flow:
 * 1. Extract JWT token from "Authorization: Bearer {token}" header
 * 2. Validate token signature and expiration
 * 3. Extract user ID from token
 * 4. Load user details from database
 * 5. Create Authentication object and set in SecurityContext
 *
 * API Plan Section 12.1: Authentication Mechanism
 * - JWT Bearer authentication
 * - Token format: "Authorization: Bearer {token}"
 * - Stateless (no server-side session storage)
 *
 * API Plan Section 12.2: Authorization Rules
 * - All endpoints except public ones require valid JWT token
 * - Return 401 Unauthorized if token missing/invalid/expired
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Filter each HTTP request to validate JWT token.
     * Runs once per request before Spring Security authorization checks.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain filter chain for continuing request processing
     * @throws ServletException if servlet error occurs
     * @throws IOException if I/O error occurs
     */
    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Extract JWT token from Authorization header
            String authHeader = request.getHeader(Constants.HEADER_AUTHORIZATION);

            // Skip authentication if no Authorization header present
            // Public endpoints will be allowed by SecurityConfig
            if (authHeader == null || authHeader.isBlank()) {
                log.debug("No Authorization header found for request: {} {}",
                    request.getMethod(), request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // Extract token from "Bearer {token}" format
            String token;
            try {
                token = jwtTokenProvider.extractTokenFromHeader(authHeader);
            } catch (InvalidTokenException e) {
                log.warn("Invalid Authorization header format: {}", e.getMessage());
                filterChain.doFilter(request, response);
                return;
            }

            // Validate token signature and expiration
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("Invalid or expired JWT token for request: {} {}",
                    request.getMethod(), request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // Extract user ID from token
            Long userId;
            try {
                userId = jwtTokenProvider.extractUserId(token);
            } catch (InvalidTokenException | TokenExpiredException e) {
                log.error("Failed to extract user ID from token: {}", e.getMessage());
                filterChain.doFilter(request, response);
                return;
            }

            // Check if user is already authenticated in current request
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("User already authenticated in SecurityContext");
                filterChain.doFilter(request, response);
                return;
            }

            // Load user details from database
            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserById(userId);
            } catch (UsernameNotFoundException e) {
                log.warn("User not found for ID from token: {}", userId);
                filterChain.doFilter(request, response);
                return;
            }

            // Create authentication object
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null, // credentials not needed (already authenticated via JWT)
                    userDetails.getAuthorities()
                );

            // Set additional details (IP address, session ID, etc.)
            authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
            );

            // Set authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT authentication successful for user ID: {} ({})",
                userId, userDetails.getUsername());

        } catch (Exception e) {
            // Log unexpected errors but don't block the request
            // Let Spring Security handle authorization failures
            log.error("Unexpected error during JWT authentication: {}", e.getMessage(), e);
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Determine if this filter should be applied to the current request.
     * Filter is applied to all requests by default.
     * Public endpoints are handled by Spring Security configuration.
     *
     * @param request HTTP request
     * @return true if filter should be applied
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Apply filter to all requests
        // Public endpoints (auth, health, actuator) are permitted in SecurityConfig
        return false;
    }
}
