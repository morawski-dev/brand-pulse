package com.morawski.dev.backend.repository;

import com.morawski.dev.backend.dto.common.ActivityType;
import com.morawski.dev.backend.entity.UserActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserActivityLog entity.
 * Provides data access methods for activity tracking and success metrics calculation.
 *
 * Success Metrics:
 * - Time to Value: USER_REGISTERED → FIRST_SOURCE_CONFIGURED_SUCCESSFULLY < 10 minutes (target: 90%)
 * - Activation: SOURCE_CONFIGURED within 7 days (target: 60%)
 * - Retention: 3+ LOGIN events in first 4 weeks (target: 35%)
 * - AI Accuracy: Track SENTIMENT_CORRECTED events
 */
@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

    /**
     * Find all activities for a user, ordered by occurrence time descending.
     *
     * @param userId User ID
     * @param pageable Pagination parameters
     * @return Page of activity logs
     */
    @Query("SELECT ual FROM UserActivityLog ual WHERE ual.user.id = :userId " +
           "ORDER BY ual.occurredAt DESC")
    Page<UserActivityLog> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find all activities for a user of a specific type.
     *
     * @param userId User ID
     * @param activityType Activity type
     * @return List of activity logs
     */
    @Query("SELECT ual FROM UserActivityLog ual WHERE ual.user.id = :userId " +
           "AND ual.activityType = :activityType ORDER BY ual.occurredAt DESC")
    List<UserActivityLog> findByUserIdAndActivityType(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType
    );

    /**
     * Find user registration event.
     * Used as starting point for Time to Value calculation.
     *
     * @param userId User ID
     * @return Optional containing registration event
     */
    @Query("SELECT ual FROM UserActivityLog ual WHERE ual.user.id = :userId " +
           "AND ual.activityType = 'USER_REGISTERED' ORDER BY ual.occurredAt ASC LIMIT 1")
    Optional<UserActivityLog> findRegistrationEvent(@Param("userId") Long userId);

    /**
     * Find first source configured event.
     * Used for Time to Value calculation (registration → first source < 10 min).
     *
     * @param userId User ID
     * @return Optional containing first source configured event
     */
    @Query("SELECT ual FROM UserActivityLog ual WHERE ual.user.id = :userId " +
           "AND ual.activityType = 'FIRST_SOURCE_CONFIGURED_SUCCESSFULLY' " +
           "ORDER BY ual.occurredAt ASC LIMIT 1")
    Optional<UserActivityLog> findFirstSourceConfiguredEvent(@Param("userId") Long userId);

    /**
     * Count login events for a user within a time range.
     * Used for Retention calculation (3+ logins in first 4 weeks).
     *
     * @param userId User ID
     * @param startDate Start of time range
     * @param endDate End of time range
     * @return Count of login events
     */
    @Query("SELECT COUNT(ual) FROM UserActivityLog ual WHERE ual.user.id = :userId " +
           "AND ual.activityType = 'LOGIN' " +
           "AND ual.occurredAt >= :startDate AND ual.occurredAt <= :endDate")
    long countLoginEvents(
            @Param("userId") Long userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Count sentiment correction events for a user.
     * Used for AI accuracy metrics.
     *
     * @param userId User ID
     * @return Count of sentiment corrections
     */
    @Query("SELECT COUNT(ual) FROM UserActivityLog ual WHERE ual.user.id = :userId " +
           "AND ual.activityType = 'SENTIMENT_CORRECTED'")
    long countSentimentCorrections(@Param("userId") Long userId);

    /**
     * Find all users who registered within a date range.
     * Used for cohort analysis and success metrics reporting.
     *
     * @param startDate Start of registration period
     * @param endDate End of registration period
     * @return List of registration events
     */
    @Query("SELECT ual FROM UserActivityLog ual WHERE ual.activityType = 'USER_REGISTERED' " +
           "AND ual.occurredAt >= :startDate AND ual.occurredAt <= :endDate " +
           "ORDER BY ual.occurredAt ASC")
    List<UserActivityLog> findRegistrationsBetween(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Find activities by type within date range.
     * Used for analytics and reporting.
     *
     * @param activityType Activity type
     * @param startDate Start date
     * @param endDate End date
     * @return List of matching activities
     */
    @Query("SELECT ual FROM UserActivityLog ual WHERE ual.activityType = :activityType " +
           "AND ual.occurredAt >= :startDate AND ual.occurredAt <= :endDate " +
           "ORDER BY ual.occurredAt DESC")
    List<UserActivityLog> findByActivityTypeBetween(
            @Param("activityType") ActivityType activityType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Check if user has any activity of specific type.
     *
     * @param userId User ID
     * @param activityType Activity type
     * @return true if activity exists
     */
    @Query("SELECT COUNT(ual) > 0 FROM UserActivityLog ual WHERE ual.user.id = :userId " +
           "AND ual.activityType = :activityType")
    boolean existsByUserIdAndActivityType(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType
    );

    /**
     * Find most recent activity of specific type for user.
     *
     * @param userId User ID
     * @param activityType Activity type
     * @return Optional containing most recent activity
     */
    @Query("SELECT ual FROM UserActivityLog ual WHERE ual.user.id = :userId " +
           "AND ual.activityType = :activityType ORDER BY ual.occurredAt DESC LIMIT 1")
    Optional<UserActivityLog> findMostRecentByUserIdAndActivityType(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType
    );

    /**
     * Count total activities for a user.
     *
     * @param userId User ID
     * @return Total activity count
     */
    @Query("SELECT COUNT(ual) FROM UserActivityLog ual WHERE ual.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * Count activities by user and activity type.
     *
     * @param userId User ID
     * @param activityType Activity type
     * @return Count of activities
     */
    @Query("SELECT COUNT(ual) FROM UserActivityLog ual WHERE ual.user.id = :userId " +
           "AND ual.activityType = :activityType")
    long countByUserIdAndActivityType(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType
    );

    /**
     * Find all dashboard view events for analytics.
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of dashboard view events
     */
    @Query("SELECT ual FROM UserActivityLog ual WHERE ual.activityType = 'VIEW_DASHBOARD' " +
           "AND ual.occurredAt >= :startDate AND ual.occurredAt <= :endDate " +
           "ORDER BY ual.occurredAt DESC")
    List<UserActivityLog> findDashboardViewsBetween(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}
