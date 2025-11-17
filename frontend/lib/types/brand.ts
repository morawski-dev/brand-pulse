/**
 * Brand and Review Source types
 * Maps to backend DTOs from com.morawski.dev.backend.dto.brand and dto.source
 */

import { SourceType } from './onboarding';

// ========================================
// ENUMS (matching backend)
// ========================================

/**
 * Authentication method for review sources
 * Maps to backend AuthMethod enum
 */
export enum AuthMethod {
  API = 'API',
  SCRAPING = 'SCRAPING',
}

/**
 * Sync status for review sources
 * Maps to backend SyncStatus enum
 */
export enum SyncStatus {
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  IN_PROGRESS = 'IN_PROGRESS',
  PENDING = 'PENDING',
}

/**
 * Job status for import tracking
 * Maps to backend JobStatus enum
 */
export enum JobStatus {
  PENDING = 'PENDING',
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
}

// ========================================
// BRAND TYPES
// ========================================

/**
 * Request DTO for creating brand (matches backend CreateBrandRequest)
 */
export interface CreateBrandRequest {
  name: string;
}

/**
 * Response DTO for brand (matches backend BrandResponse)
 * Note: Backend uses ZonedDateTime which serializes to ISO 8601 string
 */
export interface BrandResponse {
  brandId: number;
  userId: number;
  name: string;
  sourceCount?: number;
  lastManualRefreshAt: string | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * Response DTO for list of brands (matches backend BrandListResponse)
 * Used in GET /api/brands endpoint
 */
export interface BrandListResponse {
  brands: BrandResponse[];
}

// ========================================
// REVIEW SOURCE TYPES
// ========================================

/**
 * Request DTO for creating review source (matches backend CreateReviewSourceRequest)
 */
export interface CreateReviewSourceRequest {
  sourceType: SourceType;
  profileUrl: string;
  externalProfileId: string;
  authMethod: AuthMethod;
  credentialsEncrypted?: Record<string, unknown>;
}

/**
 * Response DTO for review source (matches backend ReviewSourceResponse)
 * Note: Backend uses ZonedDateTime which serializes to ISO 8601 string
 */
export interface ReviewSourceResponse {
  sourceId: number;
  brandId: number;
  sourceType: SourceType;
  profileUrl: string;
  externalProfileId: string;
  authMethod: AuthMethod;
  isActive: boolean;
  lastSyncAt: string | null;
  lastSyncStatus: SyncStatus | null;
  lastSyncError: string | null;
  nextScheduledSyncAt: string | null;
  importJobId: number | null;
  importStatus: JobStatus | null;
  createdAt: string;
  updatedAt: string;
}

// ========================================
// IMPORT PROGRESS TYPES
// ========================================

/**
 * Import progress response from polling endpoint
 * GET /api/sources/{sourceId}/import-status
 */
export interface ImportProgressResponse {
  sourceId: number;
  jobId: number | null;
  status: JobStatus;
  progress: number; // 0-100
  statusMessage: string;
  reviewsImported: number;
  totalReviews: number;
  startedAt: string | null;
  completedAt: string | null;
  error: string | null;
}

// ========================================
// API ERROR TYPES
// ========================================

/**
 * API error response from backend
 */
export interface BrandApiError {
  code: string;
  message: string;
  errors?: Array<{
    field: string;
    message: string;
  }>;
}

/**
 * Exception class for API errors
 */
export class BrandApiException extends Error {
  code: string;
  errors?: Array<{ field: string; message: string }>;

  constructor(error: BrandApiError) {
    super(error.message);
    this.name = 'BrandApiException';
    this.code = error.code;
    this.errors = error.errors;
  }
}
