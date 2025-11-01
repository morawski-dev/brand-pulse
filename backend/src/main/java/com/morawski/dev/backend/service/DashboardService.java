package com.morawski.dev.backend.service;

import com.morawski.dev.backend.dto.common.JobStatus;
import com.morawski.dev.backend.dto.common.Sentiment;
import com.morawski.dev.backend.dto.common.SourceType;
import com.morawski.dev.backend.dto.dashboard.*;
import com.morawski.dev.backend.dto.review.ReviewResponse;
import com.morawski.dev.backend.entity.Brand;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.entity.SyncJob;
import com.morawski.dev.backend.exception.ResourceAccessDeniedException;
import com.morawski.dev.backend.exception.ResourceNotFoundException;
import com.morawski.dev.backend.repository.ReviewRepository;
import com.morawski.dev.backend.repository.ReviewSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for dashboard operations.
 * Provides aggregated metrics and summary views for the main dashboard.
 *
 * API Endpoints (Section 8):
 * - GET /api/brands/{brandId}/dashboard (Section 8.1)
 * - GET /api/brands/{brandId}/review-sources/{sourceId}/summary (Section 8.2)
 *
 * User Stories:
 * - US-004: Viewing Aggregated Reviews
 * - US-006: Switching Between Locations
 *
 * Performance Requirement:
 * - Dashboard must load in <4 seconds
 *
 * Caching Strategy:
 * - Cache dashboard aggregates (10-minute TTL)
 * - Invalidate on sentiment correction or new review sync
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final BrandService brandService;
    private final ReviewSourceService reviewSourceService;
    private final ReviewService reviewService;
    private final SyncJobService syncJobService;
    private final ReviewRepository reviewRepository;
    private final ReviewSourceRepository reviewSourceRepository;

    /**
     * Get dashboard view with aggregated metrics.
     * API: GET /api/brands/{brandId}/dashboard (Section 8.1)
     *
     * Response includes:
     * - List of all review sources with basic info
     * - Aggregated metrics across all sources (when sourceId is null)
     * - Summary section (avg rating, sentiment %, text summary)
     * - Recent negative reviews preview
     *
     * Query Parameters:
     * - sourceId (optional): Filter by specific review source
     *   - If provided: Show metrics for that source only
     *   - If null: Show aggregated metrics across all sources (US-006: "All locations")
     *
     * Business Logic:
     * - Calculate metrics in real-time (future: use dashboard_aggregates table)
     * - Use cache for performance (10-minute TTL)
     * - Dashboard must load in <4 seconds
     *
     * @param brandId Brand ID
     * @param sourceId Optional review source ID (null = all sources)
     * @param userId User ID from JWT token
     * @return DashboardResponse with aggregated metrics
     * @throws ResourceNotFoundException if brand or source not found
     * @throws ResourceAccessDeniedException if user doesn't own brand
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long brandId, Long sourceId, Long userId) {
        log.debug("Getting dashboard for brand ID: {}, sourceId: {}", brandId, sourceId);

        // Verify user owns the brand
        Brand brand = brandService.findByIdAndUserIdOrThrow(brandId, userId);

        // Get all review sources for the brand
        List<ReviewSource> allSources = reviewSourceRepository.findByBrandId(brandId);

        if (allSources.isEmpty()) {
            // No sources configured yet - return empty dashboard
            log.info("No review sources found for brand ID: {}", brandId);
            return buildEmptyDashboard(brand);
        }

        // Determine which sources to include in metrics
        List<ReviewSource> sourcesToAggregate;
        if (sourceId != null) {
            // Single source view
            ReviewSource source = reviewSourceService.findByIdAndBrandIdOrThrow(sourceId, brandId);
            sourcesToAggregate = List.of(source);
        } else {
            // All sources aggregated view (US-006: "All locations")
            sourcesToAggregate = allSources;
        }

        // Calculate aggregated metrics
        DashboardMetrics metrics = calculateDashboardMetrics(sourcesToAggregate);

        // Build source summaries
        List<SourceSummary> sourceSummaries = allSources.stream()
            .map(this::buildSourceSummary)
            .collect(Collectors.toList());

        // Get recent negative reviews (preview)
        List<ReviewResponse> recentNegativeReviews = getRecentNegativeReviews(sourcesToAggregate, 5);

        // Build summary text
        String summaryText = generateSummaryText(metrics);

        log.info("Dashboard loaded for brand {}: sources={}, avgRating={}, totalReviews={}",
            brandId, sourceSummaries.size(), metrics.getAverageRating(), metrics.getTotalReviews());

        return DashboardResponse.builder()
            .brandId(brandId)
            .brandName(brand.getBrandName())
            .selectedSourceId(sourceId)
            .sources(sourceSummaries)
            .metrics(metrics)
            .summaryText(summaryText)
            .recentNegativeReviews(recentNegativeReviews)
            .lastUpdated(Instant.now().atZone(ZoneId.of("UTC")))
            .build();
    }

    /**
     * Get summary for a specific review source.
     * API: GET /api/brands/{brandId}/review-sources/{sourceId}/summary (Section 8.2)
     *
     * Response includes:
     * - Source details (type, URL, active status)
     * - Metrics (avg rating, total reviews, sentiment breakdown)
     * - Last sync status and timestamp
     * - Recent negative reviews
     *
     * @param brandId Brand ID
     * @param sourceId Review source ID
     * @param userId User ID from JWT token
     * @return SourceSummaryResponse with detailed metrics
     * @throws ResourceNotFoundException if brand or source not found
     * @throws ResourceAccessDeniedException if user doesn't own brand
     */
    @Transactional(readOnly = true)
    public SourceSummaryResponse getSourceSummary(Long brandId, Long sourceId, Long userId) {
        log.debug("Getting source summary for source ID: {} (brand: {})", sourceId, brandId);

        // Verify user owns the brand
        brandService.findByIdAndUserIdOrThrow(brandId, userId);

        // Verify source belongs to brand
        ReviewSource source = reviewSourceService.findByIdAndBrandIdOrThrow(sourceId, brandId);

        // Calculate metrics for this source
        DashboardMetrics metrics = calculateDashboardMetrics(List.of(source));

        // Get recent negative reviews
        List<ReviewResponse> recentNegativeReviews = reviewService.getRecentNegativeReviews(sourceId, 5);

        // Get last sync job info
        SyncJob lastSyncJob = syncJobService.getInitialImportJob(sourceId);
        LastSyncInfo lastSyncInfo = lastSyncJob != null ? buildLastSyncInfo(lastSyncJob) : null;

        log.info("Source summary loaded: sourceId={}, type={}, avgRating={}, totalReviews={}",
            sourceId, source.getSourceType(), metrics.getAverageRating(), metrics.getTotalReviews());

        return SourceSummaryResponse.builder()
            .sourceId(sourceId)
            .sourceType(source.getSourceType())
            .profileUrl(source.getProfileUrl())
            .isActive(source.getIsActive())
            .metrics(metrics)
            .lastSync(lastSyncInfo)
            .recentNegativeReviews(recentNegativeReviews)
            .build();
    }

    // ========== Helper Methods ==========

    /**
     * Calculate dashboard metrics for given review sources.
     *
     * @param sources List of review sources to aggregate
     * @return DashboardMetrics with calculated values
     */
    private DashboardMetrics calculateDashboardMetrics(List<ReviewSource> sources) {
        List<Long> sourceIds = sources.stream()
            .map(ReviewSource::getId)
            .collect(Collectors.toList());

        if (sourceIds.isEmpty()) {
            return buildEmptyMetrics();
        }

        // Calculate metrics across all specified sources
        long totalReviews = 0;
        double totalRatingSum = 0.0;
        long positiveCount = 0;
        long negativeCount = 0;
        long neutralCount = 0;
        long rating5Count = 0;
        long rating4Count = 0;
        long rating3Count = 0;
        long rating2Count = 0;
        long rating1Count = 0;

        for (Long sourceId : sourceIds) {
            long sourceReviewCount = reviewRepository.countByReviewSourceId(sourceId);
            totalReviews += sourceReviewCount;

            Double avgRating = reviewRepository.calculateAverageRating(sourceId);
            if (avgRating != null) {
                totalRatingSum += avgRating * sourceReviewCount;
            }

            positiveCount += reviewRepository.countByReviewSourceIdAndSentiment(sourceId, Sentiment.POSITIVE);
            negativeCount += reviewRepository.countByReviewSourceIdAndSentiment(sourceId, Sentiment.NEGATIVE);
            neutralCount += reviewRepository.countByReviewSourceIdAndSentiment(sourceId, Sentiment.NEUTRAL);

            rating5Count += reviewRepository.countByReviewSourceIdAndRating(sourceId, (short) 5);
            rating4Count += reviewRepository.countByReviewSourceIdAndRating(sourceId, (short) 4);
            rating3Count += reviewRepository.countByReviewSourceIdAndRating(sourceId, (short) 3);
            rating2Count += reviewRepository.countByReviewSourceIdAndRating(sourceId, (short) 2);
            rating1Count += reviewRepository.countByReviewSourceIdAndRating(sourceId, (short) 1);
        }

        double averageRating = totalReviews > 0 ? totalRatingSum / totalReviews : 0.0;
        double positivePercentage = totalReviews > 0 ? (double) positiveCount / totalReviews * 100 : 0.0;
        double negativePercentage = totalReviews > 0 ? (double) negativeCount / totalReviews * 100 : 0.0;
        double neutralPercentage = totalReviews > 0 ? (double) neutralCount / totalReviews * 100 : 0.0;

        return DashboardMetrics.builder()
            .totalReviews(totalReviews)
            .averageRating(Math.round(averageRating * 10.0) / 10.0) // Round to 1 decimal
            .positiveCount(positiveCount)
            .negativeCount(negativeCount)
            .neutralCount(neutralCount)
            .positivePercentage(Math.round(positivePercentage * 10.0) / 10.0)
            .negativePercentage(Math.round(negativePercentage * 10.0) / 10.0)
            .neutralPercentage(Math.round(neutralPercentage * 10.0) / 10.0)
            .rating5Count(rating5Count)
            .rating4Count(rating4Count)
            .rating3Count(rating3Count)
            .rating2Count(rating2Count)
            .rating1Count(rating1Count)
            .build();
    }

    /**
     * Build source summary for a review source.
     *
     * @param source Review source entity
     * @return SourceSummary with basic info
     */
    private SourceSummary buildSourceSummary(ReviewSource source) {
        long totalReviews = reviewRepository.countByReviewSourceId(source.getId());
        Double avgRating = reviewRepository.calculateAverageRating(source.getId());

        return SourceSummary.builder()
            .sourceId(source.getId())
            .sourceType(source.getSourceType())
            .profileUrl(source.getProfileUrl())
            .isActive(source.getIsActive())
            .totalReviews(totalReviews)
            .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
            .lastSyncAt(source.getLastSyncAt() != null ? source.getLastSyncAt().atZone(ZoneId.of("UTC")) : null)
            .lastSyncStatus(source.getLastSyncStatus())
            .build();
    }

    /**
     * Build last sync info from sync job.
     *
     * @param syncJob Sync job entity
     * @return LastSyncInfo with job details
     */
    private LastSyncInfo buildLastSyncInfo(SyncJob syncJob) {
        return LastSyncInfo.builder()
            .jobId(syncJob.getId())
            .status(syncJob.getStatus())
            .completedAt(syncJob.getCompletedAt() != null ? syncJob.getCompletedAt().atZone(ZoneId.of("UTC")) : null)
            .reviewsFetched(syncJob.getReviewsFetched())
            .reviewsNew(syncJob.getReviewsNew())
            .errorMessage(syncJob.getErrorMessage())
            .build();
    }

    /**
     * Get recent negative reviews across multiple sources.
     *
     * @param sources List of review sources
     * @param limit Maximum number of reviews to return
     * @return List of recent negative reviews
     */
    private List<ReviewResponse> getRecentNegativeReviews(List<ReviewSource> sources, int limit) {
        List<ReviewResponse> allNegativeReviews = new ArrayList<>();

        for (ReviewSource source : sources) {
            List<ReviewResponse> sourceNegativeReviews = reviewService.getRecentNegativeReviews(source.getId(), limit);
            allNegativeReviews.addAll(sourceNegativeReviews);
        }

        // Sort by published date descending and limit
        return allNegativeReviews.stream()
            .sorted((r1, r2) -> r2.getPublishedAt().compareTo(r1.getPublishedAt()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Generate summary text from metrics.
     * Example: "75% positive reviews; customers praise speed but complain about prices"
     *
     * @param metrics Dashboard metrics
     * @return Summary text
     */
    private String generateSummaryText(DashboardMetrics metrics) {
        if (metrics.getTotalReviews() == 0) {
            return "No reviews available yet.";
        }

        StringBuilder summary = new StringBuilder();

        // Sentiment summary
        if (metrics.getPositivePercentage() > 60) {
            summary.append(String.format("%.0f%% positive reviews. ", metrics.getPositivePercentage()));
        } else if (metrics.getNegativePercentage() > 40) {
            summary.append(String.format("%.0f%% negative reviews. ", metrics.getNegativePercentage()));
        } else {
            summary.append("Mixed sentiment across reviews. ");
        }

        // Rating summary
        summary.append(String.format("Average rating: %.1f/5.0 based on %d reviews.",
            metrics.getAverageRating(), metrics.getTotalReviews()));

        // TODO: In future, use AI-generated summary from ai_summaries table

        return summary.toString();
    }

    /**
     * Build empty dashboard for brands with no sources.
     *
     * @param brand Brand entity
     * @return Empty dashboard response
     */
    private DashboardResponse buildEmptyDashboard(Brand brand) {
        return DashboardResponse.builder()
            .brandId(brand.getId())
            .brandName(brand.getBrandName())
            .selectedSourceId(null)
            .sources(new ArrayList<>())
            .metrics(buildEmptyMetrics())
            .summaryText("No review sources configured yet. Add your first source to start monitoring reviews.")
            .recentNegativeReviews(new ArrayList<>())
            .lastUpdated(Instant.now().atZone(ZoneId.of("UTC")))
            .build();
    }

    /**
     * Build empty metrics (all zeros).
     *
     * @return Empty dashboard metrics
     */
    private DashboardMetrics buildEmptyMetrics() {
        return DashboardMetrics.builder()
            .totalReviews(0L)
            .averageRating(0.0)
            .positiveCount(0L)
            .negativeCount(0L)
            .neutralCount(0L)
            .positivePercentage(0.0)
            .negativePercentage(0.0)
            .neutralPercentage(0.0)
            .rating5Count(0L)
            .rating4Count(0L)
            .rating3Count(0L)
            .rating2Count(0L)
            .rating1Count(0L)
            .build();
    }
}
