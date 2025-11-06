package com.morawski.dev.backend.controller;

import com.morawski.dev.backend.dto.sync.SyncJobListResponse;
import com.morawski.dev.backend.dto.sync.SyncJobStatusResponse;
import com.morawski.dev.backend.dto.sync.TriggerSyncRequest;
import com.morawski.dev.backend.dto.sync.TriggerSyncResponse;
import com.morawski.dev.backend.security.SecurityUtils;
import com.morawski.dev.backend.service.SyncJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for sync job management endpoints.
 * Handles manual sync triggering, job status tracking, and job history.
 *
 * API Plan Section 9: Sync Job Endpoints
 * Base URL: /api
 *
 * Endpoints:
 * - POST /brands/{brandId}/sync - Trigger manual sync (Section 9.1, US-008)
 * - GET /sync-jobs/{jobId} - Get sync job status (Section 9.2)
 * - GET /brands/{brandId}/review-sources/{sourceId}/sync-jobs - List sync jobs (Section 9.3)
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class SyncJobController {

    private final SyncJobService syncJobService;

    /**
     * Trigger manual sync for brand sources.
     * API: POST /api/brands/{brandId}/sync (Section 9.1)
     * User Story: US-008 - Manual Data Refresh
     *
     * Request Body (optional):
     * - sourceId: If provided, sync only this source. If omitted, sync all sources for the brand.
     *
     * Business Logic:
     * 1. Check brands.last_manual_refresh_at
     * 2. If last_manual_refresh_at > (NOW() - INTERVAL '24 hours'), return 429 with time remaining
     * 3. Update brands.last_manual_refresh_at = NOW()
     * 4. Create sync_job(s) with job_type='MANUAL', status='PENDING'
     * 5. Trigger async sync process for each source
     * 6. Return job IDs for progress tracking
     * 7. Log activity: MANUAL_REFRESH_TRIGGERED
     *
     * Rate Limiting:
     * - 24-hour rolling window (not calendar day)
     * - Calculated: last_manual_refresh_at + 24 hours
     *
     * Success Response: 202 Accepted with job IDs
     * Error Responses:
     * - 403 Forbidden: User doesn't own this brand
     * - 429 Too Many Requests: Manual refresh already triggered in last 24 hours
     *
     * @param brandId Brand ID
     * @param request Optional trigger sync request with sourceId
     * @return TriggerSyncResponse with job details and next available sync time
     */
    @PostMapping("/api/brands/{brandId}/sync")
    public ResponseEntity<TriggerSyncResponse> triggerManualSync(
        @PathVariable Long brandId,
        @Valid @RequestBody(required = false) TriggerSyncRequest request
    ) {
        log.info("POST /api/brands/{}/sync - Trigger manual sync request received", brandId);
        Long userId = SecurityUtils.getCurrentUserId();
        Long sourceId = request != null ? request.getSourceId() : null;
        TriggerSyncResponse response = syncJobService.triggerManualSync(brandId, sourceId, userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Get sync job status by ID.
     * API: GET /api/sync-jobs/{jobId} (Section 9.2)
     *
     * Status Values:
     * - PENDING: Job queued, not started
     * - IN_PROGRESS: Currently fetching reviews
     * - COMPLETED: Successfully finished
     * - FAILED: Error occurred (see errorMessage)
     *
     * Business Logic:
     * - Used for polling during initial 90-day import (US-003)
     * - Used to track manual sync progress (US-008)
     * - Frontend can poll every 2-3 seconds until status != PENDING|IN_PROGRESS
     *
     * Success Response: 200 OK with job status
     * Error Responses:
     * - 403 Forbidden: User doesn't own the brand containing this source
     * - 404 Not Found: Job doesn't exist
     *
     * @param jobId Sync job ID
     * @return SyncJobStatusResponse with job status and statistics
     */
    @GetMapping("/api/sync-jobs/{jobId}")
    public ResponseEntity<SyncJobStatusResponse> getSyncJobStatus(@PathVariable Long jobId) {
        log.info("GET /api/sync-jobs/{} - Get sync job status request received", jobId);
        Long userId = SecurityUtils.getCurrentUserId();
        SyncJobStatusResponse response = syncJobService.getSyncJobStatus(jobId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * List sync jobs for review source with pagination.
     * API: GET /api/brands/{brandId}/review-sources/{sourceId}/sync-jobs (Section 9.3)
     *
     * Query Parameters:
     * - page (optional): Default 0
     * - size (optional): Default 20
     *
     * Note: Status filtering can be added in future iterations if needed.
     *
     * Business Logic:
     * - Ordered by created_at DESC
     * - Useful for debugging sync issues and monitoring import history
     *
     * Success Response: 200 OK with paginated job list
     * Error Responses:
     * - 403 Forbidden: User doesn't own this brand/source
     * - 404 Not Found: Source doesn't exist
     *
     * @param brandId Brand ID
     * @param sourceId Review source ID
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return SyncJobListResponse with paginated jobs
     */
    @GetMapping("/api/brands/{brandId}/review-sources/{sourceId}/sync-jobs")
    public ResponseEntity<SyncJobListResponse> getSyncJobs(
        @PathVariable Long brandId,
        @PathVariable Long sourceId,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size
    ) {
        log.info("GET /api/brands/{}/review-sources/{}/sync-jobs - List sync jobs request received",
            brandId, sourceId);
        Long userId = SecurityUtils.getCurrentUserId();
        SyncJobListResponse response = syncJobService.getSyncJobHistory(
            brandId,
            sourceId,
            userId,
            page,
            size
        );
        return ResponseEntity.ok(response);
    }
}
