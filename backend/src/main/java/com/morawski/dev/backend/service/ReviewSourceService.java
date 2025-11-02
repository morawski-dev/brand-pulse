package com.morawski.dev.backend.service;

import com.morawski.dev.backend.dto.source.*;
import com.morawski.dev.backend.entity.Brand;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.entity.User;
import com.morawski.dev.backend.exception.*;
import com.morawski.dev.backend.mapper.ReviewSourceMapper;
import com.morawski.dev.backend.repository.ReviewSourceRepository;
import com.morawski.dev.backend.util.Constants;
import com.morawski.dev.backend.util.DateTimeUtils;
import com.morawski.dev.backend.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for review source management operations.
 * Handles CRUD operations for review sources with plan limit enforcement.
 *
 * API Endpoints (Section 6):
 * - POST /api/brands/{brandId}/review-sources (Section 6.1)
 * - GET /api/brands/{brandId}/review-sources (Section 6.2)
 * - GET /api/brands/{brandId}/review-sources/{sourceId} (Section 6.3)
 * - PATCH /api/brands/{brandId}/review-sources/{sourceId} (Section 6.4)
 * - DELETE /api/brands/{brandId}/review-sources/{sourceId} (Section 6.5)
 *
 * User Stories:
 * - US-003: Configuring First Source (Step 2: Add Review Source)
 * - US-006: Switching Between Locations
 * - US-009: Free Plan Limitation (1 source maximum)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewSourceService {

    private final ReviewSourceRepository reviewSourceRepository;
    private final BrandService brandService;
    private final UserService userService;
    private final ReviewSourceMapper reviewSourceMapper;

    /**
     * Create new review source for brand.
     * API: POST /api/brands/{brandId}/review-sources (Section 6.1)
     *
     * Business Logic (from API Plan Section 6.1):
     * 1. Check user's plan: Count existing active sources for brand
     * 2. If count >= max_sources_allowed, return 403 with plan upgrade message
     * 3. Encrypt credentials using AES-256 before storing
     * 4. Create sync_job with job_type='INITIAL', status='PENDING'
     * 5. Trigger background job to import last 90 days of reviews
     * 6. Return importJobId for progress tracking
     * 7. Log activity: SOURCE_ADDED, then FIRST_SOURCE_CONFIGURED_SUCCESSFULLY if first source
     *
     * @param brandId Brand ID
     * @param userId User ID from JWT token
     * @param request Create review source request
     * @return ReviewSourceResponse with import job ID
     * @throws ResourceNotFoundException if brand not found
     * @throws ResourceAccessDeniedException if user doesn't own brand
     * @throws PlanLimitExceededException if user has reached plan limit (US-009)
     * @throws DuplicateResourceException if source already exists
     */
    @Transactional
    public ReviewSourceResponse createReviewSource(Long brandId, Long userId, CreateReviewSourceRequest request) {
        log.info("Creating review source for brand ID: {} (user: {})", brandId, userId);

        // Verify user owns the brand
        Brand brand = brandService.findByIdAndUserIdOrThrow(brandId, userId);
        User user = userService.findByIdOrThrow(userId);

        // Check plan limits (US-009: Free plan limitation)
        long activeSourceCount = reviewSourceRepository.countByBrandIdAndDeletedAtIsNull(brandId);
        if (activeSourceCount >= user.getMaxSourcesAllowed()) {
            log.warn("Plan limit exceeded: User {} has {} sources, max allowed: {}",
                userId, activeSourceCount, user.getMaxSourcesAllowed());

            throw new PlanLimitExceededException(
                "review sources",
                (int) activeSourceCount,
                user.getMaxSourcesAllowed(),
                user.getPlanType().toString()
            );
        }

        // Check for duplicate source (same brand + sourceType + externalProfileId)
        if (reviewSourceRepository.existsByBrandIdAndSourceTypeAndExternalProfileId(
            brandId,
            request.getSourceType(),
            request.getExternalProfileId()
        )) {
            log.warn("Duplicate source detected: brand={}, type={}, externalId={}",
                brandId, request.getSourceType(), request.getExternalProfileId());

            throw DuplicateResourceException.forReviewSource(
                brandId,
                request.getSourceType().name(),
                request.getExternalProfileId()
            );
        }

        // Create review source entity
        ReviewSource reviewSource = ReviewSource.builder()
            .sourceType(request.getSourceType())
            .profileUrl(request.getProfileUrl().trim())
            .externalProfileId(request.getExternalProfileId().trim())
            .authMethod(request.getAuthMethod())
            .credentialsEncrypted(request.getCredentialsEncrypted()) // TODO: Encrypt with AES-256
            .isActive(true)
            .brand(brand)
            .build();

        // Set next scheduled sync to next 3 AM CET
        Instant nextSync = DateTimeUtils.getNextDailySyncTime().toInstant(ZoneOffset.UTC);
        reviewSource.setNextScheduledSyncAt(nextSync);

        ReviewSource savedSource = reviewSourceRepository.save(reviewSource);

        // TODO: Create sync_job with job_type='INITIAL', status='PENDING'
        // TODO: Trigger background job to import last 90 days of reviews
        // TODO: Log activity: SOURCE_ADDED

        // Check if this is the first source
        boolean isFirstSource = activeSourceCount == 0;
        if (isFirstSource) {
            log.info("First source configured for user: {}", userId);
            // TODO: Log activity: FIRST_SOURCE_CONFIGURED_SUCCESSFULLY
        }

        // Build response
        ReviewSourceResponse response = reviewSourceMapper.toReviewSourceResponse(savedSource);
        // TODO: Set importJobId from created sync job
        response.setImportJobId(null); // Placeholder until sync job is implemented
        // response.setImportStatus(JobStatus.PENDING);

        log.info("Review source created: type={}, brandId={}, sourceId={}",
            savedSource.getSourceType(), brandId, savedSource.getId());

        return response;
    }

    /**
     * Get all review sources for a brand.
     * API: GET /api/brands/{brandId}/review-sources (Section 6.2)
     *
     * Business Logic:
     * - Filter by brandId and deleted_at IS NULL
     * - Never return credentials_encrypted field (security)
     *
     * @param brandId Brand ID
     * @param userId User ID from JWT token
     * @return ReviewSourceListResponse containing list of sources
     * @throws ResourceNotFoundException if brand not found
     * @throws ResourceAccessDeniedException if user doesn't own brand
     */
    @Transactional(readOnly = true)
    public ReviewSourceListResponse getReviewSources(Long brandId, Long userId) {
        log.debug("Getting review sources for brand ID: {}", brandId);

        // Verify user owns the brand
        brandService.findByIdAndUserIdOrThrow(brandId, userId);

        List<ReviewSource> sources = reviewSourceRepository.findByBrandId(brandId);

        List<ReviewSourceResponse> sourceResponses = sources.stream()
            .map(reviewSourceMapper::toReviewSourceResponse)
            .collect(Collectors.toList());

        log.info("Retrieved {} review source(s) for brand ID: {}", sourceResponses.size(), brandId);

        return ReviewSourceListResponse.builder()
            .sources(sourceResponses)
            .build();
    }

    /**
     * Get review source by ID with ownership validation.
     * API: GET /api/brands/{brandId}/review-sources/{sourceId} (Section 6.3)
     *
     * @param brandId Brand ID
     * @param sourceId Review source ID
     * @param userId User ID from JWT token
     * @return ReviewSourceResponse with source details
     * @throws ResourceNotFoundException if source not found
     * @throws ResourceAccessDeniedException if user doesn't own brand
     */
    @Transactional(readOnly = true)
    public ReviewSourceResponse getReviewSourceById(Long brandId, Long sourceId, Long userId) {
        log.debug("Getting review source ID: {} for brand ID: {}", sourceId, brandId);

        // Verify user owns the brand
        brandService.findByIdAndUserIdOrThrow(brandId, userId);

        ReviewSource source = findByIdAndBrandIdOrThrow(sourceId, brandId);

        ReviewSourceResponse response = reviewSourceMapper.toReviewSourceResponse(source);

        log.info("Retrieved review source: type={}, id={}", source.getSourceType(), sourceId);

        return response;
    }

    /**
     * Update review source.
     * API: PATCH /api/brands/{brandId}/review-sources/{sourceId} (Section 6.4)
     *
     * Business Logic:
     * - Setting isActive=false pauses automatic syncs
     * - Updating URL or credentials may trigger validation job (future)
     *
     * @param brandId Brand ID
     * @param sourceId Review source ID
     * @param userId User ID from JWT token
     * @param request Update request
     * @return Updated ReviewSourceResponse
     * @throws ResourceNotFoundException if source not found
     * @throws ResourceAccessDeniedException if user doesn't own brand
     */
    @Transactional
    public ReviewSourceResponse updateReviewSource(
        Long brandId,
        Long sourceId,
        Long userId,
        UpdateReviewSourceRequest request
    ) {
        log.info("Updating review source ID: {} for brand ID: {}", sourceId, brandId);

        // Verify user owns the brand
        brandService.findByIdAndUserIdOrThrow(brandId, userId);

        ReviewSource source = findByIdAndBrandIdOrThrow(sourceId, brandId);

        // Update fields if provided
        boolean wasActive = source.getIsActive();

        if (request.getIsActive() != null) {
            source.setIsActive(request.getIsActive());

            if (wasActive && !request.getIsActive()) {
                log.info("Review source deactivated: id={}", sourceId);
            } else if (!wasActive && request.getIsActive()) {
                log.info("Review source reactivated: id={}", sourceId);
            }
        }

        if (request.getProfileUrl() != null) {
            String oldUrl = source.getProfileUrl();
            source.setProfileUrl(request.getProfileUrl().trim());
            log.info("Review source URL updated: id={}, old={}, new={}",
                sourceId, StringUtils.truncate(oldUrl, 50), StringUtils.truncate(source.getProfileUrl(), 50));

            // TODO: May trigger validation job in future
        }

        ReviewSource savedSource = reviewSourceRepository.save(source);

        return reviewSourceMapper.toReviewSourceResponse(savedSource);
    }

    /**
     * Soft delete review source.
     * API: DELETE /api/brands/{brandId}/review-sources/{sourceId} (Section 6.5)
     *
     * Business Logic:
     * - Soft delete: Set deleted_at=NOW()
     * - Cascade deletes reviews, dashboard_aggregates, ai_summaries, sync_jobs
     * - Log activity: SOURCE_DELETED
     *
     * @param brandId Brand ID
     * @param sourceId Review source ID
     * @param userId User ID from JWT token
     * @throws ResourceNotFoundException if source not found
     * @throws ResourceAccessDeniedException if user doesn't own brand
     */
    @Transactional
    public void deleteReviewSource(Long brandId, Long sourceId, Long userId) {
        log.info("Deleting review source ID: {} for brand ID: {}", sourceId, brandId);

        // Verify user owns the brand
        brandService.findByIdAndUserIdOrThrow(brandId, userId);

        ReviewSource source = findByIdAndBrandIdOrThrow(sourceId, brandId);

        // Soft delete source
        source.softDelete();
        reviewSourceRepository.save(source);

        // TODO: Log activity: SOURCE_DELETED

        log.warn("Review source soft deleted: type={}, id={}", source.getSourceType(), sourceId);
    }

    // ========== Helper Methods ==========

    /**
     * Find review source by ID.
     *
     * @param sourceId Review source ID
     * @return ReviewSource entity
     * @throws ResourceNotFoundException if source not found
     */
    @Transactional(readOnly = true)
    public ReviewSource findByIdOrThrow(Long sourceId) {
        return reviewSourceRepository.findById(sourceId)
            .orElseThrow(() -> new ResourceNotFoundException("ReviewSource", "id", sourceId));
    }

    /**
     * Find review source by ID and brand ID with ownership validation.
     *
     * @param sourceId Review source ID
     * @param brandId Brand ID
     * @return ReviewSource entity
     * @throws ResourceNotFoundException if source not found or doesn't belong to brand
     */
    @Transactional(readOnly = true)
    public ReviewSource findByIdAndBrandIdOrThrow(Long sourceId, Long brandId) {
        return reviewSourceRepository.findByIdAndBrandId(sourceId, brandId)
            .orElseThrow(() -> {
                // Check if source exists at all
                if (!reviewSourceRepository.existsById(sourceId)) {
                    return new ResourceNotFoundException("ReviewSource", "id", sourceId);
                }
                // Source exists but doesn't belong to this brand
                return new ResourceAccessDeniedException(
                    "This review source does not belong to the specified brand"
                );
            });
    }

    /**
     * Count active review sources for a brand.
     *
     * @param brandId Brand ID
     * @return Number of active sources
     */
    @Transactional(readOnly = true)
    public long countActiveSourcesForBrand(Long brandId) {
        return reviewSourceRepository.countByBrandIdAndDeletedAtIsNull(brandId);
    }

    /**
     * Find all active review sources (for sync scheduling).
     *
     * @return List of active sources
     */
    @Transactional(readOnly = true)
    public List<ReviewSource> findActiveSources() {
        return reviewSourceRepository.findByIsActiveTrue();
    }

    /**
     * Find sources ready for sync (next_scheduled_sync_at has passed).
     * Used by CRON job at 3:00 AM CET.
     *
     * @return List of sources ready for sync
     */
    @Transactional(readOnly = true)
    public List<ReviewSource> findSourcesReadyForSync() {
        return reviewSourceRepository.findByNextScheduledSyncAtBeforeAndIsActiveTrue(Instant.now());
    }

    /**
     * Update last sync status and timestamp.
     * Called by sync job after completion.
     *
     * @param sourceId Review source ID
     * @param status Sync status (SUCCESS, FAILED)
     * @param errorMessage Error message if sync failed (null if success)
     */
    @Transactional
    public void updateSyncStatus(Long sourceId, com.morawski.dev.backend.dto.common.SyncStatus status, String errorMessage) {
        ReviewSource source = findByIdOrThrow(sourceId);

        source.setLastSyncAt(Instant.now());
        source.setLastSyncStatus(status);
        source.setLastSyncError(errorMessage);

        // Schedule next sync at next 3 AM CET
        Instant nextSync = DateTimeUtils.getNextDailySyncTime().toInstant(ZoneOffset.UTC);
        source.setNextScheduledSyncAt(nextSync);

        reviewSourceRepository.save(source);

        log.info("Sync status updated for source {}: status={}, error={}",
            sourceId, status, errorMessage != null ? StringUtils.truncate(errorMessage, 50) : "none");
    }

    /**
     * Schedule next automatic daily sync for review source (3:00 AM CET).
     * Used by scheduler after enqueuing a job to avoid duplicate scheduling.
     *
     * @param sourceId Review source ID
     */
    @Transactional
    public void scheduleNextDailySync(Long sourceId) {
        ReviewSource source = findByIdOrThrow(sourceId);
        Instant nextSync = DateTimeUtils.getNextDailySyncTime().toInstant(ZoneOffset.UTC);
        source.setNextScheduledSyncAt(nextSync);
        reviewSourceRepository.save(source);

        log.debug("Next daily sync scheduled for source {} at {}", sourceId, nextSync);
    }
}
