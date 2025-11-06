package com.morawski.dev.backend.controller;

import com.morawski.dev.backend.dto.activity.UserActivityListResponse;
import com.morawski.dev.backend.security.SecurityUtils;
import com.morawski.dev.backend.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for user activity log endpoints.
 * Handles retrieving user activity history for analytics and auditing.
 *
 * API Plan Section 10: Activity Log Endpoints (Internal/Analytics)
 * Base URL: /api/users/me/activity
 *
 * Endpoints:
 * - GET / - Get user activity log (Section 10.1)
 */
@RestController
@RequestMapping("/api/users/me/activity")
@RequiredArgsConstructor
@Slf4j
public class UserActivityController {

    private final UserActivityService userActivityService;

    /**
     * Get user activity log with pagination.
     * API: GET /api/users/me/activity (Section 10.1)
     *
     * Query Parameters:
     * - page (optional): Page number (default: 0)
     * - size (optional): Page size (default: 20)
     *
     * Note: Current implementation supports basic pagination.
     * Filtering by activityType, startDate, and endDate can be added in future iterations.
     *
     * Business Logic:
     * - Used for success metrics calculation:
     *   - Time to Value: USER_REGISTERED → FIRST_SOURCE_CONFIGURED_SUCCESSFULLY < 10 minutes
     *   - Activation: Count LOGIN events in first 4 weeks >= 3
     *   - Retention: 35% of users log in 3+ times in first 4 weeks
     * - Stored in user_activity_log table
     * - Metadata JSONB column allows flexible event properties
     *
     * Activity Types:
     * - USER_REGISTERED
     * - LOGIN, LOGOUT
     * - VIEW_DASHBOARD
     * - FILTER_APPLIED
     * - SENTIMENT_CORRECTED
     * - SOURCE_CONFIGURED
     * - SOURCE_ADDED
     * - SOURCE_DELETED
     * - MANUAL_REFRESH_TRIGGERED
     * - FIRST_SOURCE_CONFIGURED_SUCCESSFULLY
     *
     * Success Response: 200 OK with activity list
     * Error Responses:
     * - 401 Unauthorized: Missing or invalid token
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return UserActivityListResponse with paginated activity log
     */
    @GetMapping
    public ResponseEntity<UserActivityListResponse> getUserActivity(
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size
    ) {
        log.info("GET /api/users/me/activity - Get user activity log request received");
        Long userId = SecurityUtils.getCurrentUserId();
        UserActivityListResponse response = userActivityService.getUserActivityHistory(userId, page, size);
        return ResponseEntity.ok(response);
    }
}
