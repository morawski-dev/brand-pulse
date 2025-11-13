/**
 * MetricsSection - Container for all metrics cards
 *
 * Features:
 * - Grid layout for metric cards (4 columns desktop, 2 tablet, 1 mobile)
 * - 4 metric cards: average rating, total reviews, positive %, negative %
 * - AI summary card (full width below metrics)
 * - Loading skeletons for all cards
 * - Responsive design
 *
 * Uses data from DashboardSummaryResponse.metrics
 */

'use client';

import React from 'react';
import { MetricCard } from './MetricCard';
import { AISummaryCard } from './AISummaryCard';
import type { DashboardSummaryResponse } from '@/lib/types/dashboard';

// ========================================
// TYPES
// ========================================

interface MetricsSectionProps {
  summary: DashboardSummaryResponse | null;
  isLoading?: boolean;
}

// ========================================
// HELPER FUNCTIONS
// ========================================

function formatRating(rating: number): string {
  return rating.toFixed(1);
}

function formatPercentage(percentage: number): string {
  return `${percentage.toFixed(1)}%`;
}

function formatCount(count: number): string {
  if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`;
  }
  return count.toString();
}

// ========================================
// COMPONENT
// ========================================

export function MetricsSection({ summary, isLoading = false }: MetricsSectionProps) {
  return (
    <div className="space-y-6">
      {/* Metrics Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Average Rating */}
        <MetricCard
          label="Średnia ocena"
          value={summary?.metrics ? formatRating(summary.metrics.averageRating) : '-'}
          icon={
            <svg className="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
              <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
            </svg>
          }
          isLoading={isLoading}
        />

        {/* Total Reviews */}
        <MetricCard
          label="Liczba opinii"
          value={summary?.metrics ? formatCount(summary.metrics.totalReviews) : '-'}
          icon={
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z" />
            </svg>
          }
          isLoading={isLoading}
        />

        {/* Positive Sentiment */}
        <MetricCard
          label="Pozytywne"
          value={
            summary?.metrics?.sentimentDistribution
              ? formatPercentage(summary.metrics.sentimentDistribution.positivePercentage)
              : '-'
          }
          icon={
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5" />
            </svg>
          }
          isLoading={isLoading}
        />

        {/* Negative Sentiment */}
        <MetricCard
          label="Negatywne"
          value={
            summary?.metrics?.sentimentDistribution
              ? formatPercentage(summary.metrics.sentimentDistribution.negativePercentage)
              : '-'
          }
          icon={
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 14H5.236a2 2 0 01-1.789-2.894l3.5-7A2 2 0 018.736 3h4.018a2 2 0 01.485.06l3.76.94m-7 10v5a2 2 0 002 2h.096c.5 0 .905-.405.905-.904 0-.715.211-1.413.608-2.008L17 13V4m-7 10h2m5-10h2a2 2 0 012 2v6a2 2 0 01-2 2h-2.5" />
            </svg>
          }
          isLoading={isLoading}
        />
      </div>

      {/* AI Summary Card - Full Width */}
      <AISummaryCard
        aiSummary={summary?.aiSummary ?? null}
        isLoading={isLoading}
      />
    </div>
  );
}
