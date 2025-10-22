package com.morawski.dev.backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Response DTO for dashboard metrics.
 * Maps to aggregated data from dashboard_aggregates table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsResponse {

    /**
     * Total number of reviews from dashboard_aggregates.total_reviews
     */
    private Integer totalReviews;

    /**
     * Average star rating from dashboard_aggregates.avg_rating
     */
    private BigDecimal averageRating;

    /**
     * Sentiment distribution breakdown
     */
    private SentimentDistributionResponse sentimentDistribution;

    /**
     * Rating distribution (1-5 stars)
     * Key: star rating (1-5), Value: count of reviews
     */
    private Map<Integer, Integer> ratingDistribution;
}
