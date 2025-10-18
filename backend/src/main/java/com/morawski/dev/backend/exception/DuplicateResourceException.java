package com.morawski.dev.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to create a resource that already exists.
 * Results in HTTP 409 Conflict response.
 *
 * Used when:
 * - Email already exists during registration
 * - Review source with same brand+sourceType+externalProfileId already configured
 * - Duplicate review import (same review_source_id + external_review_id)
 *
 * Example usage:
 * <pre>
 * if (userRepository.existsByEmail(email)) {
 *     throw new DuplicateResourceException("User", "email", email);
 * }
 * </pre>
 */
public class DuplicateResourceException extends BrandPulseException {

    private static final String ERROR_CODE_EMAIL = "EMAIL_ALREADY_EXISTS";
    private static final String ERROR_CODE_SOURCE = "DUPLICATE_SOURCE";
    private static final String ERROR_CODE_GENERIC = "DUPLICATE_RESOURCE";

    /**
     * Factory method for duplicate email.
     *
     * @param email Email address that already exists
     */
    public static DuplicateResourceException forEmail(String email) {
        DuplicateResourceException ex = new DuplicateResourceException(
            ERROR_CODE_EMAIL,
            "An account with this email already exists"
        );
        ex.addDetail("email", email);
        return ex;
    }

    /**
     * Constructor for duplicate review source.
     *
     * @param brandId Brand ID
     * @param sourceType Source type (GOOGLE, FACEBOOK, TRUSTPILOT)
     * @param externalProfileId External profile ID
     */
    public static DuplicateResourceException forReviewSource(
        Long brandId,
        String sourceType,
        String externalProfileId
    ) {
        DuplicateResourceException ex = new DuplicateResourceException(
            ERROR_CODE_SOURCE,
            "This review source is already configured for your brand"
        );
        ex.addDetail("brandId", brandId);
        ex.addDetail("sourceType", sourceType);
        ex.addDetail("externalProfileId", externalProfileId);
        return ex;
    }

    /**
     * Generic constructor with resource type, field name, and field value.
     *
     * @param resourceType Type of resource (e.g., "User", "ReviewSource")
     * @param fieldName Name of the field that caused conflict
     * @param fieldValue Value that already exists
     */
    public DuplicateResourceException(String resourceType, String fieldName, Object fieldValue) {
        super(
            ERROR_CODE_GENERIC,
            String.format("%s with %s '%s' already exists", resourceType, fieldName, fieldValue),
            HttpStatus.CONFLICT
        );
        addDetail("resourceType", resourceType);
        addDetail("fieldName", fieldName);
        addDetail("fieldValue", fieldValue);
    }

    /**
     * Private constructor with error code and message.
     *
     * @param errorCode Specific error code
     * @param message Error message
     */
    private DuplicateResourceException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }
}
