/**
 * useSentimentUpdate Hook - Manages optimistic sentiment updates
 *
 * Responsibilities:
 * - Perform optimistic UI update before API call
 * - Call API to update sentiment
 * - Rollback on error
 * - Notify parent component on success
 *
 * Usage:
 * ```typescript
 * const { updateSentiment, isUpdating, error } = useSentimentUpdate({
 *   brandId: 1,
 *   onSuccess: () => refetchSummary(),
 * });
 *
 * await updateSentiment(reviewId, 'POSITIVE');
 * ```
 */

import { useState, useCallback } from 'react';
import { updateReviewSentiment } from '@/lib/api/reviews';
import { ApiException } from '@/lib/api/client';
import type { Sentiment } from '@/lib/types/common';
import type { ErrorResponse } from '@/lib/types/common';

// ========================================
// TYPES
// ========================================

interface UseSentimentUpdateOptions {
  brandId: number;
  onSuccess?: (reviewId: number, newSentiment: Sentiment) => void;
  onError?: (error: string) => void;
}

interface UseSentimentUpdateReturn {
  updateSentiment: (reviewId: number, newSentiment: Sentiment) => Promise<boolean>;
  isUpdating: boolean;
  error: ErrorResponse | null;
}

// ========================================
// HOOK IMPLEMENTATION
// ========================================

export function useSentimentUpdate(
  options: UseSentimentUpdateOptions
): UseSentimentUpdateReturn {
  const { brandId, onSuccess, onError } = options;

  const [isUpdating, setIsUpdating] = useState(false);
  const [error, setError] = useState<ErrorResponse | null>(null);

  // ========================================
  // UPDATE SENTIMENT
  // ========================================

  const updateSentiment = useCallback(
    async (reviewId: number, newSentiment: Sentiment): Promise<boolean> => {
      // Guard: Already updating
      if (isUpdating) {
        return false;
      }

      setIsUpdating(true);
      setError(null);

      try {
        // Call API
        const response = await updateReviewSentiment(brandId, reviewId, newSentiment);

        // Success callback
        onSuccess?.(reviewId, response.sentiment);

        return true;
      } catch (err) {
        // Handle error
        if (err instanceof ApiException) {
          setError(err.error);
          onError?.(err.error.message);
        } else if (err instanceof Error) {
          const errorResponse: ErrorResponse = {
            code: 'UNKNOWN_ERROR',
            message: err.message,
          };
          setError(errorResponse);
          onError?.(err.message);
        } else {
          const errorResponse: ErrorResponse = {
            code: 'UNKNOWN_ERROR',
            message: 'Nie udało się zaktualizować sentymentu',
          };
          setError(errorResponse);
          onError?.('Nie udało się zaktualizować sentymentu');
        }

        return false;
      } finally {
        setIsUpdating(false);
      }
    },
    [brandId, isUpdating, onSuccess, onError]
  );

  // ========================================
  // RETURN
  // ========================================

  return {
    updateSentiment,
    isUpdating,
    error,
  };
}
