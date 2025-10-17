package com.morawski.dev.backend.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.morawski.dev.backend.dto.common.PlanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Response DTO for user profile information.
 * Maps to users table fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {

    /**
     * User ID from users.id
     */
    private Long userId;

    /**
     * User email from users.email
     */
    private String email;

    /**
     * Subscription plan type from users.plan_type
     */
    private PlanType planType;

    /**
     * Maximum allowed review sources from users.max_sources_allowed
     */
    private Integer maxSourcesAllowed;

    /**
     * Email verification status from users.email_verified
     */
    private Boolean emailVerified;

    /**
     * Account creation timestamp from users.created_at
     */
    private ZonedDateTime createdAt;

    /**
     * Last update timestamp from users.updated_at
     */
    private ZonedDateTime updatedAt;
}
