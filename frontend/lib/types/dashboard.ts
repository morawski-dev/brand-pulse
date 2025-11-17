/**
 * Dashboard Types - Types for dashboard-related API responses
 * Based on backend DTO dashboard types
 */

import { ISODateString, SourceType } from './common';
import { ReviewResponse } from './review';

// ========================================
// DASHBOARD RESPONSE TYPES
// ========================================

/**
 * Sentiment distribution metrics
 * Maps to SentimentDistributionResponse in backend
 */
export interface SentimentDistributionResponse {
  positive: number;
  negative: number;
  neutral: number;
  positivePercentage: number;
  negativePercentage: number;
  neutralPercentage: number;
}

/**
 * Rating distribution (1-5 stars)
 * Key: star rating, Value: count of reviews
 */
export interface RatingDistribution {
  1: number;
  2: number;
  3: number;
  4: number;
  5: number;
}

/**
 * Dashboard metrics aggregate
 * Maps to MetricsResponse in backend
 */
export interface MetricsResponse {
  totalReviews: number;
  averageRating: number;
  sentimentDistribution: SentimentDistributionResponse;
  ratingDistribution: RatingDistribution;
}

/**
 * AI-generated summary
 * Maps to AISummaryResponse in backend
 */
export interface AISummaryResponse {
  summaryId: number;
  sourceId?: number | null;
  text: string;
  modelUsed: string;
  tokenCount?: number;
  generatedAt: ISODateString;
  validUntil: ISODateString;
}

/**
 * Time period for dashboard data
 * Maps to PeriodResponse in backend
 */
export interface PeriodResponse {
  startDate: string; // LocalDate as string (YYYY-MM-DD)
  endDate: string; // LocalDate as string (YYYY-MM-DD)
}

/**
 * Dashboard summary response
 * Maps to DashboardSummaryResponse in backend
 */
export interface DashboardSummaryResponse {
  brandId: number;
  sourceId: number | null;
  sourceName: string | null;
  period: PeriodResponse;
  metrics: MetricsResponse;
  aiSummary: AISummaryResponse | null;
  recentNegativeReviews: ReviewResponse[];
  lastUpdated: ISODateString;
}

// ========================================
// DASHBOARD QUERY PARAMETERS
// ========================================

/**
 * Dashboard summary query parameters
 */
export interface DashboardSummaryQueryParams {
  brandId: number;
  sourceId?: number | null;
  startDate?: string; // LocalDate as string
  endDate?: string; // LocalDate as string
}

// ========================================
// VIEW MODEL TYPES (Frontend-specific)
// ========================================

/**
 * Dashboard UI state
 */
export interface DashboardUIState {
  isLoading: boolean;
  isRefreshing: boolean;
  error: string | null;
  selectedLocation: 'all' | number;
}

/**
 * Combined dashboard data (all data needed for dashboard page)
 */
export interface DashboardData {
  brand: BrandSummary | null;
  sources: ReviewSourceSummary[];
  summary: DashboardSummaryResponse | null;
}

/**
 * Brand summary (minimal data for header)
 */
export interface BrandSummary {
  brandId: number;
  name: string;
  lastManualRefreshAt: ISODateString | null;
}

/**
 * Review source summary (minimal data for selector)
 */
export interface ReviewSourceSummary {
  sourceId: number;
  sourceType: SourceType;
  profileUrl: string;
  isActive: boolean;
  lastSyncAt: ISODateString | null;
  lastSyncStatus: 'SUCCESS' | 'FAILED' | null;
}

/**
 * Metric card data (for MetricCard component)
 */
export interface MetricCardData {
  label: string;
  value: string | number;
  icon: React.ReactNode;
  trend?: {
    value: number;
    direction: 'up' | 'down' | 'neutral';
  };
}
