package com.morawski.dev.backend.dto.brand;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.morawski.dev.backend.dto.source.ReviewSourceSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Detailed response DTO for brand with associated review sources.
 * Maps to brands table + review_sources table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrandDetailResponse {

    /**
     * Brand ID from brands.id
     */
    private Long brandId;

    /**
     * Brand name from brands.name
     */
    private String name;

    /**
     * Count of active review sources
     */
    private Integer sourceCount;

    /**
     * Last manual refresh timestamp from brands.last_manual_refresh_at
     */
    private ZonedDateTime lastManualRefreshAt;

    /**
     * List of review sources associated with this brand
     */
    private List<ReviewSourceSummary> sources;

    /**
     * Creation timestamp from brands.created_at
     */
    private ZonedDateTime createdAt;

    /**
     * Last update timestamp from brands.updated_at
     */
    private ZonedDateTime updatedAt;
}
