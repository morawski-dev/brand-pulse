/**
 * Common Types - Shared types across the application
 * Based on backend DTO common types
 */

// ========================================
// ENUMS
// ========================================

/**
 * Review sentiment classification
 * Maps to Sentiment enum in backend
 */
export enum Sentiment {
  POSITIVE = 'POSITIVE',
  NEUTRAL = 'NEUTRAL',
  NEGATIVE = 'NEGATIVE',
}

/**
 * Review source platform types
 * Maps to SourceType enum in backend
 */
export enum SourceType {
  GOOGLE = 'GOOGLE',
  FACEBOOK = 'FACEBOOK',
  TRUSTPILOT = 'TRUSTPILOT',
}

/**
 * Sync job status
 * Maps to JobStatus enum in backend
 */
export enum SyncStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
}

/**
 * Sync job type
 * Maps to JobType enum in backend
 */
export enum SyncJobType {
  MANUAL = 'MANUAL',
  SCHEDULED = 'SCHEDULED',
  INITIAL = 'INITIAL',
}

/**
 * User plan types
 * Maps to PlanType enum in backend
 */
export enum PlanType {
  FREE = 'FREE',
  BASIC = 'BASIC',
  PRO = 'PRO',
  ENTERPRISE = 'ENTERPRISE',
}

// ========================================
// COMMON RESPONSE TYPES
// ========================================

/**
 * Pagination metadata for list responses
 * Maps to PaginationResponse in backend
 */
export interface PaginationResponse {
  currentPage: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

/**
 * Filter metadata for list responses
 * Maps to FilterResponse in backend
 */
export interface FilterResponse {
  sourceId?: number | null;
  sentiment?: Sentiment[];
  rating?: number[];
  startDate?: string | null;
  endDate?: string | null;
}

/**
 * Standard error response from backend
 * Maps to ErrorResponse in backend
 */
export interface ErrorResponse {
  code: string;
  message: string;
  details?: Record<string, any>;
  timestamp?: string;
}

// ========================================
// UTILITY TYPES
// ========================================

/**
 * Loading state for async operations
 */
export type LoadingState = 'idle' | 'loading' | 'success' | 'error';

/**
 * Generic async state wrapper
 */
export interface AsyncState<T> {
  data: T | null;
  loading: LoadingState;
  error: ErrorResponse | null;
}

/**
 * Rating type (1-5 stars)
 */
export type Rating = 1 | 2 | 3 | 4 | 5;

/**
 * ISO 8601 date string
 */
export type ISODateString = string;
