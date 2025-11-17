/**
 * Brand API client
 * Handles brand creation and management endpoints
 */

import { getAuthToken } from '@/context/AuthContext';
import {
  CreateBrandRequest,
  BrandResponse,
  BrandListResponse,
  BrandApiError,
  BrandApiException,
} from '@/lib/types/brand';

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
// FETCH WITH TIMEOUT
// ========================================

/**
 * Fetch wrapper with timeout support
 * Aborts request after specified timeout to prevent hanging
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
      throw new BrandApiException({
        code: 'TIMEOUT_ERROR',
        message: 'Przekroczono czas oczekiwania. Spróbuj ponownie.',
      });
    }

    // Network error (no connection, DNS failure, CORS, etc.)
    throw new BrandApiException({
      code: 'NETWORK_ERROR',
      message: 'Błąd połączenia. Sprawdź połączenie internetowe.',
    });
  }
}

// ========================================
// BRAND API ENDPOINTS
// ========================================

/**
 * Creates a new brand for the authenticated user
 *
 * POST /api/brands
 *
 * Success (201):
 * - Returns BrandResponse with brand data
 * - Each user can have only one brand (MVP constraint)
 *
 * Errors:
 * - 400 VALIDATION_ERROR: Field validation failed
 * - 401 UNAUTHORIZED: Missing or invalid JWT token
 * - 409 BRAND_ALREADY_EXISTS: User already has a brand
 * - 500 INTERNAL_SERVER_ERROR: Server error
 * - NETWORK_ERROR: No connection to server
 * - TIMEOUT_ERROR: Request took too long
 *
 * @param brandData - Brand creation data (name)
 * @returns Brand response with ID and metadata
 * @throws BrandApiException on any error
 */
export async function createBrand(
  brandData: CreateBrandRequest
): Promise<BrandResponse> {
  const token = getAuthToken();

  if (!token) {
    throw new BrandApiException({
      code: 'UNAUTHORIZED',
      message: 'Musisz być zalogowany, aby utworzyć markę.',
    });
  }

  try {
    const response = await fetchWithTimeout(
      `${API_BASE_URL}/api/brands`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(brandData),
      }
    );

    // Handle error responses
    if (!response.ok) {
      let error: BrandApiError;

      try {
        // Try to parse error response from backend
        error = await response.json();
      } catch {
        // If parsing fails, create generic error based on status code
        if (response.status === 401) {
          error = {
            code: 'UNAUTHORIZED',
            message: 'Sesja wygasła. Zaloguj się ponownie.',
          };
        } else if (response.status === 409) {
          error = {
            code: 'BRAND_ALREADY_EXISTS',
            message: 'Marka została już utworzona dla tego konta.',
          };
        } else if (response.status === 500) {
          error = {
            code: 'INTERNAL_SERVER_ERROR',
            message: 'Wystąpił błąd serwera. Spróbuj ponownie później.',
          };
        } else {
          error = {
            code: 'UNKNOWN_ERROR',
            message: 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.',
          };
        }
      }

      throw new BrandApiException(error);
    }

    // Parse success response
    const result: BrandResponse = await response.json();
    return result;
  } catch (error) {
    // Re-throw BrandApiException as-is
    if (error instanceof BrandApiException) {
      throw error;
    }

    // Wrap any other errors as UNKNOWN_ERROR
    throw new BrandApiException({
      code: 'UNKNOWN_ERROR',
      message: 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.',
    });
  }
}

/**
 * Gets the authenticated user's brand
 *
 * GET /api/brands
 *
 * Success (200):
 * - Returns first brand from user's brands list
 * - Returns null if user doesn't have a brand yet (empty list)
 *
 * Note: MVP supports only ONE brand per user, so this returns brands[0]
 *
 * Errors:
 * - 401 UNAUTHORIZED: Missing or invalid JWT token
 * - 500 INTERNAL_SERVER_ERROR: Server error
 * - NETWORK_ERROR: No connection to server
 * - TIMEOUT_ERROR: Request took too long
 *
 * @returns Brand response or null if user has no brand
 * @throws BrandApiException on any error
 */
export async function getUserBrand(): Promise<BrandResponse | null> {
  const token = getAuthToken();

  if (!token) {
    throw new BrandApiException({
      code: 'UNAUTHORIZED',
      message: 'Musisz być zalogowany, aby pobrać markę.',
    });
  }

  try {
    const response = await fetchWithTimeout(
      `${API_BASE_URL}/api/brands`,
      {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    // Handle error responses
    if (!response.ok) {
      let error: BrandApiError;

      try {
        // Try to parse error response from backend
        error = await response.json();
      } catch {
        // If parsing fails, create generic error based on status code
        if (response.status === 401) {
          error = {
            code: 'UNAUTHORIZED',
            message: 'Sesja wygasła. Zaloguj się ponownie.',
          };
        } else if (response.status === 500) {
          error = {
            code: 'INTERNAL_SERVER_ERROR',
            message: 'Wystąpił błąd serwera. Spróbuj ponownie później.',
          };
        } else {
          error = {
            code: 'UNKNOWN_ERROR',
            message: 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.',
          };
        }
      }

      throw new BrandApiException(error);
    }

    // Parse success response - GET /api/brands returns { brands: BrandResponse[] }
    const result: BrandListResponse = await response.json();

    // MVP supports only one brand per user, return first or null if empty
    return result.brands[0] ?? null;
  } catch (error) {
    // Re-throw BrandApiException as-is
    if (error instanceof BrandApiException) {
      throw error;
    }

    // Wrap any other errors as UNKNOWN_ERROR
    throw new BrandApiException({
      code: 'UNKNOWN_ERROR',
      message: 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.',
    });
  }
}
