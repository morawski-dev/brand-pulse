package com.morawski.dev.backend.dto.brand;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response wrapper for list of brands.
 * Used in GET /api/brands endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandListResponse {

    /**
     * List of brands owned by the authenticated user
     */
    private List<BrandResponse> brands;
}
