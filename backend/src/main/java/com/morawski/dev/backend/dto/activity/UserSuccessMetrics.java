package com.morawski.dev.backend.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for user success metrics.
 * Tracks individual user progress against MVP success criteria.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSuccessMetrics {

    private Long userId;
    private Instant registrationDate;

    // Time to Value metrics
    private Long timeToValueMinutes;
    private Boolean timeToValueAchieved; // < 10 minutes target

    // Activation metrics
    private Boolean activationAchieved; // Source configured within 7 days

    // Retention metrics
    private Boolean retentionAchieved; // 3+ logins in first 4 weeks
    private Long totalLogins;

    // AI accuracy tracking
    private Long sentimentCorrections;
}
