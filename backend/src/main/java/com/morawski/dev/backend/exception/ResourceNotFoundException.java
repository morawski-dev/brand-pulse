package com.morawski.dev.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found in the database.
 * Results in HTTP 404 Not Found response.
 *
 * Used when:
 * - Brand with given ID doesn't exist
 * - Review source with given ID doesn't exist
 * - Review with given ID doesn't exist
 * - User with given ID doesn't exist
 * - Sync job with given ID doesn't exist
 *
 * Example usage:
 * <pre>
 * Brand brand = brandRepository.findById(brandId)
 *     .orElseThrow(() -> new ResourceNotFoundException("Brand", "id", brandId));
 * </pre>
 */
public class ResourceNotFoundException extends BrandPulseException {

    private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";

    /**
     * Constructor with resource type, field name, and field value.
     *
     * @param resourceType Type of resource (e.g., "Brand", "ReviewSource", "Review")
     * @param fieldName Name of the field used to search (e.g., "id", "email")
     * @param fieldValue Value of the field that was not found
     */
    public ResourceNotFoundException(String resourceType, String fieldName, Object fieldValue) {
        super(
            ERROR_CODE,
            String.format("%s with %s '%s' not found", resourceType, fieldName, fieldValue),
            HttpStatus.NOT_FOUND
        );
        addDetail("resourceType", resourceType);
        addDetail("fieldName", fieldName);
        addDetail("fieldValue", fieldValue);
    }

    /**
     * Constructor with custom message.
     *
     * @param message Custom error message
     */
    public ResourceNotFoundException(String message) {
        super(ERROR_CODE, message, HttpStatus.NOT_FOUND);
    }

    /**
     * Constructor with resource type and ID (most common case).
     *
     * @param resourceType Type of resource (e.g., "Brand", "ReviewSource")
     * @param id Resource ID
     */
    public ResourceNotFoundException(String resourceType, Long id) {
        this(resourceType, "id", id);
    }
}
