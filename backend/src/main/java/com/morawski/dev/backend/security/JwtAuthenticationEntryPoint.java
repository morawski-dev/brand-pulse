package com.morawski.dev.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morawski.dev.backend.util.Constants;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Authentication Entry Point for Spring Security.
 * Handles authentication failures and returns standardized 401 Unauthorized responses.
 *
 * This component is triggered when:
 * - No JWT token is provided for a protected endpoint
 * - JWT token is invalid or expired
 * - JWT token signature verification fails
 *
 * API Plan Section 14.1: Standard Error Response Format
 * Returns consistent error structure with:
 * - code: Error code (e.g., "INVALID_TOKEN")
 * - message: Human-readable error message
 * - timestamp: ISO 8601 timestamp
 * - path: Request path
 *
 * API Plan Section 14.2: HTTP Status Codes
 * - 401 Unauthorized: Missing/invalid/expired token
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Commence an authentication scheme.
     * Called when user attempts to access a protected resource without valid authentication.
     *
     * @param request HTTP request that resulted in an AuthenticationException
     * @param response HTTP response to send to the client
     * @param authException the exception that caused the invocation
     * @throws IOException if I/O error occurs
     * @throws ServletException if servlet error occurs
     */
    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException, ServletException {

        log.warn("Unauthorized access attempt to: {} {} - {}",
            request.getMethod(),
            request.getRequestURI(),
            authException.getMessage()
        );

        // Set response status to 401 Unauthorized
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Build error response according to API Plan Section 14.1
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", determineErrorCode(request));
        errorResponse.put("message", determineErrorMessage(authException));
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("path", request.getRequestURI());

        // Write JSON response
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    /**
     * Determine appropriate error code based on request context.
     *
     * @param request HTTP request
     * @return Error code string
     */
    private String determineErrorCode(HttpServletRequest request) {
        String authHeader = request.getHeader(Constants.HEADER_AUTHORIZATION);

        if (authHeader == null || authHeader.isBlank()) {
            // No authorization header provided
            return Constants.ERROR_INVALID_TOKEN;
        }

        if (!authHeader.startsWith(Constants.HEADER_BEARER_PREFIX)) {
            // Invalid header format
            return Constants.ERROR_INVALID_TOKEN;
        }

        // Token present but invalid/expired
        // Could be either INVALID_TOKEN or TOKEN_EXPIRED
        // For security reasons, we don't differentiate in the response
        return Constants.ERROR_INVALID_TOKEN;
    }

    /**
     * Determine appropriate error message based on authentication exception.
     *
     * API Plan Section 14.3: Common Error Codes
     * - INVALID_TOKEN: JWT invalid/expired
     *
     * @param authException the authentication exception
     * @return Human-readable error message
     */
    private String determineErrorMessage(AuthenticationException authException) {
        // Generic message for security
        // Don't reveal whether token is expired vs. invalid vs. missing
        return "Authentication required. Please provide a valid JWT token in the Authorization header.";
    }
}
