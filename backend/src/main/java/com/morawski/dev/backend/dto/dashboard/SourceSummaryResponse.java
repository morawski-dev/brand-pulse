package com.morawski.dev.backend.dto.dashboard;

import com.morawski.dev.backend.dto.common.SourceType;
import com.morawski.dev.backend.dto.review.ReviewResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for source summary.
 * Contains detailed metrics for a single review source.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceSummaryResponse {

    private Long sourceId;
    private SourceType sourceType;
    private String profileUrl;
    private Boolean isActive;
    private DashboardMetrics metrics;
    private LastSyncInfo lastSync;
    private List<ReviewResponse> recentNegativeReviews;
}
