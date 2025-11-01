package com.morawski.dev.backend.service;

import com.morawski.dev.backend.dto.common.*;
import com.morawski.dev.backend.dto.review.*;
import com.morawski.dev.backend.entity.Review;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.entity.SentimentChange;
import com.morawski.dev.backend.entity.User;
import com.morawski.dev.backend.exception.ResourceAccessDeniedException;
import com.morawski.dev.backend.exception.ResourceNotFoundException;
import com.morawski.dev.backend.exception.ValidationException;
import com.morawski.dev.backend.mapper.ReviewMapper;
import com.morawski.dev.backend.mapper.SentimentChangeMapper;
import com.morawski.dev.backend.repository.ReviewRepository;
import com.morawski.dev.backend.repository.SentimentChangeRepository;
import com.morawski.dev.backend.util.Constants;
import com.morawski.dev.backend.util.DateTimeUtils;
import com.morawski.dev.backend.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for review operations.
 * Handles review listing, filtering, and sentiment correction.
 *
 * API Endpoints (Section 7):
 * - GET /api/brands/{brandId}/reviews (Section 7.1)
 * - GET /api/brands/{brandId}/reviews/{reviewId} (Section 7.2)
 * - PATCH /api/brands/{brandId}/reviews/{reviewId}/sentiment (Section 7.3)
 *
 * User Stories:
 * - US-004: Viewing Aggregated Reviews
 * - US-005: Filtering Negative Reviews
 * - US-006: Switching Between Locations
 * - US-007: Manual Sentiment Correction
 *
 * Performance Requirement:
 * - Dashboard must load in <4 seconds
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final SentimentChangeRepository sentimentChangeRepository;
    private final BrandService brandService;
    private final ReviewSourceService reviewSourceService;
    private final UserService userService;
    private final ReviewMapper reviewMapper;
    private final SentimentChangeMapper sentimentChangeMapper;

    /**
     * Get reviews with filtering and pagination.
     * API: GET /api/brands/{brandId}/reviews (Section 7.1)
     *
     * Query Parameters:
     * - sourceId (optional): Filter by specific review source. If omitted, returns reviews from all sources (US-006)
     * - sentiment (optional): Filter by sentiment (comma-separated: POSITIVE,NEGATIVE,NEUTRAL)
     * - rating (optional): Filter by star rating (comma-separated: 1,2,3,4,5) (US-005)
     * - startDate (optional): Filter reviews published after this date
     * - endDate (optional): Filter reviews published before this date
     * - page (optional): Page number (0-indexed). Default: 0
     * - size (optional): Items per page. Default: 20, Max: 100
     * - sort (optional): Sort field and direction. Default: publishedAt,desc
     *
     * Business Logic:
     * - Use idx_reviews_composite_filter index for multi-criteria filtering (performance)
     * - Use idx_reviews_negative partial index when filtering rating<=2 (US-005 optimization)
     * - Filter deleted_at IS NULL
     * - Log activity: VIEW_DASHBOARD on first page load
     *
     * @param brandId Brand ID
     * @param sourceId Optional review source ID (null = all sources)
     * @param sentiments Optional list of sentiments to filter by
     * @param ratings Optional list of ratings to filter by
     * @param startDate Optional start date
     * @param endDate Optional end date
     * @param page Page number (0-indexed)
     * @param size Page size (max 100)
     * @param sortBy Sort field (default: publishedAt)
     * @param sortDirection Sort direction (default: desc)
     * @param userId User ID from JWT token
     * @return ReviewListResponse with paginated reviews
     * @throws ResourceNotFoundException if brand not found
     * @throws ResourceAccessDeniedException if user doesn't own brand
     * @throws ValidationException if parameters are invalid
     */
    @Transactional(readOnly = true)
    public ReviewListResponse getReviews(
        Long brandId,
        Long sourceId,
        List<Sentiment> sentiments,
        List<Short> ratings,
        LocalDate startDate,
        LocalDate endDate,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection,
        Long userId
    ) {
        log.debug("Getting reviews for brand ID: {}, sourceId: {}", brandId, sourceId);

        // Verify user owns the brand
        brandService.findByIdAndUserIdOrThrow(brandId, userId);

        // Validate pagination parameters
        int validatedPage = page != null ? page : Constants.DEFAULT_PAGE_NUMBER;
        int validatedSize = size != null ? Math.min(size, Constants.MAX_PAGE_SIZE) : Constants.DEFAULT_PAGE_SIZE;

        if (validatedPage < 0 || validatedSize <= 0) {
            throw new ValidationException("Invalid pagination parameters");
        }

        // Validate date range if provided
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ValidationException("End date must be after or equal to start date");
        }

        // Build sort
        Sort sort = buildSort(sortBy != null ? sortBy : "publishedAt", sortDirection);
        Pageable pageable = PageRequest.of(validatedPage, validatedSize, sort);

        // Convert LocalDate to Instant for repository queries
        Instant startInstant = startDate != null
            ? startDate.atStartOfDay(ZoneId.of("UTC")).toInstant()
            : null;
        Instant endInstant = endDate != null
            ? endDate.atTime(23, 59, 59).atZone(ZoneId.of("UTC")).toInstant()
            : null;

        // Query reviews based on filters
        Page<Review> reviewPage;

        if (sourceId != null) {
            // Verify source belongs to brand
            ReviewSource source = reviewSourceService.findByIdAndBrandIdOrThrow(sourceId, brandId);

            // Filter by specific source
            reviewPage = reviewRepository.findByMultipleFilters(
                sourceId,
                sentiments,
                ratings,
                startInstant,
                endInstant,
                pageable
            );
        } else {
            // Aggregate view - all sources for brand (US-006: "All locations")
            reviewPage = reviewRepository.findByBrandIdAndMultipleFilters(
                brandId,
                sentiments,
                ratings,
                startInstant,
                endInstant,
                pageable
            );
        }

        // Map to DTOs
        List<ReviewResponse> reviewResponses = reviewPage.getContent().stream()
            .map(reviewMapper::toReviewResponse)
            .collect(Collectors.toList());

        // Build pagination response
        PaginationResponse pagination = PaginationResponse.builder()
            .currentPage(validatedPage)
            .pageSize(validatedSize)
            .totalItems(reviewPage.getTotalElements())
            .totalPages(reviewPage.getTotalPages())
            .hasNext(reviewPage.hasNext())
            .hasPrevious(reviewPage.hasPrevious())
            .build();

        // Build filter response
        FilterResponse filters = FilterResponse.builder()
            .sourceId(sourceId)
            .sentiment(sentiments != null
                ? sentiments.stream().map(Enum::name).collect(Collectors.toList())
                : null)
            .rating(ratings != null
                ? ratings.stream().map(Short::intValue).collect(Collectors.toList())
                : null)
            .startDate(startDate)
            .endDate(endDate)
            .build();

        // TODO: Log activity: VIEW_DASHBOARD on first page load (requires UserActivityLog)

        log.info("Retrieved {} review(s) for brand {}, page {}/{}",
            reviewResponses.size(), brandId, validatedPage, reviewPage.getTotalPages());

        return ReviewListResponse.builder()
            .reviews(reviewResponses)
            .pagination(pagination)
            .filters(filters)
            .build();
    }

    /**
     * Get review by ID with sentiment change history.
     * API: GET /api/brands/{brandId}/reviews/{reviewId} (Section 7.2)
     *
     * @param brandId Brand ID
     * @param reviewId Review ID
     * @param userId User ID from JWT token
     * @return ReviewDetailResponse with full review details
     * @throws ResourceNotFoundException if review not found
     * @throws ResourceAccessDeniedException if user doesn't own brand
     */
    @Transactional(readOnly = true)
    public ReviewDetailResponse getReviewById(Long brandId, Long reviewId, Long userId) {
        log.debug("Getting review ID: {} for brand ID: {}", reviewId, brandId);

        // Verify user owns the brand
        brandService.findByIdAndUserIdOrThrow(brandId, userId);

        Review review = findByIdOrThrow(reviewId);

        // Verify review belongs to this brand
        if (!review.getReviewSource().getBrand().getId().equals(brandId)) {
            throw new ResourceAccessDeniedException("This review does not belong to the specified brand");
        }

        // Get sentiment change history
        List<SentimentChange> sentimentChanges = sentimentChangeRepository
            .findByReviewIdOrderByChangedAtDesc(reviewId);

        List<SentimentChangeResponse> changeHistory = sentimentChanges.stream()
            .map(sentimentChangeMapper::toSentimentChangeResponse)
            .collect(Collectors.toList());

        // Build detailed response
        ReviewDetailResponse response = ReviewDetailResponse.builder()
            .reviewId(review.getId())
            .sourceId(review.getReviewSource().getId())
            .sourceType(review.getReviewSource().getSourceType())
            .externalReviewId(review.getExternalReviewId())
            .content(review.getContent())
            .contentHash(review.getContentHash())
            .authorName(review.getAuthorName())
            .rating(review.getRating().intValue())
            .sentiment(review.getSentiment())
            .sentimentConfidence(review.getSentimentConfidence())
            .publishedAt(review.getPublishedAt().atZone(ZoneId.of("UTC")))
            .fetchedAt(review.getFetchedAt().atZone(ZoneId.of("UTC")))
            .createdAt(review.getCreatedAt().atZone(ZoneId.of("UTC")))
            .updatedAt(review.getUpdatedAt().atZone(ZoneId.of("UTC")))
            .sentimentChangeHistory(changeHistory)
            .build();

        log.info("Retrieved review: id={}, sentiment={}, rating={}",
            reviewId, review.getSentiment(), review.getRating());

        return response;
    }

    /**
     * Update review sentiment (manual correction by user).
     * API: PATCH /api/brands/{brandId}/reviews/{reviewId}/sentiment (Section 7.3)
     *
     * Business Logic (from API Plan Section 7.3):
     * 1. Verify user owns the brand containing this review
     * 2. Record change in sentiment_changes table:
     *    - old_sentiment: Current review.sentiment
     *    - new_sentiment: Requested sentiment
     *    - changed_by_user_id: JWT userId
     *    - change_reason: 'USER_CORRECTION'
     * 3. Update reviews.sentiment and reviews.updated_at
     * 4. Invalidate cached dashboard aggregates for this review's source
     * 5. Trigger async recalculation of dashboard_aggregates for affected date
     * 6. Log activity: SENTIMENT_CORRECTED
     *
     * Cache Invalidation:
     * - Clear cache key: dashboard:brand:{brandId}
     * - Clear cache key: summary:source:{reviewSourceId}
     *
     * @param brandId Brand ID
     * @param reviewId Review ID
     * @param userId User ID from JWT token
     * @param request Update request containing new sentiment
     * @return UpdateReviewSentimentResponse with change details
     * @throws ResourceNotFoundException if review not found
     * @throws ResourceAccessDeniedException if user doesn't own brand
     */
    @Transactional
    public UpdateReviewSentimentResponse updateReviewSentiment(
        Long brandId,
        Long reviewId,
        Long userId,
        UpdateReviewSentimentRequest request
    ) {
        log.info("Updating sentiment for review ID: {}, new sentiment: {}", reviewId, request.getSentiment());

        // Verify user owns the brand
        brandService.findByIdAndUserIdOrThrow(brandId, userId);

        Review review = findByIdOrThrow(reviewId);

        // Verify review belongs to this brand
        if (!review.getReviewSource().getBrand().getId().equals(brandId)) {
            throw new ResourceAccessDeniedException("This review does not belong to the specified brand");
        }

        Sentiment oldSentiment = review.getSentiment();
        Sentiment newSentiment = request.getSentiment();

        // Only proceed if sentiment actually changed
        if (oldSentiment.equals(newSentiment)) {
            log.debug("Sentiment unchanged for review {}: {}", reviewId, oldSentiment);
            // Return current state without changes
            return buildSentimentChangeResponse(review, oldSentiment, null);
        }

        User user = userService.findByIdOrThrow(userId);

        // Create sentiment change record
        SentimentChange sentimentChange = SentimentChange.builder()
            .review(review)
            .oldSentiment(oldSentiment)
            .newSentiment(newSentiment)
            .changedByUser(user)
            .changeReason(ChangeReason.USER_CORRECTION)
            .changedAt(Instant.now())
            .build();

        sentimentChangeRepository.save(sentimentChange);

        // Update review sentiment
        review.setSentiment(newSentiment);
        reviewRepository.save(review);

        // TODO: Invalidate cache
        // cacheManager.evict(Constants.CACHE_PREFIX_DASHBOARD + brandId);
        // cacheManager.evict(Constants.CACHE_PREFIX_SUMMARY + review.getReviewSource().getId());

        // TODO: Trigger async recalculation of dashboard_aggregates

        // TODO: Log activity: SENTIMENT_CORRECTED

        log.info("Sentiment updated for review {}: {} -> {} (user: {})",
            reviewId, oldSentiment, newSentiment, userId);

        return buildSentimentChangeResponse(review, oldSentiment, sentimentChange.getId());
    }

    // ========== Helper Methods ==========

    /**
     * Find review by ID.
     *
     * @param reviewId Review ID
     * @return Review entity
     * @throws ResourceNotFoundException if review not found
     */
    @Transactional(readOnly = true)
    public Review findByIdOrThrow(Long reviewId) {
        return reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
    }

    /**
     * Build sort object from field and direction.
     *
     * @param sortBy Sort field (publishedAt, rating, etc.)
     * @param sortDirection Sort direction (asc, desc)
     * @return Sort object
     */
    private Sort buildSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        // Validate sort field
        String validatedSortBy = switch (sortBy.toLowerCase()) {
            case "publishedat", "published" -> "publishedAt";
            case "rating" -> "rating";
            case "createdat", "created" -> "createdAt";
            default -> "publishedAt"; // Default sort
        };

        return Sort.by(direction, validatedSortBy);
    }

    /**
     * Build UpdateReviewSentimentResponse.
     *
     * @param review Updated review
     * @param previousSentiment Previous sentiment value
     * @param sentimentChangeId Sentiment change record ID (null if no change)
     * @return Response DTO
     */
    private UpdateReviewSentimentResponse buildSentimentChangeResponse(
        Review review,
        Sentiment previousSentiment,
        Long sentimentChangeId
    ) {
        return UpdateReviewSentimentResponse.builder()
            .reviewId(review.getId())
            .sentiment(review.getSentiment())
            .previousSentiment(previousSentiment)
            .updatedAt(review.getUpdatedAt().atZone(ZoneId.of("UTC")))
            .sentimentChangeId(sentimentChangeId)
            .build();
    }

    /**
     * Get most recent negative reviews for dashboard preview.
     * Used in dashboard summary.
     *
     * @param reviewSourceId Review source ID
     * @param limit Maximum number of reviews to return
     * @return List of recent negative reviews
     */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getRecentNegativeReviews(Long reviewSourceId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Review> negativeReviews = reviewRepository.findTopNRecentNegativeReviews(
            reviewSourceId,
            pageable
        );

        return negativeReviews.stream()
            .map(reviewMapper::toReviewResponse)
            .collect(Collectors.toList());
    }

    /**
     * Calculate average rating for a review source.
     *
     * @param reviewSourceId Review source ID
     * @return Average rating (1.0 to 5.0)
     */
    @Transactional(readOnly = true)
    public Double calculateAverageRating(Long reviewSourceId) {
        return reviewRepository.calculateAverageRating(reviewSourceId);
    }

    /**
     * Count reviews by sentiment for a review source.
     *
     * @param reviewSourceId Review source ID
     * @param sentiment Sentiment to count
     * @return Count of reviews
     */
    @Transactional(readOnly = true)
    public long countBySentiment(Long reviewSourceId, Sentiment sentiment) {
        return reviewRepository.countByReviewSourceIdAndSentiment(reviewSourceId, sentiment);
    }

    /**
     * Count reviews by rating for a review source.
     *
     * @param reviewSourceId Review source ID
     * @param rating Rating to count
     * @return Count of reviews
     */
    @Transactional(readOnly = true)
    public long countByRating(Long reviewSourceId, Short rating) {
        return reviewRepository.countByReviewSourceIdAndRating(reviewSourceId, rating);
    }
}
