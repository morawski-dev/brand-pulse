package com.morawski.dev.backend.scheduler;

import com.morawski.dev.backend.dto.common.JobType;
import com.morawski.dev.backend.dto.common.SourceType;
import com.morawski.dev.backend.dto.sync.SyncResult;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.entity.SyncJob;
import com.morawski.dev.backend.service.SyncJobService;
import com.morawski.dev.backend.service.sync.GoogleReviewSyncHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Background processor for sync jobs.
 * Polls for pending jobs and processes them using appropriate sync handlers.
 *
 * Processing Flow:
 * 1. Poll for pending jobs every 10 seconds
 * 2. For each pending job:
 *    - Mark as started (IN_PROGRESS)
 *    - Determine source type (Google/Facebook/Trustpilot)
 *    - Delegate to appropriate sync handler
 *    - Update job progress
 *    - Mark as completed or failed
 * 3. Process jobs asynchronously using syncJobExecutor
 *
 * API Plan Section 15.3: Async Processing
 * - Initial 90-day import (US-003)
 * - Daily CRON sync
 * - Manual refresh (US-008)
 *
 * Background Jobs:
 * - Duration: 30-120 seconds (depends on review count)
 * - Updates: sync_jobs table status
 * - Async executor: syncJobExecutor (configured in SchedulerConfig)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SyncJobProcessor {

    private final SyncJobService syncJobService;
    private final GoogleReviewSyncHandler googleReviewSyncHandler;
    // TODO: Add FacebookReviewSyncHandler (Phase 2)
    // private final FacebookReviewSyncHandler facebookReviewSyncHandler;
    // TODO: Add TrustpilotReviewSyncHandler (Phase 2)
    // private final TrustpilotReviewSyncHandler trustpilotReviewSyncHandler;

    /**
     * Poll for pending sync jobs and process them.
     *
     * Scheduled Task:
     * - Fixed delay: 10 seconds after previous execution completes
     * - Prevents overlapping executions
     * - Runs continuously as long as application is running
     *
     * Performance:
     * - Jobs are processed asynchronously to prevent blocking
     * - Maximum concurrent jobs limited by syncJobExecutor pool size (2-5 threads)
     *
     * Error Handling:
     * - Exceptions in job processing are caught and logged
     * - Job is marked as FAILED with error message
     * - Processor continues running despite individual job failures
     */
    @Scheduled(fixedDelay = 10000) // Check every 10 seconds
    public void pollAndProcessJobs() {
        log.debug("Polling for pending sync jobs...");

        try {
            List<SyncJob> pendingJobs = syncJobService.findPendingJobs();

            if (pendingJobs.isEmpty()) {
                log.trace("No pending sync jobs found");
                return;
            }

            log.info("Found {} pending sync job(s). Starting processing...", pendingJobs.size());

            // Process each job asynchronously
            for (SyncJob job : pendingJobs) {
                processJobAsync(job.getId());
            }

        } catch (Exception e) {
            log.error("Error polling for sync jobs: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a single sync job asynchronously.
     *
     * Uses dedicated syncJobExecutor to prevent blocking other async operations.
     * Each job runs in its own thread from the executor pool.
     *
     * Job Processing Steps:
     * 1. Mark job as started (status: IN_PROGRESS)
     * 2. Determine date range based on job type:
     *    - INITIAL: Last 90 days from now
     *    - SCHEDULED/MANUAL: Since last successful sync
     * 3. Delegate to appropriate sync handler (Google/Facebook/Trustpilot)
     * 4. Update job progress with sync results
     * 5. Mark job as completed or failed
     *
     * API Plan Section 13.2: Business Logic Rules
     * - 90-Day Historical Import (PRD:28)
     *
     * @param jobId Sync job ID to process
     */
    @Async("syncJobExecutor")
    public void processJobAsync(Long jobId) {
        log.info("Starting async processing for sync job ID: {}", jobId);

        try {
            // Fetch job details
            SyncJob job = syncJobService.findByIdOrThrow(jobId);
            ReviewSource reviewSource = job.getReviewSource();

            // Mark job as started
            syncJobService.markJobAsStarted(jobId);
            log.info("Sync job {} marked as IN_PROGRESS for source: {} ({})",
                jobId, reviewSource.getId(), reviewSource.getSourceType());

            // Determine date range based on job type
            LocalDate startDate = calculateStartDate(job);
            LocalDate endDate = LocalDate.now();

            log.debug("Sync job {} date range: {} to {}", jobId, startDate, endDate);

            // Delegate to appropriate sync handler based on source type
            SyncResult result = delegateToSyncHandler(reviewSource, startDate, endDate);

            // Check if sync succeeded
            if (result.isSuccess()) {
                // Update job progress
                syncJobService.updateJobProgress(
                    jobId,
                    result.getReviewsFetched(),
                    result.getReviewsNew(),
                    result.getReviewsUpdated()
                );

                // Mark job as completed
                syncJobService.markJobAsCompleted(jobId);

                log.info("Sync job {} completed successfully: fetched={}, new={}, updated={}",
                    jobId, result.getReviewsFetched(), result.getReviewsNew(), result.getReviewsUpdated());

            } else {
                // Mark job as failed
                syncJobService.markJobAsFailed(jobId, result.getErrorMessage());

                log.error("Sync job {} failed: {}", jobId, result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Unexpected error processing sync job {}: {}", jobId, e.getMessage(), e);

            try {
                syncJobService.markJobAsFailed(jobId, "Unexpected error: " + e.getMessage());
            } catch (Exception ex) {
                log.error("Failed to mark job {} as failed: {}", jobId, ex.getMessage());
            }
        }
    }

    /**
     * Delegate sync operation to appropriate handler based on source type.
     *
     * Supported Source Types:
     * - GOOGLE: GoogleReviewSyncHandler (MVP)
     * - FACEBOOK: FacebookReviewSyncHandler (Phase 2)
     * - TRUSTPILOT: TrustpilotReviewSyncHandler (Phase 2)
     *
     * @param reviewSource Review source to sync
     * @param startDate Start date for fetching reviews
     * @param endDate End date for fetching reviews
     * @return SyncResult with sync statistics
     */
    private SyncResult delegateToSyncHandler(
        ReviewSource reviewSource,
        LocalDate startDate,
        LocalDate endDate
    ) {
        SourceType sourceType = reviewSource.getSourceType();

        log.debug("Delegating sync to handler for source type: {}", sourceType);

        return switch (sourceType) {
            case GOOGLE -> googleReviewSyncHandler.syncReviews(reviewSource, startDate, endDate);
            case FACEBOOK -> {
                // TODO: Implement Facebook handler (Phase 2)
                log.warn("Facebook sync handler not implemented yet");
                yield SyncResult.failure("Facebook sync not implemented in MVP");
            }
            case TRUSTPILOT -> {
                // TODO: Implement Trustpilot handler (Phase 2)
                log.warn("Trustpilot sync handler not implemented yet");
                yield SyncResult.failure("Trustpilot sync not implemented in MVP");
            }
        };
    }

    /**
     * Calculate start date for review fetching based on job type.
     *
     * Job Type Logic:
     * - INITIAL: Last 90 days (PRD:28 - 90-day historical import)
     * - SCHEDULED: Since last successful sync (or 7 days if no previous sync)
     * - MANUAL: Since last successful sync (or 7 days if no previous sync)
     *
     * API Plan Section 13.2: 90-Day Historical Import
     *
     * @param job Sync job entity
     * @return Start date for fetching reviews
     */
    private LocalDate calculateStartDate(SyncJob job) {
        if (job.getJobType() == JobType.INITIAL) {
            // Initial import: last 90 days
            return LocalDate.now().minus(90, ChronoUnit.DAYS);
        } else {
            // SCHEDULED or MANUAL: since last successful sync
            // TODO: Implement logic to find last successful sync date
            // For now, default to last 7 days
            return LocalDate.now().minus(7, ChronoUnit.DAYS);
        }
    }
}
