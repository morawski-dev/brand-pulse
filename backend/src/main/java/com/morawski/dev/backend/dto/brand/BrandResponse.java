package com.morawski.dev.backend.dto.brand;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Response DTO for brand information.
 * Maps to brands table with additional computed fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrandResponse {

    /**
     * Brand ID from brands.id
     */
    private Long brandId;

    /**
     * Owner user ID from brands.user_id
     */
    private Long userId;

    /**
     * Brand name from brands.name
     */
    private String name;

    /**
     * Count of active review sources (computed from review_sources)
     */
    private Integer sourceCount;

    /**
     * Last manual refresh timestamp from brands.last_manual_refresh_at
     */
    private ZonedDateTime lastManualRefreshAt;

    /**
     * Creation timestamp from brands.created_at
     */
    private ZonedDateTime createdAt;

    /**
     * Last update timestamp from brands.updated_at
     */
    private ZonedDateTime updatedAt;
}
