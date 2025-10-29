package com.morawski.dev.backend.security;

import com.morawski.dev.backend.dto.common.PlanType;
import com.morawski.dev.backend.exception.ResourceAccessDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Security utility class for accessing authenticated user information.
 * Provides helper methods to retrieve current user details from Spring Security context.
 *
 * Usage in Services:
 * - Get current user ID for ownership validation
 * - Get current user plan type for feature access control
 * - Get current user email for logging/auditing
 *
 * API Plan Section 12.2: Authorization Rules
 * - Resource Ownership Validation
 * - User can only access resources they own
 */
@Slf4j
public final class SecurityUtils {

    private SecurityUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Get current authenticated user ID from SecurityContext.
     *
     * @return User ID
     * @throws ResourceAccessDeniedException if no authenticated user found
     */
    public static Long getCurrentUserId() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails.getId();
    }

    /**
     * Get current authenticated user email from SecurityContext.
     *
     * @return User email
     * @throws ResourceAccessDeniedException if no authenticated user found
     */
    public static String getCurrentUserEmail() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails.getEmail();
    }

    /**
     * Get current authenticated user's plan type from SecurityContext.
     *
     * @return PlanType (FREE, PREMIUM, etc.)
     * @throws ResourceAccessDeniedException if no authenticated user found
     */
    public static PlanType getCurrentUserPlanType() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails.getPlanType();
    }

    /**
     * Get current authenticated user's max sources allowed from SecurityContext.
     *
     * @return Maximum sources allowed for user's plan
     * @throws ResourceAccessDeniedException if no authenticated user found
     */
    public static Integer getCurrentUserMaxSourcesAllowed() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails.getMaxSourcesAllowed();
    }

    /**
     * Get current authenticated user details from SecurityContext.
     *
     * @return CustomUserDetails
     * @throws ResourceAccessDeniedException if no authenticated user found
     */
    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authenticated user found in SecurityContext");
            throw new ResourceAccessDeniedException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails)) {
            log.error("Principal is not an instance of CustomUserDetails: {}",
                principal.getClass().getName());
            throw new ResourceAccessDeniedException("Invalid authentication principal");
        }

        return (CustomUserDetails) principal;
    }

    /**
     * Check if there is an authenticated user in the current SecurityContext.
     *
     * @return true if user is authenticated
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
            && authentication.isAuthenticated()
            && authentication.getPrincipal() instanceof CustomUserDetails;
    }

    /**
     * Validate that the current user owns the specified user ID.
     * Used for authorization checks before accessing user-specific resources.
     *
     * API Plan Section 12.2: Resource Ownership Validation
     * - User can only access resources where resource.user_id = JWT.userId
     *
     * @param userId User ID to validate ownership
     * @throws ResourceAccessDeniedException if current user doesn't match the specified user ID
     */
    public static void validateUserOwnership(Long userId) {
        Long currentUserId = getCurrentUserId();

        if (!currentUserId.equals(userId)) {
            log.warn("User {} attempted to access resources of user {}",
                currentUserId, userId);
            throw new ResourceAccessDeniedException(
                "Access denied. You do not have permission to access this resource."
            );
        }
    }

    /**
     * Check if current user is on FREE plan.
     *
     * @return true if user is on FREE plan
     */
    public static boolean isFreePlan() {
        return PlanType.FREE.equals(getCurrentUserPlanType());
    }

    /**
     * Check if current user is on PREMIUM plan.
     * MVP supports only FREE and PREMIUM plans.
     *
     * @return true if user is on PREMIUM plan
     */
    public static boolean isPremiumPlan() {
        return PlanType.PREMIUM.equals(getCurrentUserPlanType());
    }

    /**
     * Get Spring Security Authentication object from SecurityContext.
     * Use this for advanced security operations.
     *
     * @return Authentication object or null if not authenticated
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
