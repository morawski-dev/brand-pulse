package com.morawski.dev.backend.controller;

import com.morawski.dev.backend.dto.auth.*;
import com.morawski.dev.backend.security.SecurityUtils;
import com.morawski.dev.backend.service.AuthService;
import com.morawski.dev.backend.service.AuthService.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for authentication and authorization endpoints.
 * Handles user registration, login, password recovery, and logout.
 *
 * API Plan Section 3: Authentication Endpoints
 * Base URL: /api/auth
 *
 * Endpoints:
 * - POST /register - Register new user (Section 3.1, US-001)
 * - POST /login - User login (Section 3.2, US-002)
 * - POST /forgot-password - Request password reset (Section 3.3)
 * - POST /reset-password - Reset password with token (Section 3.4)
 * - POST /logout - Logout user (Section 3.5)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Register new user account.
     * API: POST /api/auth/register (Section 3.1)
     * User Story: US-001 - New User Registration
     *
     * Validation:
     * - email: Required, valid email format, unique in system
     * - password: Required, minimum 8 characters, must contain uppercase, lowercase, number, special character
     * - confirmPassword: Required, must match password
     *
     * Success Response: 201 Created with JWT token
     * Error Responses:
     * - 400 Bad Request: Invalid email format, passwords don't match, password too weak
     * - 409 Conflict: Email already registered
     *
     * @param request Registration request containing email and password
     * @return AuthResponse with user details and JWT token
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/auth/register - Registration request received");
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate user and generate JWT token.
     * API: POST /api/auth/login (Section 3.2)
     * User Story: US-002 - System Login
     *
     * Success Response: 200 OK with JWT token
     * Error Responses:
     * - 401 Unauthorized: Invalid credentials
     * - 403 Forbidden: Account not verified (if email verification required)
     *
     * @param request Login request containing email and password
     * @return AuthResponse with user details and JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login - Login request received");
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Request password reset token.
     * API: POST /api/auth/forgot-password (Section 3.3)
     *
     * Business Logic:
     * - Generate unique password_reset_token (UUID)
     * - Set password_reset_expires_at to NOW() + 1 hour
     * - Send email with reset link
     * - Always return success (security: don't reveal if email exists)
     *
     * Success Response: 200 OK
     *
     * @param request Forgot password request containing email
     * @return Success message (same response regardless of email existence)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("POST /api/auth/forgot-password - Password reset request received");
        MessageResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reset password using token.
     * API: POST /api/auth/reset-password (Section 3.4)
     *
     * Business Logic:
     * - Verify token exists and hasn't expired
     * - Hash new password with BCrypt
     * - Update password_hash, clear reset token
     *
     * Success Response: 200 OK
     * Error Responses:
     * - 400 Bad Request: Passwords don't match, weak password
     * - 401 Unauthorized: Invalid or expired token
     *
     * @param request Reset password request containing token and new password
     * @return Success message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("POST /api/auth/reset-password - Password reset completion request received");
        MessageResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout current user.
     * API: POST /api/auth/logout (Section 3.5)
     *
     * Business Logic:
     * - Log activity: LOGOUT
     * - Add token to blacklist (if implementing token revocation)
     * - Client should delete token from local storage
     *
     * Note: JWT is stateless, so logout is primarily client-side.
     *
     * Success Response: 200 OK
     *
     * @return Success message
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        log.info("POST /api/auth/logout - Logout request received");
        Long userId = SecurityUtils.getCurrentUserId();
        MessageResponse response = authService.logout(userId);
        return ResponseEntity.ok(response);
    }
}
