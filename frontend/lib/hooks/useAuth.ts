/**
 * useAuth - Global authentication state management hook
 *
 * Manages:
 * - JWT token storage in localStorage
 * - Token expiry validation
 * - Authentication state
 * - Login/Logout actions
 *
 * Usage:
 * ```tsx
 * const { login, logout, isAuthenticated } = useAuth();
 * ```
 */

'use client';

import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { UseAuthReturn } from '@/lib/types/api';

// ========================================
// LOCALSTORAGE KEYS
// ========================================

const AUTH_TOKEN_KEY = 'authToken';
const TOKEN_EXPIRY_KEY = 'tokenExpiry';

// ========================================
// HOOK IMPLEMENTATION
// ========================================

export function useAuth(): UseAuthReturn {
  const router = useRouter();
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  /**
   * Check token validity on mount
   * Validates stored token and expiry timestamp
   */
  useEffect(() => {
    const token = localStorage.getItem(AUTH_TOKEN_KEY);
    const expiry = localStorage.getItem(TOKEN_EXPIRY_KEY);

    if (token && expiry) {
      const expiryDate = new Date(expiry);
      const now = new Date();

      if (expiryDate > now) {
        // Token is still valid
        setIsAuthenticated(true);
      } else {
        // Token expired, clean up
        logout();
      }
    }
  }, []);

  /**
   * Login action
   * Stores JWT token and expiry timestamp in localStorage
   *
   * @param token - JWT access token from backend
   * @param expiresAt - ISO 8601 expiry timestamp
   */
  const login = useCallback((token: string, expiresAt: string) => {
    localStorage.setItem(AUTH_TOKEN_KEY, token);
    localStorage.setItem(TOKEN_EXPIRY_KEY, expiresAt);
    setIsAuthenticated(true);
  }, []);

  /**
   * Logout action
   * Removes token from localStorage and redirects to login page
   */
  const logout = useCallback(() => {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(TOKEN_EXPIRY_KEY);
    setIsAuthenticated(false);
    router.push('/login');
  }, [router]);

  return {
    login,
    logout,
    isAuthenticated,
  };
}

/**
 * Utility function to get stored auth token
 * Used by API client for authenticated requests
 *
 * @returns JWT token or null if not authenticated
 */
export function getAuthToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(AUTH_TOKEN_KEY);
}

/**
 * Utility function to check if user is authenticated
 * Validates both token existence and expiry
 *
 * @returns true if authenticated with valid token
 */
export function isUserAuthenticated(): boolean {
  if (typeof window === 'undefined') return false;

  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  const expiry = localStorage.getItem(TOKEN_EXPIRY_KEY);

  if (!token || !expiry) return false;

  const expiryDate = new Date(expiry);
  const now = new Date();

  return expiryDate > now;
}