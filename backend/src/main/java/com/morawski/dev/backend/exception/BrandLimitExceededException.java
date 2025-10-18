package com.morawski.dev.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when user attempts to create more than one brand.
 * Results in HTTP 409 Conflict response.
 *
 * MVP Limitation: One brand per user account.
 * This restriction may be lifted in future versions.
 *
 * Example usage:
 * <pre>
 * if (brandRepository.existsByUserIdAndDeletedAtIsNull(userId)) {
 *     throw new BrandLimitExceededException();
 * }
 * </pre>
 */
public class BrandLimitExceededException extends BrandPulseException {

    private static final String ERROR_CODE = "BRAND_LIMIT_EXCEEDED";
    private static final String DEFAULT_MESSAGE = "MVP supports one brand per user account";

    /**
     * Constructor with default message.
     */
    public BrandLimitExceededException() {
        super(ERROR_CODE, DEFAULT_MESSAGE, HttpStatus.CONFLICT);
        addDetail("maxBrandsAllowed", 1);
        addDetail("mvpLimitation", true);
    }

    /**
     * Constructor with custom message.
     *
     * @param message Custom error message
     */
    public BrandLimitExceededException(String message) {
        super(ERROR_CODE, message, HttpStatus.CONFLICT);
        addDetail("maxBrandsAllowed", 1);
        addDetail("mvpLimitation", true);
    }
}
