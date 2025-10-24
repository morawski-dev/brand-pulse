package com.morawski.dev.backend.repository;

import com.morawski.dev.backend.entity.DashboardAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DashboardAggregate entity.
 * Provides data access methods for pre-calculated dashboard metrics and aggregates.
 *
 * User Stories:
 * - US-004: Viewing Aggregated Reviews
 * - US-006: Switching Between Locations
 *
 * API Endpoints:
 * - GET /api/dashboard/summary
 *
 * Performance Requirement: Dashboard must load in <4 seconds
 * Pre-calculated aggregates ensure fast dashboard loading.
 */
@Repository
public interface DashboardAggregateRepository extends JpaRepository<DashboardAggregate, Long> {

    /**
     * Find dashboard aggregate for a specific review source and date.
     * Used to retrieve pre-calculated metrics for a single day.
     *
     * @param reviewSourceId the review source ID
     * @param date the date to retrieve aggregate for
     * @return Optional containing aggregate if exists
     */
    @Query("SELECT da FROM DashboardAggregate da WHERE da.reviewSource.id = :reviewSourceId " +
           "AND da.date = :date")
    Optional<DashboardAggregate> findByReviewSourceIdAndDate(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("date") LocalDate date
    );

    /**
     * Find all aggregates for a review source within a date range.
     * Used for dashboard summary with date filters.
     *
     * API: GET /api/dashboard/summary?startDate=...&endDate=...
     *
     * @param reviewSourceId the review source ID
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of aggregates ordered by date
     */
    @Query("SELECT da FROM DashboardAggregate da WHERE da.reviewSource.id = :reviewSourceId " +
           "AND da.date >= :startDate AND da.date <= :endDate ORDER BY da.date ASC")
    List<DashboardAggregate> findByReviewSourceIdAndDateBetween(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find all aggregates for a brand (across all sources) within a date range.
     * Used for "All locations" view (US-006).
     *
     * API: GET /api/dashboard/summary?brandId=X (no sourceId)
     *
     * @param brandId the brand ID
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of aggregates for all brand's sources
     */
    @Query("SELECT da FROM DashboardAggregate da WHERE da.reviewSource.brand.id = :brandId " +
           "AND da.date >= :startDate AND da.date <= :endDate ORDER BY da.date ASC")
    List<DashboardAggregate> findByBrandIdAndDateBetween(
            @Param("brandId") Long brandId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find aggregates for last N days for a review source.
     * Common use case: Last 7/30/90 days dashboard view.
     *
     * @param reviewSourceId the review source ID
     * @param startDate the starting date (e.g., 90 days ago)
     * @return list of recent aggregates
     */
    @Query("SELECT da FROM DashboardAggregate da WHERE da.reviewSource.id = :reviewSourceId " +
           "AND da.date >= :startDate ORDER BY da.date DESC")
    List<DashboardAggregate> findRecentByReviewSourceId(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("startDate") LocalDate startDate
    );

    /**
     * Find aggregates that need recalculation (older than 1 hour).
     * Used by background job to refresh stale data.
     *
     * @param threshold timestamp threshold (e.g., 1 hour ago)
     * @return list of stale aggregates
     */
    @Query("SELECT da FROM DashboardAggregate da WHERE da.lastCalculatedAt < :threshold " +
           "ORDER BY da.lastCalculatedAt ASC")
    List<DashboardAggregate> findAggregatesNeedingRecalculation(@Param("threshold") Instant threshold);

    /**
     * Delete old aggregates beyond retention period.
     * Used for data cleanup (e.g., keep only last 365 days).
     *
     * @param cutoffDate aggregates before this date will be deleted
     */
    @Query("DELETE FROM DashboardAggregate da WHERE da.date < :cutoffDate")
    void deleteAggregatesBeforeDate(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Calculate total aggregates for a review source (sum across all dates in range).
     * Used for dashboard summary metrics.
     *
     * Returns: [totalReviews, positiveCount, negativeCount, neutralCount]
     *
     * @param reviewSourceId the review source ID
     * @param startDate start of date range
     * @param endDate end of date range
     * @return array of totals [total, positive, negative, neutral]
     */
    @Query("SELECT SUM(da.totalReviews), SUM(da.positiveCount), " +
           "SUM(da.negativeCount), SUM(da.neutralCount) " +
           "FROM DashboardAggregate da WHERE da.reviewSource.id = :reviewSourceId " +
           "AND da.date >= :startDate AND da.date <= :endDate")
    Object[] calculateTotalsForSource(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Calculate total aggregates for a brand (sum across all sources and dates).
     * Used for "All locations" dashboard view.
     *
     * API: GET /api/dashboard/summary?brandId=X (aggregated metrics)
     *
     * @param brandId the brand ID
     * @param startDate start of date range
     * @param endDate end of date range
     * @return array of totals [total, positive, negative, neutral]
     */
    @Query("SELECT SUM(da.totalReviews), SUM(da.positiveCount), " +
           "SUM(da.negativeCount), SUM(da.neutralCount) " +
           "FROM DashboardAggregate da WHERE da.reviewSource.brand.id = :brandId " +
           "AND da.date >= :startDate AND da.date <= :endDate")
    Object[] calculateTotalsForBrand(
            @Param("brandId") Long brandId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Calculate average rating for a review source across date range.
     * Weighted average based on number of reviews per day.
     *
     * @param reviewSourceId the review source ID
     * @param startDate start of date range
     * @param endDate end of date range
     * @return average rating
     */
    @Query("SELECT SUM(da.avgRating * da.totalReviews) / NULLIF(SUM(da.totalReviews), 0) " +
           "FROM DashboardAggregate da WHERE da.reviewSource.id = :reviewSourceId " +
           "AND da.date >= :startDate AND da.date <= :endDate")
    Double calculateWeightedAverageRating(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find aggregates for a specific review source ordered by date.
     * Used for time-series charts and trend analysis.
     *
     * @param reviewSourceId the review source ID
     * @return list of all aggregates for source
     */
    @Query("SELECT da FROM DashboardAggregate da WHERE da.reviewSource.id = :reviewSourceId " +
           "ORDER BY da.date ASC")
    List<DashboardAggregate> findByReviewSourceIdOrderByDate(@Param("reviewSourceId") Long reviewSourceId);

    /**
     * Check if aggregate exists for a specific source and date.
     * Used to prevent duplicate calculations.
     *
     * @param reviewSourceId the review source ID
     * @param date the date
     * @return true if aggregate exists
     */
    @Query("SELECT CASE WHEN COUNT(da) > 0 THEN true ELSE false END " +
           "FROM DashboardAggregate da WHERE da.reviewSource.id = :reviewSourceId " +
           "AND da.date = :date")
    boolean existsByReviewSourceIdAndDate(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("date") LocalDate date
    );

    /**
     * Count aggregates for a review source.
     * Used for monitoring data completeness.
     *
     * @param reviewSourceId the review source ID
     * @return count of aggregates
     */
    @Query("SELECT COUNT(da) FROM DashboardAggregate da WHERE da.reviewSource.id = :reviewSourceId")
    long countByReviewSourceId(@Param("reviewSourceId") Long reviewSourceId);
}
