/**
 * StepConfirmation component
 * Step 4: Review and confirm all entered data
 *
 * Features:
 * - Summary of brand name
 * - Summary of source type and URL
 * - Edit buttons for each section
 * - "Rozpocznij import" button
 * - Loading state during submission
 * - Error handling
 */

'use client';

import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Card } from '@/components/ui/card';
import {
  AlertCircle,
  ArrowLeft,
  Building2,
  CheckCircle2,
  Edit2,
  Link2,
  Loader2,
  Play,
  Stars,
} from 'lucide-react';
import { SourceType, OnboardingStep } from '@/lib/types/onboarding';
import { getSourceDisplayName } from '@/lib/utils/sourceValidation';

// ========================================
// TYPES
// ========================================

export interface StepConfirmationProps {
  /**
   * Brand name to confirm
   */
  brandName: string;

  /**
   * Source type to confirm
   */
  sourceType: SourceType;

  /**
   * Profile URL to confirm
   */
  profileUrl: string;

  /**
   * Callback to edit a specific step
   */
  onEdit: (step: OnboardingStep) => void;

  /**
   * Callback when user confirms and starts import
   */
  onConfirm: () => Promise<void>;

  /**
   * Callback when user clicks Back
   */
  onBack: () => void;

  /**
   * Loading state during submission
   */
  isSubmitting: boolean;

  /**
   * Error message from submission
   */
  error?: string;
}

// ========================================
// COMPONENT
// ========================================

/**
 * Step 4: Confirmation and review
 *
 * @example
 * ```tsx
 * <StepConfirmation
 *   brandName={brand.name}
 *   sourceType={reviewSource.sourceType}
 *   profileUrl={reviewSource.profileUrl}
 *   onEdit={goToStep}
 *   onConfirm={submitOnboarding}
 *   onBack={prevStep}
 *   isSubmitting={state.isSubmitting}
 *   error={state.errors.general}
 * />
 * ```
 */
export function StepConfirmation({
  brandName,
  sourceType,
  profileUrl,
  onEdit,
  onConfirm,
  onBack,
  isSubmitting,
  error,
}: StepConfirmationProps) {
  // ========================================
  // HANDLERS
  // ========================================

  /**
   * Handle confirm button click
   */
  const handleConfirm = async () => {
    try {
      await onConfirm();
    } catch (error) {
      // Error is handled by parent component
      console.error('Confirmation error:', error);
    }
  };

  // ========================================
  // COMPUTED VALUES
  // ========================================

  const platformName = getSourceDisplayName(sourceType);

  // ========================================
  // RENDER
  // ========================================

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="text-center space-y-2">
        <div className="flex justify-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-indigo-100">
            <CheckCircle2
              className="h-6 w-6 text-indigo-600"
              aria-hidden="true"
            />
          </div>
        </div>
        <h2 className="text-2xl font-bold tracking-tight text-gray-900">
          Sprawdź dane przed rozpoczęciem
        </h2>
        <p className="text-sm text-gray-600">
          Upewnij się, że wszystkie informacje są poprawne
        </p>
      </div>

      {/* Summary cards */}
      <div className="space-y-3">
        {/* Brand name card */}
        <Card className="p-4">
          <div className="flex items-start justify-between gap-4">
            <div className="flex items-start gap-3 flex-1">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-100 flex-shrink-0">
                <Building2 className="h-5 w-5 text-blue-600" aria-hidden="true" />
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="text-sm font-medium text-gray-700">
                  Nazwa marki
                </h3>
                <p className="mt-1 text-base font-semibold text-gray-900 break-words">
                  {brandName}
                </p>
              </div>
            </div>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => onEdit(OnboardingStep.BRAND_SETUP)}
              disabled={isSubmitting}
              aria-label="Edytuj nazwę marki"
            >
              <Edit2 className="h-4 w-4" />
            </Button>
          </div>
        </Card>

        {/* Source type card */}
        <Card className="p-4">
          <div className="flex items-start justify-between gap-4">
            <div className="flex items-start gap-3 flex-1">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-purple-100 flex-shrink-0">
                <Stars className="h-5 w-5 text-purple-600" aria-hidden="true" />
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="text-sm font-medium text-gray-700">Platforma</h3>
                <p className="mt-1 text-base font-semibold text-gray-900">
                  {platformName}
                </p>
              </div>
            </div>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => onEdit(OnboardingStep.SOURCE_TYPE)}
              disabled={isSubmitting}
              aria-label="Edytuj platformę"
            >
              <Edit2 className="h-4 w-4" />
            </Button>
          </div>
        </Card>

        {/* Profile URL card */}
        <Card className="p-4">
          <div className="flex items-start justify-between gap-4">
            <div className="flex items-start gap-3 flex-1 min-w-0">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-green-100 flex-shrink-0">
                <Link2 className="h-5 w-5 text-green-600" aria-hidden="true" />
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="text-sm font-medium text-gray-700">Adres URL</h3>
                <p className="mt-1 text-sm text-gray-900 break-all">
                  {profileUrl}
                </p>
              </div>
            </div>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => onEdit(OnboardingStep.SOURCE_URL)}
              disabled={isSubmitting}
              aria-label="Edytuj adres URL"
            >
              <Edit2 className="h-4 w-4" />
            </Button>
          </div>
        </Card>
      </div>

      {/* Error message */}
      {error && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {/* Info box */}
      <div className="rounded-lg bg-blue-50 p-4">
        <p className="text-sm text-blue-900">
          <strong>Co się stanie dalej?</strong>
        </p>
        <ul className="mt-2 text-sm text-blue-800 space-y-1 list-disc list-inside">
          <li>Utworzymy Twoją markę w systemie</li>
          <li>Połączymy się ze źródłem opinii {platformName}</li>
          <li>Zaimportujemy opinie z ostatnich 90 dni</li>
          <li>Przeprowadzimy analizę sentymentu AI</li>
        </ul>
        <p className="mt-2 text-xs text-blue-700">
          Import może potrwać kilka minut w zależności od liczby opinii.
        </p>
      </div>

      {/* Navigation buttons */}
      <div className="flex gap-3">
        {/* Back button */}
        <Button
          type="button"
          variant="outline"
          onClick={onBack}
          disabled={isSubmitting}
          className="flex-1"
        >
          <ArrowLeft className="mr-2 h-4 w-4" aria-hidden="true" />
          Wstecz
        </Button>

        {/* Confirm button */}
        <Button
          type="button"
          onClick={handleConfirm}
          disabled={isSubmitting}
          className="flex-1"
        >
          {isSubmitting ? (
            <>
              <Loader2
                className="mr-2 h-4 w-4 animate-spin"
                aria-hidden="true"
              />
              Tworzenie...
            </>
          ) : (
            <>
              <Play className="mr-2 h-4 w-4" aria-hidden="true" />
              Rozpocznij import
            </>
          )}
        </Button>
      </div>
    </div>
  );
}
