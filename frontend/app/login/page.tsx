/**
 * Login Page - User login view
 *
 * Route: /login
 * Public access (no authentication required)
 *
 * Features:
 * - Email/password authentication
 * - Remember me option
 * - Auto-redirect if already authenticated
 * - Links to registration and password recovery
 */

'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { LoginForm } from '@/components/auth/login-form';
import { useAuthContext } from '@/context/AuthContext';

export default function LoginPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading } = useAuthContext();

  // Auto-redirect if already authenticated
  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      router.push('/dashboard');
    }
  }, [isAuthenticated, isLoading, router]);

  // Show loading state while checking authentication
  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 via-white to-purple-50">
        <div className="text-center">
          <div className="mb-4 inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-blue-600 border-r-transparent"></div>
          <p className="text-gray-600">Sprawdzanie autoryzacji...</p>
        </div>
      </div>
    );
  }

  // Don't render login form if already authenticated (will redirect)
  if (isAuthenticated) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50">
      <div className="container mx-auto px-4 py-8">
        {/* Header with Logo */}
        <header className="mb-8 flex items-center justify-between">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary">
              <span className="text-lg font-bold text-primary-foreground">BP</span>
            </div>
            <span className="text-xl font-bold text-gray-900">BrandPulse</span>
          </Link>
        </header>

        {/* Main Content */}
        <main className="mx-auto max-w-md">
          <div className="rounded-2xl border border-gray-200 bg-white p-8 shadow-xl">
            {/* Page Title */}
            <div className="mb-8 text-center">
              <h1 className="text-3xl font-bold tracking-tight text-gray-900">
                Zaloguj się
              </h1>
              <p className="mt-2 text-sm text-gray-600">
                Zaloguj się do swojego konta BrandPulse
              </p>
            </div>

            {/* Login Form Component */}
            <LoginForm />
          </div>

          {/* Footer Links */}
          <div className="mt-6 text-center text-sm text-gray-500">
            <Link href="/" className="hover:text-gray-700 hover:underline">
              ← Wróć na stronę główną
            </Link>
          </div>
        </main>
      </div>
    </div>
  );
}
