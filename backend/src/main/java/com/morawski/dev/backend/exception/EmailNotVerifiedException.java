package com.morawski.dev.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when user attempts to login but email is not verified.
 * Results in HTTP 403 Forbidden response.
 *
 * Used when:
 * - User tries to login with unverified email (if email verification is enforced)
 * - User tries to access protected resources without email verification
 *
 * Example usage:
 * <pre>
 * if (!user.getEmailVerified()) {
 *     throw new EmailNotVerifiedException(user.getEmail());
 * }
 * </pre>
 */
public class EmailNotVerifiedException extends BrandPulseException {

    private static final String ERROR_CODE = "EMAIL_NOT_VERIFIED";
    private static final String DEFAULT_MESSAGE = "Please verify your email before logging in";

    /**
     * Constructor with default message.
     */
    public EmailNotVerifiedException() {
        super(ERROR_CODE, DEFAULT_MESSAGE, HttpStatus.FORBIDDEN);
        addDetail("verificationRequired", true);
    }

    /**
     * Constructor with email address.
     *
     * @param email Email address that needs verification
     */
    public EmailNotVerifiedException(String email) {
        super(
            ERROR_CODE,
            String.format("Email '%s' must be verified before logging in", email),
            HttpStatus.FORBIDDEN
        );
        addDetail("email", email);
        addDetail("verificationRequired", true);
    }
}
