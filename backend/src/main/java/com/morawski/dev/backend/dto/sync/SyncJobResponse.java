package com.morawski.dev.backend.dto.sync;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.morawski.dev.backend.dto.common.JobStatus;
import com.morawski.dev.backend.dto.common.JobType;
import com.morawski.dev.backend.dto.common.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Response DTO for sync job information.
 * Maps to sync_jobs table + source_type from review_sources.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SyncJobResponse {

    /**
     * Job ID from sync_jobs.id
     */
    private Long jobId;

    /**
     * Review source ID from sync_jobs.review_source_id
     */
    private Long sourceId;

    /**
     * Source platform type (joined from review_sources.source_type)
     */
    private SourceType sourceType;

    /**
     * Job type from sync_jobs.job_type
     */
    private JobType jobType;

    /**
     * Current job status from sync_jobs.status
     */
    private JobStatus status;

    /**
     * Job start timestamp from sync_jobs.started_at
     */
    private ZonedDateTime startedAt;

    /**
     * Job completion timestamp from sync_jobs.completed_at
     */
    private ZonedDateTime completedAt;

    /**
     * Total reviews fetched from sync_jobs.reviews_fetched
     */
    private Integer reviewsFetched;

    /**
     * New reviews created from sync_jobs.reviews_new
     */
    private Integer reviewsNew;

    /**
     * Existing reviews updated from sync_jobs.reviews_updated
     */
    private Integer reviewsUpdated;

    /**
     * Error message if job failed from sync_jobs.error_message
     */
    private String errorMessage;

    /**
     * Job creation timestamp from sync_jobs.created_at
     */
    private ZonedDateTime createdAt;
}
