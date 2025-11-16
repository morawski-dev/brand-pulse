/**
 * useManualRefresh Hook - Manages manual data synchronization with 24h rate limit
 *
 * Responsibilities:
 * - Track last manual refresh timestamp
 * - Calculate time remaining until next refresh available
 * - Trigger manual sync with rate limit check
 * - Poll sync job status until completion
 * - Provide UI state for refresh button (disabled/enabled/loading)
 *
 * Usage:
 * ```typescript
 * const {
 *   triggerRefresh,
 *   isRefreshing,
 *   canRefresh,
 *   nextAvailableAt,
 *   timeRemaining,
 * } = useManualRefresh(brandId);
 * ```
 */

import { useState, useEffect, useCallback, useMemo } from 'react';
import { triggerManualSync, pollSyncJobStatus, calculateTimeRemaining } from '@/lib/api/sync';
import { ApiException } from '@/lib/api/client';
import type { ManualRefreshState } from '@/lib/types/sync';

// ========================================
// TYPES
// ========================================

interface UseManualRefreshOptions {
  brandId: number;
  lastManualRefreshAt?: string | null; // ISO timestamp from brand data
  onSuccess?: () => void; // Callback after successful refresh
  onError?: (error: string) => void; // Callback on error
}

interface UseManualRefreshReturn extends ManualRefreshState {
  triggerRefresh: () => Promise<void>;
}

// ========================================
// CONSTANTS
// ========================================

const REFRESH_COOLDOWN_MS = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

// ========================================
// HOOK IMPLEMENTATION
// ========================================

export function useManualRefresh(
  options: UseManualRefreshOptions
): UseManualRefreshReturn {
  const { brandId, lastManualRefreshAt, onSuccess, onError } = options;

  // State
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [lastRefreshAt, setLastRefreshAt] = useState<Date | null>(
    lastManualRefreshAt ? new Date(lastManualRefreshAt) : null
  );
  const [nextAvailableAt, setNextAvailableAt] = useState<Date | null>(null);

  // ========================================
  // CALCULATE NEXT AVAILABLE TIME
  // ========================================

  useEffect(() => {
    if (lastRefreshAt) {
      const nextTime = new Date(lastRefreshAt.getTime() + REFRESH_COOLDOWN_MS);
      setNextAvailableAt(nextTime);
    } else {
      setNextAvailableAt(null);
    }
  }, [lastRefreshAt]);

  // ========================================
  // CHECK IF REFRESH IS AVAILABLE
  // ========================================

  const canRefresh = useMemo(() => {
    if (!lastRefreshAt) return true;

    const now = new Date();
    const diff = now.getTime() - lastRefreshAt.getTime();

    return diff >= REFRESH_COOLDOWN_MS;
  }, [lastRefreshAt]);

  // ========================================
  // CALCULATE TIME REMAINING
  // ========================================

  const timeRemaining = useMemo(() => {
    if (canRefresh || !nextAvailableAt) return '';

    return calculateTimeRemaining(nextAvailableAt.toISOString());
  }, [canRefresh, nextAvailableAt]);

  // ========================================
  // UPDATE TIME REMAINING (every minute)
  // ========================================

  useEffect(() => {
    if (canRefresh) return;

    // Update time remaining every minute
    const interval = setInterval(() => {
      // Force re-render to update timeRemaining
      setLastRefreshAt(prev => prev); // Trigger useMemo recalculation
    }, 60000); // 60 seconds

    return () => clearInterval(interval);
  }, [canRefresh]);

  // ========================================
  // TRIGGER REFRESH
  // ========================================

  const triggerRefresh = useCallback(async () => {
    // Guard: Already refreshing
    if (isRefreshing) {
      return;
    }

    // Guard: Refresh not available yet
    if (!canRefresh) {
      const errorMsg = `Manualne odświeżanie będzie dostępne za ${timeRemaining}`;
      onError?.(errorMsg);
      return;
    }

    setIsRefreshing(true);

    try {
      // Step 1: Trigger sync
      const response = await triggerManualSync(brandId);

      // Step 2: Update timestamps
      const now = new Date();
      setLastRefreshAt(now);
      setNextAvailableAt(new Date(response.nextManualSyncAvailableAt));

      // Step 3: Poll all jobs until completion
      const jobPromises = response.jobs.map(job =>
        pollSyncJobStatus(job.jobId, {
          interval: 3000, // Poll every 3 seconds
          timeout: 180000, // 3 minute timeout
          onProgress: (status) => {
            // Optional: Update progress UI
            console.log(`Job ${job.jobId} status:`, status.status);
          },
        })
      );

      await Promise.all(jobPromises);

      // Step 4: Success callback
      onSuccess?.();
    } catch (error) {
      // Handle specific error types
      if (error instanceof ApiException) {
        // Rate limit error (429)
        if (error.status === 429) {
          const errorData = error.error as any;

          if (errorData.nextAvailableAt) {
            setNextAvailableAt(new Date(errorData.nextAvailableAt));
          }

          if (errorData.lastManualRefreshAt) {
            setLastRefreshAt(new Date(errorData.lastManualRefreshAt));
          }

          const message = errorData.hoursRemaining
            ? `Manualne odświeżanie będzie dostępne za ${errorData.hoursRemaining} godzin`
            : error.error.message;

          onError?.(message);
        } else {
          onError?.(error.error.message);
        }
      } else if (error instanceof Error) {
        onError?.(error.message);
      } else {
        onError?.('Nie udało się odświeżyć danych. Spróbuj ponownie.');
      }
    } finally {
      setIsRefreshing(false);
    }
  }, [
    brandId,
    isRefreshing,
    canRefresh,
    timeRemaining,
    onSuccess,
    onError,
  ]);

  // ========================================
  // RETURN
  // ========================================

  return {
    triggerRefresh,
    isRefreshing,
    canRefresh,
    nextAvailableAt,
    timeRemaining,
    lastRefreshAt,
  };
}
