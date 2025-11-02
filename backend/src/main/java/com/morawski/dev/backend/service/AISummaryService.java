package com.morawski.dev.backend.service;

import com.morawski.dev.backend.dto.dashboard.AISummaryResponse;
import com.morawski.dev.backend.entity.AISummary;
import com.morawski.dev.backend.entity.Review;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.exception.ResourceNotFoundException;
import com.morawski.dev.backend.mapper.AISummaryMapper;
import com.morawski.dev.backend.repository.AISummaryRepository;
import com.morawski.dev.backend.repository.ReviewRepository;
import com.morawski.dev.backend.repository.ReviewSourceRepository;
import com.morawski.dev.backend.util.Constants;
import com.morawski.dev.backend.util.OpenRouterClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Service for managing AI-generated summaries of customer reviews.
 *
 * Business Logic:
 * - Returns cached summary if valid (not expired)
 * - Generates new summary if no valid cache exists
 * - Summaries are cached for 24 hours to reduce API costs
 * - Invalidates cache when reviews are updated or sentiment corrected
 *
 * User Stories:
 * - US-004: Viewing Aggregated Reviews (AI text summary)
 *
 * API Endpoints:
 * - GET /api/dashboard/summary (includes AI summary)
 * - GET /api/dashboard/summary/ai?sourceId=X
 *
 * Success Metrics:
 * - AI accuracy target: 75% correct sentiment classifications
 * - Cost optimization: Minimize OpenRouter.ai API calls via caching
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AISummaryService {

    private final AISummaryRepository aiSummaryRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewSourceRepository reviewSourceRepository;
    private final OpenRouterClient openRouterClient;
    private final AISummaryMapper aiSummaryMapper;

    /**
     * Get AI summary for a review source.
     * Returns cached summary if valid, otherwise generates a new one.
     *
     * API: GET /api/dashboard/summary/ai?sourceId=X
     * Cache key: "summary:source:{sourceId}"
     * Cache TTL: 24 hours (AI_SUMMARY_CACHE_TTL_HOURS)
     *
     * @param reviewSourceId The review source ID
     * @return AISummaryResponse containing the summary
     * @throws ResourceNotFoundException if review source doesn't exist
     */
    @Cacheable(value = "summary", key = "'source:' + #reviewSourceId")
    @Transactional(readOnly = true)
    public AISummaryResponse getSummaryForSource(Long reviewSourceId) {
        log.info("Getting AI summary for review source: {}", reviewSourceId);

        // Verify review source exists
        ReviewSource reviewSource = reviewSourceRepository.findById(reviewSourceId)
            .orElseThrow(() -> new ResourceNotFoundException("ReviewSource", reviewSourceId));

        // Check for valid cached summary (valid_until > NOW())
        Instant now = Instant.now();
        var cachedSummary = aiSummaryRepository.findValidSummaryByReviewSourceId(reviewSourceId, now);

        if (cachedSummary.isPresent()) {
            log.info("Returning cached AI summary for source: {}", reviewSourceId);
            return aiSummaryMapper.toAISummaryResponse(cachedSummary.get());
        }

        // No valid summary exists - generate new one
        log.info("No valid cached summary found, generating new summary for source: {}", reviewSourceId);
        return generateAndSaveSummary(reviewSourceId);
    }

    /**
     * Generate a new AI summary for a review source.
     * Fetches recent reviews, calls AI service, and saves the result.
     *
     * Business Logic:
     * - Fetches last 100 reviews (AI_SUMMARY_REVIEW_COUNT)
     * - Calls OpenRouter.ai for summary generation
     * - Sets validity period to 24 hours (AI_SUMMARY_VALIDITY_HOURS)
     * - Saves summary to database
     *
     * @param reviewSourceId The review source ID
     * @return AISummaryResponse containing the generated summary
     */
    @Transactional
    public AISummaryResponse generateAndSaveSummary(Long reviewSourceId) {
        log.info("Generating new AI summary for review source: {}", reviewSourceId);

        // Fetch review source for relationship mapping
        ReviewSource reviewSource = reviewSourceRepository.findById(reviewSourceId)
            .orElseThrow(() -> new ResourceNotFoundException("ReviewSource", reviewSourceId));

        // Fetch recent reviews (last 100)
        PageRequest pageRequest = PageRequest.of(0, Constants.AI_SUMMARY_REVIEW_COUNT);
        Page<Review> recentReviewsPage = reviewRepository.findRecentByReviewSourceId(
            reviewSourceId,
            pageRequest
        );
        List<Review> recentReviews = recentReviewsPage.getContent();

        if (recentReviews.isEmpty()) {
            log.warn("No reviews found for source: {}, returning empty summary", reviewSourceId);
            return createEmptySummaryResponse(reviewSourceId);
        }

        // Generate summary using AI
        String summaryText = openRouterClient.generateSummary(recentReviews);
        int tokenCount = openRouterClient.getLastTokenCount();
        String modelUsed = openRouterClient.getModelName();

        // Create and save new summary
        AISummary summary = AISummary.builder()
            .reviewSource(reviewSource)
            .summaryText(summaryText)
            .modelUsed(modelUsed)
            .tokenCount(tokenCount)
            .generatedAt(Instant.now())
            .build();

        // Set validity period (24 hours from now)
        summary.setValidityPeriod(Constants.AI_SUMMARY_VALIDITY_HOURS);

        AISummary savedSummary = aiSummaryRepository.save(summary);
        log.info("AI summary generated and saved with ID: {} for source: {}, tokens used: {}",
            savedSummary.getId(), reviewSourceId, tokenCount);

        return aiSummaryMapper.toAISummaryResponse(savedSummary);
    }

    /**
     * Asynchronously generate AI summary for a review source.
     * Used when no valid summary exists and we want to generate it in the background.
     *
     * Use case: Return 202 Accepted immediately, generate summary in background
     *
     * @param reviewSourceId The review source ID
     */
    @Async
    @Transactional
    public void generateSummaryAsync(Long reviewSourceId) {
        log.info("Asynchronously generating AI summary for review source: {}", reviewSourceId);
        try {
            generateAndSaveSummary(reviewSourceId);
        } catch (Exception e) {
            log.error("Failed to generate AI summary asynchronously for source: {}", reviewSourceId, e);
        }
    }

    /**
     * Invalidate AI summary for a review source.
     * Called when reviews are updated or sentiment is corrected.
     *
     * Business Logic:
     * - Sets valid_until to NOW() to expire current summary
     * - Clears cache for this source
     * - Next request will trigger regeneration
     *
     * Triggers:
     * - After sentiment correction (US-007)
     * - After sync job completion (new reviews imported)
     * - After manual refresh
     *
     * @param reviewSourceId The review source ID
     */
    @CacheEvict(value = "summary", key = "'source:' + #reviewSourceId")
    @Transactional
    public void invalidateSummary(Long reviewSourceId) {
        log.info("Invalidating AI summary for review source: {}", reviewSourceId);

        Instant now = Instant.now();
        var currentSummary = aiSummaryRepository.findLatestSummaryByReviewSourceId(reviewSourceId);

        currentSummary.ifPresent(summary -> {
            if (summary.isValid()) {
                summary.setValidUntil(now);
                aiSummaryRepository.save(summary);
                log.info("AI summary invalidated for source: {}", reviewSourceId);
            } else {
                log.debug("AI summary already expired for source: {}", reviewSourceId);
            }
        });
    }

    /**
     * Regenerate AI summary immediately, invalidating any existing cached version.
     * Forces fresh generation regardless of cache validity.
     *
     * Use case: User requests manual refresh of AI insights
     *
     * @param reviewSourceId The review source ID
     * @return AISummaryResponse containing the freshly generated summary
     */
    @CacheEvict(value = "summary", key = "'source:' + #reviewSourceId")
    @Transactional
    public AISummaryResponse regenerateSummary(Long reviewSourceId) {
        log.info("Regenerating AI summary for review source: {}", reviewSourceId);

        // Invalidate existing summary
        invalidateSummary(reviewSourceId);

        // Generate and return new summary
        return generateAndSaveSummary(reviewSourceId);
    }

    /**
     * Get summary history for a review source.
     * Used for debugging, analytics, and cost tracking.
     *
     * @param reviewSourceId The review source ID
     * @return List of all summaries ordered by generation date (newest first)
     */
    @Transactional(readOnly = true)
    public List<AISummaryResponse> getSummaryHistory(Long reviewSourceId) {
        log.info("Getting summary history for review source: {}", reviewSourceId);

        List<AISummary> summaries = aiSummaryRepository
            .findByReviewSourceIdOrderByGeneratedAtDesc(reviewSourceId);

        return summaries.stream()
            .map(aiSummaryMapper::toAISummaryResponse)
            .toList();
    }

    /**
     * Check if a valid summary exists for a review source.
     * Quick check without fetching the full summary entity.
     *
     * @param reviewSourceId The review source ID
     * @return true if valid summary exists
     */
    @Transactional(readOnly = true)
    public boolean hasValidSummary(Long reviewSourceId) {
        Instant now = Instant.now();
        return aiSummaryRepository.hasValidSummary(reviewSourceId, now);
    }

    /**
     * Calculate total tokens used for a review source.
     * Used for cost analysis and optimization.
     *
     * @param reviewSourceId The review source ID
     * @return Total tokens used for all summaries
     */
    @Transactional(readOnly = true)
    public Long calculateTotalTokensUsed(Long reviewSourceId) {
        Long totalTokens = aiSummaryRepository.calculateTotalTokensByReviewSourceId(reviewSourceId);
        return totalTokens != null ? totalTokens : 0L;
    }

    /**
     * Create an empty summary response when no reviews exist.
     * Returns a default message instead of failing.
     *
     * @param reviewSourceId The review source ID
     * @return AISummaryResponse with default/empty values
     */
    private AISummaryResponse createEmptySummaryResponse(Long reviewSourceId) {
        return AISummaryResponse.builder()
            .sourceId(reviewSourceId)
            .text("Brak dostępnych opinii do analizy.")
            .modelUsed("N/A")
            .tokenCount(0)
            .generatedAt(Instant.now().atZone(ZoneOffset.UTC))
            .validUntil(Instant.now().plusSeconds(Constants.AI_SUMMARY_VALIDITY_HOURS * 3600)
                .atZone(ZoneOffset.UTC))
            .build();
    }

    /**
     * Invalidate summaries for multiple sources.
     * Batch operation for performance.
     *
     * Use case: After brand-level changes affecting all sources
     *
     * @param reviewSourceIds List of review source IDs
     */
    @Transactional
    public void invalidateSummariesForSources(List<Long> reviewSourceIds) {
        log.info("Invalidating AI summaries for {} sources", reviewSourceIds.size());
        reviewSourceIds.forEach(this::invalidateSummary);
    }

    /**
     * Find and regenerate expired summaries.
     * Used by scheduled job to proactively refresh stale summaries.
     *
     * @return Count of summaries regenerated
     */
    @Transactional
    public int regenerateExpiredSummaries() {
        log.info("Finding and regenerating expired AI summaries");

        Instant now = Instant.now();
        List<AISummary> expiredSummaries = aiSummaryRepository.findExpiredSummaries(now);

        log.info("Found {} expired summaries to regenerate", expiredSummaries.size());

        expiredSummaries.forEach(summary -> {
            try {
                Long sourceId = summary.getReviewSource().getId();
                log.info("Regenerating expired summary for source: {}", sourceId);
                generateAndSaveSummary(sourceId);
            } catch (Exception e) {
                log.error("Failed to regenerate summary for source: {}",
                    summary.getReviewSource().getId(), e);
            }
        });

        return expiredSummaries.size();
    }
}
