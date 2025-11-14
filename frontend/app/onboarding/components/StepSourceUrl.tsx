/**
 * StepSourceUrl component
 * Step 3: Review source URL input and validation
 *
 * Features:
 * - URL input with real-time validation
 * - Auto-extraction of external profile ID
 * - Example URL display
 * - Help text for selected platform
 * - Loading state during validation
 * - Success/error indicators
 * - Navigation buttons
 */

'use client';

import { useEffect } from 'react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  Link2,
  Loader2,
  ExternalLink,
} from 'lucide-react';
import { SourceType } from '@/lib/types/onboarding';
import { useSourceValidation } from '@/lib/hooks/useSourceValidation';
import {
  getExampleUrl,
  getSourceDisplayName,
  getUrlHelpText,
} from '@/lib/utils/sourceValidation';
import { cn } from '@/lib/utils';

// ========================================
// TYPES
// ========================================

export interface StepSourceUrlProps {
  /**
   * Selected source type from previous step
   */
  sourceType: SourceType;

  /**
   * Current profile URL value
   */
  profileUrl: string;

  /**
   * Callback when profile URL changes
   */
  onProfileUrlChange: (url: string) => void;

  /**
   * Callback when external ID is extracted
   */
  onExternalIdChange: (id: string) => void;

  /**
   * Validation error from parent
   */
  error?: string;

  /**
   * Callback when user clicks Next
   */
  onNext: () => void;

  /**
   * Callback when user clicks Back
   */
  onBack: () => void;
}

// ========================================
// COMPONENT
// ========================================

/**
 * Step 3: Source URL input with validation
 *
 * @example
 * ```tsx
 * <StepSourceUrl
 *   sourceType={reviewSource.sourceType}
 *   profileUrl={reviewSource.profileUrl}
 *   onProfileUrlChange={(url) => updateReviewSource({ profileUrl: url })}
 *   onExternalIdChange={(id) => updateReviewSource({ externalProfileId: id })}
 *   onNext={nextStep}
 *   onBack={prevStep}
 * />
 * ```
 */
