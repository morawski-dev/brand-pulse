/**
 * StepImportProgress component
 * Step 5: Import progress tracking with auto-redirect
 *
 * Features:
 * - Animated progress bar (0-100%)
 * - Status messages from polling
 * - Reviews imported counter
 * - Success state with checkmark
 * - Countdown timer (5s) to auto-redirect
 * - Manual "Przejdź do Dashboard" button
 * - Error handling
 */

'use client';

import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  AlertCircle,
  CheckCircle2,
  Loader2,
  ArrowRight,
  Download,
} from 'lucide-react';
import { cn } from '@/lib/utils';

// ========================================
// TYPES
// ========================================

export interface StepImportProgressProps {
  /**
   * Import progress (0-100)
   */
  progress: number;

  /**
   * Status message from backend
   */
  status: string;

  /**
   * Number of reviews imported so far
   */
  reviewsImported: number;

  /**
   * Total number of reviews to import
   */
  totalReviews: number;

  /**
   * Whether import is complete
   */
  isComplete: boolean;

  /**
   * Whether import is in progress
   */
  isImporting: boolean;

  /**
   * Error message if import failed
   */
  error?: string;

  /**
   * Callback to manually navigate to dashboard
   */
  onGoToDashboard: () => void;
}

// ========================================
// CONSTANTS
// ========================================

/**
 * Countdown duration in seconds
 */
const COUNTDOWN_DURATION = 5;

// ========================================
// COMPONENT
// ========================================

/**
 * Step 5: Import progress with auto-redirect
 *
 * @example
 * ```tsx
 * <StepImportProgress
 *   progress={state.importProgress.progress}
 *   status={state.importProgress.status}
 *   reviewsImported={state.importProgress.reviewsImported}
 *   totalReviews={state.importProgress.totalReviews}
 *   isComplete={state.importProgress.progress >= 100}
 *   isImporting={state.importProgress.isImporting}
 *   error={state.errors.general}
 *   onGoToDashboard={goToDashboard}
 * />
 * ```
 */
export function StepImportProgress({
  progress,
  status,
  reviewsImported,
  totalReviews,
  isComplete,
  isImporting,
  error,
  onGoToDashboard,
}: StepImportProgressProps) {
  const [countdown, setCountdown] = useState(COUNTDOWN_DURATION);

  // ========================================
  // COUNTDOWN EFFECT
  // ========================================

  /**
   * Start countdown when import is complete
   * Auto-redirect happens in useOnboarding hook
   */
  useEffect(() => {
    if (!isComplete) {
      setCountdown(COUNTDOWN_DURATION);
      return;
    }

    const interval = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [isComplete]);

  // ========================================
  // RENDER
  // ========================================

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="text-center space-y-2">
        <div className="flex justify-center">
          <div
            className={cn(
              'flex h-16 w-16 items-center justify-center rounded-full transition-colors',
              {
                'bg-blue-100': isImporting,
                'bg-green-100': isComplete,
                'bg-red-100': error,
              }
            )}
          >
            {isImporting && (
              <Loader2
                className="h-8 w-8 animate-spin text-blue-600"
                aria-hidden="true"
              />
            )}
            {isComplete && (
              <CheckCircle2
                className="h-8 w-8 text-green-600"
                aria-hidden="true"
              />
            )}
            {error && (
              <AlertCircle
                className="h-8 w-8 text-red-600"
                aria-hidden="true"
              />
            )}
          </div>
        </div>
        <h2 className="text-2xl font-bold tracking-tight text-gray-900">
          {isComplete
            ? 'Import zakończony!'
            : error
            ? 'Wystąpił błąd'
            : 'Importowanie opinii...'}
        </h2>
        <p className="text-sm text-gray-600">
          {isComplete
            ? 'Twoje opinie zostały zaimportowane i przeanalizowane'
            : error
            ? 'Nie udało się zaimportować opinii'
            : 'Proszę czekać, trwa pobieranie i analiza opinii'}
        </p>
      </div>

      {/* Progress section */}
      {!error && (
        <div className="space-y-4">
          {/* Progress bar */}
          <div className="space-y-2">
            <div className="flex items-center justify-between text-sm">
              <span className="font-medium text-gray-700">Postęp importu</span>
              <span className="font-semibold text-gray-900">
                {Math.round(progress)}%
              </span>
            </div>
            <Progress value={progress} className="h-3" />
          </div>

          {/* Status message */}
          <div
            className="rounded-lg bg-gray-50 p-4"
            role="status"
            aria-live="polite"
          >
            <div className="flex items-start gap-3">
              {isImporting && (
                <Download
                  className="h-5 w-5 text-blue-600 animate-bounce"
                  aria-hidden="true"
                />
              )}
              <div className="flex-1">
                <p className="text-sm font-medium text-gray-900">{status}</p>
                {totalReviews > 0 && (
                  <p className="mt-1 text-xs text-gray-600">
                    Zaimportowano {reviewsImported} z {totalReviews} opinii
                  </p>
                )}
              </div>
            </div>
          </div>

          {/* Completion info */}
          {isComplete && (
            <div className="rounded-lg bg-green-50 p-4">
              <div className="flex items-start gap-3">
                <CheckCircle2
                  className="h-5 w-5 text-green-600 flex-shrink-0"
                  aria-hidden="true"
                />
                <div className="flex-1">
                  <p className="text-sm font-medium text-green-900">
                    Wszystko gotowe!
                  </p>
                  <p className="mt-1 text-xs text-green-700">
                    {reviewsImported > 0
                      ? `Zaimportowaliśmy ${reviewsImported} opinii i przeanalizowaliśmy ich sentyment.`
                      : 'System jest gotowy do użycia.'}
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Error message */}
      {error && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {/* Action button */}
      <div className="space-y-3">
        {isComplete && (
          <>
            {/* Countdown info */}
            {countdown > 0 && (
              <p className="text-center text-sm text-gray-600">
                Przekierowanie za {countdown} sekund...
              </p>
            )}

            {/* Manual redirect button */}
            <Button
              type="button"
              onClick={onGoToDashboard}
              className="w-full"
              size="lg"
            >
              Przejdź do Dashboard
              <ArrowRight className="ml-2 h-5 w-5" aria-hidden="true" />
            </Button>
          </>
        )}

        {/* Error retry button */}
        {error && (
          <Button
            type="button"
            onClick={onGoToDashboard}
            variant="outline"
            className="w-full"
          >
            Przejdź do Dashboard
            <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
          </Button>
        )}
      </div>

      {/* Loading state message */}
      {isImporting && !error && (
        <div className="text-center">
          <p className="text-xs text-gray-500">
            Import może potrwać kilka minut. Nie zamykaj tej strony.
          </p>
        </div>
      )}
    </div>
  );
}
