package com.morawski.dev.backend.service;

import com.morawski.dev.backend.dto.auth.*;
import com.morawski.dev.backend.dto.common.PlanType;
import com.morawski.dev.backend.entity.User;
import com.morawski.dev.backend.exception.*;
import com.morawski.dev.backend.mapper.UserMapper;
import com.morawski.dev.backend.security.JwtTokenProvider;
import com.morawski.dev.backend.util.Constants;
import com.morawski.dev.backend.util.StringUtils;
import com.morawski.dev.backend.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for authentication and authorization operations.
 * Handles user registration, login, password reset, and JWT token management.
 *
 * API Endpoints (Section 3):
 * - POST /api/auth/register (Section 3.1)
 * - POST /api/auth/login (Section 3.2)
 * - POST /api/auth/forgot-password (Section 3.3)
 * - POST /api/auth/reset-password (Section 3.4)
 * - POST /api/auth/logout (Section 3.5)
 *
 * User Stories:
 * - US-001: New User Registration
 * - US-002: System Login
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    /**
     * Register new user account.
     * API: POST /api/auth/register (Section 3.1)
     *
     * Business Logic:
     * - Hash password using BCrypt (minimum 10 rounds)
     * - Set plan_type='FREE', max_sources_allowed=1
     * - Generate JWT token with 60-minute expiration
     * - Auto-login user (return token immediately)
     * - Log activity: USER_REGISTERED
     *
     * @param request Registration request containing email and password
     * @return AuthResponse with user details and JWT token
     * @throws ValidationException if passwords don't match or are invalid
     * @throws DuplicateResourceException if email already exists
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Processing registration for email: {}", StringUtils.maskEmail(request.getEmail()));

        // Validate password confirmation
        if (!ValidationUtils.passwordsMatch(request.getPassword(), request.getConfirmPassword())) {
            throw new ValidationException("Passwords do not match");
        }

        // Validate password strength (already validated by @Pattern, but double-check)
        if (!StringUtils.isStrongPassword(request.getPassword())) {
            throw new ValidationException(
                "Password must contain uppercase, lowercase, number, and special character"
            );
        }

        // Hash password using BCrypt
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // Create user with FREE plan defaults
        User user = userService.createUser(request.getEmail(), passwordHash);

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user);
        Instant expiresAt = jwtTokenProvider.getTokenExpiration();

        // Build response
        AuthResponse response = buildAuthResponse(user, token, expiresAt, true);

        // TODO: Log activity: USER_REGISTERED (requires UserActivityLog entity)

        log.info("User registered successfully: {} (ID: {})",
            StringUtils.maskEmail(user.getEmail()), user.getId());

        return response;
    }

    /**
     * Authenticate user and generate JWT token.
     * API: POST /api/auth/login (Section 3.2)
     *
     * Business Logic:
     * - Validate credentials against hashed password
     * - Generate JWT token containing: userId, email, planType, maxSourcesAllowed
     * - Log activity: LOGIN
     *
     * @param request Login request containing email and password
     * @return AuthResponse with user details and JWT token
     * @throws InvalidCredentialsException if email or password is incorrect
     * @throws EmailNotVerifiedException if account not verified (optional, for future)
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Processing login for email: {}", StringUtils.maskEmail(request.getEmail()));

        // Find user by email
        Optional<User> userOptional = userService.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            log.warn("Login failed: User not found for email: {}", StringUtils.maskEmail(request.getEmail()));
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userOptional.get();

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: Invalid password for user ID: {}", user.getId());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Optional: Check if email is verified (disabled in MVP)
        // if (!user.getEmailVerified()) {
        //     throw new EmailNotVerifiedException("Please verify your email before logging in");
        // }

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user);
        Instant expiresAt = jwtTokenProvider.getTokenExpiration();

        // Build response
        AuthResponse response = buildAuthResponse(user, token, expiresAt, false);

        // TODO: Log activity: LOGIN (requires UserActivityLog entity)

        log.info("User logged in successfully: {} (ID: {})",
            StringUtils.maskEmail(user.getEmail()), user.getId());

        return response;
    }

    /**
     * Initiate password reset flow.
     * API: POST /api/auth/forgot-password (Section 3.3)
     *
     * Business Logic:
     * - Generate unique password_reset_token (UUID)
     * - Set password_reset_expires_at to NOW() + 1 hour
     * - Send email with reset link: https://app.brandpulse.io/reset-password?token={token}
     * - Always return success (security: don't reveal if email exists)
     *
     * @param request Forgot password request containing email
     * @return Success message (same response regardless of email existence)
     */
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        log.info("Processing password reset request for email: {}",
            StringUtils.maskEmail(request.getEmail()));

        // Find user by email
        Optional<User> userOptional = userService.findByEmail(request.getEmail());

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Generate password reset token (UUID)
            String resetToken = UUID.randomUUID().toString();

            // Set expiration to 1 hour from now
            Instant expiresAt = Instant.now().plus(
                Constants.PASSWORD_RESET_TOKEN_EXPIRATION_HOURS,
                ChronoUnit.HOURS
            );

            // Save token to database
            userService.setPasswordResetToken(user.getId(), resetToken, expiresAt);

            // TODO: Send password reset email (requires EmailService)
            // String resetLink = String.format("https://app.brandpulse.io/reset-password?token=%s", resetToken);
            // emailService.sendPasswordResetEmail(user.getEmail(), resetLink);

            log.info("Password reset token generated for user: {}",
                StringUtils.maskEmail(user.getEmail()));
        } else {
            log.debug("Password reset requested for non-existent email: {}",
                StringUtils.maskEmail(request.getEmail()));
        }

        // Always return success message (security: don't reveal if email exists)
        return new MessageResponse(
            "If an account with this email exists, a password reset link has been sent"
        );
    }

    /**
     * Complete password reset with token.
     * API: POST /api/auth/reset-password (Section 3.4)
     *
     * Business Logic:
     * - Verify token exists and hasn't expired
     * - Hash new password with BCrypt
     * - Update password_hash, clear password_reset_token and password_reset_expires_at
     * - Invalidate all existing JWT tokens for this user (optional security measure)
     *
     * @param request Reset password request containing token and new password
     * @return Success message
     * @throws ValidationException if passwords don't match or are invalid
     * @throws InvalidTokenException if token is invalid or expired
     */
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset with token");

        // Validate password confirmation
        if (!ValidationUtils.passwordsMatch(request.getNewPassword(), request.getConfirmPassword())) {
            throw new ValidationException("Passwords do not match");
        }

        // Validate password strength
        if (!StringUtils.isStrongPassword(request.getNewPassword())) {
            throw new ValidationException(
                "Password must contain uppercase, lowercase, number, and special character"
            );
        }

        // Find user by valid reset token
        Optional<User> userOptional = userService.findByValidPasswordResetToken(request.getToken());

        if (userOptional.isEmpty()) {
            log.warn("Password reset failed: Invalid or expired token");
            throw new InvalidTokenException("Password reset token is invalid or has expired");
        }

        User user = userOptional.get();

        // Hash new password
        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());

        // Update password
        userService.updatePassword(user.getId(), newPasswordHash);

        // Clear reset token
        userService.clearPasswordResetToken(user.getId());

        // TODO: Invalidate all existing JWT tokens for this user (requires token blacklist)

        log.info("Password reset successfully for user: {}",
            StringUtils.maskEmail(user.getEmail()));

        return new MessageResponse(
            "Password reset successfully. You can now log in with your new password."
        );
    }

    /**
     * Logout user.
     * API: POST /api/auth/logout (Section 3.5)
     *
     * Business Logic:
     * - Log activity: LOGOUT
     * - Add token to blacklist (if implementing token revocation)
     * - Client should delete token from local storage
     *
     * Note: JWT is stateless, so logout is primarily client-side.
     * Server-side logout would require token blacklist (Redis cache).
     *
     * @param userId User ID from JWT token
     * @return Success message
     */
    @Transactional
    public MessageResponse logout(Long userId) {
        log.info("Processing logout for user ID: {}", userId);

        // Verify user exists
        User user = userService.findByIdOrThrow(userId);

        // TODO: Add token to blacklist (requires Redis cache implementation)
        // TODO: Log activity: LOGOUT (requires UserActivityLog entity)

        log.info("User logged out: {}", StringUtils.maskEmail(user.getEmail()));

        return new MessageResponse("Logged out successfully");
    }

    /**
     * Build AuthResponse from user entity and JWT token.
     * Helper method to construct response DTOs.
     *
     * @param user User entity
     * @param token JWT token string
     * @param expiresAt Token expiration instant
     * @param includeCreatedAt Whether to include createdAt (true for registration, false for login)
     * @return AuthResponse DTO
     */
    private AuthResponse buildAuthResponse(User user, String token, Instant expiresAt, boolean includeCreatedAt) {
        AuthResponse.AuthResponseBuilder builder = AuthResponse.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .planType(user.getPlanType())
            .maxSourcesAllowed(user.getMaxSourcesAllowed())
            .emailVerified(user.getEmailVerified())
            .token(token)
            .expiresAt(expiresAt.atZone(ZoneId.of("UTC")));

        // Include createdAt only for registration response
        if (includeCreatedAt) {
            builder.createdAt(user.getCreatedAt().atZone(ZoneId.of("UTC")));
        }

        return builder.build();
    }

    /**
     * Simple message response DTO for API responses.
     * Used for forgot-password, reset-password, logout endpoints.
     */
    public record MessageResponse(String message) {}
}
