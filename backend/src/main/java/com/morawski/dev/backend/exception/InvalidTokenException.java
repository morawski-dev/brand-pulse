package com.morawski.dev.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when JWT token is invalid or malformed.
 * Results in HTTP 401 Unauthorized response.
 *
 * Used when:
 * - JWT signature is invalid
 * - JWT format is malformed
 * - JWT claims are missing or invalid
 *
 * Example usage:
 * <pre>
 * try {
 *     Claims claims = jwtTokenProvider.validateToken(token);
 * } catch (JwtException e) {
 *     throw new InvalidTokenException("Invalid JWT token", e);
 * }
 * </pre>
 */
public class InvalidTokenException extends BrandPulseException {

    private static final String ERROR_CODE = "INVALID_TOKEN";
    private static final String DEFAULT_MESSAGE = "Authentication token is invalid";

    /**
     * Constructor with default message.
     */
    public InvalidTokenException() {
        super(ERROR_CODE, DEFAULT_MESSAGE, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Constructor with custom message.
     *
     * @param message Custom error message
     */
    public InvalidTokenException(String message) {
        super(ERROR_CODE, message, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Constructor with custom message and cause.
     *
     * @param message Custom error message
     * @param cause The underlying cause (e.g., JwtException)
     */
    public InvalidTokenException(String message, Throwable cause) {
        super(ERROR_CODE, message, HttpStatus.UNAUTHORIZED, cause);
    }
}
