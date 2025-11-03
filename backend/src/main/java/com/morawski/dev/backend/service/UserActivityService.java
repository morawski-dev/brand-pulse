package com.morawski.dev.backend.service;

import com.morawski.dev.backend.dto.activity.*;
import com.morawski.dev.backend.dto.common.ActivityType;
import com.morawski.dev.backend.entity.User;
import com.morawski.dev.backend.entity.UserActivityLog;
import com.morawski.dev.backend.exception.ResourceNotFoundException;
import com.morawski.dev.backend.mapper.UserActivityLogMapper;
import com.morawski.dev.backend.repository.UserActivityLogRepository;
import com.morawski.dev.backend.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for user activity tracking and success metrics calculation.
 * Records user actions for analytics and calculates key business metrics.
 *
 * API Endpoints (Section 10):
 * - GET /api/users/me/activity (Section 10.1)
 * - POST /api/users/me/activity (Section 10.2)
 * - GET /api/admin/metrics/success (Section 10.3 - admin only)
 *
 * Success Metrics (from PRD):
 * - Time to Value: USER_REGISTERED → FIRST_SOURCE_CONFIGURED_SUCCESSFULLY < 10 minutes (target: 90%)
 * - Activation: SOURCE_CONFIGURED within 7 days (target: 60%)
 * - Retention: 3+ LOGIN events in first 4 weeks (target: 35%)
 * - AI Accuracy: Track SENTIMENT_CORRECTED events
 *
 * Activity Types:
 * - USER_REGISTERED, LOGIN, LOGOUT
 * - VIEW_DASHBOARD, FILTER_APPLIED
 * - SENTIMENT_CORRECTED
 * - SOURCE_CONFIGURED, SOURCE_ADDED, SOURCE_DELETED
 * - MANUAL_REFRESH_TRIGGERED
 * - FIRST_SOURCE_CONFIGURED_SUCCESSFULLY
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityService {

    private final UserActivityLogRepository userActivityLogRepository;
    private final UserService userService;
    private final UserActivityLogMapper userActivityLogMapper;

    /**
     * Log user activity event.
     * API: POST /api/users/me/activity (Section 10.2)
     *
     * Business Logic:
     * - Create activity log record with optional metadata
     * - Metadata stored as JSONB for flexibility
     * - Used for analytics and success metrics calculation
     *
     * @param userId User ID from JWT token
     * @param request Activity log request
     * @return UserActivityResponse with created activity
     */
    @Transactional
    public UserActivityResponse logActivity(Long userId, LogActivityRequest request) {
        log.debug("Logging activity: type={}, userId={}", request.getActivityType(), userId);

        User user = userService.findByIdOrThrow(userId);

        UserActivityLog activityLog = UserActivityLog.builder()
            .activityType(request.getActivityType())
            .occurredAt(request.getOccurredAt() != null ? request.getOccurredAt() : Instant.now())
            .metadata(request.getMetadata())
            .user(user)
            .build();

        UserActivityLog savedActivity = userActivityLogRepository.save(activityLog);

        log.info("Activity logged: type={}, userId={}, activityId={}",
            request.getActivityType(), userId, savedActivity.getId());

        return userActivityLogMapper.toUserActivityResponse(savedActivity);
    }

    /**
     * Log user registration event.
     * Called internally by AuthService during registration.
     *
     * @param user User entity
     */
    @Transactional
    public void logRegistration(User user) {
        UserActivityLog activityLog = UserActivityLog.builder()
            .activityType(ActivityType.USER_REGISTERED)
            .occurredAt(Instant.now())
            .user(user)
            .build();

        userActivityLogRepository.save(activityLog);
        log.info("Registration activity logged for user: {}", user.getId());
    }

    /**
     * Log user login event.
     * Called internally by AuthService during login.
     *
     * @param user User entity
     */
    @Transactional
    public void logLogin(User user) {
        UserActivityLog activityLog = UserActivityLog.builder()
            .activityType(ActivityType.LOGIN)
            .occurredAt(Instant.now())
            .user(user)
            .build();

        userActivityLogRepository.save(activityLog);
        log.debug("Login activity logged for user: {}", user.getId());
    }

    /**
     * Log first source configured successfully event.
     * Called internally by ReviewSourceService when first source is created.
     *
     * @param userId User ID
     * @param sourceId Review source ID
     */
    @Transactional
    public void logFirstSourceConfigured(Long userId, Long sourceId) {
        User user = userService.findByIdOrThrow(userId);

        Map<String, Object> metadata = Map.of("sourceId", sourceId);

        UserActivityLog activityLog = UserActivityLog.builder()
            .activityType(ActivityType.FIRST_SOURCE_CONFIGURED_SUCCESSFULLY)
            .occurredAt(Instant.now())
            .metadata(metadata)
            .user(user)
            .build();

        userActivityLogRepository.save(activityLog);
        log.info("First source configured activity logged: userId={}, sourceId={}", userId, sourceId);
    }

    /**
     * Log sentiment correction event.
     * Called internally by ReviewService when user corrects sentiment.
     *
     * @param userId User ID
     * @param reviewId Review ID
     * @param oldSentiment Old sentiment value
     * @param newSentiment New sentiment value
     */
    @Transactional
    public void logSentimentCorrection(Long userId, Long reviewId, String oldSentiment, String newSentiment) {
        User user = userService.findByIdOrThrow(userId);

        Map<String, Object> metadata = Map.of(
            "reviewId", reviewId,
            "oldSentiment", oldSentiment,
            "newSentiment", newSentiment
        );

        UserActivityLog activityLog = UserActivityLog.builder()
            .activityType(ActivityType.SENTIMENT_CORRECTED)
            .occurredAt(Instant.now())
            .metadata(metadata)
            .user(user)
            .build();

        userActivityLogRepository.save(activityLog);
        log.info("Sentiment correction activity logged: userId={}, reviewId={}", userId, reviewId);
    }

    /**
     * Log manual refresh triggered event.
     * Called internally by SyncJobService when user triggers manual sync.
     *
     * @param userId User ID
     * @param sourceId Review source ID
     * @param jobId Sync job ID
     */
    @Transactional
    public void logManualRefreshTriggered(Long userId, Long sourceId, Long jobId) {
        User user = userService.findByIdOrThrow(userId);

        Map<String, Object> metadata = Map.of(
            "sourceId", sourceId,
            "jobId", jobId
        );

        UserActivityLog activityLog = UserActivityLog.builder()
            .activityType(ActivityType.MANUAL_REFRESH_TRIGGERED)
            .occurredAt(Instant.now())
            .metadata(metadata)
            .user(user)
            .build();

        userActivityLogRepository.save(activityLog);
        log.info("Manual refresh activity logged: userId={}, sourceId={}, jobId={}", userId, sourceId, jobId);
    }

    /**
     * Get user activity history.
     * API: GET /api/users/me/activity (Section 10.1)
     *
     * Query Parameters:
     * - page (optional): Page number (0-indexed). Default: 0
     * - size (optional): Items per page. Default: 20, Max: 100
     *
     * @param userId User ID from JWT token
     * @param page Page number
     * @param size Page size
     * @return UserActivityListResponse with paginated activities
     */
    @Transactional(readOnly = true)
    public UserActivityListResponse getUserActivityHistory(Long userId, Integer page, Integer size) {
        log.debug("Getting activity history for user ID: {}", userId);

        int validatedPage = page != null ? page : Constants.DEFAULT_PAGE_NUMBER;
        int validatedSize = size != null ? Math.min(size, Constants.MAX_PAGE_SIZE) : Constants.DEFAULT_PAGE_SIZE;

        Pageable pageable = PageRequest.of(validatedPage, validatedSize);

        Page<UserActivityLog> activityPage = userActivityLogRepository.findByUserId(userId, pageable);

        List<UserActivityResponse> activityResponses = activityPage.getContent().stream()
            .map(userActivityLogMapper::toUserActivityResponse)
            .collect(Collectors.toList());

        log.info("Retrieved {} activity(ies) for user {}, page {}/{}",
            activityResponses.size(), userId, validatedPage, activityPage.getTotalPages());

        com.morawski.dev.backend.dto.common.PaginationResponse pagination =
            com.morawski.dev.backend.dto.common.PaginationResponse.builder()
                .currentPage(validatedPage)
                .pageSize(validatedSize)
                .totalItems(activityPage.getTotalElements())
                .totalPages(activityPage.getTotalPages())
                .hasNext(activityPage.hasNext())
                .hasPrevious(activityPage.hasPrevious())
                .build();

        return UserActivityListResponse.builder()
            .activities(activityResponses)
            .pagination(pagination)
            .build();
    }

    // ========== Success Metrics Calculation ==========

    /**
     * Calculate Time to Value for a user.
     * Target: USER_REGISTERED → FIRST_SOURCE_CONFIGURED_SUCCESSFULLY < 10 minutes (90%)
     *
     * @param userId User ID
     * @return Duration between registration and first source configured (null if not configured yet)
     */
    @Transactional(readOnly = true)
    public Duration calculateTimeToValue(Long userId) {
        Optional<UserActivityLog> registration = userActivityLogRepository.findRegistrationEvent(userId);
        Optional<UserActivityLog> firstSource = userActivityLogRepository.findFirstSourceConfiguredEvent(userId);

        if (registration.isEmpty() || firstSource.isEmpty()) {
            return null;
        }

        return Duration.between(registration.get().getOccurredAt(), firstSource.get().getOccurredAt());
    }

    /**
     * Check if user achieved Time to Value target (<10 minutes).
     *
     * @param userId User ID
     * @return true if time to value < 10 minutes
     */
    @Transactional(readOnly = true)
    public boolean hasAchievedTimeToValueTarget(Long userId) {
        Duration timeToValue = calculateTimeToValue(userId);
        return timeToValue != null && timeToValue.toMinutes() < 10;
    }

    /**
     * Check if user achieved Activation target (source configured within 7 days).
     * Target: 60% of users configure a source within 7 days
     *
     * @param userId User ID
     * @return true if user configured source within 7 days of registration
     */
    @Transactional(readOnly = true)
    public boolean hasAchievedActivationTarget(Long userId) {
        Optional<UserActivityLog> registration = userActivityLogRepository.findRegistrationEvent(userId);
        Optional<UserActivityLog> firstSource = userActivityLogRepository.findFirstSourceConfiguredEvent(userId);

        if (registration.isEmpty() || firstSource.isEmpty()) {
            return false;
        }

        Duration timeSinceRegistration = Duration.between(registration.get().getOccurredAt(), firstSource.get().getOccurredAt());
        return timeSinceRegistration.toDays() <= 7;
    }

    /**
     * Check if user achieved Retention target (3+ logins in first 4 weeks).
     * Target: 35% of users have 3+ logins in first 4 weeks
     *
     * @param userId User ID
     * @return true if user has 3+ login events in first 4 weeks after registration
     */
    @Transactional(readOnly = true)
    public boolean hasAchievedRetentionTarget(Long userId) {
        Optional<UserActivityLog> registration = userActivityLogRepository.findRegistrationEvent(userId);

        if (registration.isEmpty()) {
            return false;
        }

        Instant registrationDate = registration.get().getOccurredAt();
        Instant fourWeeksLater = registrationDate.plus(28, ChronoUnit.DAYS);

        long loginCount = userActivityLogRepository.countLoginEvents(userId, registrationDate, fourWeeksLater);

        return loginCount >= 3;
    }

    /**
     * Calculate success metrics for a user.
     * Used for admin dashboard and analytics.
     *
     * @param userId User ID
     * @return UserSuccessMetrics with all metrics
     */
    @Transactional(readOnly = true)
    public UserSuccessMetrics calculateUserSuccessMetrics(Long userId) {
        Duration timeToValue = calculateTimeToValue(userId);
        boolean activationAchieved = hasAchievedActivationTarget(userId);
        boolean retentionAchieved = hasAchievedRetentionTarget(userId);

        Optional<UserActivityLog> registration = userActivityLogRepository.findRegistrationEvent(userId);
        long loginCount = userActivityLogRepository.countByUserIdAndActivityType(userId, ActivityType.LOGIN);
        long sentimentCorrectionCount = userActivityLogRepository.countSentimentCorrections(userId);

        return UserSuccessMetrics.builder()
            .userId(userId)
            .registrationDate(registration.map(UserActivityLog::getOccurredAt).orElse(null))
            .timeToValueMinutes(timeToValue != null ? timeToValue.toMinutes() : null)
            .timeToValueAchieved(hasAchievedTimeToValueTarget(userId))
            .activationAchieved(activationAchieved)
            .retentionAchieved(retentionAchieved)
            .totalLogins(loginCount)
            .sentimentCorrections(sentimentCorrectionCount)
            .build();
    }

    /**
     * Calculate global success metrics across all users.
     * API: GET /api/admin/metrics/success (Section 10.3 - admin only)
     *
     * @param startDate Start date for cohort analysis
     * @param endDate End date for cohort analysis
     * @return GlobalSuccessMetrics with aggregated metrics
     */
    @Transactional(readOnly = true)
    public GlobalSuccessMetrics calculateGlobalSuccessMetrics(Instant startDate, Instant endDate) {
        log.info("Calculating global success metrics for period: {} to {}", startDate, endDate);

        List<UserActivityLog> registrations = userActivityLogRepository.findRegistrationsBetween(startDate, endDate);

        long totalUsers = registrations.size();
        if (totalUsers == 0) {
            return GlobalSuccessMetrics.builder()
                .periodStart(startDate)
                .periodEnd(endDate)
                .totalUsers(0L)
                .timeToValueAchievedCount(0L)
                .timeToValueAchievedPercentage(0.0)
                .activationAchievedCount(0L)
                .activationAchievedPercentage(0.0)
                .retentionAchievedCount(0L)
                .retentionAchievedPercentage(0.0)
                .averageTimeToValueMinutes(0.0)
                .build();
        }

        long timeToValueCount = 0;
        long activationCount = 0;
        long retentionCount = 0;
        double totalTimeToValue = 0;
        int usersWithTimeToValue = 0;

        for (UserActivityLog registration : registrations) {
            Long userId = registration.getUser().getId();

            if (hasAchievedTimeToValueTarget(userId)) {
                timeToValueCount++;
            }

            if (hasAchievedActivationTarget(userId)) {
                activationCount++;
            }

            if (hasAchievedRetentionTarget(userId)) {
                retentionCount++;
            }

            Duration timeToValue = calculateTimeToValue(userId);
            if (timeToValue != null) {
                totalTimeToValue += timeToValue.toMinutes();
                usersWithTimeToValue++;
            }
        }

        double averageTimeToValue = usersWithTimeToValue > 0 ? totalTimeToValue / usersWithTimeToValue : 0.0;

        GlobalSuccessMetrics metrics = GlobalSuccessMetrics.builder()
            .periodStart(startDate)
            .periodEnd(endDate)
            .totalUsers(totalUsers)
            .timeToValueAchievedCount(timeToValueCount)
            .timeToValueAchievedPercentage((double) timeToValueCount / totalUsers * 100)
            .activationAchievedCount(activationCount)
            .activationAchievedPercentage((double) activationCount / totalUsers * 100)
            .retentionAchievedCount(retentionCount)
            .retentionAchievedPercentage((double) retentionCount / totalUsers * 100)
            .averageTimeToValueMinutes(averageTimeToValue)
            .build();

        log.info("Global metrics calculated: totalUsers={}, timeToValue={:.2f}%, activation={:.2f}%, retention={:.2f}%",
            totalUsers, metrics.getTimeToValueAchievedPercentage(),
            metrics.getActivationAchievedPercentage(), metrics.getRetentionAchievedPercentage());

        return metrics;
    }
}
