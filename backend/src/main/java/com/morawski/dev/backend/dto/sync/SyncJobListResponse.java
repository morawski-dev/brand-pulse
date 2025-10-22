package com.morawski.dev.backend.dto.sync;

import com.morawski.dev.backend.dto.common.PaginationResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response wrapper for paginated sync job list.
 * Used in GET /api/brands/{brandId}/review-sources/{sourceId}/sync-jobs endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncJobListResponse {

    /**
     * Paginated list of sync jobs
     */
    private List<SyncJobResponse> jobs;

    /**
     * Pagination metadata
     */
    private PaginationResponse pagination;
}
