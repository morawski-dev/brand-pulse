/**
 * useSourceValidation hook
 * Real-time validation for review source URLs with debouncing
 *
 * Features:
 * - Debounced URL validation (500ms delay)
 * - Automatic external ID extraction
 * - Loading state management
 * - Error handling
 */

'use client';

import { useState, useCallback, useEffect } from 'react';
import { useDebounce } from 'use-debounce';
import { SourceType, ValidationResult } from '@/lib/types/onboarding';
import { validateAndExtract } from '@/lib/utils/sourceValidation';

// ========================================
// CONSTANTS
// ========================================

/**
 * Debounce delay for URL validation (500ms)
 * Prevents excessive validation while user is typing
 */
const VALIDATION_DEBOUNCE_MS = 500;

// ========================================
// HOOK RETURN TYPE
// ========================================

export interface UseSourceValidationReturn {
  // Validation state
  isValidating: boolean;
  validationResult: ValidationResult;
  debouncedUrl: string;

  // Validation function
  validateUrl: (url: string) => ValidationResult;

  // Manual validation trigger
  revalidate: () => void;

  // Reset validation state
  reset: () => void;
}

// ========================================
// HOOK IMPLEMENTATION
// ========================================

/**
 * Hook for validating review source URLs with debouncing
 *
 * Usage:
 * ```tsx
 * const {
 *   isValidating,
 *   validationResult,
 *   validateUrl
 * } = useSourceValidation(SourceType.GOOGLE);
 *
 * // In input onChange:
 * onChange={(url) => {
 *   const result = validateUrl(url);
 *   if (result.isValid && result.externalId) {
 *     updateReviewSource({ profileUrl: url, externalId: result.externalId });
 *   }
 * }}
 * ```
 *
 * @param sourceType - Source platform type (Google/Facebook/Trustpilot)
 * @returns Validation state and functions
 */
export function useSourceValidation(
  sourceType: SourceType | null
): UseSourceValidationReturn {
  const [url, setUrl] = useState<string>('');
  const [isValidating, setIsValidating] = useState(false);
  const [validationResult, setValidationResult] = useState<ValidationResult>({
    isValid: false,
  });

  // Debounce URL to avoid validating on every keystroke
  const [debouncedUrl] = useDebounce(url, VALIDATION_DEBOUNCE_MS);

  // ========================================
  // VALIDATION FUNCTION
  // ========================================

  /**
   * Validates URL and extracts external ID
   * Called immediately (not debounced) for form submission
   *
   * @param urlToValidate - URL to validate
   * @returns Validation result
   */
  const validateUrl = useCallback(
    (urlToValidate: string): ValidationResult => {
      // Update internal URL state for debouncing
      setUrl(urlToValidate);

      // Source type must be selected
      if (!sourceType) {
        const result: ValidationResult = {
          isValid: false,
          error: 'Wybierz typ źródła',
        };
        setValidationResult(result);
        return result;
      }

      // Empty URL is not an error (user is still typing)
      if (!urlToValidate || urlToValidate.trim().length === 0) {
        const result: ValidationResult = {
          isValid: false,
        };
        setValidationResult(result);
        return result;
      }

      // Perform validation and extraction
      setIsValidating(true);

      try {
        const result = validateAndExtract(urlToValidate, sourceType);
        setValidationResult(result);
        setIsValidating(false);
        return result;
      } catch (error) {
        const errorResult: ValidationResult = {
          isValid: false,
          error: 'Wystąpił błąd podczas walidacji URL',
        };
        setValidationResult(errorResult);
        setIsValidating(false);
        return errorResult;
      }
    },
    [sourceType]
  );

  // ========================================
  // DEBOUNCED VALIDATION
  // ========================================

  /**
   * Auto-validate when debounced URL changes
   * This provides real-time feedback while user types
   */
  useEffect(() => {
    if (!debouncedUrl || debouncedUrl.trim().length === 0) {
      setIsValidating(false);
      setValidationResult({ isValid: false });
      return;
    }

    if (!sourceType) {
      setIsValidating(false);
      setValidationResult({
        isValid: false,
        error: 'Wybierz typ źródła',
      });
      return;
    }

    // Validate debounced URL
    setIsValidating(true);

    try {
      const result = validateAndExtract(debouncedUrl, sourceType);
      setValidationResult(result);
      setIsValidating(false);
    } catch (error) {
      setValidationResult({
        isValid: false,
        error: 'Wystąpił błąd podczas walidacji URL',
      });
      setIsValidating(false);
    }
  }, [debouncedUrl, sourceType]);

  // ========================================
  // MANUAL REVALIDATION
  // ========================================

  /**
   * Manually trigger revalidation
   * Useful when source type changes
   */
  const revalidate = useCallback(() => {
    if (url && sourceType) {
      validateUrl(url);
    }
  }, [url, sourceType, validateUrl]);

  // ========================================
  // RESET SOURCE TYPE CHANGE
  // ========================================

  /**
   * Reset validation when source type changes
   */
  useEffect(() => {
    if (url && sourceType) {
      // Revalidate with new source type
      revalidate();
    } else {
      // Clear validation if no source type
      setValidationResult({ isValid: false });
    }
  }, [sourceType]); // Intentionally not including url and revalidate to avoid loops

  // ========================================
  // RESET FUNCTION
  // ========================================

  /**
   * Reset validation state
   * Useful when navigating between steps
   */
  const reset = useCallback(() => {
    setUrl('');
    setIsValidating(false);
    setValidationResult({ isValid: false });
  }, []);

  // ========================================
  // RETURN
  // ========================================

  return {
    isValidating,
    validationResult,
    debouncedUrl,
    validateUrl,
    revalidate,
    reset,
  };
}
