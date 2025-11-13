/**
 * ReviewsList - List of reviews with infinite scroll support
 *
 * Features:
 * - Map reviews to ReviewCard components
 * - Intersection Observer for infinite scroll
 * - LoadMore button fallback
 * - Loading skeleton during initial load
 * - Empty state when no reviews
 * - Keyboard navigation support (passed via props)
 *
 * Note: Virtualization will be added in optimization phase (Phase 6)
 */

'use client';

import React, { useRef, useEffect } from 'react';
import { ReviewCard } from './ReviewCard';
import { ReviewsLoadingSkeleton } from './ReviewsLoadingSkeleton';
import { ReviewsEmptyState } from './ReviewsEmptyState';
import { Button } from '@/components/ui/button';
import type { ReviewViewModel } from '@/lib/types/review';
import type { PaginationResponse } from '@/lib/types/common';
import type { Sentiment } from '@/lib/types/common';

// ========================================
// TYPES
// ========================================

interface ReviewsListProps {
  reviews: ReviewViewModel[];
  pagination: PaginationResponse | null;
  onLoadMore: () => void;
  isLoading: boolean;
  isLoadingMore: boolean;
  isEmpty: boolean;
  hasActiveFilters: boolean;
  onClearFilters: () => void;
  onAddSource?: () => void;
  onSentimentChange: (reviewId: number, newSentiment: Sentiment) => void;
  selectedIndex?: number | null; // For keyboard navigation
}

// ========================================
// COMPONENT
// ========================================

export function ReviewsList({
  reviews,
  pagination,
  onLoadMore,
  isLoading,
  isLoadingMore,
  isEmpty,
  hasActiveFilters,
  onClearFilters,
  onAddSource,
  onSentimentChange,
  selectedIndex = null,
}: ReviewsListProps) {
  const loadMoreRef = useRef<HTMLDivElement>(null);

  // ========================================
  // INTERSECTION OBSERVER FOR INFINITE SCROLL
  // ========================================

  useEffect(() => {
    if (!loadMoreRef.current) return;
    if (!pagination?.hasNext) return;
    if (isLoadingMore) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          onLoadMore();
        }
      },
      {
        rootMargin: '200px', // Load when 200px from bottom
      }
    );

    observer.observe(loadMoreRef.current);

    return () => {
      observer.disconnect();
    };
  }, [pagination?.hasNext, isLoadingMore, onLoadMore]);

  // ========================================
  // RENDER LOADING
  // ========================================

  if (isLoading) {
    return <ReviewsLoadingSkeleton count={5} />;
  }

  // ========================================
  // RENDER EMPTY STATE
  // ========================================

  if (isEmpty) {
    return (
      <ReviewsEmptyState
        hasActiveFilters={hasActiveFilters}
        onClearFilters={onClearFilters}
        onAddSource={onAddSource}
      />
    );
  }

  // ========================================
  // RENDER REVIEWS LIST
  // ========================================

  return (
    <div className="space-y-4">
      {/* Reviews */}
      {reviews.map((review, index) => (
        <div key={review.reviewId} data-review-index={index}>
          <ReviewCard
            review={review}
            onSentimentChange={onSentimentChange}
            isSelected={selectedIndex === index}
          />
        </div>
      ))}

      {/* Loading More Indicator */}
      {isLoadingMore && (
        <ReviewsLoadingSkeleton count={3} />
      )}

      {/* Intersection Observer Target */}
      <div ref={loadMoreRef} className="h-4" />

      {/* Load More Button (Fallback) */}
      {pagination?.hasNext && !isLoadingMore && (
        <div className="flex justify-center pt-4">
          <Button onClick={onLoadMore} variant="outline">
            <svg className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
            </svg>
            Załaduj więcej
          </Button>
        </div>
      )}

      {/* End of List Message */}
      {!pagination?.hasNext && reviews.length > 0 && (
        <div className="text-center py-6 text-sm text-gray-500">
          Wyświetlono wszystkie opinie ({pagination?.totalItems} {pagination?.totalItems === 1 ? 'opinia' : 'opinii'})
        </div>
      )}
    </div>
  );
}
