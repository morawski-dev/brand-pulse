package com.morawski.dev.backend.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Response DTO for dashboard summary (US-004, US-006).
 * Aggregates data from dashboard_aggregates, ai_summaries, and reviews tables.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardSummaryResponse {

    /**
     * Brand ID for this dashboard
     */
    private Long brandId;

    /**
     * Review source ID (null for "All locations" view)
     */
    private Long sourceId;

    /**
     * Source name (e.g., "GOOGLE", null for "All locations")
     */
    private String sourceName;

    /**
     * Time period for this dashboard
     */
    private PeriodResponse period;

    /**
     * Aggregated metrics
     */
    private MetricsResponse metrics;

    /**
     * AI-generated summary
     */
    private AISummaryResponse aiSummary;

    /**
     * Top 3 most recent negative reviews (rating <= 2)
     */
    private List<ReviewSummaryResponse> recentNegativeReviews;

    /**
     * Last data update timestamp
     */
    private ZonedDateTime lastUpdated;
}
