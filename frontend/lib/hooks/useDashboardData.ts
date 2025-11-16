/**
 * useDashboardData Hook - Main hook for fetching all dashboard data
 *
 * Responsibilities:
 * - Fetch brand, sources, summary, and reviews
 * - Manage loading, error, success states
 * - Support pagination and infinite scroll
 * - Cache data and provide refetch mechanism
 * - Handle filter changes and location switching
 *
 * Usage:
 * ```typescript
 * const {
 *   brand,
 *   sources,
 *   summary,
 *   reviews,
 *   pagination,
 *   isLoading,
 *   error,
 *   refetch,
 *   loadMore,
 * } = useDashboardData({
 *   brandId: 1,
 *   sourceId: null, // "All locations"
 *   filters: { sentiment: new Set(['NEGATIVE']), ... },
 *   page: 0,
 * });
 * ```
 */

import { useState, useEffect, useCallback, useRef } from 'react';
import { getUserBrand } from '@/lib/api/brand';
import { fetchDashboardSummary, fetchReviews } from '@/lib/api/dashboard';
import { ApiException } from '@/lib/api/client';
import type { BrandResponse } from '@/lib/types/brand';
import type { ReviewSourceResponse } from '@/lib/types/brand';
import type {
  DashboardSummaryResponse,
  ReviewSourceSummary,
} from '@/lib/types/dashboard';
import type {
  ReviewViewModel,
  ReviewFilterState,
} from '@/lib/types/review';
import type { PaginationResponse, ErrorResponse } from '@/lib/types/common';
import { buildSentimentParam, buildRatingParam, formatISODate } from '@/lib/api/dashboard';

// ========================================
// TYPES
// ========================================

interface UseDashboardDataOptions {
  brandId?: number | null; // Auto-fetch from API if not provided
  sourceId?: number | null; // null = "All locations"
  filters?: ReviewFilterState;
  page?: number;
  pageSize?: number;
}

interface UseDashboardDataReturn {
  // Data
  brand: BrandResponse | null;
  sources: ReviewSourceSummary[];
  summary: DashboardSummaryResponse | null;
  reviews: ReviewViewModel[];
  pagination: PaginationResponse | null;

  // Loading states
  isLoading: boolean;
  isLoadingMore: boolean;
  isSummaryLoading: boolean;
  isReviewsLoading: boolean;

  // Error state
  error: ErrorResponse | null;

  // Actions
  refetch: () => Promise<void>;
  loadMore: () => Promise<void>;
  invalidate: () => void;
}

// ========================================
// HOOK IMPLEMENTATION
// ========================================

