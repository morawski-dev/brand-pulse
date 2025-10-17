package com.morawski.dev.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.morawski.dev.backend.dto.common.PlanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Response DTO for successful authentication (register/login).
 * Contains user details and JWT token.
 * Maps to users table + JWT payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

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
     * JWT access token (60-minute expiration)
     */
    private String token;

    /**
     * Token expiration timestamp
     */
    private ZonedDateTime expiresAt;

    /**
     * Account creation timestamp from users.created_at (included only on registration)
     */
    private ZonedDateTime createdAt;
}
