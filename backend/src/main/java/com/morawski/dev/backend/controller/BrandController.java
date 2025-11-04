package com.morawski.dev.backend.controller;

import com.morawski.dev.backend.dto.brand.*;
import com.morawski.dev.backend.security.SecurityUtils;
import com.morawski.dev.backend.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for brand management endpoints.
 * Handles CRUD operations for brand entities.
 *
 * API Plan Section 5: Brand Endpoints
 * Base URL: /api/brands
 *
 * Endpoints:
 * - POST / - Create brand (Section 5.1, US-003 Step 1)
 * - GET / - Get user's brands (Section 5.2)
 * - GET /{brandId} - Get brand by ID (Section 5.3)
 * - PATCH /{brandId} - Update brand (Section 5.4)
 * - DELETE /{brandId} - Delete brand (Section 5.5)
 */
@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
@Slf4j
public class BrandController {

    private final BrandService brandService;

    /**
     * Create new brand for authenticated user.
     * API: POST /api/brands (Section 5.1)
     * User Story: US-003 - Configuring First Source (Step 1)
     *
     * Validation:
     * - name: Required, 1-255 characters
     *
     * Success Response: 201 Created
     * Error Responses:
     * - 400 Bad Request: Invalid name
     * - 409 Conflict: User already has a brand (MVP limitation)
     *
     * @param request Create brand request containing name
     * @return BrandResponse with created brand details
     */
    @PostMapping
    public ResponseEntity<BrandResponse> createBrand(@Valid @RequestBody CreateBrandRequest request) {
        log.info("POST /api/brands - Create brand request received");
        Long userId = SecurityUtils.getCurrentUserId();
        BrandResponse response = brandService.createBrand(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all brands for authenticated user.
     * API: GET /api/brands (Section 5.2)
     *
     * Business Logic:
     * - Filter brands by authenticated user (JWT userId)
     * - Exclude soft-deleted brands (deleted_at IS NULL)
     * - Include count of active review sources
     *
     * Success Response: 200 OK with list of brands
     *
     * @return BrandListResponse with user's brands
     */
    @GetMapping
    public ResponseEntity<BrandListResponse> getUserBrands() {
        log.info("GET /api/brands - Get user brands request received");
        Long userId = SecurityUtils.getCurrentUserId();
        BrandListResponse response = brandService.getUserBrands(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get brand by ID with details.
     * API: GET /api/brands/{brandId} (Section 5.3)
     *
     * Success Response: 200 OK with brand details including sources
     * Error Responses:
     * - 403 Forbidden: User doesn't own this brand
     * - 404 Not Found: Brand doesn't exist
     *
     * @param brandId Brand ID
     * @return BrandDetailResponse with brand and sources
     */
    @GetMapping("/{brandId}")
    public ResponseEntity<BrandDetailResponse> getBrandById(@PathVariable Long brandId) {
        log.info("GET /api/brands/{} - Get brand by ID request received", brandId);
        Long userId = SecurityUtils.getCurrentUserId();
        BrandDetailResponse response = brandService.getBrandById(userId, brandId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update brand information.
     * API: PATCH /api/brands/{brandId} (Section 5.4)
     *
     * Success Response: 200 OK with updated brand
     * Error Responses:
     * - 400 Bad Request: Invalid name
     * - 403 Forbidden: User doesn't own this brand
     * - 404 Not Found: Brand doesn't exist
     *
     * @param brandId Brand ID
     * @param request Update request containing new name
     * @return BrandResponse with updated brand details
     */
    @PatchMapping("/{brandId}")
    public ResponseEntity<BrandResponse> updateBrand(
        @PathVariable Long brandId,
        @Valid @RequestBody UpdateBrandRequest request
    ) {
        log.info("PATCH /api/brands/{} - Update brand request received", brandId);
        Long userId = SecurityUtils.getCurrentUserId();
        BrandResponse response = brandService.updateBrand(userId, brandId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete brand (soft delete).
     * API: DELETE /api/brands/{brandId} (Section 5.5)
     *
     * Business Logic:
     * - Soft delete: Set deleted_at=NOW()
     * - Cascade: All review_sources, reviews, dashboard_aggregates are also soft-deleted
     * - Hard delete after 90 days (background job)
     *
     * Success Response: 204 No Content
     * Error Responses:
     * - 403 Forbidden: User doesn't own this brand
     * - 404 Not Found: Brand doesn't exist
     *
     * @param brandId Brand ID
     * @return No content
     */
    @DeleteMapping("/{brandId}")
    public ResponseEntity<Void> deleteBrand(@PathVariable Long brandId) {
        log.info("DELETE /api/brands/{} - Delete brand request received", brandId);
        Long userId = SecurityUtils.getCurrentUserId();
        brandService.deleteBrand(userId, brandId);
        return ResponseEntity.noContent().build();
    }
}
