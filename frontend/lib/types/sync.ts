/**
 * Sync Types - Types for sync job-related API responses
 * Based on backend DTO sync types
 */

import { SyncStatus, SyncJobType, SourceType, ISODateString } from './common';

// ========================================
// SYNC RESPONSE TYPES
// ========================================

/**
 * Sync job response
 * Maps to SyncJobResponse in backend
 */
export interface SyncJobResponse {
  jobId: number;
  sourceId: number;
  sourceType: SourceType;
  jobType: SyncJobType;
  status: SyncStatus;
  createdAt: ISODateString;
}

/**
 * Sync job status response (detailed)
 * Maps to SyncJobStatusResponse in backend
 */
export interface SyncJobStatusResponse {
  jobId: number;
  jobType: SyncJobType;
  status: SyncStatus;
  reviewSourceId: number;
  reviewsFetched: number | null;
  reviewsNew: number | null;
  reviewsUpdated: number | null;
  createdAt: ISODateString;
  startedAt: ISODateString | null;
  completedAt: ISODateString | null;
  duration: string | null; // ISO 8601 duration format (e.g., "PT2M30S")
  errorMessage: string | null;
}

/**
 * Response after triggering manual sync
 * Maps to TriggerSyncResponse in backend
 */
export interface TriggerSyncResponse {
  message: string;
  jobs: SyncJobResponse[];
  nextManualSyncAvailableAt: ISODateString;
}

/**
 * Sync job list response
 * Maps to SyncJobListResponse in backend
 */
export interface SyncJobListResponse {
  jobs: SyncJobStatusResponse[];
  pagination: {
    currentPage: number;
    totalItems: number;
    totalPages: number;
  };
}

// ========================================
// SYNC REQUEST TYPES
// ========================================

/**
 * Request to trigger manual sync
 * Maps to TriggerSyncRequest in backend
 */
export interface TriggerSyncRequest {
  sourceId?: number; // Optional: sync specific source, or all if omitted
}

// ========================================
// VIEW MODEL TYPES (Frontend-specific)
// ========================================

/**
 * Manual refresh state (UI)
 */
export interface ManualRefreshState {
  isRefreshing: boolean;
  canRefresh: boolean;
  nextAvailableAt: Date | null;
  timeRemaining: string; // Human-readable (e.g., "22 godziny 15 minut")
  lastRefreshAt: Date | null;
}

/**
 * Sync job progress (for polling UI)
 */
export interface SyncJobProgress {
  jobId: number;
  status: SyncStatus;
  progress: number; // 0-100 percentage
  message: string;
  error?: string;
}
