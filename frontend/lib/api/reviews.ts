/**
 * Reviews API - API functions for review management endpoints
 *
 * Endpoints:
 * - PATCH /api/brands/{brandId}/reviews/{reviewId}/sentiment - Update review sentiment
 * - GET /api/brands/{brandId}/reviews/{reviewId} - Get review details (future)
 */

import { apiClient } from './client';
import {
  UpdateReviewSentimentRequest,
  UpdateReviewSentimentResponse,
  ReviewDetailResponse,
} from '@/lib/types/review';
import { Sentiment } from '@/lib/types/common';

// ========================================
// UPDATE REVIEW SENTIMENT
// ========================================

/**
 * Update review sentiment (manual correction by user)
 *
 * PATCH /api/brands/{brandId}/reviews/{reviewId}/sentiment
 *
 * Request body:
 * - sentiment: New sentiment value (POSITIVE, NEGATIVE, NEUTRAL)
 *
 * Success (200):
 * - Returns UpdateReviewSentimentResponse with updated sentiment and change ID
 * - Records change in sentiment_changes table for audit trail
 * - Invalidates cached dashboard metrics
 *
 * Errors:
 * - 400 BAD_REQUEST: Invalid sentiment value
 * - 401 UNAUTHORIZED: Missing or invalid JWT token
 * - 403 FORBIDDEN: User doesn't own this review's brand
 * - 404 NOT_FOUND: Review not found
 * - 500 INTERNAL_SERVER_ERROR: Server error
 *
 * Business logic:
 * - New sentiment must be different from current sentiment
 * - Change is recorded with user ID and timestamp
 * - Dashboard aggregates are recalculated asynchronously
 *
 * @param brandId - Brand ID
 * @param reviewId - Review ID
 * @param sentiment - New sentiment value
 * @returns Update response with previous and new sentiment
 * @throws ApiException on error
 */
export async function updateReviewSentiment(
  brandId: number,
  reviewId: number,
  sentiment: Sentiment
): Promise<UpdateReviewSentimentResponse> {
  const request: UpdateReviewSentimentRequest = {
    sentiment,
  };

  return await apiClient.patch<UpdateReviewSentimentResponse>(
    `/api/brands/${brandId}/reviews/${reviewId}/sentiment`,
    request
  );
}

// ========================================
// GET REVIEW DETAILS (Future implementation)
// ========================================

/**
 * Fetch detailed review information including sentiment change history
 *
 * GET /api/brands/{brandId}/reviews/{reviewId}
 *
 * Success (200):
 * - Returns ReviewDetailResponse with full review data and change history
 *
 * Errors:
 * - 401 UNAUTHORIZED: Missing or invalid JWT token
 * - 403 FORBIDDEN: User doesn't own this review's brand
 * - 404 NOT_FOUND: Review not found
 * - 500 INTERNAL_SERVER_ERROR: Server error
 *
 * @param brandId - Brand ID
 * @param reviewId - Review ID
 * @returns Detailed review response
 * @throws ApiException on error
 */
export async function fetchReviewDetails(
  brandId: number,
  reviewId: number
): Promise<ReviewDetailResponse> {
  return await apiClient.get<ReviewDetailResponse>(
    `/api/brands/${brandId}/reviews/${reviewId}`
  );
}
