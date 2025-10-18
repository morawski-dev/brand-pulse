package com.morawski.dev.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when user attempts to exceed their plan limits.
 * Results in HTTP 403 Forbidden response.
 *
 * Used when:
 * - Free plan user tries to add 2nd review source (US-009)
 * - User tries to create 2nd brand (MVP limitation)
 * - Future: User exceeds API rate limits per plan
 *
 * Example usage:
 * <pre>
 * int activeSourceCount = reviewSourceRepository.countByBrandIdAndDeletedAtIsNull(brandId);
 * if (activeSourceCount >= user.getMaxSourcesAllowed()) {
 *     throw new PlanLimitExceededException(
 *         "review sources",
 *         activeSourceCount,
 *         user.getMaxSourcesAllowed(),
 *         user.getPlanType().toString()
 *     );
 * }
 * </pre>
 */
public class PlanLimitExceededException extends BrandPulseException {

    private static final String ERROR_CODE = "PLAN_LIMIT_EXCEEDED";

    /**
     * Constructor with resource type, current count, max allowed, and plan type.
     *
     * @param resourceType Type of resource (e.g., "review sources", "brands")
     * @param currentCount Current count of resources
     * @param maxAllowed Maximum allowed by plan
     * @param planType Plan type (e.g., "FREE", "PREMIUM")
     */
    public PlanLimitExceededException(
        String resourceType,
        int currentCount,
        int maxAllowed,
        String planType
    ) {
        super(
            ERROR_CODE,
            String.format(
                "%s plan allows %d %s. Paid plans coming soon.",
                planType,
                maxAllowed,
                resourceType
            ),
            HttpStatus.FORBIDDEN
        );
        addDetail("resourceType", resourceType);
        addDetail("currentCount", currentCount);
        addDetail("maxAllowed", maxAllowed);
        addDetail("planType", planType);
    }
}
