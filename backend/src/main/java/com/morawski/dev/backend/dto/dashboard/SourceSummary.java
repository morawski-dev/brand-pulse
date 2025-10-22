package com.morawski.dev.backend.dto.dashboard;

import com.morawski.dev.backend.dto.common.SourceType;
import com.morawski.dev.backend.dto.common.SyncStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * DTO for source summary in dashboard.
 * Contains basic info about a review source.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceSummary {

    private Long sourceId;
    private SourceType sourceType;
    private String profileUrl;
    private Boolean isActive;
    private Long totalReviews;
    private Double averageRating;
    private ZonedDateTime lastSyncAt;
    private SyncStatus lastSyncStatus;
}
