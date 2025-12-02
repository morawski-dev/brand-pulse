package com.morawski.dev.backend.dto.sync;

import com.morawski.dev.backend.dto.common.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Response DTO for initial import progress, used for polling during onboarding.
 *
 * API: GET /api/sources/{sourceId}/import-status
 * Frontend contract: frontend/lib/types/brand.ts -> ImportProgressResponse
 *
 * The frontend onboarding wizard polls this every 2 seconds and stops when
 * {@code progress >= 100}. Progress is derived from the INITIAL sync job status
 * (PENDING -> 0, IN_PROGRESS -> 50, COMPLETED -> 100, FAILED -> 0).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportProgressResponse {

    /** Review source being imported. */
    private Long sourceId;

    /** Initial import job ID (null if no job has been created yet). */
    private Long jobId;

    /** Job status (PENDING, IN_PROGRESS, COMPLETED, FAILED). */
    private JobStatus status;

    /** Progress percentage 0-100 (derived from status). */
    private Integer progress;

    /** Human-readable status message in Polish. */
    private String statusMessage;

    /** Number of new reviews imported so far. */
    private Integer reviewsImported;

    /** Total reviews fetched from the source. */
    private Integer totalReviews;

    /** When the import started (null if not started yet). */
    private ZonedDateTime startedAt;

    /** When the import finished (null if still running). */
    private ZonedDateTime completedAt;

    /** Error message if the import failed (null otherwise). */
    private String error;
}
