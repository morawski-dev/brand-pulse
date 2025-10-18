package com.morawski.dev.backend.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Exception thrown when JWT token has expired.
 * Results in HTTP 401 Unauthorized response.
 *
 * Used when:
 * - JWT exp claim is before current time
 * - Password reset token has exceeded its validity period
 *
 * Example usage:
 * <pre>
 * if (claims.getExpiration().before(new Date())) {
 *     throw new TokenExpiredException(claims.getExpiration().toInstant());
 * }
 * </pre>
 */
public class TokenExpiredException extends BrandPulseException {

    private static final String ERROR_CODE = "TOKEN_EXPIRED";
    private static final String DEFAULT_MESSAGE = "Authentication token has expired";

    /**
     * Constructor with default message.
     */
    public TokenExpiredException() {
        super(ERROR_CODE, DEFAULT_MESSAGE, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Constructor with expiration time.
     *
     * @param expiredAt When the token expired
     */
    public TokenExpiredException(Instant expiredAt) {
        super(ERROR_CODE, "Authentication token expired at " + expiredAt, HttpStatus.UNAUTHORIZED);
        addDetail("expiredAt", expiredAt);
    }

    /**
     * Constructor with custom message.
     *
     * @param message Custom error message
     */
    public TokenExpiredException(String message) {
        super(ERROR_CODE, message, HttpStatus.UNAUTHORIZED);
    }
}
