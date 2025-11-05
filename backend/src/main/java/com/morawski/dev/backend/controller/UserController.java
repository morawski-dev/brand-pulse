package com.morawski.dev.backend.controller;

import com.morawski.dev.backend.dto.user.UpdateUserProfileRequest;
import com.morawski.dev.backend.dto.user.UserProfileResponse;
import com.morawski.dev.backend.security.SecurityUtils;
import com.morawski.dev.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for user profile management endpoints.
 * Handles retrieving and updating user profile information.
 *
 * API Plan Section 4: User Endpoints
 * Base URL: /api/users
 *
 * Endpoints:
 * - GET /me - Get current user profile (Section 4.1)
 * - PATCH /me - Update current user profile (Section 4.2)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Get current authenticated user's profile.
     * API: GET /api/users/me (Section 4.1)
     *
     * Success Response: 200 OK with user profile
     * Error Responses:
     * - 401 Unauthorized: Missing or invalid token
     *
     * @return UserProfileResponse with user details
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        log.info("GET /api/users/me - Get current user profile request received");
        Long userId = SecurityUtils.getCurrentUserId();
        UserProfileResponse response = userService.getCurrentUserProfile(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update current authenticated user's profile.
     * API: PATCH /api/users/me (Section 4.2)
     *
     * Business Logic:
     * - If email changed, set email_verified=false and send verification email
     * - Updating email requires re-verification
     *
     * Success Response: 200 OK with updated user profile
     * Error Responses:
     * - 400 Bad Request: Invalid email format
     * - 401 Unauthorized: Missing or invalid token
     * - 409 Conflict: Email already in use
     *
     * @param request Update request containing new email
     * @return UserProfileResponse with updated user details
     */
    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateCurrentUser(
        @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        log.info("PATCH /api/users/me - Update current user profile request received");
        Long userId = SecurityUtils.getCurrentUserId();
        UserProfileResponse response = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(response);
    }
}
