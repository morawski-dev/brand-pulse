/**
 * StepBrandSetup component
 * Step 1: Brand name input form
 *
 * Features:
 * - Text input for brand name (1-255 characters)
 * - Real-time validation
 * - Error display
 * - Next button
 */

'use client';

import React, { useState, useCallback, useEffect } from 'react';
import { useDebounce } from 'use-debounce';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { AlertCircle, ArrowRight, Building2 } from 'lucide-react';
import { cn } from '@/lib/utils';

// ========================================
// TYPES
// ========================================

export interface StepBrandSetupProps {
  /**
   * Current brand name value
   */
  brandName: string;

  /**
   * Callback when brand name changes
   */
  onBrandNameChange: (name: string) => void;

  /**
   * Validation error message
   */
  error?: string;

  /**
   * Callback when user clicks Next
   */
  onNext: () => void;

  /**
   * Loading state during submission
   */
  isSubmitting?: boolean;
}

// ========================================
// VALIDATION
// ========================================

/**
 * Validates brand name
 * Returns error message or undefined if valid
 */
function validateBrandName(name: string): string | undefined {
  const trimmed = name.trim();

  if (trimmed.length === 0) {
    return 'Nazwa marki jest wymagana';
  }

  if (trimmed.length > 255) {
    return 'Nazwa marki może mieć maksymalnie 255 znaków';
  }

  return undefined;
}

// ========================================
// COMPONENT
// ========================================

/**
 * Step 1: Brand name setup
 *
 * @example
 * ```tsx
 * <StepBrandSetup
 *   brandName={brand.name}
 *   onBrandNameChange={(name) => updateBrand({ name })}
 *   onNext={nextStep}
 * />
 * ```
 */
export function StepBrandSetup({
  brandName,
  onBrandNameChange,
  error,
  onNext,
  isSubmitting = false,
}: StepBrandSetupProps) {
  const [touched, setTouched] = useState(false);
  const [localError, setLocalError] = useState<string | undefined>();

  // Debounce validation to avoid validating on every keystroke
  const [debouncedName] = useDebounce(brandName, 300);

  // ========================================
  // VALIDATION
  // ========================================

  /**
   * Validate on blur or debounced change
   */
  const validate = useCallback(() => {
    if (!touched) return;
    const validationError = validateBrandName(debouncedName);
    setLocalError(validationError);
  }, [debouncedName, touched]);

  // Validate when debounced name changes
  React.useEffect(() => {
    validate();
  }, [validate]);

  // ========================================
  // HANDLERS
  // ========================================

  /**
   * Handle input change
   */
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    onBrandNameChange(value);
  };

  /**
   * Handle input blur (mark as touched)
   */
  const handleBlur = () => {
    setTouched(true);
    validate();
  };

  /**
   * Handle form submit
   */
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // Mark as touched
    setTouched(true);

    // Validate before proceeding
    const validationError = validateBrandName(brandName);
    if (validationError) {
      setLocalError(validationError);
      return;
    }

    // Clear local error and proceed
    setLocalError(undefined);
    onNext();
  };

  // ========================================
  // COMPUTED VALUES
  // ========================================

  const displayError = error || localError;
  const isValid = !validateBrandName(brandName);
  const showError = touched && displayError;

  // ========================================
  // RENDER
  // ========================================

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="text-center space-y-2">
        <div className="flex justify-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-blue-100">
            <Building2 className="h-6 w-6 text-blue-600" aria-hidden="true" />
          </div>
        </div>
        <h2 className="text-2xl font-bold tracking-tight text-gray-900">
          Jak nazywa się Twoja marka?
        </h2>
        <p className="text-sm text-gray-600">
          Podaj nazwę swojej firmy lub marki, którą chcesz monitorować
        </p>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Brand name input */}
        <div className="space-y-2">
          <Label htmlFor="brand-name" className="text-sm font-medium">
            Nazwa marki
          </Label>
          <Input
            id="brand-name"
            type="text"
            placeholder="np. Moja Restauracja, Salon Piękności XYZ"
            value={brandName}
            onChange={handleChange}
            onBlur={handleBlur}
            disabled={isSubmitting}
            className={cn(
              'w-full',
              showError && 'border-red-500 focus-visible:ring-red-500'
            )}
            aria-invalid={showError ? 'true' : 'false'}
            aria-describedby={
              showError ? 'brand-name-error' : 'brand-name-help'
            }
            maxLength={255}
            autoFocus
          />

          {/* Help text */}
          {!showError && (
            <p id="brand-name-help" className="text-xs text-gray-500">
              {brandName.length}/255 znaków
            </p>
          )}

          {/* Error message */}
          {showError && (
            <p
              id="brand-name-error"
              className="text-xs text-red-600 flex items-center gap-1"
              role="alert"
            >
              <AlertCircle className="h-3 w-3" aria-hidden="true" />
              {displayError}
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

        {/* Next button */}
        <Button
          type="submit"
          className="w-full"
          disabled={!isValid || isSubmitting}
        >
          {isSubmitting ? (
            <>
              <span className="mr-2">Trwa przetwarzanie...</span>
              <div className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
            </>
          ) : (
            <>
              Dalej
              <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
            </>
          )}
        </Button>
      </form>

      {/* Additional info */}
      <div className="rounded-lg bg-blue-50 p-4">
        <p className="text-sm text-blue-900">
          <strong>Wskazówka:</strong> W planie darmowym możesz monitorować jedną
          markę. Nazwa marki będzie widoczna tylko dla Ciebie.
        </p>
      </div>
    </div>
  );
}
