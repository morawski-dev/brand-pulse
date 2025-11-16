/**
 * useOnboarding hook
 * Main hook for managing the 5-step onboarding wizard flow
 *
 * Responsibilities:
 * - Multi-step form state management
 * - Brand and review source creation
 * - Import progress polling
 * - Navigation between steps
 * - Error handling
 * - Auto-redirect after completion
 */

'use client';

import { useState, useCallback, useRef, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import {
  OnboardingState,
  OnboardingStep,
  BrandData,
  ReviewSourceData,
  INITIAL_ONBOARDING_STATE,
} from '@/lib/types/onboarding';
import { createBrand } from '@/lib/api/brand';
import { createReviewSource, getImportProgress } from '@/lib/api/reviewSource';
import { AuthMethod, BrandApiException } from '@/lib/types/brand';

// ========================================
// CONSTANTS
// ========================================

/**
 * Polling interval for import progress (2 seconds)
 */
const IMPORT_POLL_INTERVAL = 2000;

/**
 * Auto-redirect delay after completion (5 seconds)
 */
const REDIRECT_DELAY = 5000;

// ========================================
// HOOK RETURN TYPE
// ========================================

export interface UseOnboardingReturn {
  // State
  state: OnboardingState;

  // Navigation
  nextStep: () => void;
  prevStep: () => void;
  goToStep: (step: OnboardingStep) => void;

  // Data updates
  updateBrand: (data: Partial<BrandData>) => void;
  updateReviewSource: (data: Partial<ReviewSourceData>) => void;

  // Form submission
  submitOnboarding: () => Promise<void>;

  // Manual redirect
  goToDashboard: () => void;
}

// ========================================
// HOOK IMPLEMENTATION
// ========================================

/**
 * Main hook for managing onboarding flow
 *
 * Usage:
 * ```tsx
 * const {
 *   state,
 *   nextStep,
 *   prevStep,
 *   updateBrand,
 *   updateReviewSource,
 *   submitOnboarding
 * } = useOnboarding();
 * ```
 */
export function useOnboarding(): UseOnboardingReturn {
  const router = useRouter();
  const [state, setState] = useState<OnboardingState>(INITIAL_ONBOARDING_STATE);
  const pollIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const redirectTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  // ========================================
  // CLEANUP ON UNMOUNT
  // ========================================

  useEffect(() => {
    return () => {
      // Clear polling interval on unmount
      if (pollIntervalRef.current) {
        clearInterval(pollIntervalRef.current);
      }
      // Clear redirect timeout on unmount
      if (redirectTimeoutRef.current) {
        clearTimeout(redirectTimeoutRef.current);
      }
    };
  }, []);

  // ========================================
  // NAVIGATION FUNCTIONS
  // ========================================

  /**
   * Navigate to next step
   * Validates current step before advancing
   */
  const nextStep = useCallback(() => {
    setState((prev) => {
      const nextStepValue = Math.min(
        prev.currentStep + 1,
        OnboardingStep.IMPORT_PROGRESS
      );

      return {
        ...prev,
        currentStep: nextStepValue,
        errors: {}, // Clear errors on step change
      };
    });
  }, []);

  /**
   * Navigate to previous step
   * Cannot go back from import progress
   */
  const prevStep = useCallback(() => {
    setState((prev) => {
      // Cannot go back from import progress
      if (prev.currentStep === OnboardingStep.IMPORT_PROGRESS) {
        return prev;
      }

      const prevStepValue = Math.max(
        prev.currentStep - 1,
        OnboardingStep.BRAND_SETUP
      );

      return {
        ...prev,
        currentStep: prevStepValue,
        errors: {}, // Clear errors on step change
      };
    });
  }, []);

  /**
   * Jump to specific step
   * Used for editing from confirmation page
   *
   * @param step - Target step number
   */
  const goToStep = useCallback((step: OnboardingStep) => {
    setState((prev) => ({
      ...prev,
      currentStep: step,
      errors: {}, // Clear errors on step change
    }));
  }, []);

  // ========================================
  // DATA UPDATE FUNCTIONS
  // ========================================

  /**
   * Update brand data (step 1)
   *
   * @param data - Partial brand data to update
   */
  const updateBrand = useCallback((data: Partial<BrandData>) => {
    setState((prev) => ({
      ...prev,
      brand: { ...prev.brand, ...data },
    }));
  }, []);

  /**
   * Update review source data (steps 2-3)
   *
   * @param data - Partial review source data to update
   */
  const updateReviewSource = useCallback((data: Partial<ReviewSourceData>) => {
    setState((prev) => ({
      ...prev,
      reviewSource: { ...prev.reviewSource, ...data },
    }));
  }, []);

  // ========================================
  // IMPORT PROGRESS POLLING
  // ========================================

  /**
   * Start polling import progress status
   * Polls every 2 seconds until completion
   *
   * @param sourceId - Review source ID to track
   */
  const startImportPolling = useCallback(
    (sourceId: number) => {
      // Clear any existing polling
      if (pollIntervalRef.current) {
        clearInterval(pollIntervalRef.current);
      }

      // Start new polling
      pollIntervalRef.current = setInterval(async () => {
        try {
          const progress = await getImportProgress(sourceId);

          setState((prev) => ({
            ...prev,
            importProgress: {
              isImporting: progress.progress < 100,
              progress: progress.progress,
              status: progress.statusMessage,
              reviewsImported: progress.reviewsImported,
              totalReviews: progress.totalReviews,
            },
          }));

          // Stop polling when complete
          if (progress.progress >= 100) {
            if (pollIntervalRef.current) {
              clearInterval(pollIntervalRef.current);
              pollIntervalRef.current = null;
            }

            // Schedule auto-redirect after 5 seconds
            redirectTimeoutRef.current = setTimeout(() => {
              router.push('/dashboard');
            }, REDIRECT_DELAY);
          }
        } catch (error) {
          // Stop polling on error
          if (pollIntervalRef.current) {
            clearInterval(pollIntervalRef.current);
            pollIntervalRef.current = null;
          }

          setState((prev) => ({
            ...prev,
            errors: {
              general:
                error instanceof BrandApiException
                  ? error.message
                  : 'Nie udało się pobrać statusu importu.',
            },
            importProgress: {
              ...prev.importProgress,
              isImporting: false,
            },
          }));
        }
      }, IMPORT_POLL_INTERVAL);
    },
    [router]
  );

  // ========================================
  // FORM SUBMISSION
  // ========================================

  /**
   * Submit onboarding data
   * Creates brand, then review source, then starts import polling
   *
   * Flow:
   * 1. Validate data
   * 2. Create brand via POST /api/brands
   * 3. Create review source via POST /api/brands/{id}/sources
   * 4. Navigate to import progress step
   * 5. Start polling import status
   */
  const submitOnboarding = useCallback(async () => {
    try {
      setState((prev) => ({ ...prev, isSubmitting: true, errors: {} }));

      // Validate brand name
      if (!state.brand.name || state.brand.name.trim().length === 0) {
        throw new Error('Nazwa marki jest wymagana');
      }

      // Validate source type
      if (!state.reviewSource.sourceType) {
        throw new Error('Wybierz typ źródła opinii');
      }

      // Validate profile URL
      if (
        !state.reviewSource.profileUrl ||
        state.reviewSource.profileUrl.trim().length === 0
      ) {
        throw new Error('Podaj adres URL profilu');
      }

      // Validate external profile ID
      if (
        !state.reviewSource.externalProfileId ||
        state.reviewSource.externalProfileId.trim().length === 0
      ) {
        throw new Error('Nie udało się wyodrębnić ID profilu z URL');
      }

      // Step 1: Create brand
      const brandResponse = await createBrand({
        name: state.brand.name.trim(),
      });

      // Step 2: Create review source
      const sourceResponse = await createReviewSource(brandResponse.brandId, {
        sourceType: state.reviewSource.sourceType,
        profileUrl: state.reviewSource.profileUrl.trim(),
        externalProfileId: state.reviewSource.externalProfileId.trim(),
        authMethod: AuthMethod.SCRAPING, // MVP default
      });

      // Step 3: Move to import progress step
      setState((prev) => ({
        ...prev,
        isSubmitting: false,
        currentStep: OnboardingStep.IMPORT_PROGRESS,
        importProgress: {
          isImporting: true,
          progress: 0,
          status: 'Rozpoczynanie importu opinii...',
          reviewsImported: 0,
          totalReviews: 0,
        },
      }));

      // Step 4: Start polling import status
      startImportPolling(sourceResponse.sourceId);
    } catch (error) {
      let errorMessage = 'Wystąpił błąd podczas konfiguracji.';

      if (error instanceof BrandApiException) {
        errorMessage = error.message;
      } else if (error instanceof Error) {
        errorMessage = error.message;
      }

      setState((prev) => ({
        ...prev,
        isSubmitting: false,
        errors: {
          general: errorMessage,
        },
      }));

      throw error;
    }
  }, [state.brand, state.reviewSource, startImportPolling]);

  // ========================================
  // MANUAL REDIRECT
  // ========================================

  /**
   * Manual redirect to dashboard
   * Used when user clicks "Go to Dashboard" button
   */
  const goToDashboard = useCallback(() => {
    // Clear any pending redirect
    if (redirectTimeoutRef.current) {
      clearTimeout(redirectTimeoutRef.current);
    }

    // Clear polling
    if (pollIntervalRef.current) {
      clearInterval(pollIntervalRef.current);
    }

    router.push('/dashboard');
  }, [router]);

  // ========================================
  // RETURN
  // ========================================

  return {
    state,
    nextStep,
    prevStep,
    goToStep,
    updateBrand,
    updateReviewSource,
    submitOnboarding,
    goToDashboard,
  };
}
