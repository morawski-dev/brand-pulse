package com.morawski.dev.backend.scheduler;

import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.entity.SyncJob;
import com.morawski.dev.backend.service.ReviewSourceService;
import com.morawski.dev.backend.service.SyncJobService;
import com.morawski.dev.backend.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled task responsible for creating daily sync jobs for active review sources.
 *
 * API Plan Section 15.3: Async Processing
 * - Daily CRON sync at 3:00 AM CET (job_type = SCHEDULED)
 *
 * Flow:
 * 1. Runs every day at 03:00 CET
 * 2. Fetches review sources whose nextScheduledSyncAt has passed
 * 3. Skips sources that already have active jobs (PENDING / IN_PROGRESS)
 * 4. Enqueues new sync_job records with job_type=SCHEDULED
 * 5. Updates nextScheduledSyncAt to next day 03:00 CET
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyReviewSyncScheduler {

    private final ReviewSourceService reviewSourceService;
    private final SyncJobService syncJobService;

    /**
     * Triggered daily at 03:00 CET to create scheduled sync jobs.
     */
    @Scheduled(cron = Constants.CRON_DAILY_SYNC, zone = "Europe/Warsaw")
    public void scheduleDailySyncJobs() {
        log.info("Running daily review sync scheduler at 03:00 CET");

        try {
            List<ReviewSource> sourcesReadyForSync = reviewSourceService.findSourcesReadyForSync();

            if (sourcesReadyForSync.isEmpty()) {
                log.info("No review sources pending scheduled sync");
                return;
            }

            log.info("Found {} review source(s) ready for scheduled sync", sourcesReadyForSync.size());

            for (ReviewSource source : sourcesReadyForSync) {
                Long sourceId = source.getId();

                if (syncJobService.hasActiveJobForSource(sourceId)) {
                    log.warn("Skipping scheduled sync for source {} - active job already running", sourceId);
                    continue;
                }

                SyncJob scheduledJob = syncJobService.createScheduledSyncJob(source);
                reviewSourceService.scheduleNextDailySync(sourceId);

                log.info("Scheduled daily sync job created: jobId={}, sourceId={}, type={}",
                    scheduledJob.getId(), sourceId, source.getSourceType());
            }

        } catch (Exception ex) {
            log.error("Daily review sync scheduler failed: {}", ex.getMessage(), ex);
        }
    }
}
