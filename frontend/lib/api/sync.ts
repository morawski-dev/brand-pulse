/**
 * Sync API - API functions for manual data synchronization
 *
 * Endpoints:
 * - POST /api/brands/{brandId}/sync - Trigger manual sync
 * - GET /api/sync-jobs/{jobId} - Get sync job status
 * - GET /api/brands/{brandId}/review-sources/{sourceId}/sync-jobs - List sync jobs (future)
 */

import { apiClient } from './client';
import {
  TriggerSyncRequest,
  TriggerSyncResponse,
  SyncJobStatusResponse,
  SyncJobListResponse,
} from '@/lib/types/sync';

// ========================================
// TRIGGER MANUAL SYNC
// ========================================

/**
 * Trigger manual data synchronization
 *
 * POST /api/brands/{brandId}/sync
 *
 * Request body (optional):
 * - sourceId: Sync specific source only, omit to sync all sources
 *
 * Success (202 Accepted):
 * - Returns TriggerSyncResponse with created job IDs
 * - Updates brand.last_manual_refresh_at timestamp
 * - Returns nextManualSyncAvailableAt (24 hours from now)
 *
 * Errors:
 * - 401 UNAUTHORIZED: Missing or invalid JWT token
 * - 403 FORBIDDEN: User doesn't own this brand
 * - 404 NOT_FOUND: Brand or source not found
 * - 429 TOO_MANY_REQUESTS: Manual sync already triggered < 24h ago
 *   Response includes:
 *   - code: RATE_LIMIT_EXCEEDED
 *   - lastManualRefreshAt: Timestamp of last refresh
 *   - nextAvailableAt: When sync will be available
 *   - hoursRemaining: Hours until next sync
 * - 500 INTERNAL_SERVER_ERROR: Server error
 *
 * Business logic:
 * - Rate limited to once per 24 hours (rolling window)
 * - Creates sync_job records with status PENDING
 * - Background worker processes jobs asynchronously
 * - Jobs fetch new reviews from external APIs
 *
 * @param brandId - Brand ID
 * @param sourceId - Optional source ID (sync all if omitted)
 * @returns Trigger sync response with job IDs and next available time
 * @throws ApiException on error (including 429 rate limit)
 */
export async function triggerManualSync(
  brandId: number,
  sourceId?: number
): Promise<TriggerSyncResponse> {
  const request: TriggerSyncRequest = {};

  if (sourceId !== undefined) {
    request.sourceId = sourceId;
  }

  return await apiClient.post<TriggerSyncResponse>(
    `/api/brands/${brandId}/sync`,
    request
  );
}

// ========================================
// GET SYNC JOB STATUS
// ========================================

/**
 * Fetch sync job status for monitoring progress
 *
 * GET /api/sync-jobs/{jobId}
 *
 * Success (200):
 * - Returns SyncJobStatusResponse with job status and metrics
 *
 * Job status values:
 * - PENDING: Job queued, not started yet
 * - IN_PROGRESS: Currently fetching reviews
 * - COMPLETED: Job finished successfully
 * - FAILED: Job failed (see errorMessage)
 *
 * Errors:
 * - 401 UNAUTHORIZED: Missing or invalid JWT token
 * - 403 FORBIDDEN: User doesn't own the brand for this job's source
 * - 404 NOT_FOUND: Job not found
 * - 500 INTERNAL_SERVER_ERROR: Server error
 *
 * Usage:
 * - Poll this endpoint every 2-3 seconds during sync
 * - Stop polling when status is COMPLETED or FAILED
 * - Display progress metrics (reviewsFetched, reviewsNew, reviewsUpdated)
 * - Show duration and error message if available
 *
 * @param jobId - Sync job ID
 * @returns Sync job status response
 * @throws ApiException on error
 */
export async function getSyncJobStatus(
  jobId: number
): Promise<SyncJobStatusResponse> {
  return await apiClient.get<SyncJobStatusResponse>(
    `/api/sync-jobs/${jobId}`
  );
}

// ========================================
// LIST SYNC JOBS (Future implementation)
// ========================================

