package com.morawski.dev.backend.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response wrapper for list of email reports.
 * Used in GET /api/users/me/email-reports endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailReportListResponse {

    /**
     * List of email reports sent to the user
     */
    private List<EmailReportResponse> reports;
}
