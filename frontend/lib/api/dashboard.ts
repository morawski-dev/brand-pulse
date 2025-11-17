/**
 * Dashboard API - API functions for dashboard endpoints
 *
 * Endpoints:
 * - GET /api/dashboard/summary - Dashboard summary with metrics and AI insights
 * - GET /api/brands/{brandId}/reviews - Paginated review list with filters
 */

import { apiClient } from './client';
import {
  DashboardSummaryResponse,
  DashboardSummaryQueryParams,
} from '@/lib/types/dashboard';
import {
  ReviewListResponse,
  ReviewListQueryParams,
} from '@/lib/types/review';
import { Sentiment, Rating } from '@/lib/types/common';

// ========================================
// DASHBOARD SUMMARY
// ========================================

/**
 * Fetch dashboard summary with metrics and AI insights
 *
 * GET /api/dashboard/summary
 *
 * Query parameters:
 * - brandId (required): Brand ID
 * - sourceId (optional): Filter by specific source, omit for "All locations"
 * - startDate (optional): Start date for metrics (YYYY-MM-DD)
 * - endDate (optional): End date for metrics (YYYY-MM-DD)
 *
 * Success (200):
 * - Returns DashboardSummaryResponse with metrics, AI summary, recent negative reviews
 *
 * Errors:
 * - 400 BAD_REQUEST: Invalid date format or endDate before startDate
 * - 401 UNAUTHORIZED: Missing or invalid JWT token
 * - 403 FORBIDDEN: User doesn't own this brand
 * - 404 NOT_FOUND: Brand or source not found
 * - 500 INTERNAL_SERVER_ERROR: Server error
 *
 * @param params - Query parameters
 * @returns Dashboard summary response
 * @throws ApiException on error
 */
export async function fetchDashboardSummary(
  params: DashboardSummaryQueryParams
): Promise<DashboardSummaryResponse> {
  const queryParams: Record<string, any> = {
    brandId: params.brandId,
  };

  if (params.sourceId !== undefined && params.sourceId !== null) {
    queryParams.sourceId = params.sourceId;
  }

  if (params.startDate) {
    queryParams.startDate = params.startDate;
  }

  if (params.endDate) {
    queryParams.endDate = params.endDate;
  }

  return await apiClient.get<DashboardSummaryResponse>(
    '/api/dashboard/summary',
    queryParams
  );
}

// ========================================
// REVIEWS LIST
// ========================================

/**
 * Fetch paginated reviews list with filters
 *
 * GET /api/brands/{brandId}/reviews
 *
 * Query parameters:
 * - sourceId (optional): Filter by source ID
 * - sentiment (optional): Filter by sentiment (comma-separated: POSITIVE,NEGATIVE,NEUTRAL)
 * - rating (optional): Filter by star rating (comma-separated: 1,2,3,4,5)
 * - startDate (optional): Filter reviews published after this date (ISO 8601)
 * - endDate (optional): Filter reviews published before this date (ISO 8601)
 * - page (optional): Page number (0-indexed, default: 0)
 * - size (optional): Items per page (default: 20, max: 100)
 * - sort (optional): Sort field and direction (default: publishedAt,desc)
 *
 * Success (200):
 * - Returns ReviewListResponse with paginated reviews
 *
 * Errors:
 * - 400 BAD_REQUEST: Invalid filter values or date format
 * - 401 UNAUTHORIZED: Missing or invalid JWT token
 * - 403 FORBIDDEN: User doesn't own this brand
 * - 404 NOT_FOUND: Brand not found
 * - 500 INTERNAL_SERVER_ERROR: Server error
 *
 * @param params - Query parameters
 * @returns Paginated review list response
 * @throws ApiException on error
 */
export async function fetchReviews(
  params: ReviewListQueryParams
): Promise<ReviewListResponse> {
  const { brandId, ...restParams } = params;

  const queryParams: Record<string, any> = {};

  if (restParams.sourceId !== undefined && restParams.sourceId !== null) {
    queryParams.sourceId = restParams.sourceId;
  }

  if (restParams.sentiment) {
    queryParams.sentiment = restParams.sentiment;
  }

  if (restParams.rating) {
    queryParams.rating = restParams.rating;
  }

  if (restParams.startDate) {
    queryParams.startDate = restParams.startDate;
  }

  if (restParams.endDate) {
    queryParams.endDate = restParams.endDate;
  }

  if (restParams.page !== undefined) {
    queryParams.page = restParams.page;
  }

  if (restParams.size !== undefined) {
    queryParams.size = restParams.size;
  }

  if (restParams.sort) {
    queryParams.sort = restParams.sort;
  }

  return await apiClient.get<ReviewListResponse>(
    `/api/brands/${brandId}/reviews`,
    queryParams
  );
}

// ========================================
// HELPER FUNCTIONS
// ========================================

/**
 * Build sentiment query parameter from Set
 * Converts Set<Sentiment> to comma-separated string
 */
export function buildSentimentParam(sentiments: Set<Sentiment>): string | undefined {
  if (sentiments.size === 0) return undefined;
  return Array.from(sentiments).join(',');
}

/**
 * Build rating query parameter from Set
 * Converts Set<Rating> to comma-separated string
 */
export function buildRatingParam(ratings: Set<Rating>): string | undefined {
  if (ratings.size === 0) return undefined;
  return Array.from(ratings).join(',');
}

/**
 * Convert Date to LocalDate string (YYYY-MM-DD)
 */
export function formatLocalDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Convert Date to ISO 8601 string
 */
export function formatISODate(date: Date): string {
  return date.toISOString();
}
