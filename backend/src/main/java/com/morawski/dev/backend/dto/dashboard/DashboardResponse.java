package com.morawski.dev.backend.dto.dashboard;

import com.morawski.dev.backend.dto.review.ReviewResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Response DTO for dashboard view.
 * Contains aggregated metrics and summary for a brand.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private Long brandId;
    private String brandName;
    private Long selectedSourceId;
    private List<SourceSummary> sources;
    private DashboardMetrics metrics;
    private String summaryText;
    private List<ReviewResponse> recentNegativeReviews;
    private ZonedDateTime lastUpdated;
}
