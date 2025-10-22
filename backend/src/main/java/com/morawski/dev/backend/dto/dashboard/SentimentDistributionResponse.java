package com.morawski.dev.backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for sentiment distribution metrics.
 * Maps to aggregated data from dashboard_aggregates table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentDistributionResponse {

    /**
     * Count of positive reviews from dashboard_aggregates.positive_count
     */
    private Integer positive;

    /**
     * Count of negative reviews from dashboard_aggregates.negative_count
     */
    private Integer negative;

    /**
     * Count of neutral reviews from dashboard_aggregates.neutral_count
     */
    private Integer neutral;

    /**
     * Percentage of positive reviews (computed)
     */
    private BigDecimal positivePercentage;

    /**
     * Percentage of negative reviews (computed)
     */
    private BigDecimal negativePercentage;

    /**
     * Percentage of neutral reviews (computed)
     */
    private BigDecimal neutralPercentage;
}
