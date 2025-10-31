/**
 * useLoginForm - Custom hook for login form state management
 *
 * Manages:
 * - Form data (email, password, rememberMe)
 * - Validation (real-time and on submit)
 * - Submit logic with API integration
 * - Password visibility toggle
 * - Error handling
 *
 * Usage:
 * ```tsx
 * const {
 *   formData,
 *   errors,
 *   isLoading,
 *   showPassword,
 *   handleEmailChange,
 *   handlePasswordChange,
 *   handleRememberMeChange,
 *   togglePasswordVisibility,
 *   handleSubmit,
 *   isFormValid
 * } = useLoginForm();
 * ```
 */

'use client';

import { useState, useCallback, FormEvent } from 'react';
import {
  LoginFormData,
  LoginFormErrors,
  initialLoginFormData,
} from '@/lib/types/auth';
import { useAuthContext } from '@/context/AuthContext';
import { ApiException } from '@/lib/api/auth';

// ========================================
// HOOK IMPLEMENTATION
// ========================================

export interface UseLoginFormReturn {
  // State
  formData: LoginFormData;
  errors: LoginFormErrors;
  isLoading: boolean;
  showPassword: boolean;

  // Change handlers
  handleEmailChange: (email: string) => void;
  handlePasswordChange: (password: string) => void;
  handleRememberMeChange: (checked: boolean) => void;
  togglePasswordVisibility: () => void;

  // Submit and validation
  handleSubmit: (e: FormEvent<HTMLFormElement>) => Promise<void>;
  validateEmail: (email: string) => string | undefined;
  validatePassword: (password: string) => string | undefined;
  isFormValid: boolean;
  clearError: (field?: keyof LoginFormErrors) => void;
}

export function useLoginForm(): UseLoginFormReturn {
  const { login } = useAuthContext();

  // Form data state
  const [formData, setFormData] = useState<LoginFormData>(initialLoginFormData);

  // Error state
  const [errors, setErrors] = useState<LoginFormErrors>({});

  // UI state
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  // ========================================
  // VALIDATION FUNCTIONS
  // ========================================

  /**
   * Validate email format
   * Returns error message or undefined if valid
   */
  const validateEmail = useCallback((email: string): string | undefined => {
    if (!email.trim()) {
      return 'Email jest wymagany';
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return 'Nieprawidłowy format email';
    }

    return undefined;
  }, []);

  /**
   * Validate password
   * Returns error message or undefined if valid
   */
  const validatePassword = useCallback((password: string): string | undefined => {
    if (!password.trim()) {
      return 'Hasło jest wymagane';
    }

    return undefined;
  }, []);

  /**
   * Check if entire form is valid
   */
  const isFormValid =
    formData.email.trim() !== '' &&
    formData.password.trim() !== '' &&
    !errors.email &&
    !errors.password;

  // ========================================
  // CHANGE HANDLERS
  // ========================================

  /**
   * Handle email change with real-time validation
   */
  const handleEmailChange = useCallback((email: string) => {
    setFormData(prev => ({ ...prev, email }));

    // Clear email error if user is typing
    setErrors(prev => ({ ...prev, email: undefined }));
  }, []);

  /**
   * Handle password change
   */
  const handlePasswordChange = useCallback((password: string) => {
    setFormData(prev => ({ ...prev, password }));

    // Clear password error if user is typing
    setErrors(prev => ({ ...prev, password: undefined }));
  }, []);

  /**
   * Handle remember me checkbox change
   */
  const handleRememberMeChange = useCallback((checked: boolean) => {
    setFormData(prev => ({ ...prev, rememberMe: checked }));
  }, []);

  /**
   * Toggle password visibility
   */
  const togglePasswordVisibility = useCallback(() => {
    setShowPassword(prev => !prev);
  }, []);

  /**
   * Clear specific error or all errors
   */
  const clearError = useCallback((field?: keyof LoginFormErrors) => {
    if (field) {
      setErrors(prev => ({ ...prev, [field]: undefined }));
    } else {
      setErrors({});
    }
  }, []);

  // ========================================
  // SUBMIT HANDLER
  // ========================================

  /**
   * Handle form submission
   * Validates form, calls login API, handles errors
   */
  const handleSubmit = useCallback(
    async (e: FormEvent<HTMLFormElement>) => {
      e.preventDefault();

      // Clear previous errors
      setErrors({});

      // Validate email
      const emailError = validateEmail(formData.email);
      if (emailError) {
        setErrors(prev => ({ ...prev, email: emailError }));
        return;
      }

      // Validate password
      const passwordError = validatePassword(formData.password);
      if (passwordError) {
        setErrors(prev => ({ ...prev, password: passwordError }));
        return;
      }

      // Submit if valid
      try {
        setIsLoading(true);

        await login(
          {
            email: formData.email,
            password: formData.password,
          },
          formData.rememberMe
        );

        // Success - login function in AuthContext handles redirect
      } catch (error) {
        // Handle API errors
        if (error instanceof ApiException) {
          const apiError = error.error;

          // Map API error codes to field-specific or general errors
          if (apiError.code === 'INVALID_CREDENTIALS') {
            setErrors({
              general: 'Nieprawidłowy email lub hasło. Sprawdź dane i spróbuj ponownie.',
            });

            // Clear password field for security
            setFormData(prev => ({ ...prev, password: '' }));
          } else if (apiError.code === 'EMAIL_NOT_VERIFIED') {
            setErrors({
              general:
                'Proszę zweryfikować email przed zalogowaniem. Sprawdź swoją skrzynkę pocztową.',
            });
          } else if (apiError.code === 'NETWORK_ERROR') {
            setErrors({
              general: 'Błąd połączenia. Sprawdź połączenie internetowe i spróbuj ponownie.',
            });
          } else if (apiError.code === 'TIMEOUT_ERROR') {
            setErrors({
              general: 'Żądanie przekroczyło limit czasu. Spróbuj ponownie.',
            });
          } else {
            setErrors({
              general: apiError.message || 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.',
            });
          }
        } else {
          // Unknown error
          setErrors({
            general: 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.',
          });
        }
      } finally {
        setIsLoading(false);
      }
    },
    [formData, login, validateEmail, validatePassword]
  );

  // ========================================
  // RETURN VALUE
  // ========================================

  return {
    // State
    formData,
    errors,
    isLoading,
    showPassword,

    // Change handlers
    handleEmailChange,
    handlePasswordChange,
    handleRememberMeChange,
    togglePasswordVisibility,

    // Submit and validation
    handleSubmit,
    validateEmail,
    validatePassword,
    isFormValid,
    clearError,
  };
}
