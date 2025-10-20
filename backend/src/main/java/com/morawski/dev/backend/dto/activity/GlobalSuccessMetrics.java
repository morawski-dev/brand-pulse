package com.morawski.dev.backend.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for global success metrics across all users.
 * Used for admin dashboard and business analytics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSuccessMetrics {

    private Instant periodStart;
    private Instant periodEnd;
    private Long totalUsers;

    // Time to Value metrics
    private Long timeToValueAchievedCount;
    private Double timeToValueAchievedPercentage;
    private Double averageTimeToValueMinutes;

    // Activation metrics
    private Long activationAchievedCount;
    private Double activationAchievedPercentage;

    // Retention metrics
    private Long retentionAchievedCount;
    private Double retentionAchievedPercentage;
}
