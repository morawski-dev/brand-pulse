package com.morawski.dev.backend.controller;

import com.morawski.dev.backend.dto.source.*;
import com.morawski.dev.backend.security.SecurityUtils;
import com.morawski.dev.backend.service.ReviewSourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for review source management endpoints.
 * Handles CRUD operations for review sources (Google, Facebook, Trustpilot).
 *
 * API Plan Section 6: Review Source Endpoints
 * Base URL: /api/brands/{brandId}/review-sources
 *
 * Endpoints:
 * - POST / - Create review source (Section 6.1, US-003 Step 2, US-009)
 * - GET / - List review sources for brand (Section 6.2)
 * - GET /{sourceId} - Get review source by ID (Section 6.3)
 * - PATCH /{sourceId} - Update review source (Section 6.4)
 * - DELETE /{sourceId} - Delete review source (Section 6.5)
 */
@RestController
@RequestMapping("/api/brands/{brandId}/review-sources")
@RequiredArgsConstructor
@Slf4j
public class ReviewSourceController {

    private final ReviewSourceService reviewSourceService;

    /**
     * Create new review source for brand.
     * API: POST /api/brands/{brandId}/review-sources (Section 6.1)
     * User Stories: US-003 (Step 2), US-009 (Freemium limitation)
     *
     * Validation:
     * - sourceType: Required, must be 'GOOGLE', 'FACEBOOK', or 'TRUSTPILOT'
     * - profileUrl: Required, valid URL format
     * - externalProfileId: Required, unique per brand+sourceType
     * - authMethod: Required, must be 'API' or 'SCRAPING'
     *
     * Business Logic:
     * 1. Check user's plan: Count existing active sources for brand
     * 2. If count >= max_sources_allowed, return 403 with plan upgrade message
     * 3. Encrypt credentials using AES-256 before storing
     * 4. Create sync_job with job_type='INITIAL', status='PENDING'
     * 5. Trigger background job to import last 90 days of reviews
     * 6. Return importJobId for progress tracking
     *
     * Success Response: 201 Created with importJobId
     * Error Responses:
     * - 400 Bad Request: Invalid sourceType, invalid URL
     * - 403 Forbidden: User doesn't own brand, or plan limit exceeded
     * - 409 Conflict: Duplicate source (same brand+sourceType+externalProfileId)
     *
     * @param brandId Brand ID
     * @param request Create review source request
     * @return ReviewSourceResponse with created source and import job ID
     */
    @PostMapping
    public ResponseEntity<ReviewSourceResponse> createReviewSource(
        @PathVariable Long brandId,
        @Valid @RequestBody CreateReviewSourceRequest request
    ) {
        log.info("POST /api/brands/{}/review-sources - Create review source request received", brandId);
        Long userId = SecurityUtils.getCurrentUserId();
        ReviewSourceResponse response = reviewSourceService.createReviewSource(brandId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all review sources for brand.
     * API: GET /api/brands/{brandId}/review-sources (Section 6.2)
     *
     * Business Logic:
     * - Filter by brandId and deleted_at IS NULL
     * - Never return credentials_encrypted field in API responses (security)
     *
     * Success Response: 200 OK with list of sources
     * Error Responses:
     * - 403 Forbidden: User doesn't own this brand
     *
     * @param brandId Brand ID
     * @return ReviewSourceListResponse with list of sources
     */
    @GetMapping
    public ResponseEntity<ReviewSourceListResponse> getReviewSources(@PathVariable Long brandId) {
        log.info("GET /api/brands/{}/review-sources - List review sources request received", brandId);
        Long userId = SecurityUtils.getCurrentUserId();
        ReviewSourceListResponse response = reviewSourceService.getReviewSources(brandId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get review source by ID.
     * API: GET /api/brands/{brandId}/review-sources/{sourceId} (Section 6.3)
     *
     * Success Response: 200 OK with source details
     * Error Responses:
     * - 403 Forbidden: User doesn't own this brand/source
     * - 404 Not Found: Source doesn't exist
     *
     * @param brandId Brand ID
     * @param sourceId Review source ID
     * @return ReviewSourceResponse with source details
     */
    @GetMapping("/{sourceId}")
    public ResponseEntity<ReviewSourceResponse> getReviewSourceById(
        @PathVariable Long brandId,
        @PathVariable Long sourceId
    ) {
        log.info("GET /api/brands/{}/review-sources/{} - Get review source by ID request received",
            brandId, sourceId);
        Long userId = SecurityUtils.getCurrentUserId();
        ReviewSourceResponse response = reviewSourceService.getReviewSourceById(brandId, sourceId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update review source.
     * API: PATCH /api/brands/{brandId}/review-sources/{sourceId} (Section 6.4)
     *
     * Business Logic:
     * - Setting isActive=false pauses automatic syncs
     * - Updating URL or credentials may trigger validation job
     *
     * Success Response: 200 OK with updated source
     * Error Responses:
     * - 403 Forbidden: User doesn't own this brand/source
     * - 404 Not Found: Source doesn't exist
     *
     * @param brandId Brand ID
     * @param sourceId Review source ID
     * @param request Update request
     * @return ReviewSourceResponse with updated source details
     */
    @PatchMapping("/{sourceId}")
    public ResponseEntity<ReviewSourceResponse> updateReviewSource(
        @PathVariable Long brandId,
        @PathVariable Long sourceId,
        @Valid @RequestBody UpdateReviewSourceRequest request
    ) {
        log.info("PATCH /api/brands/{}/review-sources/{} - Update review source request received",
            brandId, sourceId);
        Long userId = SecurityUtils.getCurrentUserId();
        ReviewSourceResponse response = reviewSourceService.updateReviewSource(
            brandId, sourceId, userId, request
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Delete review source (soft delete).
     * API: DELETE /api/brands/{brandId}/review-sources/{sourceId} (Section 6.5)
     *
     * Business Logic:
     * - Soft delete: Set deleted_at=NOW()
     * - Cascade deletes reviews, dashboard_aggregates, ai_summaries, sync_jobs
     * - Log activity: SOURCE_DELETED
     *
     * Success Response: 204 No Content
     * Error Responses:
     * - 403 Forbidden: User doesn't own this brand/source
     * - 404 Not Found: Source doesn't exist
     *
     * @param brandId Brand ID
     * @param sourceId Review source ID
     * @return No content
     */
    @DeleteMapping("/{sourceId}")
    public ResponseEntity<Void> deleteReviewSource(
        @PathVariable Long brandId,
        @PathVariable Long sourceId
    ) {
        log.info("DELETE /api/brands/{}/review-sources/{} - Delete review source request received",
            brandId, sourceId);
        Long userId = SecurityUtils.getCurrentUserId();
        reviewSourceService.deleteReviewSource(brandId, sourceId, userId);
        return ResponseEntity.noContent().build();
    }
}
