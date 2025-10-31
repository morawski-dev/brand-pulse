/**
 * General API and utility types
 */

import { FormEvent } from 'react';
import { ApiError, FormErrors, PasswordRequirement, PasswordStrengthResult, RegisterFormData } from './auth';

// ========================================
// API CLIENT TYPES
// ========================================

/**
 * HTTP methods supported by API client
 */
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

/**
 * Options for API requests
 */
export interface ApiRequestOptions {
  method: HttpMethod;
  headers?: Record<string, string>;
  body?: any;
  timeout?: number; // milliseconds, default 30000
}

/**
 * Generic API response wrapper
 * Discriminated union for type-safe error handling
 */
export type ApiResult<T> =
  | { success: true; data: T }
  | { success: false; error: ApiError };

// ========================================
// VALIDATION TYPES
// ========================================

/**
 * Result of field validation
 * undefined = no error, string = error message
 */
export type ValidationResult = string | undefined;

// ========================================
// CUSTOM HOOK RETURN TYPES
// ========================================

/**
 * Return type for useRegisterForm hook
 * Manages entire registration form state and logic
 */
export interface UseRegisterFormReturn {
  formData: RegisterFormData;
  errors: FormErrors;
  isSubmitting: boolean;
  isValid: boolean;
  apiError: ApiError | null;
  showSuccessModal: boolean;
  handleFieldChange: (field: keyof RegisterFormData, value: any) => void;
  handleBlur: (field: keyof RegisterFormData) => void;
  handleSubmit: (e: FormEvent) => Promise<void>;
  setShowSuccessModal: (show: boolean) => void;
}

/**
 * Return type for usePasswordStrength hook
 * Calculates password strength in real-time
 */
export interface UsePasswordStrengthReturn {
  strength: PasswordStrengthResult;
  requirements: PasswordRequirement[];
}

/**
 * Return type for useAuth hook
 * Global authentication state management
 */
export interface UseAuthReturn {
  login: (token: string, expiresAt: string) => void;
  logout: () => void;
  isAuthenticated: boolean;
}