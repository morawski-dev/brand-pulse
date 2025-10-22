package com.morawski.dev.backend.dto.sync;

import com.morawski.dev.backend.dto.common.JobStatus;
import com.morawski.dev.backend.dto.common.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * Response DTO for sync job status.
 * Used for monitoring job progress.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncJobStatusResponse {

    private Long jobId;
    private JobType jobType;
    private JobStatus status;
    private Long reviewSourceId;

    // Progress metrics
    private Integer reviewsFetched;
    private Integer reviewsNew;
    private Integer reviewsUpdated;

    // Timing
    private ZonedDateTime createdAt;
    private ZonedDateTime startedAt;
    private ZonedDateTime completedAt;
    private Duration duration;

    // Error info
    private String errorMessage;
}
