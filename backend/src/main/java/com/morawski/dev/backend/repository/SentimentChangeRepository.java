package com.morawski.dev.backend.repository;

import com.morawski.dev.backend.dto.common.ChangeReason;
import com.morawski.dev.backend.dto.common.Sentiment;
import com.morawski.dev.backend.entity.SentimentChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository interface for SentimentChange entity.
 * Provides data access methods for sentiment change tracking and AI accuracy analytics.
 *
 * User Stories:
 * - US-007: Manual Sentiment Correction
 *
 * API Endpoints:
 * - GET /api/brands/{brandId}/reviews/{reviewId} (includes sentimentChangeHistory)
 * - PATCH /api/brands/{brandId}/reviews/{reviewId}/sentiment
 *
 * Success Metrics:
 * - AI Accuracy: 75% of sentiment classifications should be correct (measured via user corrections)
 */
@Repository
public interface SentimentChangeRepository extends JpaRepository<SentimentChange, Long> {

    /**
     * Find all sentiment changes for a specific review.
     * Used to display sentiment change history in review details.
     *
     * API: GET /api/brands/{brandId}/reviews/{reviewId}
     *
     * @param reviewId the review ID
     * @return list of sentiment changes ordered by most recent first
     */
    @Query("SELECT sc FROM SentimentChange sc WHERE sc.review.id = :reviewId " +
           "ORDER BY sc.changedAt DESC")
    List<SentimentChange> findByReviewIdOrderByChangedAtDesc(@Param("reviewId") Long reviewId);

    /**
     * Find sentiment changes by change reason.
     * Used for analytics and AI accuracy metrics.
     *
     * Examples:
     * - AI_INITIAL: Initial AI classification
     * - USER_CORRECTION: User manually changed sentiment
     * - AI_REANALYSIS: AI re-analyzed after model update
     *
     * @param changeReason the change reason
     * @return list of sentiment changes
     */
    @Query("SELECT sc FROM SentimentChange sc WHERE sc.changeReason = :changeReason " +
           "ORDER BY sc.changedAt DESC")
    List<SentimentChange> findByChangeReason(@Param("changeReason") ChangeReason changeReason);

    /**
     * Count sentiment changes by change reason.
     * Used for analytics dashboard and AI accuracy metrics.
     *
     * Success Metric: If USER_CORRECTION count < 25% of AI_INITIAL count, then AI accuracy >= 75%
     *
     * @param changeReason the change reason
     * @return count of changes
     */
    @Query("SELECT COUNT(sc) FROM SentimentChange sc WHERE sc.changeReason = :changeReason")
    long countByChangeReason(@Param("changeReason") ChangeReason changeReason);

    /**
     * Find all user corrections for a specific review source.
     * Used to calculate AI accuracy per source.
     *
     * @param reviewSourceId the review source ID
     * @return list of user corrections
     */
    @Query("SELECT sc FROM SentimentChange sc WHERE sc.review.reviewSource.id = :reviewSourceId " +
           "AND sc.changeReason = 'USER_CORRECTION' ORDER BY sc.changedAt DESC")
    List<SentimentChange> findUserCorrectionsByReviewSourceId(@Param("reviewSourceId") Long reviewSourceId);

    /**
     * Find all user corrections by a specific user.
     * Used for user activity analytics.
     *
     * @param userId the user ID
     * @return list of corrections made by user
     */
    @Query("SELECT sc FROM SentimentChange sc WHERE sc.changedByUser.id = :userId " +
           "AND sc.changeReason = 'USER_CORRECTION' ORDER BY sc.changedAt DESC")
    List<SentimentChange> findUserCorrectionsByUserId(@Param("userId") Long userId);

