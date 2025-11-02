package com.morawski.dev.backend.service.sync;

import com.morawski.dev.backend.dto.common.Sentiment;
import com.morawski.dev.backend.dto.sync.SyncResult;
import com.morawski.dev.backend.entity.Review;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handler for synchronizing reviews from Google My Business API.
 *
 * Responsibilities:
 * - Fetch reviews from Google API
 * - Detect duplicates using external_review_id
 * - Update existing reviews if content changed
 * - Create new reviews with AI sentiment classification
 * - Track sync statistics
 *
 * User Stories:
 * - US-003: Initial 90-day import when source is configured
 * - US-008: Manual refresh (max once per 24h)
 *
 * API Plan Section 15.3: Background Jobs
 * - Initial 90-day import (triggered when source created)
 * - Daily CRON sync (3:00 AM CET)
 * - Manual sync (user-triggered)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleReviewSyncHandler {

    private final ReviewRepository reviewRepository;
    // TODO: Add GoogleApiClient when implemented
    // private final GoogleApiClient googleApiClient;
    // TODO: Add OpenRouterClient for sentiment analysis
    // private final OpenRouterClient openRouterClient;

    /**
     * Sync reviews from Google for a specific review source.
     *
     * API Plan Section 13.2: Duplicate Review Prevention
     * - Same review cannot be imported twice (same source + external ID)
     * - Update if content changed (based on content_hash)
     * - Skip if duplicate
     *
     * @param reviewSource Review source to sync
     * @param startDate Start date for fetching reviews (for initial 90-day import)
     * @param endDate End date for fetching reviews
     * @return SyncResult with statistics
     */
    @Transactional
    public SyncResult syncReviews(ReviewSource reviewSource, LocalDate startDate, LocalDate endDate) {
        log.info("Starting Google review sync for source ID: {} (from {} to {})",
            reviewSource.getId(), startDate, endDate);

        try {
            // Fetch reviews from Google API
            List<GoogleReviewDTO> googleReviews = fetchReviewsFromGoogle(reviewSource, startDate, endDate);

            int reviewsFetched = googleReviews.size();
            int reviewsNew = 0;
            int reviewsUpdated = 0;

            // Process each review
            for (GoogleReviewDTO googleReview : googleReviews) {
                // Check if review already exists
                Optional<Review> existingReview = reviewRepository
                    .findByReviewSourceIdAndExternalReviewId(
                        reviewSource.getId(),
                        googleReview.getExternalId()
                    );

                if (existingReview.isPresent()) {
                    // Review exists - check if content changed
                    String newContentHash = calculateContentHash(googleReview.getContent());

                    if (!existingReview.get().getContentHash().equals(newContentHash)) {
                        // Content changed - update review
                        updateExistingReview(existingReview.get(), googleReview, newContentHash);
                        reviewsUpdated++;
                        log.debug("Updated review: externalId={}", googleReview.getExternalId());
                    }
                    // else: Skip - duplicate with same content
                } else {
                    // New review - create
                    createNewReview(reviewSource, googleReview);
                    reviewsNew++;
                    log.debug("Created new review: externalId={}", googleReview.getExternalId());
                }
            }

            log.info("Google sync completed for source {}: fetched={}, new={}, updated={}",
                reviewSource.getId(), reviewsFetched, reviewsNew, reviewsUpdated);

            return SyncResult.success(reviewsFetched, reviewsNew, reviewsUpdated);

        } catch (Exception e) {
            log.error("Google sync failed for source {}: {}", reviewSource.getId(), e.getMessage(), e);
            return SyncResult.failure("Google API error: " + e.getMessage());
        }
    }

    /**
     * Fetch reviews from Google My Business API.
     *
     * TODO: Implement actual Google API integration.
     * For now, returns mock data for testing.
     *
     * Google API Integration Plan:
     * 1. Decrypt credentials from reviewSource.credentialsEncrypted
     * 2. Extract Google API key or OAuth token
     * 3. Call Google Places API: GET /maps/api/place/details/json
     * 4. Parse reviews from response
     * 5. Map to GoogleReviewDTO
     *
     * @param reviewSource Review source with API credentials
     * @param startDate Start date for filtering reviews
     * @param endDate End date for filtering reviews
     * @return List of Google reviews
     */
    private List<GoogleReviewDTO> fetchReviewsFromGoogle(
        ReviewSource reviewSource,
        LocalDate startDate,
        LocalDate endDate
    ) {
        log.debug("Fetching reviews from Google API for source: {}", reviewSource.getId());

        // TODO: Replace with actual Google API call
        // Example:
        // String apiKey = decryptCredentials(reviewSource.getCredentialsEncrypted());
        // String placeId = reviewSource.getExternalProfileId();
        // String url = String.format(
        //     "https://maps.googleapis.com/maps/api/place/details/json?place_id=%s&key=%s&fields=reviews",
        //     placeId, apiKey
        // );
        // ResponseEntity<GooglePlacesResponse> response = restTemplate.getForEntity(url, GooglePlacesResponse.class);
        // return mapToGoogleReviewDTOs(response.getBody().getReviews());

        // Mock data for development
        log.warn("Using mock Google API data - implement actual API integration");
        return createMockGoogleReviews();
    }

    /**
     * Create mock Google reviews for testing.
     * TODO: Remove when Google API integration is implemented.
     *
     * @return List of mock reviews
     */
    private List<GoogleReviewDTO> createMockGoogleReviews() {
        List<GoogleReviewDTO> mockReviews = new ArrayList<>();

        mockReviews.add(new GoogleReviewDTO(
            "google_review_001",
            "Excellent service! The staff was very friendly and professional.",
            "Jan Kowalski",
            (short) 5,
            Instant.now().minus(5, ChronoUnit.DAYS)
        ));

        mockReviews.add(new GoogleReviewDTO(
            "google_review_002",
            "Good experience overall, but a bit pricey.",
            "Anna Nowak",
            (short) 4,
            Instant.now().minus(10, ChronoUnit.DAYS)
        ));

        mockReviews.add(new GoogleReviewDTO(
            "google_review_003",
            "Terrible service. Had to wait 45 minutes. Not recommended!",
            "Piotr Wiśniewski",
            (short) 1,
            Instant.now().minus(2, ChronoUnit.DAYS)
        ));

        return mockReviews;
    }

    /**
     * Create new review entity from Google review data.
     *
     * Sentiment Classification Strategy:
     * - Rating 4-5: POSITIVE
     * - Rating 3: NEUTRAL
     * - Rating 1-2: NEGATIVE
     *
     * TODO: Replace with AI-based sentiment analysis using OpenRouterClient
     *
     * @param reviewSource Review source entity
     * @param googleReview Google review DTO
     */
    private void createNewReview(ReviewSource reviewSource, GoogleReviewDTO googleReview) {
        String contentHash = calculateContentHash(googleReview.getContent());
        Sentiment sentiment = classifySentiment(googleReview.getRating(), googleReview.getContent());

        Review review = Review.builder()
            .externalReviewId(googleReview.getExternalId())
            .content(googleReview.getContent())
            .contentHash(contentHash)
            .authorName(googleReview.getAuthorName())
            .rating(googleReview.getRating())
            .sentiment(sentiment)
            .sentimentConfidence(calculateSentimentConfidence(googleReview.getRating()))
            .publishedAt(googleReview.getPublishedAt())
            .fetchedAt(Instant.now())
            .reviewSource(reviewSource)
            .build();

        reviewRepository.save(review);
    }

    /**
     * Update existing review with new content.
     *
     * @param existingReview Existing review entity
     * @param googleReview Updated Google review data
     * @param newContentHash New content hash
     */
    private void updateExistingReview(Review existingReview, GoogleReviewDTO googleReview, String newContentHash) {
        // Re-classify sentiment if content changed
        Sentiment newSentiment = classifySentiment(googleReview.getRating(), googleReview.getContent());

        existingReview.setContent(googleReview.getContent());
        existingReview.setContentHash(newContentHash);
        existingReview.setRating(googleReview.getRating());
        existingReview.setSentiment(newSentiment);
        existingReview.setSentimentConfidence(calculateSentimentConfidence(googleReview.getRating()));
        existingReview.setFetchedAt(Instant.now());

        reviewRepository.save(existingReview);
    }

    /**
     * Classify sentiment based on rating and content.
     *
     * Simple heuristic (TODO: Replace with AI):
     * - Rating 4-5: POSITIVE
     * - Rating 3: NEUTRAL
     * - Rating 1-2: NEGATIVE
     *
     * Future: Use OpenRouterClient.analyzeSentiment(content) for AI-based classification
     *
     * @param rating Star rating (1-5)
     * @param content Review text content
     * @return Sentiment classification
     */
    private Sentiment classifySentiment(short rating, String content) {
        // TODO: Implement AI-based sentiment analysis
        // Example:
        // return openRouterClient.analyzeSentiment(content);

        // Simple heuristic for now
        if (rating >= 4) {
            return Sentiment.POSITIVE;
        } else if (rating == 3) {
            return Sentiment.NEUTRAL;
        } else {
            return Sentiment.NEGATIVE;
        }
    }

    /**
     * Calculate sentiment confidence score.
     *
     * For rating-based classification:
     * - Extreme ratings (1, 5): High confidence (0.90)
     * - Moderate ratings (2, 4): Medium confidence (0.75)
     * - Neutral rating (3): Low confidence (0.60)
     *
     * TODO: Replace with actual AI confidence from OpenRouterClient
     *
     * @param rating Star rating
     * @return Confidence score (0.0 to 1.0)
     */
    private BigDecimal calculateSentimentConfidence(short rating) {
        // TODO: Get actual confidence from AI model
        return switch (rating) {
            case 1, 5 -> BigDecimal.valueOf(0.90);
            case 2, 4 -> BigDecimal.valueOf(0.75);
            case 3 -> BigDecimal.valueOf(0.60);
            default -> BigDecimal.valueOf(0.50);
        };
    }

    /**
     * Calculate SHA-256 hash of review content for duplicate detection.
     *
     * API Plan Section 13.2: Duplicate Review Prevention
     * - Uses content_hash to detect content changes
     *
     * @param content Review content text
     * @return Hex-encoded SHA-256 hash
     */
    private String calculateContentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate content hash", e);
        }
    }

    /**
     * Convert byte array to hex string.
     *
     * @param bytes Byte array
     * @return Hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * DTO for Google review data.
     * Internal structure for mapping Google API response.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class GoogleReviewDTO {
        private String externalId;
        private String content;
        private String authorName;
        private Short rating;
        private Instant publishedAt;
    }
}
