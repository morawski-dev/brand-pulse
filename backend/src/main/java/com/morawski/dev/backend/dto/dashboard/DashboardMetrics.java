package com.morawski.dev.backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for dashboard metrics.
 * Contains aggregated review statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetrics {

    private Long totalReviews;
    private Double averageRating;

    // Sentiment counts
    private Long positiveCount;
    private Long negativeCount;
    private Long neutralCount;

    // Sentiment percentages
    private Double positivePercentage;
    private Double negativePercentage;
    private Double neutralPercentage;

    // Rating distribution
    private Long rating5Count;
    private Long rating4Count;
    private Long rating3Count;
    private Long rating2Count;
    private Long rating1Count;
}
