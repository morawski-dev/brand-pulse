package com.morawski.dev.backend.dto.email;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.morawski.dev.backend.dto.common.DeliveryStatus;
import com.morawski.dev.backend.dto.common.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Response DTO for email report information.
 * Maps to email_reports table.
 * Used for tracking weekly email engagement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailReportResponse {

    /**
     * Report ID from email_reports.id
     */
    private Long reportId;

    /**
     * Report type from email_reports.report_type
     */
    private ReportType reportType;

    /**
     * Email sent timestamp from email_reports.sent_at
     */
    private ZonedDateTime sentAt;

    /**
     * Email opened timestamp from email_reports.opened_at
     */
    private ZonedDateTime openedAt;

    /**
     * Link clicked timestamp from email_reports.clicked_at
     */
    private ZonedDateTime clickedAt;

    /**
     * Report period start date from email_reports.period_start
     */
    private LocalDate periodStart;

    /**
     * Report period end date from email_reports.period_end
     */
    private LocalDate periodEnd;

    /**
     * Number of reviews in period from email_reports.reviews_count
     */
    private Integer reviewsCount;

    /**
     * Number of new negative reviews from email_reports.new_negative_count
     */
    private Integer newNegativeCount;

    /**
     * Email delivery status from email_reports.delivery_status
     */
    private DeliveryStatus deliveryStatus;
}
