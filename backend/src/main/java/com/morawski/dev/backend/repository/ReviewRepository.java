package com.morawski.dev.backend.repository;

import com.morawski.dev.backend.dto.common.Sentiment;
import com.morawski.dev.backend.entity.Review;
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
 * Repository interface for Review entity.
 * Provides data access methods for review listing, filtering, and sentiment management.
 *
 * User Stories:
 * - US-004: Viewing Aggregated Reviews
 * - US-005: Filtering Negative Reviews
 * - US-006: Switching Between Locations
 * - US-007: Manual Sentiment Correction
 *
 * API Endpoints:
 * - GET /api/brands/{brandId}/reviews (with filtering and pagination)
 * - GET /api/brands/{brandId}/reviews/{reviewId}
 * - PATCH /api/brands/{brandId}/reviews/{reviewId}/sentiment
 *
 * Performance: Dashboard must load in <4 seconds
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Find review by ID and review source ID (for authorization).
     * Ensures review belongs to the specified source.
     *
     * API: GET /api/brands/{brandId}/reviews/{reviewId}
     *
     * @param id the review ID
     * @param reviewSourceId the review source ID
     * @return Optional containing review if found
     */
    @Query("SELECT r FROM Review r WHERE r.id = :id AND r.reviewSource.id = :reviewSourceId " +
           "AND r.deletedAt IS NULL")
    Optional<Review> findByIdAndReviewSourceId(
            @Param("id") Long id,
            @Param("reviewSourceId") Long reviewSourceId
    );

    /**
     * Find review by review source ID and external review ID.
     * Used for duplicate detection during sync.
     *
     * @param reviewSourceId the review source ID
     * @param externalReviewId the external review ID
     * @return Optional containing review if found
     */
    @Query("SELECT r FROM Review r WHERE r.reviewSource.id = :reviewSourceId " +
           "AND r.externalReviewId = :externalReviewId AND r.deletedAt IS NULL")
    Optional<Review> findByReviewSourceIdAndExternalReviewId(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("externalReviewId") String externalReviewId
    );

    /**
     * Find all reviews for a specific review source with pagination.
     * Used for basic review listing without filters.
     *
     * API: GET /api/brands/{brandId}/reviews?sourceId={sourceId}
     *
     * @param reviewSourceId the review source ID
     * @param pageable pagination parameters
     * @return page of reviews
     */
    @Query("SELECT r FROM Review r WHERE r.reviewSource.id = :reviewSourceId " +
           "AND r.deletedAt IS NULL")
    Page<Review> findByReviewSourceId(
            @Param("reviewSourceId") Long reviewSourceId,
            Pageable pageable
    );

    /**
     * Find all reviews for a brand (across all sources) with pagination.
     * Used for aggregated view (US-006: "All locations").
     *
     * API: GET /api/brands/{brandId}/reviews (no sourceId filter)
     *
     * @param brandId the brand ID
     * @param pageable pagination parameters
     * @return page of reviews
     */
    @Query("SELECT r FROM Review r WHERE r.reviewSource.brand.id = :brandId " +
           "AND r.deletedAt IS NULL")
    Page<Review> findByBrandId(@Param("brandId") Long brandId, Pageable pageable);

    /**
     * Find reviews by sentiment with pagination.
     * Used for filtering positive/negative/neutral reviews.
     *
     * API: GET /api/brands/{brandId}/reviews?sentiment=NEGATIVE
     *
     * @param reviewSourceId the review source ID
     * @param sentiment the sentiment filter
     * @param pageable pagination parameters
     * @return page of reviews
     */
    @Query("SELECT r FROM Review r WHERE r.reviewSource.id = :reviewSourceId " +
           "AND r.sentiment = :sentiment AND r.deletedAt IS NULL")
    Page<Review> findByReviewSourceIdAndSentiment(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("sentiment") Sentiment sentiment,
            Pageable pageable
    );

    /**
     * Find reviews by rating with pagination.
     * Used for filtering by star rating.
     *
     * API: GET /api/brands/{brandId}/reviews?rating=1,2
     *
     * @param reviewSourceId the review source ID
     * @param ratings list of ratings to filter by
     * @param pageable pagination parameters
     * @return page of reviews
     */
    @Query("SELECT r FROM Review r WHERE r.reviewSource.id = :reviewSourceId " +
           "AND r.rating IN :ratings AND r.deletedAt IS NULL")
    Page<Review> findByReviewSourceIdAndRatingIn(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("ratings") List<Short> ratings,
            Pageable pageable
    );

    /**
     * Find negative reviews (rating <= 2) for a specific source.
     * Uses partial index idx_reviews_negative for optimization (US-005).
     *
     * API: GET /api/brands/{brandId}/reviews?rating=1,2
     *
     * @param reviewSourceId the review source ID
     * @param pageable pagination parameters
     * @return page of negative reviews
     */
    @Query("SELECT r FROM Review r WHERE r.reviewSource.id = :reviewSourceId " +
           "AND r.rating <= 2 AND r.deletedAt IS NULL")
    Page<Review> findNegativeReviewsByReviewSourceId(
            @Param("reviewSourceId") Long reviewSourceId,
            Pageable pageable
    );

    /**
     * Find reviews within a date range.
     * Used for time-based filtering.
     *
     * API: GET /api/brands/{brandId}/reviews?startDate=...&endDate=...
     *
     * @param reviewSourceId the review source ID
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination parameters
     * @return page of reviews
     */
    @Query("SELECT r FROM Review r WHERE r.reviewSource.id = :reviewSourceId " +
           "AND r.publishedAt >= :startDate AND r.publishedAt <= :endDate " +
           "AND r.deletedAt IS NULL")
    Page<Review> findByReviewSourceIdAndPublishedAtBetween(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    /**
     * Find reviews with multiple filters (sentiment, rating, date range).
     * Uses composite index idx_reviews_composite_filter for performance.
     *
     * API: GET /api/brands/{brandId}/reviews?sentiment=NEGATIVE&rating=1,2&startDate=...
     *
     * @param reviewSourceId the review source ID
     * @param sentiments list of sentiments to filter by
     * @param ratings list of ratings to filter by
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination parameters
     * @return page of reviews
     */
    @Query("SELECT r FROM Review r WHERE r.reviewSource.id = :reviewSourceId " +
           "AND (COALESCE(:sentiments, null) IS NULL OR r.sentiment IN :sentiments) " +
           "AND (COALESCE(:ratings, null) IS NULL OR r.rating IN :ratings) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR r.publishedAt >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR r.publishedAt <= :endDate) " +
           "AND r.deletedAt IS NULL")
    Page<Review> findByMultipleFilters(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("sentiments") List<Sentiment> sentiments,
            @Param("ratings") List<Short> ratings,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    /**
     * Find reviews for a brand with multiple filters (aggregated view).
     * Used when sourceId is not specified - returns reviews from all sources.
     *
     * API: GET /api/brands/{brandId}/reviews (all sources, with filters)
     *
     * @param brandId the brand ID
     * @param sentiments list of sentiments to filter by
     * @param ratings list of ratings to filter by
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination parameters
     * @return page of reviews
     */
    @Query("SELECT r FROM Review r WHERE r.reviewSource.brand.id = :brandId " +
           "AND (COALESCE(:sentiments, null) IS NULL OR r.sentiment IN :sentiments) " +
           "AND (COALESCE(:ratings, null) IS NULL OR r.rating IN :ratings) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR r.publishedAt >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR r.publishedAt <= :endDate) " +
           "AND r.deletedAt IS NULL")
    Page<Review> findByBrandIdAndMultipleFilters(
            @Param("brandId") Long brandId,
            @Param("sentiments") List<Sentiment> sentiments,
            @Param("ratings") List<Short> ratings,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    /**
     * Find most recent reviews for a source (for AI summary generation).
     * Returns latest N reviews ordered by published date.
     *
     * Used by AI summary service to analyze recent feedback.
     *
     * @param reviewSourceId the review source ID
     * @param pageable pagination parameters (typically limit 100)
     * @return page of recent reviews
     */
    @Query("SELECT r FROM Review r WHERE r.reviewSource.id = :reviewSourceId " +
           "AND r.deletedAt IS NULL ORDER BY r.publishedAt DESC")
    Page<Review> findRecentByReviewSourceId(
            @Param("reviewSourceId") Long reviewSourceId,
            Pageable pageable
    );

    /**
     * Find top N most recent negative reviews for dashboard preview.
     * Used in dashboard summary to show recent issues.
     *
     * API: GET /api/dashboard/summary (recentNegativeReviews field)
     *
     * @param reviewSourceId the review source ID
     * @param pageable pagination parameters (typically limit 3-5)
     * @return list of recent negative reviews
     */
    @Query("SELECT r FROM Review r WHERE r.reviewSource.id = :reviewSourceId " +
           "AND r.rating <= 2 AND r.deletedAt IS NULL ORDER BY r.publishedAt DESC")
    List<Review> findTopNRecentNegativeReviews(
            @Param("reviewSourceId") Long reviewSourceId,
            Pageable pageable
    );

    /**
     * Count reviews by sentiment for a specific source.
     * Used for dashboard aggregates and analytics.
     *
     * @param reviewSourceId the review source ID
     * @param sentiment the sentiment to count
     * @return count of reviews with specified sentiment
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewSource.id = :reviewSourceId " +
           "AND r.sentiment = :sentiment AND r.deletedAt IS NULL")
    long countByReviewSourceIdAndSentiment(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("sentiment") Sentiment sentiment
    );

    /**
     * Count all reviews for a specific source.
     * Used for dashboard metrics.
     *
     * @param reviewSourceId the review source ID
     * @return total count of reviews
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewSource.id = :reviewSourceId " +
           "AND r.deletedAt IS NULL")
    long countByReviewSourceId(@Param("reviewSourceId") Long reviewSourceId);

    /**
     * Count reviews by rating for a specific source.
     * Used for rating distribution in dashboard.
     *
     * @param reviewSourceId the review source ID
     * @param rating the rating to count
     * @return count of reviews with specified rating
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewSource.id = :reviewSourceId " +
           "AND r.rating = :rating AND r.deletedAt IS NULL")
    long countByReviewSourceIdAndRating(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("rating") Short rating
    );

    /**
     * Calculate average rating for a review source.
     * Used for dashboard metrics.
     *
     * @param reviewSourceId the review source ID
     * @return average rating (1.0 to 5.0)
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewSource.id = :reviewSourceId " +
           "AND r.deletedAt IS NULL")
    Double calculateAverageRating(@Param("reviewSourceId") Long reviewSourceId);

    /**
     * Find reviews imported during a specific date range.
     * Used for sync job verification and debugging.
     *
     * @param reviewSourceId the review source ID
     * @param startDate start of fetch date range
     * @param endDate end of fetch date range
     * @return list of reviews fetched in date range
     */
    @Query("SELECT r FROM Review r WHERE r.reviewSource.id = :reviewSourceId " +
           "AND r.fetchedAt >= :startDate AND r.fetchedAt <= :endDate " +
           "AND r.deletedAt IS NULL")
    List<Review> findByReviewSourceIdAndFetchedAtBetween(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Find reviews with manually corrected sentiment.
     * Used for AI accuracy metrics (Success Metric: 75% accuracy).
     *
     * @param reviewSourceId the review source ID
     * @return list of reviews with user corrections
     */
    @Query("SELECT r FROM Review r JOIN r.sentimentChanges sc " +
           "WHERE r.reviewSource.id = :reviewSourceId " +
           "AND sc.changeReason = 'USER_CORRECTION' AND r.deletedAt IS NULL")
    List<Review> findReviewsWithManualSentimentCorrections(@Param("reviewSourceId") Long reviewSourceId);
}
