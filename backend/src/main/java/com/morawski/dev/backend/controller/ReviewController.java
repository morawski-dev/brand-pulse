package com.morawski.dev.backend.controller;

import com.morawski.dev.backend.dto.common.Sentiment;
import com.morawski.dev.backend.dto.review.*;
import com.morawski.dev.backend.security.SecurityUtils;
import com.morawski.dev.backend.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for review management endpoints.
 * Handles listing reviews, viewing review details, and updating sentiment.
 *
 * API Plan Section 7: Review Endpoints
 * Base URL: /api/brands/{brandId}/reviews
 *
 * Endpoints:
 * - GET / - List reviews with filtering (Section 7.1, US-004, US-005, US-006)
 * - GET /{reviewId} - Get review by ID (Section 7.2)
 * - PATCH /{reviewId}/sentiment - Update review sentiment (Section 7.3, US-007)
 */
@RestController
@RequestMapping("/api/brands/{brandId}/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * List reviews with filtering and pagination.
     * API: GET /api/brands/{brandId}/reviews (Section 7.1)
     * User Stories: US-004 (Viewing), US-005 (Filtering Negative), US-006 (Switching Locations)
     *
     * Query Parameters:
     * - sourceId (optional): Filter by specific review source
     * - sentiment (optional): Filter by sentiment (POSITIVE, NEGATIVE, NEUTRAL) - comma-separated
     * - rating (optional): Filter by star rating (1-5) - comma-separated
     * - startDate (optional): Filter reviews published after this date (ISO 8601 date format: yyyy-MM-dd)
     * - endDate (optional): Filter reviews published before this date (ISO 8601 date format: yyyy-MM-dd)
     * - page (optional): Page number (0-indexed), default: 0
     * - size (optional): Items per page, default: 20, max: 100
     * - sort (optional): Sort field and direction, default: publishedAt,desc
     *
     * Business Logic:
     * - Use indexes for performance
     * - Filter deleted_at IS NULL
     * - Dashboard must load in <4 seconds
     * - Use Spring Cache with Caffeine (10-minute TTL)
     *
     * Success Response: 200 OK with paginated reviews
     * Error Responses:
     * - 400 Bad Request: Invalid filter values, invalid date format
     * - 403 Forbidden: User doesn't own this brand
     *
     * @param brandId Brand ID
     * @param sourceId Optional review source ID filter
     * @param sentiment Optional sentiment filter (comma-separated)
     * @param rating Optional rating filter (comma-separated)
     * @param startDate Optional start date filter (yyyy-MM-dd)
     * @param endDate Optional end date filter (yyyy-MM-dd)
     * @param page Page number (default: 0)
     * @param size Page size (default: 20, max: 100)
     * @param sort Sort criteria (default: publishedAt,desc)
     * @return ReviewListResponse with paginated reviews
     */
    @GetMapping
    public ResponseEntity<ReviewListResponse> getReviews(
        @PathVariable Long brandId,
        @RequestParam(required = false) Long sourceId,
        @RequestParam(required = false) List<Sentiment> sentiment,
        @RequestParam(required = false) List<Integer> rating,
        @RequestParam(required = false) LocalDate startDate,
        @RequestParam(required = false) LocalDate endDate,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        @RequestParam(defaultValue = "publishedAt,desc") String sort
    ) {
        log.info("GET /api/brands/{}/reviews - List reviews request received with filters", brandId);

        // Parse sort parameter
        String[] sortParams = sort.split(",");
        String sortBy = sortParams[0];
        String sortDirection = sortParams.length > 1 ? sortParams[1] : "desc";

        // Convert List<Integer> to List<Short> for ratings
        List<Short> ratingsAsShort = rating != null
            ? rating.stream().map(Integer::shortValue).collect(Collectors.toList())
            : null;

        Long userId = SecurityUtils.getCurrentUserId();
        ReviewListResponse response = reviewService.getReviews(
            brandId,
            sourceId,
            sentiment,
            ratingsAsShort,
            startDate,
            endDate,
            page,
            size,
            sortBy,
            sortDirection,
            userId
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get review by ID with full details.
     * API: GET /api/brands/{brandId}/reviews/{reviewId} (Section 7.2)
     *
     * Response includes sentiment change history for audit trail.
     *
     * Success Response: 200 OK with review details
     * Error Responses:
     * - 403 Forbidden: User doesn't own the brand containing this review
     * - 404 Not Found: Review doesn't exist
     *
     * @param brandId Brand ID
     * @param reviewId Review ID
     * @return ReviewDetailResponse with full review details
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewDetailResponse> getReviewById(
        @PathVariable Long brandId,
        @PathVariable Long reviewId
    ) {
        log.info("GET /api/brands/{}/reviews/{} - Get review by ID request received",
            brandId, reviewId);
        Long userId = SecurityUtils.getCurrentUserId();
        ReviewDetailResponse response = reviewService.getReviewById(brandId, reviewId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update review sentiment (manual correction).
     * API: PATCH /api/brands/{brandId}/reviews/{reviewId}/sentiment (Section 7.3)
     * User Story: US-007 - Manual Sentiment Correction
     *
     * Validation:
     * - sentiment: Required, must be 'POSITIVE', 'NEGATIVE', or 'NEUTRAL'
     *
     * Business Logic:
     * 1. Verify user owns the brand containing this review
     * 2. Record change in sentiment_changes table
     * 3. Update reviews.sentiment and reviews.updated_at
     * 4. Invalidate cached dashboard aggregates for this review's source
     * 5. Trigger async recalculation of dashboard_aggregates
     * 6. Log activity: SENTIMENT_CORRECTED
     *
     * Cache Invalidation:
     * - Clear cache key: dashboard:brand:{brandId}
     * - Clear cache key: summary:source:{reviewSourceId}
     *
     * Success Response: 200 OK with updated sentiment
     * Error Responses:
     * - 400 Bad Request: Invalid sentiment value
     * - 403 Forbidden: User doesn't own this review
     * - 404 Not Found: Review doesn't exist
     *
     * @param brandId Brand ID
     * @param reviewId Review ID
     * @param request Update sentiment request
     * @return UpdateReviewSentimentResponse with updated sentiment
     */
    @PatchMapping("/{reviewId}/sentiment")
    public ResponseEntity<UpdateReviewSentimentResponse> updateReviewSentiment(
        @PathVariable Long brandId,
        @PathVariable Long reviewId,
        @Valid @RequestBody UpdateReviewSentimentRequest request
    ) {
        log.info("PATCH /api/brands/{}/reviews/{}/sentiment - Update review sentiment request received",
            brandId, reviewId);
        Long userId = SecurityUtils.getCurrentUserId();
        UpdateReviewSentimentResponse response = reviewService.updateReviewSentiment(
            brandId,
            reviewId,
            userId,
            request
        );
        return ResponseEntity.ok(response);
    }
}
