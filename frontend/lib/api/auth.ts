/**
 * Authentication API client
 * Handles registration, login, and password recovery endpoints
 */

import {
  RegisterRequest,
  RegisterResponse,
  LoginRequest,
  AuthResponse,
  ApiError,
  API_ERROR_MESSAGES,
} from '@/lib/types/auth';

// ========================================
// CONFIGURATION
// ========================================

/**
 * API base URL from environment variable
 * Falls back to localhost for development
 */
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

/**
 * Default timeout for API requests (30 seconds)
 */
const API_TIMEOUT = 30000;

// ========================================
// EXCEPTION CLASS
// ========================================

/**
 * Custom exception for API errors
 * Wraps ApiError from backend response
 */
export class ApiException extends Error {
  constructor(public error: ApiError) {
    super(error.message);
    this.name = 'ApiException';

    // Maintains proper stack trace for debugging
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, ApiException);
    }
  }
}

// ========================================
// FETCH WITH TIMEOUT
// ========================================

/**
 * Fetch wrapper with timeout support
 * Aborts request after specified timeout to prevent hanging
 *
 * @param url - Request URL
 * @param options - Fetch options
 * @param timeout - Timeout in milliseconds (default 30s)
 * @returns Fetch response
 * @throws ApiException on timeout or network error
 */
async function fetchWithTimeout(
  url: string,
  options: RequestInit,
  timeout: number = API_TIMEOUT
): Promise<Response> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeout);

  try {
    const response = await fetch(url, {
      ...options,
      signal: controller.signal,
    });
    clearTimeout(timeoutId);
    return response;
  } catch (error: any) {
    clearTimeout(timeoutId);

    // Timeout error
    if (error.name === 'AbortError') {
      throw new ApiException({
        code: 'TIMEOUT_ERROR',
        message: API_ERROR_MESSAGES.TIMEOUT_ERROR,
      });
    }

    // Network error (no connection, DNS failure, CORS, etc.)
    throw new ApiException({
      code: 'NETWORK_ERROR',
      message: API_ERROR_MESSAGES.NETWORK_ERROR,
    });
  }
}

// ========================================
// REGISTRATION API
// ========================================

/**
 * Registers a new user account
 *
 * POST /api/auth/register
 *
 * Success (201):
 * - Returns AuthResponse with user data and JWT token
 * - Token should be stored in localStorage
 * - User should be redirected to /onboarding
 *
 * Errors:
 * - 400 VALIDATION_ERROR: Field validation failed
 * - 409 EMAIL_ALREADY_EXISTS: Email already registered
 * - 500 INTERNAL_SERVER_ERROR: Server error
 * - NETWORK_ERROR: No connection to server
 * - TIMEOUT_ERROR: Request took too long
 *
 * @param data - Registration form data
 * @returns Registration response with user data and token
 * @throws ApiException on any error
 */
export async function registerUser(
  data: RegisterRequest
): Promise<RegisterResponse> {
  try {
    const response = await fetchWithTimeout(
      `${API_BASE_URL}/api/auth/register`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
      }
    );

    // Handle error responses
    if (!response.ok) {
      let error: ApiError;

      try {
        // Try to parse error response from backend
        error = await response.json();
      } catch {
        // If parsing fails, create generic error based on status code
        error = {
          code: response.status === 500 ? 'INTERNAL_SERVER_ERROR' : 'UNKNOWN_ERROR',
          message: API_ERROR_MESSAGES[
            response.status === 500 ? 'INTERNAL_SERVER_ERROR' : 'UNKNOWN_ERROR'
          ],
        };
      }

      throw new ApiException(error);
    }

    // Parse success response
    const result: RegisterResponse = await response.json();
    return result;
  } catch (error) {
    // Re-throw ApiException as-is
    if (error instanceof ApiException) {
      throw error;
    }

    // Wrap any other errors as UNKNOWN_ERROR
    throw new ApiException({
      code: 'UNKNOWN_ERROR',
      message: API_ERROR_MESSAGES.UNKNOWN_ERROR,
    });
  }
}

// ========================================
// LOGIN API
// ========================================

/**
 * Authenticates an existing user
 *
 * POST /api/auth/login
 *
 * Success (200):
 * - Returns AuthResponse with user data and JWT token
 * - Token should be stored in localStorage/sessionStorage based on rememberMe
 * - User should be redirected to /dashboard
 *
 * Errors:
 * - 401 INVALID_CREDENTIALS: Wrong email or password
 * - 403 EMAIL_NOT_VERIFIED: Account exists but email not verified
 * - 500 INTERNAL_SERVER_ERROR: Server error
 * - NETWORK_ERROR: No connection to server
 * - TIMEOUT_ERROR: Request took too long
 *
 * @param credentials - Login credentials (email and password)
 * @returns Authentication response with user data and token
 * @throws ApiException on any error
 */
export async function loginUser(
  credentials: LoginRequest
): Promise<AuthResponse> {
  try {
    const response = await fetchWithTimeout(
      `${API_BASE_URL}/api/auth/login`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(credentials),
      }
    );

    // Handle error responses
    if (!response.ok) {
      let error: ApiError;

      try {
        // Try to parse error response from backend
        error = await response.json();
      } catch {
        // If parsing fails, create generic error based on status code
        if (response.status === 401) {
          error = {
            code: 'INVALID_CREDENTIALS',
            message: 'Nieprawidłowy email lub hasło',
          };
        } else if (response.status === 403) {
          error = {
            code: 'EMAIL_NOT_VERIFIED',
            message: 'Proszę zweryfikować email przed zalogowaniem',
          };
        } else if (response.status === 500) {
          error = {
            code: 'INTERNAL_SERVER_ERROR',
            message: API_ERROR_MESSAGES.INTERNAL_SERVER_ERROR,
          };
        } else {
          error = {
            code: 'UNKNOWN_ERROR',
            message: API_ERROR_MESSAGES.UNKNOWN_ERROR,
          };
        }
      }

      throw new ApiException(error);
    }

    // Parse success response
    const result: AuthResponse = await response.json();
    return result;
  } catch (error) {
    // Re-throw ApiException as-is
    if (error instanceof ApiException) {
      throw error;
    }

    // Wrap any other errors as UNKNOWN_ERROR
    throw new ApiException({
      code: 'UNKNOWN_ERROR',
      message: API_ERROR_MESSAGES.UNKNOWN_ERROR,
    });
  }
}

// ========================================
// FUTURE: Additional auth endpoints
// ========================================

/**
 * TODO: Implement password recovery
 * POST /api/auth/forgot-password
 */
// export async function forgotPassword(email: string): Promise<void> { }

/**
 * TODO: Implement password reset
 * POST /api/auth/reset-password
 */
// export async function resetPassword(data: ResetPasswordRequest): Promise<void> { }