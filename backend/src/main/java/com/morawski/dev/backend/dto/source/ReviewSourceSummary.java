package com.morawski.dev.backend.dto.source;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.morawski.dev.backend.dto.common.SourceType;
import com.morawski.dev.backend.dto.common.SyncStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Summary DTO for review source (subset of ReviewSourceResponse).
 * Used in BrandDetailResponse to avoid excessive data.
 * Maps to review_sources table (selected fields).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewSourceSummary {

    /**
     * Review source ID from review_sources.id
     */
    private Long sourceId;

    /**
     * Platform type from review_sources.source_type
     */
    private SourceType sourceType;

    /**
     * Profile URL from review_sources.profile_url
     */
    private String profileUrl;

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
}
