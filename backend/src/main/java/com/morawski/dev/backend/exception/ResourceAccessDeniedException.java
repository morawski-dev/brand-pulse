package com.morawski.dev.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when user attempts to access a resource they don't own.
 * Results in HTTP 403 Forbidden response.
 *
 * Used when:
 * - User tries to access another user's brand
 * - User tries to access another user's review source
 * - User tries to access another user's reviews
 * - User tries to modify resources they don't own
 *
 * Example usage:
 * <pre>
 * Brand brand = brandRepository.findById(brandId).orElseThrow(...);
 * if (!brand.getUser().getId().equals(currentUserId)) {
 *     throw new ResourceAccessDeniedException("Brand", brandId);
 * }
 * </pre>
 *
 * Note: This is different from Spring Security's AccessDeniedException,
 * which is used for permission/role-based access control.
 */
public class ResourceAccessDeniedException extends BrandPulseException {

    private static final String ERROR_CODE = "RESOURCE_ACCESS_DENIED";
    private static final String DEFAULT_MESSAGE = "You don't have permission to access this resource";

    /**
     * Constructor with default message.
     */
    public ResourceAccessDeniedException() {
        super(ERROR_CODE, DEFAULT_MESSAGE, HttpStatus.FORBIDDEN);
    }

    /**
     * Constructor with custom message.
     *
     * @param message Custom error message
     */
    public ResourceAccessDeniedException(String message) {
        super(ERROR_CODE, message, HttpStatus.FORBIDDEN);
    }

    /**
     * Constructor with resource type and ID.
     *
     * @param resourceType Type of resource (e.g., "Brand", "ReviewSource")
     * @param resourceId Resource ID that user tried to access
     */
    public ResourceAccessDeniedException(String resourceType, Long resourceId) {
        super(
            ERROR_CODE,
            String.format("You don't have permission to access %s with ID %d", resourceType, resourceId),
            HttpStatus.FORBIDDEN
        );
        addDetail("resourceType", resourceType);
        addDetail("resourceId", resourceId);
    }
}