/**
 * Review Source API client
 * Handles review source creation, management, and import progress tracking
 */

import { getAuthToken } from '@/context/AuthContext';
import {
  CreateReviewSourceRequest,
  ReviewSourceResponse,
  ImportProgressResponse,
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
// REVIEW SOURCE API ENDPOINTS
// ========================================

/**
 * Creates a new review source for a brand
 *
 * POST /api/brands/{brandId}/sources
 *
 * Success (201):
 * - Returns ReviewSourceResponse with source data and import job ID
 * - Automatically starts initial import of last 90 days of reviews
 * - Free plan allows only 1 source per brand
 *
 * Errors:
 * - 400 VALIDATION_ERROR: Field validation failed (invalid URL, etc.)
 * - 401 UNAUTHORIZED: Missing or invalid JWT token
 * - 403 SOURCE_LIMIT_REACHED: Free plan limit reached (1 source)
 * - 404 BRAND_NOT_FOUND: Brand doesn't exist
 * - 409 DUPLICATE_SOURCE: Source with same external ID already exists
 * - 500 INTERNAL_SERVER_ERROR: Server error
 * - NETWORK_ERROR: No connection to server
 * - TIMEOUT_ERROR: Request took too long
 *
 * @param brandId - Brand ID to add source to
 * @param sourceData - Review source creation data
 * @returns Review source response with ID and import job ID
 * @throws BrandApiException on any error
 */
export async function createReviewSource(
  brandId: number,
  sourceData: CreateReviewSourceRequest
): Promise<ReviewSourceResponse> {
  const token = getAuthToken();

  if (!token) {
    throw new BrandApiException({
      code: 'UNAUTHORIZED',
      message: 'Musisz być zalogowany, aby dodać źródło opinii.',
    });
  }

  try {
    const response = await fetchWithTimeout(
      `${API_BASE_URL}/api/brands/${brandId}/review-sources`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(sourceData),
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
        } else if (response.status === 403) {
          error = {
            code: 'SOURCE_LIMIT_REACHED',
            message:
              'Plan darmowy pozwala na dodanie 1 źródła. Przejdź na plan premium aby dodać więcej.',
          };
        } else if (response.status === 404) {
          error = {
            code: 'BRAND_NOT_FOUND',
            message: 'Marka nie została znaleziona.',
          };
        } else if (response.status === 409) {
          error = {
            code: 'DUPLICATE_SOURCE',
            message: 'To źródło zostało już dodane.',
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
    const result: ReviewSourceResponse = await response.json();
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
 * Gets import progress status for a review source
 *
 * GET /api/sources/{sourceId}/import-status
 *
 * Success (200):
 * - Returns ImportProgressResponse with progress percentage and status
 * - Use for polling during onboarding (every 2 seconds)
 * - Progress ranges from 0-100
 *
 * Errors:
 * - 401 UNAUTHORIZED: Missing or invalid JWT token
 * - 404 SOURCE_NOT_FOUND: Source doesn't exist
 * - 500 INTERNAL_SERVER_ERROR: Server error
 * - NETWORK_ERROR: No connection to server
 * - TIMEOUT_ERROR: Request took too long
 *
 * @param sourceId - Review source ID
 * @returns Import progress response
 * @throws BrandApiException on any error
 */
export async function getImportProgress(
  sourceId: number
): Promise<ImportProgressResponse> {
  const token = getAuthToken();

  if (!token) {
    throw new BrandApiException({
      code: 'UNAUTHORIZED',
      message: 'Musisz być zalogowany, aby sprawdzić status importu.',
    });
  }

  try {
    const response = await fetchWithTimeout(
      `${API_BASE_URL}/api/sources/${sourceId}/import-status`,
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
        } else if (response.status === 404) {
          error = {
            code: 'SOURCE_NOT_FOUND',
            message: 'Źródło opinii nie zostało znalezione.',
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
    const result: ImportProgressResponse = await response.json();
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
 * Gets all review sources for a brand
 *
 * GET /api/brands/{brandId}/sources
 *
 * Success (200):
 * - Returns array of ReviewSourceResponse
 *
 * Errors:
 * - 401 UNAUTHORIZED: Missing or invalid JWT token
 * - 404 BRAND_NOT_FOUND: Brand doesn't exist
 * - 500 INTERNAL_SERVER_ERROR: Server error
 * - NETWORK_ERROR: No connection to server
 * - TIMEOUT_ERROR: Request took too long
 *
 * @param brandId - Brand ID
 * @returns Array of review source responses
 * @throws BrandApiException on any error
 */
export async function getReviewSources(
  brandId: number
): Promise<ReviewSourceResponse[]> {
  const token = getAuthToken();

  if (!token) {
    throw new BrandApiException({
      code: 'UNAUTHORIZED',
      message: 'Musisz być zalogowany, aby pobrać źródła opinii.',
    });
  }

  try {
    const response = await fetchWithTimeout(
      `${API_BASE_URL}/api/brands/${brandId}/review-sources`,
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
        } else if (response.status === 404) {
          error = {
            code: 'BRAND_NOT_FOUND',
            message: 'Marka nie została znaleziona.',
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

    // Parse success response. Backend wraps the list in { sources: [...] }
    // (ReviewSourceListResponse), so unwrap it to a bare array here.
    const result: { sources: ReviewSourceResponse[] } = await response.json();
    return result.sources ?? [];
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
