package com.morawski.dev.backend.dto.sync;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for triggering manual sync (US-008).
 * If sourceId is null, sync all sources for the brand.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TriggerSyncRequest {

    /**
     * Optional: specific review source ID to sync
     * If null, sync all sources for the brand
     */
    private Long sourceId;
}
