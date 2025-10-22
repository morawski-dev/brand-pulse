package com.morawski.dev.backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Response DTO for time period in dashboard queries.
 * Represents the date range for dashboard metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodResponse {

    /**
     * Start date of the period
     */
    private LocalDate startDate;

    /**
     * End date of the period
     */
    private LocalDate endDate;
}