export function StepSourceUrl({
  sourceType,
  profileUrl,
  onProfileUrlChange,
  onExternalIdChange,
  error,
  onNext,
  onBack,
}: StepSourceUrlProps) {
  // ========================================
  // VALIDATION HOOK
  // ========================================

  const { isValidating, validationResult, validateUrl } =
    useSourceValidation(sourceType);

  // ========================================
  // EFFECTS
  // ========================================

  /**
   * Update external ID when validation succeeds
   */
  useEffect(() => {
    if (validationResult.isValid && validationResult.externalId) {
      onExternalIdChange(validationResult.externalId);
    }
  }, [validationResult, onExternalIdChange]);

  // ========================================
  // HANDLERS
  // ========================================

  /**
   * Handle URL input change
   */
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    onProfileUrlChange(value);
    validateUrl(value);
  };

  /**
   * Handle Next button click
   */
  const handleNext = () => {
    // Validate before proceeding
    const result = validateUrl(profileUrl);
    if (!result.isValid) {
      return;
    }
    onNext();
  };

  /**
   * Open example URL in new tab
   */
  const handleOpenExample = () => {
    const exampleUrl = getExampleUrl(sourceType);
    window.open(exampleUrl, '_blank', 'noopener,noreferrer');
  };

  // ========================================
  // COMPUTED VALUES
  // ========================================

  const platformName = getSourceDisplayName(sourceType);
  const exampleUrl = getExampleUrl(sourceType);
  const helpText = getUrlHelpText(sourceType);
  const isValid = validationResult.isValid;
  const hasError = !isValidating && profileUrl.length > 0 && !isValid;
  const showSuccess = !isValidating && isValid;

  // ========================================
  // RENDER
  // ========================================

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="text-center space-y-2">
        <div className="flex justify-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-green-100">
            <Link2 className="h-6 w-6 text-green-600" aria-hidden="true" />
          </div>
        </div>
        <h2 className="text-2xl font-bold tracking-tight text-gray-900">
          Podaj link do profilu {platformName}
        </h2>
        <p className="text-sm text-gray-600">{helpText}</p>
      </div>

      {/* Form */}
      <div className="space-y-4">
        {/* URL input */}
        <div className="space-y-2">
          <Label htmlFor="profile-url" className="text-sm font-medium">
            Adres URL profilu
          </Label>
          <div className="relative">
            <Input
              id="profile-url"
              type="url"
              placeholder={exampleUrl}
              value={profileUrl}
              onChange={handleChange}
              className={cn('w-full pr-10', {
                'border-red-500 focus-visible:ring-red-500': hasError,
                'border-green-500 focus-visible:ring-green-500': showSuccess,
              })}
              aria-invalid={hasError ? 'true' : 'false'}
              aria-describedby={
                hasError ? 'url-error' : showSuccess ? 'url-success' : 'url-help'
              }
              autoFocus
            />

            {/* Status icon */}
            <div className="absolute right-3 top-1/2 -translate-y-1/2">
              {isValidating && (
                <Loader2
                  className="h-5 w-5 animate-spin text-blue-600"
                  aria-hidden="true"
                />
              )}
              {showSuccess && (
                <CheckCircle2
                  className="h-5 w-5 text-green-600"
                  aria-hidden="true"
                />
              )}
              {hasError && (
                <AlertCircle
                  className="h-5 w-5 text-red-600"
                  aria-hidden="true"
                />
              )}
            </div>
          </div>

          {/* Help text */}
          {!hasError && !showSuccess && (
            <p id="url-help" className="text-xs text-gray-500">
              Wklej pełny adres URL z paska przeglądarki
            </p>
          )}

          {/* Success message */}
          {showSuccess && validationResult.externalId && (
            <p
              id="url-success"
              className="text-xs text-green-600 flex items-center gap-1"
            >
              <CheckCircle2 className="h-3 w-3" aria-hidden="true" />
              Poprawny URL. Znaleziono profil: {validationResult.externalId}
            </p>
          )}

          {/* Error message */}
          {hasError && (
            <p
              id="url-error"
              className="text-xs text-red-600 flex items-center gap-1"
              role="alert"
            >
              <AlertCircle className="h-3 w-3" aria-hidden="true" />
              {validationResult.error || 'Nieprawidłowy format URL'}
            </p>
          )}
        </div>

        {/* API Error Alert */}
        {error && (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        {/* Example URL box */}
        <div className="rounded-lg border border-gray-200 bg-gray-50 p-4 space-y-2">
          <p className="text-sm font-medium text-gray-700">Przykładowy URL:</p>
          <div className="flex items-start gap-2">
            <code className="flex-1 text-xs text-gray-600 break-all">
              {exampleUrl}
            </code>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={handleOpenExample}
              className="flex-shrink-0"
              aria-label={`Otwórz przykładowy profil ${platformName}`}
            >
              <ExternalLink className="h-4 w-4" />
            </Button>
          </div>
        </div>

        {/* Platform-specific instructions */}
        <div className="rounded-lg bg-blue-50 p-4">
          <p className="text-sm text-blue-900">
            <strong>Jak znaleźć URL:</strong>
          </p>
          <ul className="mt-2 text-sm text-blue-800 space-y-1 list-disc list-inside">
            {sourceType === SourceType.GOOGLE && (
              <>
                <li>Wyszukaj swoją firmę w Google Maps</li>
                <li>Skopiuj adres URL z paska przeglądarki</li>
                <li>URL powinien zawierać "/maps/place/"</li>
              </>
            )}
            {sourceType === SourceType.FACEBOOK && (
              <>
                <li>Otwórz stronę firmową na Facebooku</li>
                <li>Skopiuj adres URL z paska przeglądarki</li>
                <li>URL powinien zawierać "facebook.com/"</li>
              </>
            )}
            {sourceType === SourceType.TRUSTPILOT && (
              <>
                <li>Wyszukaj swoją firmę na Trustpilot</li>
                <li>Skopiuj adres URL z paska przeglądarki</li>
                <li>URL powinien zawierać "/review/"</li>
              </>
            )}
          </ul>
        </div>
      </div>

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
          disabled={!isValid || isValidating}
          className="flex-1"
        >
          {isValidating ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" aria-hidden="true" />
              Sprawdzanie...
            </>
          ) : (
            <>
              Dalej
              <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
            </>
          )}
        </Button>
      </div>
    </div>
  );
}
