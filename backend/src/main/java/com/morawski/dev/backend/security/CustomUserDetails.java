package com.morawski.dev.backend.security;

import com.morawski.dev.backend.dto.common.PlanType;
import com.morawski.dev.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom implementation of Spring Security UserDetails interface.
 * Wraps the User entity and provides authentication and authorization details.
 *
 * This class is used by Spring Security to:
 * - Authenticate users during login
 * - Authorize access to protected resources
 * - Store user information in the security context
 *
 * API Plan Section 12.1: JWT Token Structure
 * - Contains user ID, email, planType, and maxSourcesAllowed
 * - Used for stateless authentication with JWT tokens
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private Long id;
    private String email;
    private String passwordHash;
    private PlanType planType;
    private Integer maxSourcesAllowed;
    private Boolean emailVerified;
    private Boolean accountDeleted;

    /**
     * Create CustomUserDetails from User entity.
     *
     * @param user User entity from database
     * @return CustomUserDetails instance
     */
    public static CustomUserDetails fromUser(User user) {
        return new CustomUserDetails(
            user.getId(),
            user.getEmail(),
            user.getPasswordHash(),
            user.getPlanType(),
            user.getMaxSourcesAllowed(),
            user.getEmailVerified(),
            user.isDeleted()
        );
    }

    /**
     * Returns the authorities granted to the user.
     * MVP version: All users have ROLE_USER.
     * Future: Could differentiate between FREE, PREMIUM, ADMIN, etc.
     *
     * @return Collection of granted authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // MVP: All authenticated users have ROLE_USER
        // Future enhancement: Could add ROLE_PREMIUM, ROLE_ADMIN based on planType
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * Returns the password used to authenticate the user.
     *
     * @return BCrypt hashed password
     */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    /**
     * Returns the username used to authenticate the user.
     * In BrandPulse, the email is used as the username.
     *
     * @return User email
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Indicates whether the user's account has expired.
     * In BrandPulse MVP, accounts don't expire.
     *
     * @return true (accounts never expire)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is locked or unlocked.
     * In BrandPulse MVP, we don't implement account locking.
     * Future: Could implement after N failed login attempts.
     *
     * @return true if account is not deleted
     */
    @Override
    public boolean isAccountNonLocked() {
        return !accountDeleted;
    }

    /**
     * Indicates whether the user's credentials (password) has expired.
     * In BrandPulse MVP, credentials don't expire.
     * Future: Could force password change after X days.
     *
     * @return true (credentials never expire)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled or disabled.
     * In BrandPulse, a user is enabled if:
     * - Account is not soft-deleted
     * - Email is verified (optional based on configuration)
     *
     * API Plan Section 3.2: Email verification may be required for login
     *
     * @return true if user is enabled
     */
    @Override
    public boolean isEnabled() {
        // User is enabled if account is not deleted
        // Note: Email verification check happens in authentication logic
        // to return specific error code (ERROR_EMAIL_NOT_VERIFIED)
        return !accountDeleted;
    }

    /**
     * Get user ID.
     * Used for JWT token generation and authorization checks.
     *
     * @return User ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Get user email.
     *
     * @return User email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Get user's plan type.
     * Used for plan limit enforcement (API Plan Section 13.2).
     *
     * @return PlanType (FREE, PREMIUM, etc.)
     */
    public PlanType getPlanType() {
        return planType;
    }

    /**
     * Get maximum sources allowed for user's plan.
     * Used for freemium gate enforcement (US-009).
     *
     * @return Maximum sources allowed
     */
    public Integer getMaxSourcesAllowed() {
        return maxSourcesAllowed;
    }

    /**
     * Check if user's email is verified.
     *
     * @return true if email is verified
     */
    public Boolean isEmailVerified() {
        return emailVerified;
    }
}
