package com.morawski.dev.backend.controller;

import com.morawski.dev.backend.dto.dashboard.AISummaryResponse;
import com.morawski.dev.backend.dto.dashboard.DashboardResponse;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.security.SecurityUtils;
import com.morawski.dev.backend.service.AISummaryService;
import com.morawski.dev.backend.service.BrandService;
import com.morawski.dev.backend.service.DashboardService;
import com.morawski.dev.backend.service.ReviewSourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for dashboard and summary endpoints.
 * Provides aggregated metrics, AI insights, and dashboard data.
 *
 * API Plan Section 8: Dashboard Endpoints
 * Base URL: /api/dashboard
 *
 * Endpoints:
 * - GET /summary - Get dashboard summary (Section 8.1, US-004, US-006)
 * - GET /summary/ai - Get AI summary for source (Section 8.2)
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;
    private final AISummaryService aiSummaryService;
    private final ReviewSourceService reviewSourceService;
    private final BrandService brandService;

    /**
     * Get dashboard summary with metrics and AI insights.
     * API: GET /api/dashboard/summary (Section 8.1)
     * User Stories: US-004 (Viewing Aggregated Reviews), US-006 (Switching Between Locations)
     *
     * Query Parameters:
     * - brandId (required): Brand ID to get summary for
     * - sourceId (optional): Filter by specific source. If omitted, returns aggregated data for all sources
     *
     * Business Logic:
     * - Pull data from dashboard_aggregates table (pre-calculated during CRON job)
     * - If sourceId provided, filter aggregates by that source only
     * - If sourceId omitted, SUM aggregates across all sources for the brand (US-006 "All locations")
     * - Fetch latest valid AI summary from ai_summaries WHERE valid_until > NOW()
     * - Include top 3 most recent negative reviews (rating <= 2)
     * - Use cached results (Spring Cache with Caffeine, 10-minute TTL)
     *
     * Performance Optimization:
     * - Critical: Must load in <4 seconds (PRD requirement)
     * - Pre-calculation in dashboard_aggregates reduces query complexity
     * - Caching strategy: Cache key dashboard:brand:{brandId}:source:{sourceId}
     *
     * Success Response: 200 OK with dashboard summary
     * Error Responses:
     * - 400 Bad Request: Invalid date format, endDate before startDate
     * - 403 Forbidden: User doesn't own this brand
     * - 404 Not Found: Brand or source doesn't exist
     *
     * @param brandId Brand ID
     * @param sourceId Optional source ID filter
     * @return DashboardResponse with metrics and AI summary
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardResponse> getDashboardSummary(
        @RequestParam Long brandId,
        @RequestParam(required = false) Long sourceId
    ) {
        log.info("GET /api/dashboard/summary - Dashboard summary request received for brand {}", brandId);

        Long userId = SecurityUtils.getCurrentUserId();
        DashboardResponse response = dashboardService.getDashboard(brandId, sourceId, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get AI-generated summary for review source.
     * API: GET /api/dashboard/summary/ai (Section 8.2)
     *
     * Business Logic:
     * - Return latest summary WHERE valid_until > NOW()
     * - If no valid summary exists, trigger async generation and return 202 Accepted
     * - Cache with key: summary:source:{sourceId}
     *
     * Success Response: 200 OK with AI summary
     * Error Responses:
     * - 202 Accepted: No valid summary exists, generation in progress
     * - 403 Forbidden: User doesn't own this source
     * - 404 Not Found: Source doesn't exist
     *
     * @param sourceId Review source ID
     * @return AISummaryResponse with AI-generated summary
     */
    @GetMapping("/summary/ai")
    public ResponseEntity<AISummaryResponse> getAISummary(@RequestParam Long sourceId) {
        log.info("GET /api/dashboard/summary/ai - AI summary request received for source {}", sourceId);

        // Verify user owns the source (through brand ownership)
        Long userId = SecurityUtils.getCurrentUserId();
        ReviewSource source = reviewSourceService.findByIdOrThrow(sourceId);
        brandService.findByIdAndUserIdOrThrow(source.getBrand().getId(), userId);

        AISummaryResponse response = aiSummaryService.getSummaryForSource(sourceId);
        return ResponseEntity.ok(response);
    }
}
