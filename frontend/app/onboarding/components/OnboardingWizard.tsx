/**
 * OnboardingWizard component
 * Main orchestrator for the 5-step onboarding flow
 *
 * Features:
 * - Protected route (requires authentication)
 * - Multi-step wizard state management
 * - Progress indicator
 * - Conditional rendering of steps
 * - Integration with useOnboarding hook
 * - Error boundary handling
 * - Auto-redirect to login if not authenticated
 */

'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthContext } from '@/context/AuthContext';
import { useOnboarding } from '@/lib/hooks/useOnboarding';
import { OnboardingStep } from '@/lib/types/onboarding';
import { ProgressIndicator } from './ProgressIndicator';
import { StepBrandSetup } from './StepBrandSetup';
import { StepSourceType } from './StepSourceType';
import { StepSourceUrl } from './StepSourceUrl';
import { StepConfirmation } from './StepConfirmation';
import { StepImportProgress } from './StepImportProgress';

// ========================================
// COMPONENT
// ========================================

/**
 * Main onboarding wizard component
 *
 * Manages the entire 5-step onboarding flow:
 * 1. Brand setup (name)
 * 2. Source type selection (Google/Facebook/Trustpilot)
 * 3. Source URL input
 * 4. Confirmation
 * 5. Import progress
 *
 * @example
 * ```tsx
 * <OnboardingWizard />
 * ```
 */
export function OnboardingWizard() {
  // ========================================
  // HOOKS
  // ========================================

  const router = useRouter();
  const { isAuthenticated, isLoading: authLoading } = useAuthContext();
  const [isCheckingAuth, setIsCheckingAuth] = useState(true);

  const {
    state,
    nextStep,
    prevStep,
    goToStep,
    updateBrand,
    updateReviewSource,
    submitOnboarding,
    goToDashboard,
  } = useOnboarding();

  // ========================================
  // AUTH GUARD
  // ========================================

  /**
   * Protect route - redirect to login if not authenticated
   */
  useEffect(() => {
    if (!authLoading) {
      if (!isAuthenticated) {
        // User is not logged in, redirect to login
        router.push('/login?redirect=/onboarding');
      } else {
        // User is authenticated, allow access
        setIsCheckingAuth(false);
      }
    }
  }, [isAuthenticated, authLoading, router]);

  // ========================================
  // LOADING STATE
  // ========================================

  /**
   * Show loading spinner while checking authentication
   */
  if (authLoading || isCheckingAuth) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50">
        <div className="container mx-auto px-4 py-8">
          <div className="flex min-h-[60vh] items-center justify-center">
            <div className="text-center">
              <div className="mb-4 inline-block h-12 w-12 animate-spin rounded-full border-4 border-blue-600 border-t-transparent" />
              <p className="text-gray-600">Sprawdzanie uwierzytelnienia...</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // ========================================
  // RENDER CURRENT STEP
  // ========================================

  const renderStep = () => {
    switch (state.currentStep) {
      case OnboardingStep.BRAND_SETUP:
        return (
          <StepBrandSetup
            brandName={state.brand.name}
            onBrandNameChange={(name) => updateBrand({ name })}
            error={state.errors.brand}
            onNext={nextStep}
            isSubmitting={state.isSubmitting}
          />
        );

      case OnboardingStep.SOURCE_TYPE:
        return (
          <StepSourceType
            selectedType={state.reviewSource.sourceType}
            onSelectType={(type) => updateReviewSource({ sourceType: type })}
            onNext={nextStep}
            onBack={prevStep}
            error={state.errors.sourceType}
          />
        );

      case OnboardingStep.SOURCE_URL:
        return (
          <StepSourceUrl
            sourceType={state.reviewSource.sourceType!}
            profileUrl={state.reviewSource.profileUrl}
            onProfileUrlChange={(url) =>
              updateReviewSource({ profileUrl: url })
            }
            onExternalIdChange={(id) =>
              updateReviewSource({ externalProfileId: id })
            }
            error={state.errors.profileUrl}
            onNext={nextStep}
            onBack={prevStep}
          />
        );

      case OnboardingStep.CONFIRMATION:
        return (
          <StepConfirmation
            brandName={state.brand.name}
            sourceType={state.reviewSource.sourceType!}
            profileUrl={state.reviewSource.profileUrl}
            onEdit={goToStep}
            onConfirm={submitOnboarding}
            onBack={prevStep}
            isSubmitting={state.isSubmitting}
            error={state.errors.general}
          />
        );

      case OnboardingStep.IMPORT_PROGRESS:
        return (
          <StepImportProgress
            progress={state.importProgress.progress}
            status={state.importProgress.status}
            reviewsImported={state.importProgress.reviewsImported}
            totalReviews={state.importProgress.totalReviews}
            isComplete={state.importProgress.progress >= 100}
            isImporting={state.importProgress.isImporting}
            error={state.errors.general}
            onGoToDashboard={goToDashboard}
          />
        );

      default:
        return null;
    }
  };

  // ========================================
  // RENDER
  // ========================================

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50">
      {/* Skip to main content for accessibility */}
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-blue-600 focus:px-4 focus:py-2 focus:text-white focus:outline-none focus:ring-2 focus:ring-blue-600 focus:ring-offset-2"
      >
        Przejdź do treści głównej
      </a>

      <div className="container mx-auto px-4 py-8">
        {/* Header with logo */}
        <header className="mb-8">
          <div className="flex items-center justify-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-600">
              <span className="text-xl font-bold text-white">BP</span>
            </div>
            <span className="text-xl font-bold text-gray-900">BrandPulse</span>
          </div>
        </header>

        {/* Main content */}
        <main id="main-content" className="mx-auto max-w-3xl">
          {/* Progress indicator (hide on import step) */}
          {state.currentStep !== OnboardingStep.IMPORT_PROGRESS && (
            <div className="mb-8">
              <ProgressIndicator currentStep={state.currentStep} />
            </div>
          )}

          {/* Step content card */}
          <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-xl md:p-8">
            {renderStep()}
          </div>

          {/* Footer info */}
          {state.currentStep === OnboardingStep.BRAND_SETUP && (
            <p className="mt-6 text-center text-xs text-gray-500">
              Rejestracja zajmie około 2-3 minut
            </p>
          )}
        </main>
      </div>
    </div>
  );
}
