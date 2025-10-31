/**
 * Register Page - User registration view
 *
 * Route: /register
 * Public access (no authentication required)
 *
 * Features:
 * - Registration form with email/password validation
 * - Password strength indicator
 * - GDPR consent checkboxes
 * - Free plan information
 * - Auto-login after successful registration
 * - Redirect to /onboarding after registration
 *
 * Redirects:
 * - If already authenticated → /dashboard (optional, not implemented yet)
 */

import { Metadata } from 'next';
import Link from 'next/link';
import { RegisterForm } from './components/RegisterForm';

// ========================================
// METADATA
// ========================================

export const metadata: Metadata = {
  title: 'Rejestracja - BrandPulse',
  description:
    'Utwórz konto i zacznij monitorować opinie za darmo. Plan darmowy: 1 źródło opinii bez karty kredytowej.',
};

// ========================================
// PAGE COMPONENT
// ========================================

export default function RegisterPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50">
      {/* Skip to main content link for accessibility */}
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-blue-600 focus:px-4 focus:py-2 focus:text-white focus:outline-none focus:ring-2 focus:ring-blue-600 focus:ring-offset-2"
      >
        Przejdź do treści głównej
      </a>

      {/* Container */}
      <div className="container mx-auto px-4 py-8">
        {/* Page Header */}
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
        <main id="main-content" className="mx-auto max-w-md">
          {/* Registration Form Card */}
          <div className="rounded-2xl border border-gray-200 bg-white p-8 shadow-xl">
            <RegisterForm />
          </div>

          {/* Footer Info */}
          <p className="mt-8 text-center text-sm text-gray-500">
            Rejestrując się, akceptujesz nasze{' '}
            <Link
              href="/terms"
              className="text-blue-600 hover:underline"
            >
              warunki korzystania
            </Link>{' '}
            oraz{' '}
            <Link
              href="/privacy"
              className="text-blue-600 hover:underline"
            >
              politykę prywatności
            </Link>
            .
          </p>
        </main>
      </div>
    </div>
  );
}
