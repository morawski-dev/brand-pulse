package com.morawski.dev.backend.dto.source;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response wrapper for list of review sources.
 * Used in GET /api/brands/{brandId}/review-sources endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSourceListResponse {

    /**
     * List of review sources for the specified brand
     */
    private List<ReviewSourceResponse> sources;
}
