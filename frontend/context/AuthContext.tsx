/**
 * AuthContext - Global authentication state management
 *
 * Provides:
 * - User authentication state (user, isAuthenticated, isLoading)
 * - Login/logout actions
 * - Token storage with rememberMe support
 * - Automatic token expiry validation
 *
 * Usage:
 * ```tsx
 * // In app layout or root
 * <AuthProvider>
 *   <App />
 * </AuthProvider>
 *
 * // In components
 * const { user, isAuthenticated, login, logout } = useAuthContext();
 * ```
 */

'use client';

import React, { createContext, useContext, useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { AuthResponse, AuthState, LoginRequest } from '@/lib/types/auth';
import { loginUser, ApiException } from '@/lib/api/auth';

// ========================================
// LOCALSTORAGE/SESSIONSTORAGE KEYS
// ========================================

const AUTH_TOKEN_KEY = 'auth_token';
const TOKEN_EXPIRY_KEY = 'auth_expires_at';
const USER_DATA_KEY = 'auth_user';

// ========================================
// CONTEXT TYPE
// ========================================

interface AuthContextType extends AuthState {
  /**
   * Authenticate user with credentials
   * Calls API and stores token based on rememberMe
   */
  login: (credentials: LoginRequest, rememberMe: boolean) => Promise<void>;

  /**
   * Logout user and clear stored data
   * Redirects to login page
   */
  logout: () => void;

  /**
   * Check if user is authenticated with valid token
   */
  checkAuth: () => boolean;

  /**
   * API error from last login attempt
   */
  loginError: string | null;
}

// ========================================
// CONTEXT CREATION
// ========================================

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// ========================================
// PROVIDER COMPONENT
// ========================================

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<AuthResponse | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [loginError, setLoginError] = useState<string | null>(null);

  /**
   * Check stored authentication on mount
   */
  useEffect(() => {
    checkStoredAuth();
  }, []);

  /**
   * Validate stored token and restore user session
   */
  const checkStoredAuth = () => {
    try {
      // Try localStorage first
      let token = localStorage.getItem(AUTH_TOKEN_KEY);
      let expiry = localStorage.getItem(TOKEN_EXPIRY_KEY);
      let userData = localStorage.getItem(USER_DATA_KEY);

      // Fallback to sessionStorage
      if (!token) {
        token = sessionStorage.getItem(AUTH_TOKEN_KEY);
        expiry = sessionStorage.getItem(TOKEN_EXPIRY_KEY);
        userData = sessionStorage.getItem(USER_DATA_KEY);
      }

      if (token && expiry && userData) {
        const expiryDate = new Date(expiry);
        const now = new Date();

        if (expiryDate > now) {
          // Token is still valid
          const parsedUser: AuthResponse = JSON.parse(userData);
          setUser(parsedUser);
          setIsAuthenticated(true);
        } else {
          // Token expired, clean up
          clearAuth();
        }
      }
    } catch (error) {
      console.error('Failed to restore authentication:', error);
      clearAuth();
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Login user with credentials
   * Calls API and stores token based on rememberMe
   */
  const login = useCallback(async (credentials: LoginRequest, rememberMe: boolean) => {
    try {
      setIsLoading(true);
      setLoginError(null);

      // Call login API
      const response = await loginUser(credentials);

      // Choose storage based on rememberMe
      const storage = rememberMe ? localStorage : sessionStorage;

      // Store token and user data
      storage.setItem(AUTH_TOKEN_KEY, response.token);
      storage.setItem(TOKEN_EXPIRY_KEY, response.expiresAt);
      storage.setItem(USER_DATA_KEY, JSON.stringify(response));

      // Update state
      setUser(response);
      setIsAuthenticated(true);
      setIsLoading(false);

      // Redirect to dashboard
      router.push('/dashboard');
    } catch (error) {
      setIsLoading(false);

      if (error instanceof ApiException) {
        setLoginError(error.error.message);
        throw error;
      }

      const errorMessage = 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.';
      setLoginError(errorMessage);
      throw new Error(errorMessage);
    }
  }, [router]);

  /**
   * Logout user and clear all stored data
   */
  const logout = useCallback(() => {
    clearAuth();
    router.push('/login');
  }, [router]);

  /**
   * Clear all authentication data from storage
   */
  const clearAuth = () => {
    // Clear from both storages
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(TOKEN_EXPIRY_KEY);
    localStorage.removeItem(USER_DATA_KEY);
    sessionStorage.removeItem(AUTH_TOKEN_KEY);
    sessionStorage.removeItem(TOKEN_EXPIRY_KEY);
    sessionStorage.removeItem(USER_DATA_KEY);

    setUser(null);
    setIsAuthenticated(false);
  };

  /**
   * Check if user is currently authenticated
   */
  const checkAuth = useCallback((): boolean => {
    if (typeof window === 'undefined') return false;

    // Try localStorage first
    let token = localStorage.getItem(AUTH_TOKEN_KEY);
    let expiry = localStorage.getItem(TOKEN_EXPIRY_KEY);

    // Fallback to sessionStorage
    if (!token) {
      token = sessionStorage.getItem(AUTH_TOKEN_KEY);
      expiry = sessionStorage.getItem(TOKEN_EXPIRY_KEY);
    }

    if (!token || !expiry) return false;

    const expiryDate = new Date(expiry);
    const now = new Date();

    return expiryDate > now;
  }, []);

  const value: AuthContextType = {
    user,
    isAuthenticated,
    isLoading,
    loginError,
    login,
    logout,
    checkAuth,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// ========================================
// HOOK TO USE CONTEXT
// ========================================

/**
 * Hook to access auth context
 * Must be used within AuthProvider
 */
export function useAuthContext(): AuthContextType {
  const context = useContext(AuthContext);

  if (context === undefined) {
    throw new Error('useAuthContext must be used within AuthProvider');
  }

  return context;
}

// ========================================
// UTILITY FUNCTIONS
// ========================================

/**
 * Get stored auth token (for API requests)
 * Checks both localStorage and sessionStorage
 */
export function getAuthToken(): string | null {
  if (typeof window === 'undefined') return null;

  let token = localStorage.getItem(AUTH_TOKEN_KEY);
  if (!token) {
    token = sessionStorage.getItem(AUTH_TOKEN_KEY);
  }

  return token;
}

/**
 * Check if user is authenticated (utility function)
 */
export function isUserAuthenticated(): boolean {
  if (typeof window === 'undefined') return false;

  // Try localStorage first
  let token = localStorage.getItem(AUTH_TOKEN_KEY);
  let expiry = localStorage.getItem(TOKEN_EXPIRY_KEY);

  // Fallback to sessionStorage
  if (!token) {
    token = sessionStorage.getItem(AUTH_TOKEN_KEY);
    expiry = sessionStorage.getItem(TOKEN_EXPIRY_KEY);
  }

  if (!token || !expiry) return false;

  const expiryDate = new Date(expiry);
  const now = new Date();

  return expiryDate > now;
}
