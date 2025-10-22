package com.morawski.dev.backend.dto.sync;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Response DTO for manual sync trigger operation (US-008).
 * Returns created sync jobs and next available sync time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TriggerSyncResponse {

    /**
     * Success message
     */
    private String message;

    /**
     * List of created sync jobs
     */
    private List<SyncJobResponse> jobs;

    /**
     * Next timestamp when manual sync will be available
     * (24 hours from now due to rate limiting)
     */
    private ZonedDateTime nextManualSyncAvailableAt;
}
