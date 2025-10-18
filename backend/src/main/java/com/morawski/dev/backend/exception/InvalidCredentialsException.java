package com.morawski.dev.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when user provides invalid login credentials.
 * Results in HTTP 401 Unauthorized response.
 *
 * Used during login when:
 * - Email doesn't exist in the system
 * - Password doesn't match the stored hash
 *
 * Security note: Don't reveal whether email or password was incorrect
 * to prevent user enumeration attacks.
 *
 * Example usage:
 * <pre>
 * if (!passwordEncoder.matches(password, user.getPasswordHash())) {
 *     throw new InvalidCredentialsException();
 * }
 * </pre>
 */
public class InvalidCredentialsException extends BrandPulseException {

    private static final String ERROR_CODE = "INVALID_CREDENTIALS";
    private static final String DEFAULT_MESSAGE = "Invalid email or password";

    /**
     * Constructor with default message.
     */
    public InvalidCredentialsException() {
        super(ERROR_CODE, DEFAULT_MESSAGE, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Constructor with custom message.
     *
     * @param message Custom error message
     */
    public InvalidCredentialsException(String message) {
        super(ERROR_CODE, message, HttpStatus.UNAUTHORIZED);
    }
}
