package com.morawski.dev.backend.dto.dashboard;

import com.morawski.dev.backend.dto.common.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * DTO for last sync job information.
 * Used in dashboard source summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastSyncInfo {

    private Long jobId;
    private JobStatus status;
    private ZonedDateTime completedAt;
    private Integer reviewsFetched;
    private Integer reviewsNew;
    private String errorMessage;
}
