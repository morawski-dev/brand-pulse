/**
 * Review Types - Types for review-related API responses and requests
 * Based on backend DTO review types
 */

import { Sentiment, SourceType, PaginationResponse, FilterResponse, ISODateString, Rating } from './common';

// ========================================
// REVIEW RESPONSE TYPES
// ========================================

/**
 * Review response DTO
 * Maps to ReviewResponse in backend
 */
export interface ReviewResponse {
  reviewId: number;
  sourceId: number;
  sourceType: SourceType;
  externalReviewId: string;
  content: string;
  authorName: string;
  rating: Rating;
  sentiment: Sentiment;
  sentimentConfidence: number;
  publishedAt: ISODateString;
  fetchedAt: ISODateString;
  createdAt: ISODateString;
}

/**
 * Paginated review list response
 * Maps to ReviewListResponse in backend
 */
export interface ReviewListResponse {
  reviews: ReviewResponse[];
  pagination: PaginationResponse;
  filters: FilterResponse;
}

/**
 * Detailed review response with sentiment change history
 * Maps to ReviewDetailResponse in backend
 */
export interface ReviewDetailResponse extends ReviewResponse {
  contentHash: string;
  updatedAt: ISODateString;
  sentimentChangeHistory?: SentimentChangeResponse[];
}

/**
 * Sentiment change history entry
 * Maps to SentimentChangeResponse in backend
 */
export interface SentimentChangeResponse {
  changeId: number;
  changedAt: ISODateString;
  oldSentiment: Sentiment | null;
  newSentiment: Sentiment;
  changeReason: string;
  changedByUserId: number | null;
}

// ========================================
// REVIEW REQUEST TYPES
// ========================================

/**
 * Request to update review sentiment
 * Maps to UpdateReviewSentimentRequest in backend
 */
export interface UpdateReviewSentimentRequest {
  sentiment: Sentiment;
}

/**
 * Response after updating review sentiment
 * Maps to UpdateReviewSentimentResponse in backend
 */
export interface UpdateReviewSentimentResponse {
  reviewId: number;
  sentiment: Sentiment;
  previousSentiment: Sentiment;
  updatedAt: ISODateString;
  sentimentChangeId: number;
}

// ========================================
// VIEW MODEL TYPES (Frontend-specific)
// ========================================

/**
 * Review view model with optimistic update support
 * Used in UI to show pending sentiment changes
 */
export interface ReviewViewModel extends ReviewResponse {
  isOptimisticUpdate?: boolean;
  previousSentiment?: Sentiment;
}

/**
 * Filter state for review list (UI state)
 */
export interface ReviewFilterState {
  sourceId: number | null;
  sentiment: Set<Sentiment>;
  rating: Set<Rating>;
  startDate: Date | null;
  endDate: Date | null;
}

/**
 * Review list query parameters
 */
export interface ReviewListQueryParams {
  brandId: number;
  sourceId?: number | null;
  sentiment?: string; // comma-separated
  rating?: string; // comma-separated
  startDate?: ISODateString;
  endDate?: ISODateString;
  page?: number;
  size?: number;
  sort?: string;
}
