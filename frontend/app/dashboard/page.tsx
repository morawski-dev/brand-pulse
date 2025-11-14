/**
 * Dashboard Page - Main application dashboard
 *
 * Route: /dashboard
 * Protected: Requires authentication and completed onboarding
 *
 * Features:
 * - Aggregated review list with filters (US-004, US-005)
 * - Sentiment analysis summary with AI insights
 * - Source switcher - "All locations" vs individual sources (US-006)
 * - Manual refresh button with 24h cooldown (US-008)
 * - Manual sentiment correction (US-007)
 */

'use client';

import { Metadata } from 'next';
import React, { useState, useCallback } from 'react';
import { toast } from 'sonner';
import {
  DashboardLayout,
  MetricsSection,
  FiltersBar,
  ReviewsList,
  KeyboardShortcutsHelp,
} from './components';
import {
  useDashboardData,
  useReviewFilters,
  useManualRefresh,
  useSentimentUpdate,
  useKeyboardNavigation,
} from '@/lib/hooks';
import { toastMessages, getSentimentLabel } from '@/lib/utils/toastMessages';
import type { Sentiment } from '@/lib/types/common';
import type { ReviewViewModel } from '@/lib/types/review';

// ========================================
// COMPONENT
// ========================================

export default function DashboardPage() {
  // ========================================
  // STATE - Location Selection
  // ========================================

  const [selectedLocation, setSelectedLocation] = useState<'all' | number>('all');

  // ========================================
  // HOOKS - Filters
  // ========================================

  const {
    filters,
    setFilter,
    clearFilter,
    clearAllFilters,
    activeFilterCount,
  } = useReviewFilters();

  // ========================================
  // HOOKS - Dashboard Data
  // ========================================

  const {
    brand,
    sources,
    summary,
    reviews,
    pagination,
    isLoading,
    isLoadingMore,
    isSummaryLoading,
    isReviewsLoading,
    error,
    refetch,
    loadMore,
    invalidate,
  } = useDashboardData({
    // brandId is auto-fetched by the hook if not provided
    sourceId: selectedLocation === 'all' ? null : selectedLocation,
    filters,
    page: 0,
    pageSize: 20,
  });

  // ========================================
  // HOOKS - Manual Refresh
  // ========================================

  const {
    triggerRefresh,
    isRefreshing,
    canRefresh,
    timeRemaining,
  } = useManualRefresh({
    brandId: brand?.brandId || 0,
    lastManualRefreshAt: brand?.lastManualRefreshAt,
    onSuccess: () => {
      // Refetch dashboard data after successful sync
      invalidate();
      toast.success(toastMessages.refresh.success);
    },
    onError: (errorMsg) => {
      toast.error(errorMsg);
    },
  });

  // ========================================
  // HOOKS - Sentiment Update
  // ========================================

  const { updateSentiment, isUpdating } = useSentimentUpdate({
    brandId: brand?.brandId || 0,
    onSuccess: (reviewId, newSentiment) => {
      // Refetch summary to update metrics
      invalidate();
      toast.success(toastMessages.sentimentUpdate.success(getSentimentLabel(newSentiment)));
    },
    onError: (errorMsg) => {
      toast.error(errorMsg || toastMessages.sentimentUpdate.error);
    },
  });

  // ========================================
  // COMPUTED - Empty State
  // ========================================

  const isEmpty = !isLoading && reviews.length === 0;
  const hasActiveFilters = activeFilterCount > 0;

  // ========================================
  // HOOKS - Keyboard Navigation
  // ========================================

  const { selectedIndex } = useKeyboardNavigation({
    itemCount: reviews.length,
    disabled: isLoading || isEmpty,
  });

  // ========================================
  // HANDLERS - Location Change
  // ========================================

  const handleLocationChange = useCallback((locationId: 'all' | number) => {
    setSelectedLocation(locationId);
  }, []);

  // ========================================
  // HANDLERS - Sentiment Change (Optimistic Update)
  // ========================================

  const handleSentimentChange = useCallback(
    async (reviewId: number, newSentiment: Sentiment) => {
      // Find the review
      const review = reviews.find((r) => r.reviewId === reviewId);
      if (!review) return;

      // Perform API call (useSentimentUpdate handles optimistic update internally)
      await updateSentiment(reviewId, newSentiment);
    },
    [reviews, updateSentiment]
  );

  // ========================================
  // HANDLERS - Filter Change
  // ========================================

  const handleFilterChange = useCallback(
    (newFilters: typeof filters) => {
      // Filters are already updated by useReviewFilters
      // This will trigger refetch in useDashboardData via useEffect
    },
    []
  );

  // ========================================
  // ERROR STATE
  // ========================================

  if (error && !isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 via-white to-purple-50">
        <div className="max-w-md w-full mx-4">
          <div className="rounded-lg border bg-white p-8 shadow-lg text-center space-y-4">
            <div className="flex justify-center">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-red-100">
                <svg className="h-8 w-8 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
              </div>
            </div>
            <h2 className="text-xl font-semibold text-gray-900">Wystąpił błąd</h2>
            <p className="text-sm text-gray-600">{error.message}</p>
            <button
              onClick={() => refetch()}
              className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              Spróbuj ponownie
            </button>
          </div>
        </div>
      </div>
    );
  }

  // ========================================
  // RENDER
  // ========================================

  return (
    <>
      <DashboardLayout
        brand={brand}
        sources={sources}
        selectedLocation={selectedLocation}
        onLocationChange={handleLocationChange}
      >
        <div className="space-y-6">
        {/* Metrics Section */}
        <MetricsSection
          summary={summary}
          isLoading={isSummaryLoading}
        />

        {/* Filters Bar */}
        <FiltersBar
          filters={filters}
          onFiltersChange={handleFilterChange}
          onFilterRemove={clearFilter}
          onClearAllFilters={clearAllFilters}
          onRefresh={brand ? triggerRefresh : undefined}
          canRefresh={canRefresh}
          isRefreshing={isRefreshing}
          timeRemaining={timeRemaining}
        />

        {/* Reviews List */}
        <ReviewsList
          reviews={reviews}
          pagination={pagination}
          onLoadMore={loadMore}
          isLoading={isReviewsLoading}
          isLoadingMore={isLoadingMore}
          isEmpty={isEmpty}
          hasActiveFilters={hasActiveFilters}
          onClearFilters={clearAllFilters}
          onAddSource={() => {
            // TODO: Navigate to add source page
            console.log('Navigate to add source');
          }}
          onSentimentChange={handleSentimentChange}
          selectedIndex={selectedIndex}
        />
        </div>
      </DashboardLayout>

      {/* Keyboard Shortcuts Help */}
      <KeyboardShortcutsHelp />
    </>
  );
}
