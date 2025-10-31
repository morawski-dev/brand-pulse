package com.morawski.dev.backend.service;

import com.morawski.dev.backend.dto.common.PlanType;
import com.morawski.dev.backend.dto.user.UpdateUserProfileRequest;
import com.morawski.dev.backend.dto.user.UserProfileResponse;
import com.morawski.dev.backend.entity.User;
import com.morawski.dev.backend.exception.DuplicateResourceException;
import com.morawski.dev.backend.exception.ResourceNotFoundException;
import com.morawski.dev.backend.exception.ValidationException;
import com.morawski.dev.backend.mapper.UserMapper;
import com.morawski.dev.backend.repository.UserRepository;
import com.morawski.dev.backend.util.StringUtils;
import com.morawski.dev.backend.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for user profile management and account operations.
 * Handles user CRUD operations, email verification, and password reset flows.
 *
 * API Endpoints:
 * - GET /api/users/me (Section 4.1)
 * - PATCH /api/users/me (Section 4.2)
 *
 * Business Logic:
 * - Email change requires re-verification (set email_verified=false)
 * - MVP: One brand per user account
 * - Free plan: max_sources_allowed=1, plan_type='FREE'
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Get current user profile by user ID.
     * API: GET /api/users/me
     *
     * @param userId User ID from JWT token
     * @return UserProfileResponse containing user details
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(Long userId) {
        log.debug("Getting profile for user ID: {}", userId);

        User user = findByIdOrThrow(userId);
        UserProfileResponse response = userMapper.toUserProfileResponse(user);

        log.info("Retrieved profile for user: {}", StringUtils.maskEmail(user.getEmail()));
        return response;
    }

    /**
     * Update user profile (currently only email can be updated).
     * API: PATCH /api/users/me
     *
     * Business Logic:
     * - If email changed, set email_verified=false
     * - Email must be unique in system
     * - Triggers email verification flow (handled by caller)
     *
     * @param userId User ID from JWT token
     * @param request Update request containing new email
     * @return Updated UserProfileResponse
     * @throws ResourceNotFoundException if user not found
     * @throws DuplicateResourceException if new email already exists
     * @throws ValidationException if email is invalid
     */
    @Transactional
    public UserProfileResponse updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        log.debug("Updating profile for user ID: {}", userId);

        User user = findByIdOrThrow(userId);
        String oldEmail = user.getEmail();

        // Validate and update email if provided
        if (request.getEmail() != null) {
            String newEmail = request.getEmail().trim();

            // Validate email format
            if (!StringUtils.isValidEmail(newEmail)) {
                throw new ValidationException("Invalid email format");
            }

            // Check if email changed
            if (!newEmail.equalsIgnoreCase(oldEmail)) {
                // Check if new email already exists
                if (userRepository.existsByEmailIgnoreCase(newEmail)) {
                    throw DuplicateResourceException.forEmail(newEmail);
                }

                // Update email and reset verification status
                user.setEmail(newEmail);
                user.setEmailVerified(false);

                log.info("Email changed for user {}: {} -> {}",
                    userId,
                    StringUtils.maskEmail(oldEmail),
                    StringUtils.maskEmail(newEmail)
                );

                // TODO: Trigger email verification flow (will be handled by caller/event)
            }
        }

        User savedUser = userRepository.save(user);
        UserProfileResponse response = userMapper.toUserProfileResponse(savedUser);

        log.info("Updated profile for user: {}", StringUtils.maskEmail(savedUser.getEmail()));
        return response;
    }

    // ========== Helper Methods (used by other services) ==========

    /**
     * Find user by ID.
     *
     * @param userId User ID
     * @return Optional containing user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Find user by ID or throw exception.
     *
     * @param userId User ID
     * @return User entity
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User findByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    /**
     * Find user by email (case-insensitive).
     *
     * @param email User email address
     * @return Optional containing user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(email.trim());
    }

    /**
     * Check if email already exists in the system.
     *
     * @param email Email address to check
     * @return true if email exists
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return false;
        }
        return userRepository.existsByEmailIgnoreCase(email.trim());
    }

    /**
     * Save user entity.
     *
     * @param user User to save
     * @return Saved user entity
     */
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Create a new user with FREE plan.
     * Used during registration (US-001).
     *
     * Business Logic (API Plan Section 3.1):
     * - Set plan_type='FREE', max_sources_allowed=1
     * - Email not verified initially
     *
     * @param email User email address
     * @param passwordHash BCrypt hashed password
     * @return Created user entity
     * @throws DuplicateResourceException if email already exists
     */
    @Transactional
    public User createUser(String email, String passwordHash) {
        log.debug("Creating new user with email: {}", StringUtils.maskEmail(email));

        // Validate email uniqueness
        if (existsByEmail(email)) {
            throw DuplicateResourceException.forEmail(email);
        }

        // Create user with FREE plan defaults
        User user = User.builder()
            .email(email.trim().toLowerCase())
            .passwordHash(passwordHash)
            .planType(PlanType.FREE)
            .maxSourcesAllowed(1)  // MVP: Free plan allows 1 source
            .emailVerified(false)
            .build();

        User savedUser = userRepository.save(user);
        log.info("Created new user: {} (ID: {})", StringUtils.maskEmail(savedUser.getEmail()), savedUser.getId());

        return savedUser;
    }

    /**
     * Update user password.
     * Used during password reset flow (US-003.4).
     *
     * @param userId User ID
     * @param newPasswordHash BCrypt hashed new password
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional
    public void updatePassword(Long userId, String newPasswordHash) {
        log.debug("Updating password for user ID: {}", userId);

        User user = findByIdOrThrow(userId);
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);

        log.info("Password updated for user: {}", StringUtils.maskEmail(user.getEmail()));
    }

    /**
     * Set email verification status.
     *
     * @param userId User ID
     * @param verified Verification status
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional
    public void setEmailVerified(Long userId, boolean verified) {
        log.debug("Setting email verification status for user ID: {} to {}", userId, verified);

        User user = findByIdOrThrow(userId);
        user.setEmailVerified(verified);

        if (verified) {
            user.setVerificationToken(null);  // Clear verification token
        }

        userRepository.save(user);
        log.info("Email verification status updated for user: {}", StringUtils.maskEmail(user.getEmail()));
    }

    /**
     * Generate and set verification token for email verification.
     *
     * @param userId User ID
     * @return Generated verification token
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional
    public String generateVerificationToken(Long userId) {
        log.debug("Generating verification token for user ID: {}", userId);

        User user = findByIdOrThrow(userId);
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        userRepository.save(user);

        log.info("Verification token generated for user: {}", StringUtils.maskEmail(user.getEmail()));
        return token;
    }

    /**
     * Find user by verification token.
     *
     * @param token Verification token
     * @return Optional containing user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findByVerificationToken(String token) {
        if (StringUtils.isBlank(token)) {
            return Optional.empty();
        }
        return userRepository.findByVerificationToken(token);
    }

    /**
     * Set password reset token and expiration.
     * API: POST /api/auth/forgot-password (Section 3.3)
     *
     * Business Logic:
     * - Token expires in 1 hour
     * - Token is UUID
     *
     * @param userId User ID
     * @param token Password reset token (UUID)
     * @param expiresAt Token expiration timestamp
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional
    public void setPasswordResetToken(Long userId, String token, Instant expiresAt) {
        log.debug("Setting password reset token for user ID: {}", userId);

        User user = findByIdOrThrow(userId);
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiresAt(expiresAt);
        userRepository.save(user);

        log.info("Password reset token set for user: {}", StringUtils.maskEmail(user.getEmail()));
    }

    /**
     * Clear password reset token after successful password reset.
     *
     * @param userId User ID
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional
    public void clearPasswordResetToken(Long userId) {
        log.debug("Clearing password reset token for user ID: {}", userId);

        User user = findByIdOrThrow(userId);
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);

        log.info("Password reset token cleared for user: {}", StringUtils.maskEmail(user.getEmail()));
    }

    /**
     * Find user by password reset token (must not be expired).
     * API: POST /api/auth/reset-password (Section 3.4)
     *
     * @param token Password reset token
     * @return Optional containing user if token is valid
     */
    @Transactional(readOnly = true)
    public Optional<User> findByValidPasswordResetToken(String token) {
        if (StringUtils.isBlank(token)) {
            return Optional.empty();
        }
        return userRepository.findByValidPasswordResetToken(token, Instant.now());
    }

    /**
     * Soft delete user account.
     * Sets deleted_at timestamp, making account inaccessible.
     *
     * @param userId User ID
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional
    public void softDeleteUser(Long userId) {
        log.debug("Soft deleting user ID: {}", userId);

        User user = findByIdOrThrow(userId);
        user.softDelete();
        userRepository.save(user);

        log.warn("User account deleted: {}", StringUtils.maskEmail(user.getEmail()));
    }

    /**
     * Upgrade user plan type.
     * Future feature for premium plans.
     *
     * @param userId User ID
     * @param planType New plan type
     * @param maxSourcesAllowed Maximum sources allowed for new plan
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional
    public void upgradePlan(Long userId, PlanType planType, Integer maxSourcesAllowed) {
        log.debug("Upgrading plan for user ID: {} to {}", userId, planType);

        if (!ValidationUtils.isPositive(maxSourcesAllowed)) {
            throw new ValidationException("Max sources allowed must be positive");
        }

        User user = findByIdOrThrow(userId);
        user.setPlanType(planType);
        user.setMaxSourcesAllowed(maxSourcesAllowed);
        userRepository.save(user);

        log.info("Plan upgraded for user {}: {} (max sources: {})",
            StringUtils.maskEmail(user.getEmail()),
            planType,
            maxSourcesAllowed
        );
    }

    /**
     * Get user's plan details (type and max sources allowed).
     *
     * @param userId User ID
     * @return Array containing [planType, maxSourcesAllowed]
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public Object[] getUserPlanDetails(Long userId) {
        User user = findByIdOrThrow(userId);
        return new Object[]{user.getPlanType(), user.getMaxSourcesAllowed()};
    }

    /**
     * Count active users (not deleted).
     * Used for analytics and metrics.
     *
     * @return Count of active users
     */
    @Transactional(readOnly = true)
    public long countActiveUsers() {
        return userRepository.countActiveUsers();
    }
}
