/**
 * useRegisterForm - Main registration form logic hook
 *
 * Manages:
 * - Form state (data, errors, submission status)
 * - Field validation (debounced for UX)
 * - Form submission with API integration
 * - Success/error handling
 * - Auto-login after registration
 *
 * Usage:
 * ```tsx
 * const {
 *   formData,
 *   errors,
 *   isSubmitting,
 *   isValid,
 *   apiError,
 *   showSuccessModal,
 *   handleFieldChange,
 *   handleBlur,
 *   handleSubmit,
 *   setShowSuccessModal
 * } = useRegisterForm();
 * ```
 */

'use client';

import { useState, useCallback, useMemo, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import { useDebouncedCallback } from 'use-debounce';
import {
  RegisterFormData,
  FormErrors,
  ApiError,
  initialFormData,
} from '@/lib/types/auth';
import { UseRegisterFormReturn } from '@/lib/types/api';
import { useAuth } from '@/lib/hooks/useAuth';
import { registerUser, ApiException } from '@/lib/api/auth';
import {
  validateEmail,
  validatePassword,
  validateConfirmPassword,
  validateConsents,
} from '../utils/validation';

// ========================================
// HOOK IMPLEMENTATION
// ========================================

export function useRegisterForm(): UseRegisterFormReturn {
  const router = useRouter();
  const { login } = useAuth();

  // ========================================
  // STATE
  // ========================================

  const [formData, setFormData] = useState<RegisterFormData>(initialFormData);
  const [errors, setErrors] = useState<FormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [touched, setTouched] = useState<Set<keyof RegisterFormData>>(new Set());
  const [apiError, setApiError] = useState<ApiError | null>(null);
  const [showSuccessModal, setShowSuccessModal] = useState(false);

  // ========================================
  // VALIDATION FUNCTIONS
  // ========================================

  /**
   * Validates a single form field
   *
   * @param field - Field name to validate
   * @param value - Field value
   * @returns Error message or undefined if valid
   */
  const validateField = useCallback(
    (field: keyof RegisterFormData, value: any): string | undefined => {
      switch (field) {
        case 'email':
          return validateEmail(value);

        case 'password':
          return validatePassword(value);

        case 'confirmPassword':
          return validateConfirmPassword(formData.password, value);

        case 'termsAccepted':
        case 'privacyAccepted':
          return validateConsents({
            termsAccepted:
              field === 'termsAccepted' ? value : formData.termsAccepted,
            privacyAccepted:
              field === 'privacyAccepted' ? value : formData.privacyAccepted,
          });

        default:
          return undefined;
      }
    },
    [formData]
  );

  /**
   * Validates entire form before submission
   *
   * @returns true if form is valid, false otherwise
   */
  const validateForm = useCallback((): boolean => {
    const newErrors: FormErrors = {};

    // Validate email
    const emailError = validateEmail(formData.email);
    if (emailError) newErrors.email = emailError;

    // Validate password
    const passwordError = validatePassword(formData.password);
    if (passwordError) newErrors.password = passwordError;

    // Validate confirm password
    const confirmPasswordError = validateConfirmPassword(
      formData.password,
      formData.confirmPassword
    );
    if (confirmPasswordError) newErrors.confirmPassword = confirmPasswordError;

    // Validate consents
    const consentsError = validateConsents({
      termsAccepted: formData.termsAccepted,
      privacyAccepted: formData.privacyAccepted,
    });
    if (consentsError) newErrors.consents = consentsError;

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [formData]);

  // ========================================
  // DEBOUNCED VALIDATION
  // ========================================

  /**
   * Debounced field validation (300ms delay)
   * Only runs if field was already touched by user
   */
  const debouncedValidation = useDebouncedCallback(
    (field: keyof RegisterFormData, value: any) => {
      const error = validateField(field, value);
      setErrors((prev) => {
        const newErrors = { ...prev };

        // Map form fields to error fields (consents are combined)
        const errorField = (field === 'termsAccepted' || field === 'privacyAccepted')
          ? 'consents'
          : field as keyof FormErrors;

        if (error) {
          newErrors[errorField] = error;
        } else {
          delete newErrors[errorField];
        }
        return newErrors;
      });
    },
    300
  );

  // ========================================
  // HANDLERS
  // ========================================

  /**
   * Handles field value change
   * Updates form data and triggers debounced validation if field was touched
   *
   * @param field - Field name
   * @param value - New field value
   */
  const handleFieldChange = useCallback(
    (field: keyof RegisterFormData, value: any) => {
      setFormData((prev) => ({ ...prev, [field]: value }));

      // Only validate if field has been touched (blur event occurred)
      if (touched.has(field)) {
        debouncedValidation(field, value);
      }
    },
    [touched, debouncedValidation]
  );

  /**
   * Handles field blur event
   * Marks field as touched and validates immediately
   *
   * @param field - Field name
   */
  const handleBlur = useCallback(
    (field: keyof RegisterFormData) => {
      setTouched((prev) => new Set(prev).add(field));
      const error = validateField(field, formData[field]);
      setErrors((prev) => {
        const newErrors = { ...prev };

        // Map form fields to error fields (consents are combined)
        const errorField = (field === 'termsAccepted' || field === 'privacyAccepted')
          ? 'consents'
          : field as keyof FormErrors;

        if (error) {
          newErrors[errorField] = error;
        } else {
          delete newErrors[errorField];
        }
        return newErrors;
      });
    },
    [formData, validateField]
  );

  /**
   * Handles form submission
   * Validates form, calls API, handles success/error
   *
   * @param e - Form submit event
   */
  const handleSubmit = useCallback(
    async (e: FormEvent) => {
      e.preventDefault();

      // Mark all fields as touched
      setTouched(
        new Set(Object.keys(formData) as (keyof RegisterFormData)[])
      );

      // Validate entire form
      if (!validateForm()) {
        // Focus first field with error for accessibility
        setTimeout(() => {
          const firstErrorField = document.querySelector('[aria-invalid="true"]') as HTMLElement;
          if (firstErrorField) {
            firstErrorField.focus();
          }
        }, 0);
        return;
      }

      setIsSubmitting(true);
      setApiError(null);

      try {
        // Call registration API
        const response = await registerUser({
          email: formData.email,
          password: formData.password,
          confirmPassword: formData.confirmPassword,
        });

        // Auto-login user with returned token
        login(response.token, response.expiresAt);

        // Show success modal
        setShowSuccessModal(true);

        // Auto-redirect to onboarding after 2 seconds
        setTimeout(() => {
          router.push('/onboarding');
        }, 2000);
      } catch (error) {
        handleApiError(error);
      } finally {
        setIsSubmitting(false);
      }
    },
    [formData, validateForm, login, router]
  );

  /**
   * Handles API errors
   * Maps backend errors to form field errors
   *
   * @param error - Error from API call
   */
  const handleApiError = useCallback((error: any) => {
    if (error instanceof ApiException) {
      const apiError = error.error;

      // Map field errors from backend to form errors
      if (apiError.errors && apiError.errors.length > 0) {
        const newErrors: FormErrors = {};
        apiError.errors.forEach((fieldError) => {
          const field = fieldError.field as keyof FormErrors;
          newErrors[field] = fieldError.message;
        });
        setErrors((prev) => ({ ...prev, ...newErrors }));
      }

      // Set general API error for display in ErrorAlert
      setApiError(apiError);
    } else {
      // Network or unknown error
      setApiError({
        code: 'UNKNOWN_ERROR',
        message: 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.',
      });
    }
  }, []);

  // ========================================
  // COMPUTED VALUES
  // ========================================

  /**
   * Check if form is valid for submission
   * All required fields filled + no validation errors
   */
  const isValid = useMemo(() => {
    // Check that all error values are undefined (not just empty object)
    const hasErrors = Object.values(errors).some(error => error !== undefined);

    return !!(
      formData.email &&
      formData.password &&
      formData.confirmPassword &&
      formData.termsAccepted &&
      formData.privacyAccepted &&
      !hasErrors
    );
  }, [formData, errors]);

  // ========================================
  // RETURN
  // ========================================

  return {
    formData,
    errors,
    isSubmitting,
    isValid,
    apiError,
    showSuccessModal,
    handleFieldChange,
    handleBlur,
    handleSubmit,
    setShowSuccessModal,
  };
}
