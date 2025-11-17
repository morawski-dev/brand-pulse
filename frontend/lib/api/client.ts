/**
 * API Client - Centralized HTTP client for all API requests
 *
 * Features:
 * - JWT authentication via Authorization header
 * - Request/response interceptors
 * - Error handling with typed exceptions
 * - Timeout support (30s default)
 * - CORS support
 */

import { getAuthToken } from '@/context/AuthContext';

// ========================================
// CONFIGURATION
// ========================================

/**
 * API base URL from environment variable
 * Falls back to localhost:8080 for development
 */
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

/**
 * Default timeout for API requests (30 seconds)
 */
export const API_TIMEOUT = 30000;

// ========================================
// ERROR TYPES
// ========================================

/**
 * Standard error response structure from backend
 */
export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, any>;
}

/**
 * Custom exception class for API errors
 */
export class ApiException extends Error {
  public readonly error: ApiError;
  public readonly status?: number;

  constructor(error: ApiError, status?: number) {
    super(error.message);
    this.name = 'ApiException';
    this.error = error;
    this.status = status;
  }
}

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
      throw new ApiException({
        code: 'TIMEOUT_ERROR',
        message: 'Przekroczono czas oczekiwania. Spróbuj ponownie.',
      });
    }

    // Network error (no connection, DNS failure, CORS, etc.)
    throw new ApiException({
      code: 'NETWORK_ERROR',
      message: 'Błąd połączenia. Sprawdź połączenie internetowe.',
    });
  }
}

// ========================================
// REQUEST BUILDER
// ========================================

/**
 * Build request options with authentication and default headers
 */
function buildRequestOptions(
  method: string,
  body?: any,
  additionalHeaders?: Record<string, string>
): RequestInit {
  const token = getAuthToken();

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...additionalHeaders,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const options: RequestInit = {
    method,
    headers,
  };

  if (body) {
    options.body = JSON.stringify(body);
  }

  return options;
}

// ========================================
// ERROR HANDLER
// ========================================

/**
 * Handle HTTP error responses
 * Tries to parse error from backend, falls back to generic messages
 */
async function handleErrorResponse(response: Response): Promise<never> {
  let error: ApiError;

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
        code: 'FORBIDDEN',
        message: 'Nie masz uprawnień do wykonania tej operacji.',
      };
    } else if (response.status === 404) {
      error = {
        code: 'NOT_FOUND',
        message: 'Nie znaleziono zasobu.',
      };
    } else if (response.status === 409) {
      error = {
        code: 'CONFLICT',
        message: 'Konflikt zasobów.',
      };
    } else if (response.status === 429) {
      error = {
        code: 'RATE_LIMIT_EXCEEDED',
        message: 'Przekroczono limit żądań. Spróbuj później.',
      };
    } else if (response.status >= 500) {
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

  throw new ApiException(error, response.status);
}

// ========================================
// HTTP METHODS
// ========================================

/**
 * Perform GET request
 */
export async function apiGet<T>(
  endpoint: string,
  params?: Record<string, any>
): Promise<T> {
  const url = new URL(`${API_BASE_URL}${endpoint}`);

  // Add query parameters
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        // Handle arrays (for multi-select filters)
        if (Array.isArray(value)) {
          url.searchParams.append(key, value.join(','));
        } else {
          url.searchParams.append(key, String(value));
        }
      }
    });
  }

  try {
    const response = await fetchWithTimeout(
      url.toString(),
      buildRequestOptions('GET')
    );

    if (!response.ok) {
      await handleErrorResponse(response);
    }

    return await response.json();
  } catch (error) {
    if (error instanceof ApiException) {
      throw error;
    }
    throw new ApiException({
      code: 'UNKNOWN_ERROR',
      message: 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.',
    });
  }
}

/**
 * Perform POST request
 */
export async function apiPost<T>(
  endpoint: string,
  body?: any
): Promise<T> {
  try {
    const response = await fetchWithTimeout(
      `${API_BASE_URL}${endpoint}`,
      buildRequestOptions('POST', body)
    );

    if (!response.ok) {
      await handleErrorResponse(response);
    }

    // Handle 204 No Content
    if (response.status === 204) {
      return {} as T;
    }

    return await response.json();
  } catch (error) {
    if (error instanceof ApiException) {
      throw error;
    }
    throw new ApiException({
      code: 'UNKNOWN_ERROR',
      message: 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.',
    });
  }
}

/**
 * Perform PATCH request
 */
export async function apiPatch<T>(
  endpoint: string,
  body?: any
): Promise<T> {
  try {
    const response = await fetchWithTimeout(
      `${API_BASE_URL}${endpoint}`,
      buildRequestOptions('PATCH', body)
    );

    if (!response.ok) {
      await handleErrorResponse(response);
    }

    // Handle 204 No Content
    if (response.status === 204) {
      return {} as T;
    }

    return await response.json();
  } catch (error) {
    if (error instanceof ApiException) {
      throw error;
    }
    throw new ApiException({
      code: 'UNKNOWN_ERROR',
      message: 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.',
    });
  }
}

/**
 * Perform PUT request
 */
export async function apiPut<T>(
  endpoint: string,
  body?: any
): Promise<T> {
  try {
    const response = await fetchWithTimeout(
      `${API_BASE_URL}${endpoint}`,
      buildRequestOptions('PUT', body)
    );

    if (!response.ok) {
      await handleErrorResponse(response);
    }

    // Handle 204 No Content
    if (response.status === 204) {
      return {} as T;
    }

    return await response.json();
  } catch (error) {
    if (error instanceof ApiException) {
      throw error;
    }
    throw new ApiException({
      code: 'UNKNOWN_ERROR',
      message: 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.',
    });
  }
}

/**
 * Perform DELETE request
 */
export async function apiDelete<T = void>(
  endpoint: string
): Promise<T> {
  try {
    const response = await fetchWithTimeout(
      `${API_BASE_URL}${endpoint}`,
      buildRequestOptions('DELETE')
    );

    if (!response.ok) {
      await handleErrorResponse(response);
    }

    // Handle 204 No Content
    if (response.status === 204) {
      return {} as T;
    }

    // Most DELETE requests return 204, but handle JSON responses
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return await response.json();
    }

    return {} as T;
  } catch (error) {
    if (error instanceof ApiException) {
      throw error;
    }
    throw new ApiException({
      code: 'UNKNOWN_ERROR',
      message: 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.',
    });
  }
}

// ========================================
// EXPORTS
// ========================================

export const apiClient = {
  get: apiGet,
  post: apiPost,
  patch: apiPatch,
  put: apiPut,
  delete: apiDelete,
};

export default apiClient;
