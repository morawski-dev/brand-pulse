package com.morawski.dev.backend.repository;

import com.morawski.dev.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository interface for User entity.
 * Provides data access methods for user authentication, registration, and profile management.
 *
 * User Stories:
 * - US-001: New User Registration
 * - US-002: System Login
 *
 * API Endpoints:
 * - POST /api/auth/register
 * - POST /api/auth/login
 * - POST /api/auth/forgot-password
 * - POST /api/auth/reset-password
 * - GET /api/users/me
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email address (case-insensitive).
     * Used for login and registration duplicate checks.
     *
     * @param email the user's email address
     * @return Optional containing user if found
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Check if email already exists in the system (case-insensitive).
     * Used during registration to prevent duplicate accounts.
     *
     * @param email the email to check
     * @return true if email exists
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Find user by email verification token.
     * Used during email verification flow.
     *
     * @param token the verification token
     * @return Optional containing user if found
     */
    Optional<User> findByVerificationToken(String token);

    /**
     * Find user by password reset token.
     * Used during password reset flow.
     *
     * @param token the password reset token
     * @return Optional containing user if found
     */
    Optional<User> findByPasswordResetToken(String token);

    /**
     * Find user by valid password reset token (not expired).
     * Combines token lookup with expiration check.
     *
     * API: POST /api/auth/reset-password
     *
     * @param token the password reset token
     * @param now current timestamp
     * @return Optional containing user if token is valid
     */
    @Query("SELECT u FROM User u WHERE u.passwordResetToken = :token " +
           "AND u.passwordResetExpiresAt > :now AND u.deletedAt IS NULL")
    Optional<User> findByValidPasswordResetToken(
            @Param("token") String token,
            @Param("now") Instant now
    );

    /**
     * Count active users (not deleted).
     * Used for analytics and metrics.
     *
     * @return count of active users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL")
    long countActiveUsers();

    /**
     * Find users with verified emails.
     * Used for email campaign targeting.
     *
     * @return list of verified users
     */
    @Query("SELECT u FROM User u WHERE u.emailVerified = true AND u.deletedAt IS NULL")
    java.util.List<User> findVerifiedUsers();

    /**
     * Find users by plan type.
     * Used for plan analytics and upgrade targeting.
     *
     * @param planType the plan type (FREE or PREMIUM)
     * @return list of users with specified plan
     */
    @Query("SELECT u FROM User u WHERE u.planType = :planType AND u.deletedAt IS NULL")
    java.util.List<User> findByPlanType(@Param("planType") com.morawski.dev.backend.dto.common.PlanType planType);
}