export function useDashboardData(
  options: UseDashboardDataOptions
): UseDashboardDataReturn {
  const {
    brandId: providedBrandId,
    sourceId = null,
    filters,
    page = 0,
    pageSize = 20,
  } = options;

  // State
  const [brand, setBrand] = useState<BrandResponse | null>(null);
  const [sources, setSources] = useState<ReviewSourceSummary[]>([]);
  const [summary, setSummary] = useState<DashboardSummaryResponse | null>(null);
  const [reviews, setReviews] = useState<ReviewViewModel[]>([]);
  const [pagination, setPagination] = useState<PaginationResponse | null>(null);

  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [isSummaryLoading, setIsSummaryLoading] = useState(false);
  const [isReviewsLoading, setIsReviewsLoading] = useState(false);
  const [error, setError] = useState<ErrorResponse | null>(null);

  // Current page tracking for infinite scroll
  const [currentPage, setCurrentPage] = useState(page);

  // Ref to track if initial load is done
  const isInitialLoadRef = useRef(true);

  // Ref to prevent duplicate requests
  const isLoadingRef = useRef(false);

  // Derived brandId (from prop or fetched brand)
  const brandId = providedBrandId ?? brand?.brandId ?? null;

  // ========================================
  // FETCH BRAND DATA
  // ========================================

  const fetchBrandData = useCallback(async () => {
    if (providedBrandId) {
      // Brand ID provided, skip fetching brand
      return providedBrandId;
    }

    try {
      const brandData = await getUserBrand();
      if (!brandData) {
        throw new Error('Nie znaleziono marki dla tego użytkownika');
      }
      setBrand(brandData);
      return brandData.brandId;
    } catch (err) {
      if (err instanceof ApiException) {
        setError(err.error);
      } else {
        setError({
          code: 'UNKNOWN_ERROR',
          message: 'Nie udało się pobrać danych marki',
        });
      }
      throw err;
    }
  }, [providedBrandId]);

  // ========================================
  // FETCH SOURCES
  // ========================================

  const fetchSources = useCallback(async (fetchedBrandId: number) => {
    try {
      // Note: We'll need to implement this endpoint in brand.ts
      // For now, using empty array
      // const sourcesData = await fetchReviewSources(fetchedBrandId);
      // setSources(sourcesData.sources);
      setSources([]);
    } catch (err) {
      // Don't fail the whole load if sources fail
      console.error('Failed to fetch sources:', err);
    }
  }, []);

  // ========================================
  // FETCH DASHBOARD SUMMARY
  // ========================================

  const fetchSummaryData = useCallback(async (fetchedBrandId: number) => {
    setIsSummaryLoading(true);
    try {
      const summaryData = await fetchDashboardSummary({
        brandId: fetchedBrandId,
        sourceId: sourceId ?? undefined,
        // startDate and endDate can be added from filters if needed
      });
      setSummary(summaryData);
    } catch (err) {
      if (err instanceof ApiException) {
        // Don't set global error, just log
        console.error('Failed to fetch summary:', err);
      }
    } finally {
      setIsSummaryLoading(false);
    }
  }, [sourceId]);

  // ========================================
  // FETCH REVIEWS
  // ========================================

  const fetchReviewsData = useCallback(
    async (fetchedBrandId: number, pageNum: number, append: boolean = false) => {
      // Prevent duplicate requests
      if (isLoadingRef.current) return;

      isLoadingRef.current = true;

      if (append) {
        setIsLoadingMore(true);
      } else {
        setIsReviewsLoading(true);
      }

      try {
        // Build query params
        const sentiment = filters?.sentiment
          ? buildSentimentParam(filters.sentiment)
          : undefined;
        const rating = filters?.rating
          ? buildRatingParam(filters.rating)
          : undefined;
        const startDate = filters?.startDate
          ? formatISODate(filters.startDate)
          : undefined;
        const endDate = filters?.endDate
          ? formatISODate(filters.endDate)
          : undefined;

        const reviewsData = await fetchReviews({
          brandId: fetchedBrandId,
          sourceId: sourceId ?? undefined,
          sentiment,
          rating,
          startDate,
          endDate,
          page: pageNum,
          size: pageSize,
          sort: 'publishedAt,desc',
        });

        // Update reviews
        if (append) {
          // Append for infinite scroll
          setReviews((prev) => [...prev, ...reviewsData.reviews]);
        } else {
          // Replace for initial load or filter change
          setReviews(reviewsData.reviews);
        }

        setPagination(reviewsData.pagination);
        setCurrentPage(pageNum);
      } catch (err) {
        if (err instanceof ApiException) {
          if (!append) {
            // Only set error for initial load, not for loadMore
            setError(err.error);
          }
        }
      } finally {
        setIsReviewsLoading(false);
        setIsLoadingMore(false);
        isLoadingRef.current = false;
      }
    },
    [sourceId, filters, pageSize]
  );

  // ========================================
  // INITIAL LOAD
  // ========================================

  useEffect(() => {
    let isMounted = true;

    const loadInitialData = async () => {
      setIsLoading(true);
      setError(null);

      try {
        // Step 1: Get brand ID
        const fetchedBrandId = await fetchBrandData();

        if (!isMounted) return;

        // Step 2: Fetch sources (parallel with summary and reviews)
        fetchSources(fetchedBrandId);

        // Step 3: Fetch summary and reviews in parallel
        await Promise.all([
          fetchSummaryData(fetchedBrandId),
          fetchReviewsData(fetchedBrandId, 0, false),
        ]);

        isInitialLoadRef.current = false;
      } catch (err) {
        // Error already set in fetchBrandData
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    if (isInitialLoadRef.current) {
      loadInitialData();
    }

    return () => {
      isMounted = false;
    };
  }, [fetchBrandData, fetchSources, fetchSummaryData, fetchReviewsData]);

  // ========================================
  // REFETCH ON FILTERS OR SOURCE CHANGE
  // ========================================

  useEffect(() => {
    // Skip initial load (handled above)
    if (isInitialLoadRef.current) return;
    if (!brandId) return;

    // Reset to page 0 and refetch
    setCurrentPage(0);
    fetchSummaryData(brandId);
    fetchReviewsData(brandId, 0, false);
  }, [sourceId, filters, brandId, fetchSummaryData, fetchReviewsData]);

  // ========================================
  // LOAD MORE (Infinite Scroll)
  // ========================================

  const loadMore = useCallback(async () => {
    if (!brandId) return;
    if (isLoadingMore || isLoadingRef.current) return;
    if (!pagination?.hasNext) return;

    const nextPage = currentPage + 1;
    await fetchReviewsData(brandId, nextPage, true);
  }, [brandId, currentPage, pagination, isLoadingMore, fetchReviewsData]);

  // ========================================
  // REFETCH
  // ========================================

  const refetch = useCallback(async () => {
    if (!brandId) return;

    setError(null);
    setCurrentPage(0);

    await Promise.all([
      fetchSummaryData(brandId),
      fetchReviewsData(brandId, 0, false),
    ]);
  }, [brandId, fetchSummaryData, fetchReviewsData]);

  // ========================================
  // INVALIDATE CACHE
  // ========================================

  const invalidate = useCallback(() => {
    // Clear all data and trigger refetch
    setSummary(null);
    setReviews([]);
    setPagination(null);
    setCurrentPage(0);

    if (brandId) {
      refetch();
    }
  }, [brandId, refetch]);

  // ========================================
  // RETURN
  // ========================================

  return {
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
  };
}