    /**
     * Find sentiment changes within a date range.
     * Used for time-based analytics.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of sentiment changes
     */
    @Query("SELECT sc FROM SentimentChange sc WHERE sc.changedAt >= :startDate " +
           "AND sc.changedAt <= :endDate ORDER BY sc.changedAt DESC")
    List<SentimentChange> findByChangedAtBetween(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Calculate AI accuracy for a specific review source.
     * Returns the percentage of AI classifications that were NOT corrected by users.
     *
     * Formula: 1 - (USER_CORRECTIONS / TOTAL_AI_INITIAL) * 100
     *
     * @param reviewSourceId the review source ID
     * @return AI accuracy percentage (0.0 to 100.0)
     */
    @Query("SELECT (1.0 - CAST(COUNT(CASE WHEN sc.changeReason = 'USER_CORRECTION' THEN 1 END) AS double) / " +
           "NULLIF(COUNT(CASE WHEN sc.changeReason = 'AI_INITIAL' THEN 1 END), 0)) * 100.0 " +
           "FROM SentimentChange sc WHERE sc.review.reviewSource.id = :reviewSourceId")
    Double calculateAIAccuracyForSource(@Param("reviewSourceId") Long reviewSourceId);

    /**
     * Calculate global AI accuracy across all review sources.
     * Used for overall system metrics.
     *
     * Success Metric: Should be >= 75%
     *
     * @return AI accuracy percentage (0.0 to 100.0)
     */
    @Query("SELECT (1.0 - CAST(COUNT(CASE WHEN sc.changeReason = 'USER_CORRECTION' THEN 1 END) AS double) / " +
           "NULLIF(COUNT(CASE WHEN sc.changeReason = 'AI_INITIAL' THEN 1 END), 0)) * 100.0 " +
           "FROM SentimentChange sc")
    Double calculateGlobalAIAccuracy();

    /**
     * Find most common sentiment correction patterns.
     * Returns list of corrections grouped by old -> new sentiment.
     * Used to improve AI model training.
     *
     * Example result: "NEUTRAL -> NEGATIVE: 45 corrections"
     *
     * @param reviewSourceId the review source ID
     * @return list of correction patterns with counts
     */
    @Query("SELECT sc.oldSentiment, sc.newSentiment, COUNT(sc) " +
           "FROM SentimentChange sc WHERE sc.review.reviewSource.id = :reviewSourceId " +
           "AND sc.changeReason = 'USER_CORRECTION' " +
           "GROUP BY sc.oldSentiment, sc.newSentiment ORDER BY COUNT(sc) DESC")
    List<Object[]> findCorrectionPatternsByReviewSourceId(@Param("reviewSourceId") Long reviewSourceId);

    /**
     * Find reviews that have been corrected multiple times.
     * Indicates potentially ambiguous sentiment.
     *
     * @param reviewSourceId the review source ID
     * @param minCorrections minimum number of corrections
     * @return list of review IDs with multiple corrections
     */
    @Query("SELECT sc.review.id, COUNT(sc) FROM SentimentChange sc " +
           "WHERE sc.review.reviewSource.id = :reviewSourceId " +
           "GROUP BY sc.review.id HAVING COUNT(sc) >= :minCorrections")
    List<Object[]> findReviewsWithMultipleCorrections(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("minCorrections") long minCorrections
    );

    /**
     * Find sentiment changes for a specific old -> new sentiment transition.
     * Used for targeted AI model improvements.
     *
     * @param oldSentiment the original sentiment
     * @param newSentiment the corrected sentiment
     * @return list of matching changes
     */
    @Query("SELECT sc FROM SentimentChange sc WHERE sc.oldSentiment = :oldSentiment " +
           "AND sc.newSentiment = :newSentiment AND sc.changeReason = 'USER_CORRECTION' " +
           "ORDER BY sc.changedAt DESC")
    List<SentimentChange> findByOldSentimentAndNewSentiment(
            @Param("oldSentiment") Sentiment oldSentiment,
            @Param("newSentiment") Sentiment newSentiment
    );

    /**
     * Count total sentiment changes for a review source.
     * Used for engagement metrics.
     *
     * @param reviewSourceId the review source ID
     * @return count of all changes
     */
    @Query("SELECT COUNT(sc) FROM SentimentChange sc WHERE sc.review.reviewSource.id = :reviewSourceId")
    long countByReviewSourceId(@Param("reviewSourceId") Long reviewSourceId);
}