/**
 * Fetch sync job history for a source
 *
 * GET /api/brands/{brandId}/review-sources/{sourceId}/sync-jobs
 *
 * Query parameters:
 * - page (optional): Page number (0-indexed, default: 0)
 * - size (optional): Items per page (default: 20)
 * - status (optional): Filter by job status
 *
 * Success (200):
 * - Returns SyncJobListResponse with paginated job history
 *
 * Errors:
 * - 401 UNAUTHORIZED: Missing or invalid JWT token
 * - 403 FORBIDDEN: User doesn't own this brand/source
 * - 404 NOT_FOUND: Brand or source not found
 * - 500 INTERNAL_SERVER_ERROR: Server error
 *
 * @param brandId - Brand ID
 * @param sourceId - Review source ID
 * @param params - Query parameters
 * @returns Paginated sync job list
 * @throws ApiException on error
 */
export async function listSyncJobs(
  brandId: number,
  sourceId: number,
  params?: {
    page?: number;
    size?: number;
    status?: string;
  }
): Promise<SyncJobListResponse> {
  const queryParams: Record<string, any> = {};

  if (params?.page !== undefined) {
    queryParams.page = params.page;
  }

  if (params?.size !== undefined) {
    queryParams.size = params.size;
  }

  if (params?.status) {
    queryParams.status = params.status;
  }

  return await apiClient.get<SyncJobListResponse>(
    `/api/brands/${brandId}/review-sources/${sourceId}/sync-jobs`,
    queryParams
  );
}

// ========================================
// HELPER FUNCTIONS
// ========================================

/**
 * Poll sync job status until completion or timeout
 *
 * Usage:
 * ```typescript
 * const result = await pollSyncJobStatus(jobId, {
 *   onProgress: (status) => console.log(status),
 *   timeout: 180000, // 3 minutes
 * });
 * ```
 *
 * @param jobId - Job ID to poll
 * @param options - Polling options
 * @returns Final job status
 * @throws Error on timeout or job failure
 */
export async function pollSyncJobStatus(
  jobId: number,
  options?: {
    interval?: number; // Poll interval in ms (default: 3000)
    timeout?: number; // Timeout in ms (default: 180000 = 3min)
    onProgress?: (status: SyncJobStatusResponse) => void;
  }
): Promise<SyncJobStatusResponse> {
  const interval = options?.interval ?? 3000;
  const timeout = options?.timeout ?? 180000;
  const startTime = Date.now();

  while (true) {
    const status = await getSyncJobStatus(jobId);

    // Notify progress callback
    if (options?.onProgress) {
      options.onProgress(status);
    }

    // Job completed successfully
    if (status.status === 'COMPLETED') {
      return status;
    }

    // Job failed
    if (status.status === 'FAILED') {
      throw new Error(status.errorMessage || 'Synchronizacja nie powiodła się');
    }

    // Check timeout
    if (Date.now() - startTime > timeout) {
      throw new Error('Przekroczono czas oczekiwania na synchronizację');
    }

    // Wait before next poll
    await new Promise((resolve) => setTimeout(resolve, interval));
  }
}

/**
 * Calculate time remaining until next manual sync is available
 *
 * @param nextAvailableAt - ISO timestamp when sync will be available
 * @returns Human-readable string (e.g., "22 godziny 15 minut")
 */
export function calculateTimeRemaining(nextAvailableAt: string): string {
  const now = new Date();
  const available = new Date(nextAvailableAt);
  const diff = available.getTime() - now.getTime();

  if (diff <= 0) {
    return 'dostępne teraz';
  }

  const hours = Math.floor(diff / (1000 * 60 * 60));
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));

  if (hours > 0) {
    return `${hours} ${hours === 1 ? 'godzina' : hours < 5 ? 'godziny' : 'godzin'} ${minutes} ${minutes === 1 ? 'minuta' : minutes < 5 ? 'minuty' : 'minut'}`;
  }

  return `${minutes} ${minutes === 1 ? 'minuta' : minutes < 5 ? 'minuty' : 'minut'}`;
}
