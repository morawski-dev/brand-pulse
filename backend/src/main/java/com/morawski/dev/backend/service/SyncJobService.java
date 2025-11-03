package com.morawski.dev.backend.service;

import com.morawski.dev.backend.dto.common.JobStatus;
import com.morawski.dev.backend.dto.common.JobType;
import com.morawski.dev.backend.dto.sync.*;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.entity.SyncJob;
import com.morawski.dev.backend.exception.RateLimitExceededException;
import com.morawski.dev.backend.exception.ResourceAccessDeniedException;
import com.morawski.dev.backend.exception.ResourceNotFoundException;
import com.morawski.dev.backend.exception.SyncInProgressException;
import com.morawski.dev.backend.exception.ValidationException;
import com.morawski.dev.backend.mapper.SyncJobMapper;
import com.morawski.dev.backend.repository.SyncJobRepository;
import com.morawski.dev.backend.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for sync job operations.
 * Handles manual data refresh, initial imports, and job monitoring.
 *
 * API Endpoints (Section 9):
 * - POST /api/brands/{brandId}/sync (Section 9.1)
 * - GET /api/sync-jobs/{jobId} (Section 9.2)
 * - GET /api/brands/{brandId}/review-sources/{sourceId}/sync-jobs (Section 9.3)
 *
 * User Stories:
 * - US-003: Configuring First Source (initial 90-day import)
 * - US-008: Manual Data Refresh (24-hour cooldown)
 *
 * Job Types:
 * - INITIAL: First-time import of last 90 days (triggered when source is created)
 * - SCHEDULED: Daily CRON job at 3:00 AM CET
 * - MANUAL: User-triggered refresh (max once per 24h)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncJobService {

    private final SyncJobRepository syncJobRepository;
    private final ReviewSourceService reviewSourceService;
    private final BrandService brandService;
    private final SyncJobMapper syncJobMapper;

    /**
     * Trigger manual data refresh for a review source.
     * API: POST /api/brands/{brandId}/sync (Section 9.1)
     *
     * Business Logic (from API Plan Section 9.1):
     * 1. Check if user owns the brand
     * 2. Verify sourceId belongs to brandId
     * 3. Check last manual sync time (must be >24 hours ago)
     * 4. Create sync_job record with job_type='MANUAL', status='PENDING'
     * 5. Enqueue background job to processor
     * 6. Return jobId for progress tracking
     * 7. Log activity: MANUAL_REFRESH_TRIGGERED
     *
     * Rate Limiting:
     * - Manual refresh allowed once per 24 hours per source
     * - Error 429 if cooldown period not elapsed
     *
     * @param brandId Brand ID
     * @param sourceId Review source ID
     * @param userId User ID from JWT token
     * @return TriggerSyncResponse with job ID
     * @throws ResourceNotFoundException if brand or source not found
     * @throws ResourceAccessDeniedException if user doesn't own brand
     * @throws RateLimitExceededException if 24-hour cooldown not elapsed (US-008)
     */
    @Transactional
    public TriggerSyncResponse triggerManualSync(Long brandId, Long sourceId, Long userId) {
        log.info("Triggering manual sync for source ID: {} (brand: {}, user: {})", sourceId, brandId, userId);

        // Verify user owns the brand
        brandService.findByIdAndUserIdOrThrow(brandId, userId);

        // Verify source belongs to brand
        ReviewSource reviewSource = reviewSourceService.findByIdAndBrandIdOrThrow(sourceId, brandId);

        // Check rate limiting: last manual sync must be >24 hours ago
        syncJobRepository.findMostRecentByReviewSourceId(sourceId)
            .ifPresent(lastJob -> {
                if (lastJob.getJobType() == JobType.MANUAL) {
                    Instant cooldownExpiry = lastJob.getCreatedAt().plus(24, ChronoUnit.HOURS);
                    if (Instant.now().isBefore(cooldownExpiry)) {
                        long hoursRemaining = Duration.between(Instant.now(), cooldownExpiry).toHours();
                        log.warn("Manual sync rate limit exceeded for source {}: cooldown expires in {} hours",
                            sourceId, hoursRemaining);

                        throw new RateLimitExceededException(
                            String.format("Manual refresh allowed once per 24 hours. Try again in %d hours.", hoursRemaining),
                            lastJob.getCreatedAt(),
                            cooldownExpiry,
                            hoursRemaining
                        );
                    }
                }
            });

        // Prevent duplicate jobs while another sync is running
        if (hasActiveJobForSource(sourceId)) {
            log.warn("Manual sync blocked - active job already running for source {}", sourceId);
            throw new SyncInProgressException(sourceId);
        }

        // Create sync job
        SyncJob syncJob = SyncJob.builder()
            .jobType(JobType.MANUAL)
            .status(JobStatus.PENDING)
            .reviewSource(reviewSource)
            .build();

        SyncJob savedJob = syncJobRepository.save(syncJob);

        // TODO: Enqueue background job to processor
        // TODO: Log activity: MANUAL_REFRESH_TRIGGERED

        log.info("Manual sync job created: jobId={}, sourceId={}", savedJob.getId(), sourceId);

        SyncJobResponse jobResponse = syncJobMapper.toSyncJobResponse(savedJob);

        return TriggerSyncResponse.builder()
            .message("Sync job started. Check progress using jobId.")
            .jobs(List.of(jobResponse))
            .nextManualSyncAvailableAt(Instant.now().plus(24, ChronoUnit.HOURS).atZone(java.time.ZoneId.of("UTC")))
            .build();
    }

    /**
     * Create initial import job for a newly created review source.
     * Called internally by ReviewSourceService when source is created.
     *
     * Business Logic:
     * - Import last 90 days of reviews
     * - Job type: INITIAL
     * - Tracks onboarding progress (US-003)
     *
     * @param reviewSource Review source entity
     * @return Created sync job
     */
    @Transactional
    public SyncJob createInitialImportJob(ReviewSource reviewSource) {
        log.info("Creating initial import job for review source ID: {}", reviewSource.getId());

        SyncJob syncJob = SyncJob.builder()
            .jobType(JobType.INITIAL)
            .status(JobStatus.PENDING)
            .reviewSource(reviewSource)
            .build();

        SyncJob savedJob = syncJobRepository.save(syncJob);

        // TODO: Enqueue background job to processor (90-day import)

        log.info("Initial import job created: jobId={}, sourceId={}", savedJob.getId(), reviewSource.getId());

        return savedJob;
    }

    /**
     * Create scheduled sync job for a review source.
     * Used by daily CRON scheduler at 3:00 AM CET.
     *
     * @param reviewSource Review source entity
     * @return Created sync job
     */
    @Transactional
    public SyncJob createScheduledSyncJob(ReviewSource reviewSource) {
        log.info("Creating scheduled sync job for review source ID: {}", reviewSource.getId());

        SyncJob syncJob = SyncJob.builder()
            .jobType(JobType.SCHEDULED)
            .status(JobStatus.PENDING)
            .reviewSource(reviewSource)
            .build();

        SyncJob savedJob = syncJobRepository.save(syncJob);

        log.debug("Scheduled sync job created: jobId={}, sourceId={}", savedJob.getId(), reviewSource.getId());

        return savedJob;
    }

    /**
     * Get sync job status by ID.
     * API: GET /api/sync-jobs/{jobId} (Section 9.2)
     *
     * Response includes:
     * - Job status (PENDING, IN_PROGRESS, COMPLETED, FAILED)
     * - Progress metrics (reviews fetched, new, updated)
     * - Duration and error message if failed
     *
     * @param jobId Sync job ID
     * @param userId User ID from JWT token (for ownership validation)
     * @return SyncJobStatusResponse with job details
     * @throws ResourceNotFoundException if job not found
     * @throws ResourceAccessDeniedException if user doesn't own the brand
     */
    @Transactional(readOnly = true)
    public SyncJobStatusResponse getSyncJobStatus(Long jobId, Long userId) {
        log.debug("Getting sync job status for job ID: {}", jobId);

        SyncJob syncJob = findByIdOrThrow(jobId);

        // Verify user owns the brand that contains this source
        Long brandId = syncJob.getReviewSource().getBrand().getId();
        brandService.findByIdAndUserIdOrThrow(brandId, userId);

        SyncJobStatusResponse response = syncJobMapper.toSyncJobStatusResponse(syncJob);

        log.info("Retrieved sync job status: jobId={}, status={}, reviewsFetched={}",
            jobId, syncJob.getStatus(), syncJob.getReviewsFetched());

        return response;
    }

    /**
     * Get sync job history for a review source.
     * API: GET /api/brands/{brandId}/review-sources/{sourceId}/sync-jobs (Section 9.3)
     *
     * Query Parameters:
     * - page (optional): Page number (0-indexed). Default: 0
     * - size (optional): Items per page. Default: 20, Max: 100
     *
     * @param brandId Brand ID
     * @param sourceId Review source ID
     * @param userId User ID from JWT token
     * @param page Page number (0-indexed)
     * @param size Page size (max 100)
     * @return SyncJobListResponse with paginated job history
     * @throws ResourceNotFoundException if brand or source not found
     * @throws ResourceAccessDeniedException if user doesn't own brand
     */
    @Transactional(readOnly = true)
    public SyncJobListResponse getSyncJobHistory(
        Long brandId,
        Long sourceId,
        Long userId,
        Integer page,
        Integer size
    ) {
        log.debug("Getting sync job history for source ID: {} (brand: {})", sourceId, brandId);

        // Verify user owns the brand
        brandService.findByIdAndUserIdOrThrow(brandId, userId);

        // Verify source belongs to brand
        reviewSourceService.findByIdAndBrandIdOrThrow(sourceId, brandId);

        // Validate pagination parameters
        int validatedPage = page != null ? page : Constants.DEFAULT_PAGE_NUMBER;
        int validatedSize = size != null ? Math.min(size, Constants.MAX_PAGE_SIZE) : Constants.DEFAULT_PAGE_SIZE;

        if (validatedPage < 0 || validatedSize <= 0) {
            throw new ValidationException("Invalid pagination parameters");
        }

        Pageable pageable = PageRequest.of(validatedPage, validatedSize);

        Page<SyncJob> jobPage = syncJobRepository.findByReviewSourceId(sourceId, pageable);

        List<SyncJobResponse> jobResponses = jobPage.getContent().stream()
            .map(syncJobMapper::toSyncJobResponse)
            .collect(Collectors.toList());

        log.info("Retrieved {} sync job(s) for source {}, page {}/{}",
            jobResponses.size(), sourceId, validatedPage, jobPage.getTotalPages());

        com.morawski.dev.backend.dto.common.PaginationResponse pagination =
            com.morawski.dev.backend.dto.common.PaginationResponse.builder()
                .currentPage(validatedPage)
                .pageSize(validatedSize)
                .totalItems(jobPage.getTotalElements())
                .totalPages(jobPage.getTotalPages())
                .hasNext(jobPage.hasNext())
                .hasPrevious(jobPage.hasPrevious())
                .build();

        return SyncJobListResponse.builder()
            .jobs(jobResponses)
            .pagination(pagination)
            .build();
    }

    /**
     * Get initial import job for a review source.
     * Used to track onboarding progress (US-003).
     *
     * @param reviewSourceId Review source ID
     * @return SyncJob if found
     */
    @Transactional(readOnly = true)
    public SyncJob getInitialImportJob(Long reviewSourceId) {
        return syncJobRepository.findInitialImportJob(reviewSourceId)
            .orElse(null);
    }

    /**
     * Check if review source has any active jobs (pending or in progress).
     * Used to prevent duplicate scheduling.
     *
     * @param reviewSourceId Review source ID
     * @return true if active job exists
     */
    @Transactional(readOnly = true)
    public boolean hasActiveJobForSource(Long reviewSourceId) {
        return syncJobRepository.existsActiveJobForSource(reviewSourceId);
    }

    /**
     * Find pending jobs for background processor.
     * Used by job scheduler to pick up work.
     *
     * @return List of pending jobs
     */
    @Transactional(readOnly = true)
    public List<SyncJob> findPendingJobs() {
        return syncJobRepository.findPendingJobs();
    }

    /**
     * Find stuck jobs (running for more than threshold duration).
     * Used for detecting and handling hung jobs.
     *
     * @param thresholdMinutes Minutes after which job is considered stuck
     * @return List of stuck jobs
     */
    @Transactional(readOnly = true)
    public List<SyncJob> findStuckJobs(int thresholdMinutes) {
        Instant threshold = Instant.now().minus(thresholdMinutes, ChronoUnit.MINUTES);
        return syncJobRepository.findStuckJobs(threshold);
    }

    /**
     * Update job progress during execution.
     * Called by background job processor.
     *
     * @param jobId Job ID
     * @param reviewsFetched Number of reviews fetched
     * @param reviewsNew Number of new reviews
     * @param reviewsUpdated Number of updated reviews
     */
    @Transactional
    public void updateJobProgress(Long jobId, int reviewsFetched, int reviewsNew, int reviewsUpdated) {
        SyncJob job = findByIdOrThrow(jobId);

        job.setReviewsFetched(reviewsFetched);
        job.setReviewsNew(reviewsNew);
        job.setReviewsUpdated(reviewsUpdated);

        syncJobRepository.save(job);

        log.debug("Job progress updated: jobId={}, fetched={}, new={}, updated={}",
            jobId, reviewsFetched, reviewsNew, reviewsUpdated);
    }

    /**
     * Mark job as started.
     * Called by background job processor when starting work.
     *
     * @param jobId Job ID
     */
    @Transactional
    public void markJobAsStarted(Long jobId) {
        SyncJob job = findByIdOrThrow(jobId);
        job.markAsStarted();
        syncJobRepository.save(job);

        log.info("Sync job started: jobId={}", jobId);
    }

    /**
     * Mark job as completed.
     * Called by background job processor on successful completion.
     *
     * @param jobId Job ID
     */
    @Transactional
    public void markJobAsCompleted(Long jobId) {
        SyncJob job = findByIdOrThrow(jobId);
        job.markAsCompleted();
        syncJobRepository.save(job);

        log.info("Sync job completed: jobId={}, duration={} seconds, reviews: fetched={}, new={}, updated={}",
            jobId, job.getDuration().getSeconds(), job.getReviewsFetched(), job.getReviewsNew(), job.getReviewsUpdated());
    }

    /**
     * Mark job as failed.
     * Called by background job processor on error.
     *
     * @param jobId Job ID
     * @param errorMessage Error message
     */
    @Transactional
    public void markJobAsFailed(Long jobId, String errorMessage) {
        SyncJob job = findByIdOrThrow(jobId);
        job.markAsFailed(errorMessage);
        syncJobRepository.save(job);

        log.error("Sync job failed: jobId={}, error={}", jobId, errorMessage);
    }

    // ========== Helper Methods ==========

    /**
     * Find sync job by ID.
     *
     * @param jobId Job ID
     * @return SyncJob entity
     * @throws ResourceNotFoundException if job not found
     */
    @Transactional(readOnly = true)
    public SyncJob findByIdOrThrow(Long jobId) {
        return syncJobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("SyncJob", "id", jobId));
    }

    /**
     * Calculate average job duration for a review source.
     *
     * @param reviewSourceId Review source ID
     * @return Average duration in seconds (null if no completed jobs)
     */
    @Transactional(readOnly = true)
    public Double calculateAverageDuration(Long reviewSourceId) {
        return syncJobRepository.calculateAverageDuration(reviewSourceId);
    }

    /**
     * Count jobs by status for a review source.
     *
     * @param reviewSourceId Review source ID
     * @param status Job status
     * @return Count of jobs
     */
    @Transactional(readOnly = true)
    public long countByStatus(Long reviewSourceId, JobStatus status) {
        return syncJobRepository.countByReviewSourceIdAndStatus(reviewSourceId, status);
    }
}
