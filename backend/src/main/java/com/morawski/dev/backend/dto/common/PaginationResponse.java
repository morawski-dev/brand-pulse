package com.morawski.dev.backend.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Common pagination response wrapper for list endpoints.
 * Used across multiple API endpoints to provide consistent pagination metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginationResponse {

    /**
     * Current page number (0-indexed)
     */
    private Integer currentPage;

    /**
     * Number of items per page
     */
    private Integer pageSize;

    /**
     * Total number of items across all pages
     */
    private Long totalItems;

    /**
     * Total number of pages
     */
    private Integer totalPages;

    /**
     * Whether there is a next page available
     */
    private Boolean hasNext;

    /**
     * Whether there is a previous page available
     */
    private Boolean hasPrevious;
}
