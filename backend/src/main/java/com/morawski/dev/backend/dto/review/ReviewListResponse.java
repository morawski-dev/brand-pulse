package com.morawski.dev.backend.dto.review;

import com.morawski.dev.backend.dto.common.FilterResponse;
import com.morawski.dev.backend.dto.common.PaginationResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response wrapper for paginated review list (US-004, US-005).
 * Used in GET /api/brands/{brandId}/reviews endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewListResponse {

    /**
     * Paginated list of reviews
     */
    private List<ReviewResponse> reviews;

    /**
     * Pagination metadata
     */
    private PaginationResponse pagination;

    /**
     * Active filters applied to the query
     */
    private FilterResponse filters;
}
