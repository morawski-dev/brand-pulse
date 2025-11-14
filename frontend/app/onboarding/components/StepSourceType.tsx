/**
 * StepSourceType component
 * Step 2: Source type selection (Google/Facebook/Trustpilot)
 *
 * Features:
 * - 3 selectable cards for each platform
 * - Google is marked as recommended (MVP priority)
 * - Navigation buttons (Back, Next)
 * - Responsive grid layout
 */

'use client';

import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { AlertCircle, ArrowLeft, ArrowRight, Stars } from 'lucide-react';
import { SourceType, SOURCE_TYPE_METADATA } from '@/lib/types/onboarding';
import { SourceTypeCard } from './SourceTypeCard';
import { getAllSourceTypes } from '@/lib/utils/sourceValidation';

// ========================================
// TYPES
// ========================================

export interface StepSourceTypeProps {
  /**
   * Currently selected source type
   */
  selectedType: SourceType | null;

  /**
   * Callback when source type is selected
   */
  onSelectType: (type: SourceType) => void;

  /**
   * Callback when user clicks Next
   */
  onNext: () => void;

  /**
   * Callback when user clicks Back
   */
  onBack: () => void;

  /**
   * Validation error message
   */
  error?: string;
}

// ========================================
// COMPONENT
// ========================================

/**
 * Step 2: Source type selection
 *
 * @example
 * ```tsx
 * <StepSourceType
 *   selectedType={reviewSource.sourceType}
 *   onSelectType={(type) => updateReviewSource({ sourceType: type })}
 *   onNext={nextStep}
 *   onBack={prevStep}
 * />
 * ```
 */
export function StepSourceType({
  selectedType,
  onSelectType,
  onNext,
  onBack,
  error,
}: StepSourceTypeProps) {
  // ========================================
  // HANDLERS
  // ========================================

  /**
   * Handle Next button click
   */
  const handleNext = () => {
    if (!selectedType) {
      return;
    }
    onNext();
  };

  // ========================================
  // COMPUTED VALUES
  // ========================================

  const isValid = selectedType !== null;
  const sourceTypes = getAllSourceTypes();

  // ========================================
  // RENDER
  // ========================================

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="text-center space-y-2">
        <div className="flex justify-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-purple-100">
            <Stars className="h-6 w-6 text-purple-600" aria-hidden="true" />
          </div>
        </div>
        <h2 className="text-2xl font-bold tracking-tight text-gray-900">
          Skąd chcesz pobierać opinie?
        </h2>
        <p className="text-sm text-gray-600">
          Wybierz platformę, z której będziemy importować Twoje opinie
        </p>
      </div>

      {/* Source type cards grid */}
      <div
        role="radiogroup"
        aria-label="Wybór platformy z opiniami"
        className="grid grid-cols-1 gap-4 md:grid-cols-3"
      >
        {sourceTypes.map((type) => {
          const metadata = SOURCE_TYPE_METADATA[type];

          return (
            <SourceTypeCard
              key={type}
              sourceType={type}
              displayName={metadata.displayName}
              description={metadata.description}
              isRecommended={metadata.isRecommended}
              isSelected={selectedType === type}
              onSelect={() => onSelectType(type)}
            />
          );
        })}
      </div>

      {/* Error message */}
      {error && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {/* Info box for recommended platform */}
      {selectedType === SourceType.GOOGLE && (
        <div className="rounded-lg bg-green-50 p-4">
          <p className="text-sm text-green-900">
            <strong>Świetny wybór!</strong> Google jest naszą zalecaną platformą
            dzięki stabilnemu API i wysokiej jakości danych.
          </p>
        </div>
      )}

      {/* Info box for Facebook/Trustpilot */}
      {(selectedType === SourceType.FACEBOOK ||
        selectedType === SourceType.TRUSTPILOT) && (
        <div className="rounded-lg bg-blue-50 p-4">
          <p className="text-sm text-blue-900">
            <strong>Uwaga:</strong> Dla {SOURCE_TYPE_METADATA[selectedType].displayName} używamy
            web scrapingu. Import może być wolniejszy niż dla Google.
          </p>
        </div>
      )}

      {/* Navigation buttons */}
      <div className="flex gap-3">
        {/* Back button */}
        <Button
          type="button"
          variant="outline"
          onClick={onBack}
          className="flex-1"
        >
          <ArrowLeft className="mr-2 h-4 w-4" aria-hidden="true" />
          Wstecz
        </Button>

        {/* Next button */}
        <Button
          type="button"
          onClick={handleNext}
          disabled={!isValid}
          className="flex-1"
        >
          Dalej
          <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
        </Button>
      </div>

      {/* Additional help */}
      <div className="text-center">
        <p className="text-xs text-gray-500">
          W planie darmowym możesz monitorować opinie z jednej platformy
        </p>
      </div>
    </div>
  );
}
