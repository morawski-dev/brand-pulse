/**
 * Authentication and Registration related types
 * Based on backend DTOs from com.morawski.dev.backend.dto.auth
 */

// ========================================
// ENUMS
// ========================================

/**
 * User subscription plan types
 * Maps to backend PlanType enum
 */
export enum PlanType {
  FREE = 'FREE',
  PREMIUM = 'PREMIUM'
}

/**
 * Password strength levels
 */
export type PasswordStrength = 'weak' | 'fair' | 'good' | 'strong';

// ========================================
// REQUEST/RESPONSE DTOs (Backend Contract)
// ========================================

/**
 * Registration request DTO
 * Maps to backend RegisterRequest.java
 */
export interface RegisterRequest {
  email: string;
  password: string;
  confirmPassword: string;
}

/**
 * Login request DTO
 * Maps to backend LoginRequest.java
 */
export interface LoginRequest {
  email: string;
  password: string;
}

/**
 * Registration/Authentication response DTO
 * Maps to backend AuthResponse.java
 */
export interface RegisterResponse {
  userId: number;
  email: string;
  planType: PlanType;
  maxSourcesAllowed: number;
  emailVerified: boolean;
  token: string;
  expiresAt: string; // ISO 8601 date string
  createdAt: string; // ISO 8601 date string
}

/**
 * Authentication response DTO (same as RegisterResponse)
 * Used for login and registration responses
 * Maps to backend AuthResponse.java
 */
export type AuthResponse = RegisterResponse;

/**
 * API error response
 * Returned by backend on validation or business logic errors
 */
export interface ApiError {
  code: string;
  message: string;
  errors?: FieldError[];
}

/**
 * Field-level validation error
 * Part of ApiError.errors array
 */
export interface FieldError {
  field: string;
  message: string;
}

// ========================================
// FORM DATA (Frontend Internal)
// ========================================

/**
 * Internal form data model
 * Includes UI-only fields (consents) not sent to backend
 */
export interface RegisterFormData {
  email: string;
  password: string;
  confirmPassword: string;
  termsAccepted: boolean;
  privacyAccepted: boolean;
}

/**
 * Initial empty form state
 */
export const initialFormData: RegisterFormData = {
  email: '',
  password: '',
  confirmPassword: '',
  termsAccepted: false,
  privacyAccepted: false
};

/**
 * Form validation errors per field
 */
export interface FormErrors {
  email?: string;
  password?: string;
  confirmPassword?: string;
  consents?: string;
  general?: string; // For API errors that don't map to specific fields
}

/**
 * Complete form state including data, errors, and UI flags
 */
export interface FormState {
  data: RegisterFormData;
  errors: FormErrors;
  isSubmitting: boolean;
  isValid: boolean;
  touched: Set<keyof RegisterFormData>;
}

// ========================================
// PASSWORD VALIDATION
// ========================================

/**
 * Password strength calculation result
 */
export interface PasswordStrengthResult {
  strength: PasswordStrength;
  score: number; // 0-5
  feedback: string; // Display label: "Słabe", "Średnie", "Dobre", "Silne"
}

/**
 * Single password requirement with validation state
 */
export interface PasswordRequirement {
  id: string; // Unique ID (e.g., 'length', 'uppercase')
  label: string; // Display text (e.g., 'Minimum 8 znaków')
  isMet: boolean; // Whether requirement is satisfied
}

// ========================================
// CONSENT STATE
// ========================================

/**
 * GDPR consent checkbox state
 */
export interface ConsentState {
  termsAccepted: boolean;
  privacyAccepted: boolean;
}

// ========================================
// ERROR MESSAGES MAPPING
// ========================================

/**
 * Map API error codes to Polish user-friendly messages
 */
export const API_ERROR_MESSAGES: Record<string, string> = {
  // Authentication errors
  EMAIL_ALREADY_EXISTS: 'Konto z tym adresem email już istnieje. Może chcesz się zalogować?',
  VALIDATION_ERROR: 'Dane formularza są nieprawidłowe. Sprawdź wszystkie pola.',

  // Network errors
  NETWORK_ERROR: 'Nie udało się połączyć z serwerem. Sprawdź połączenie internetowe.',
  TIMEOUT_ERROR: 'Żądanie przekroczyło limit czasu. Spróbuj ponownie.',

  // Server errors
  INTERNAL_SERVER_ERROR: 'Wystąpił błąd serwera. Spróbuj ponownie później.',
  SERVICE_UNAVAILABLE: 'Serwis jest chwilowo niedostępny. Spróbuj ponownie za chwilę.',

  // Default
  UNKNOWN_ERROR: 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.'
};

/**
 * Map backend field validation error codes to Polish messages
 */
export const FIELD_ERROR_MESSAGES: Record<string, string> = {
  'email.invalid': 'Nieprawidłowy format adresu email',
  'email.required': 'Email jest wymagany',
  'password.too_short': 'Hasło musi mieć minimum 8 znaków',
  'password.missing_uppercase': 'Hasło musi zawierać wielką literę',
  'password.missing_lowercase': 'Hasło musi zawierać małą literę',
  'password.missing_number': 'Hasło musi zawierać cyfrę',
  'password.missing_special': 'Hasło musi zawierać znak specjalny',
  'confirmPassword.mismatch': 'Hasła nie są zgodne'
};

// ========================================
// LOGIN FORM TYPES
// ========================================

/**
 * Login API error response
 * Extends base ApiError with login-specific codes
 */
export interface ApiErrorResponse extends ApiError {
  code: 'INVALID_CREDENTIALS' | 'EMAIL_NOT_VERIFIED' | string;
  verificationRequired?: boolean;
}

/**
 * Login form data model
 * Internal state for login form
 */
export interface LoginFormData {
  email: string;
  password: string;
  rememberMe: boolean;
}

/**
 * Login form validation errors
 */
export interface LoginFormErrors {
  email?: string;
  password?: string;
  general?: string; // For API errors that don't map to specific fields
}

/**
 * Login UI state model
 */
export interface LoginUIState {
  isLoading: boolean;
  showPassword: boolean;
  isDemoMode: boolean;
}

/**
 * Combined login form state for custom hook
 */
export interface LoginFormState {
  formData: LoginFormData;
  errors: LoginFormErrors;
  uiState: LoginUIState;
}

// ========================================
// AUTH CONTEXT TYPES
// ========================================

/**
 * Global authentication state
 */
export interface AuthState {
  user: AuthResponse | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

/**
 * Auth context value with state and actions
 */
export interface AuthContextValue extends AuthState {
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => void;
  checkAuth: () => boolean;
}

/**
 * Initial empty login form state
 */
export const initialLoginFormData: LoginFormData = {
  email: '',
  password: '',
  rememberMe: false
};