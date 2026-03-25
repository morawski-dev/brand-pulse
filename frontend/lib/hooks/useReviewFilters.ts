/**
 * useReviewFilters Hook - Manages review filter state and URL synchronization
 *
 * Responsibilities:
 * - Manage filter state (sentiment, rating, dateRange)
 * - Sync filters with URL search params for deep linking
 * - Provide actions to set/clear filters
 * - Calculate active filter count for UI badges
 *
 * Usage:
 * ```typescript
 * const {
 *   filters,
 *   setFilter,
 *   clearFilter,
 *   clearAllFilters,
 *   activeFilterCount,
 * } = useReviewFilters();
 * ```
 */

import { useState, useEffect, useCallback, useMemo } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import type { ReviewFilterState } from '@/lib/types/review';
import { Sentiment, Rating } from '@/lib/types/common';

// ========================================
// TYPES
// ========================================

interface UseReviewFiltersReturn {
  filters: ReviewFilterState;
  setFilter: (key: keyof ReviewFilterState, value: any) => void;
  clearFilter: (key: keyof ReviewFilterState, value?: any) => void;
  clearAllFilters: () => void;
  activeFilterCount: number;
}

// ========================================
// HELPER FUNCTIONS
// ========================================

/**
 * Parse sentiment from URL param string
 */
function parseSentimentParam(param: string | null): Set<Sentiment> {
  if (!param) return new Set();

  const values = param.split(',').filter(v =>
    ['POSITIVE', 'NEGATIVE', 'NEUTRAL'].includes(v)
  ) as Sentiment[];

  return new Set(values);
}

/**
 * Parse rating from URL param string
 */
function parseRatingParam(param: string | null): Set<Rating> {
  if (!param) return new Set();

  const values = param
    .split(',')
    .map(v => parseInt(v, 10))
    .filter(v => v >= 1 && v <= 5) as Rating[];

  return new Set(values);
}

/**
 * Parse date from URL param string
 */
function parseDateParam(param: string | null): Date | null {
  if (!param) return null;

  try {
    const date = new Date(param);
    return isNaN(date.getTime()) ? null : date;
  } catch {
    return null;
  }
}

/**
 * Serialize Set to comma-separated string
 */
function serializeSet<T>(set: Set<T>): string {
  return Array.from(set).join(',');
}

// ========================================
// HOOK IMPLEMENTATION
// ========================================

