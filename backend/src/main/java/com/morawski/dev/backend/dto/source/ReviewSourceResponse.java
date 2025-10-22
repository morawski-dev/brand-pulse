package com.morawski.dev.backend.dto.source;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.morawski.dev.backend.dto.common.AuthMethod;
import com.morawski.dev.backend.dto.common.JobStatus;
import com.morawski.dev.backend.dto.common.SourceType;
import com.morawski.dev.backend.dto.common.SyncStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Response DTO for review source information.
 * Maps to review_sources table.
 * NOTE: Never includes credentials_encrypted field for security.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewSourceResponse {

    /**
     * Review source ID from review_sources.id
     */
    private Long sourceId;

    /**
     * Parent brand ID from review_sources.brand_id
     */
    private Long brandId;

    /**
     * Platform type from review_sources.source_type
     */
    private SourceType sourceType;

    /**
     * Profile URL from review_sources.profile_url
     */
    private String profileUrl;

    /**
     * External platform ID from review_sources.external_profile_id
     */
    private String externalProfileId;

    /**
     * Authentication method from review_sources.auth_method
     */
    private AuthMethod authMethod;

    /**
     * Active status from review_sources.is_active
     */
    private Boolean isActive;

    /**
     * Last sync timestamp from review_sources.last_sync_at
     */
    private ZonedDateTime lastSyncAt;

    /**
     * Last sync status from review_sources.last_sync_status
     */
    private SyncStatus lastSyncStatus;

    /**
     * Last sync error message from review_sources.last_sync_error
     */
    private String lastSyncError;

    /**
     * Next scheduled sync timestamp from review_sources.next_scheduled_sync_at
     */
    private ZonedDateTime nextScheduledSyncAt;

    /**
     * Initial import job ID (for progress tracking during onboarding)
     */
    private Long importJobId;

    /**
     * Import job status (for progress tracking during onboarding)
     */
    private JobStatus importStatus;

    /**
     * Creation timestamp from review_sources.created_at
     */
    private ZonedDateTime createdAt;

    /**
     * Last update timestamp from review_sources.updated_at
     */
    private ZonedDateTime updatedAt;
}