export function useReviewFilters(): UseReviewFiltersReturn {
  const searchParams = useSearchParams();
  const router = useRouter();

  // Initialize filters from URL on mount
  const initialFilters = useMemo((): ReviewFilterState => {
    return {
      sourceId: searchParams.get('sourceId')
        ? parseInt(searchParams.get('sourceId')!, 10)
        : null,
      sentiment: parseSentimentParam(searchParams.get('sentiment')),
      rating: parseRatingParam(searchParams.get('rating')),
      startDate: parseDateParam(searchParams.get('startDate')),
      endDate: parseDateParam(searchParams.get('endDate')),
    };
  }, []); // Only run on mount

  const [filters, setFilters] = useState<ReviewFilterState>(initialFilters);

  // ========================================
  // SYNC FILTERS TO URL
  // ========================================

  const updateURL = useCallback((newFilters: ReviewFilterState) => {
    const params = new URLSearchParams();

    // Add sourceId
    if (newFilters.sourceId !== null) {
      params.set('sourceId', String(newFilters.sourceId));
    }

    // Add sentiment
    if (newFilters.sentiment.size > 0) {
      params.set('sentiment', serializeSet(newFilters.sentiment));
    }

    // Add rating
    if (newFilters.rating.size > 0) {
      params.set('rating', serializeSet(newFilters.rating));
    }

    // Add startDate
    if (newFilters.startDate) {
      params.set('startDate', newFilters.startDate.toISOString());
    }

    // Add endDate
    if (newFilters.endDate) {
      params.set('endDate', newFilters.endDate.toISOString());
    }

    // Update URL without reload
    const newUrl = params.toString()
      ? `${window.location.pathname}?${params.toString()}`
      : window.location.pathname;

    router.replace(newUrl, { scroll: false });
  }, [router]);

  // ========================================
  // SET FILTER
  // ========================================

  const setFilter = useCallback((
    key: keyof ReviewFilterState,
    value: any
  ) => {
    setFilters(prev => {
      const newFilters = { ...prev };

      if (key === 'sentiment' || key === 'rating') {
        // Toggle value in Set
        const currentSet = new Set<any>(prev[key] as Iterable<any>);

        if (currentSet.has(value)) {
          currentSet.delete(value);
        } else {
          currentSet.add(value);
        }

        newFilters[key] = currentSet as any;
      } else {
        // Direct assignment for sourceId, startDate, endDate
        newFilters[key] = value;
      }

      updateURL(newFilters);
      return newFilters;
    });
  }, [updateURL]);

  // ========================================
  // CLEAR FILTER
  // ========================================

  const clearFilter = useCallback((
    key: keyof ReviewFilterState,
    value?: any
  ) => {
    setFilters(prev => {
      const newFilters = { ...prev };

      if (key === 'sentiment' || key === 'rating') {
        if (value !== undefined) {
          // Remove specific value from Set
          const currentSet = new Set<any>(prev[key] as Iterable<any>);
          currentSet.delete(value);
          newFilters[key] = currentSet as any;
        } else {
          // Clear entire Set
          newFilters[key] = new Set() as any;
        }
      } else {
        // Clear to null/default
        if (key === 'sourceId') {
          newFilters[key] = null;
        } else {
          newFilters[key] = null;
        }
      }

      updateURL(newFilters);
      return newFilters;
    });
  }, [updateURL]);

  // ========================================
  // CLEAR ALL FILTERS
  // ========================================

  const clearAllFilters = useCallback(() => {
    const clearedFilters: ReviewFilterState = {
      sourceId: null,
      sentiment: new Set(),
      rating: new Set(),
      startDate: null,
      endDate: null,
    };

    setFilters(clearedFilters);
    updateURL(clearedFilters);
  }, [updateURL]);

  // ========================================
  // ACTIVE FILTER COUNT
  // ========================================

  const activeFilterCount = useMemo(() => {
    let count = 0;

    if (filters.sentiment.size > 0) count++;
    if (filters.rating.size > 0) count++;
    if (filters.startDate !== null) count++;
    if (filters.endDate !== null) count++;
    // Note: sourceId is not counted as it's handled by LocationSelector

    return count;
  }, [filters]);

  // ========================================
  // SYNC URL TO FILTERS (on external navigation)
  // ========================================

  useEffect(() => {
    const currentSentiment = parseSentimentParam(searchParams.get('sentiment'));
    const currentRating = parseRatingParam(searchParams.get('rating'));
    const currentStartDate = parseDateParam(searchParams.get('startDate'));
    const currentEndDate = parseDateParam(searchParams.get('endDate'));
    const currentSourceId = searchParams.get('sourceId')
      ? parseInt(searchParams.get('sourceId')!, 10)
      : null;

    // Check if URL params differ from current state
    const urlDiffers =
      currentSourceId !== filters.sourceId ||
      !areSetsEqual(currentSentiment, filters.sentiment) ||
      !areSetsEqual(currentRating, filters.rating) ||
      currentStartDate?.getTime() !== filters.startDate?.getTime() ||
      currentEndDate?.getTime() !== filters.endDate?.getTime();

    if (urlDiffers) {
      setFilters({
        sourceId: currentSourceId,
        sentiment: currentSentiment,
        rating: currentRating,
        startDate: currentStartDate,
        endDate: currentEndDate,
      });
    }
  }, [searchParams]); // Only depend on searchParams, not filters to avoid loops

  // ========================================
  // RETURN
  // ========================================

  return {
    filters,
    setFilter,
    clearFilter,
    clearAllFilters,
    activeFilterCount,
  };
}

// ========================================
// UTILITY FUNCTIONS
// ========================================

/**
 * Check if two Sets are equal
 */
function areSetsEqual<T>(a: Set<T>, b: Set<T>): boolean {
  if (a.size !== b.size) return false;
  for (const item of a) {
    if (!b.has(item)) return false;
  }
  return true;
}
