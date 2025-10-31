/**
 * Onboarding Page - First-time user setup (PLACEHOLDER)
 *
 * Route: /onboarding
 * Protected: Requires authentication
 *
 * Flow:
 * 1. User completes registration
 * 2. Redirected here automatically
 * 3. Configure first review source (Google/Facebook/Trustpilot)
 * 4. Redirect to /dashboard
 *
 * TODO: Implement onboarding flow (US-003)
 */

import { Metadata } from 'next';
import Link from 'next/link';

export const metadata: Metadata = {
  title: 'Konfiguracja konta - BrandPulse',
  description: 'Skonfiguruj pierwsze źródło opinii',
};

export default function OnboardingPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50">
      <div className="container mx-auto px-4 py-8">
        <header className="mb-8">
          <div className="flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-600">
              <span className="text-xl font-bold text-white">BP</span>
            </div>
            <span className="text-xl font-bold text-gray-900">BrandPulse</span>
          </div>
        </header>

        <main className="mx-auto max-w-2xl">
          <div className="rounded-2xl border border-gray-200 bg-white p-8 shadow-xl">
            <div className="space-y-6 text-center">
              <h1 className="text-3xl font-bold tracking-tight text-gray-900">
                Witaj w BrandPulse! 🎉
              </h1>
              <p className="text-gray-600">
                Twoje konto zostało utworzone pomyślnie.
              </p>
              <p className="text-gray-600">
                Strona konfiguracji pierwszego źródła opinii w trakcie implementacji.
              </p>
              <Link
                href="/"
                className="inline-block text-blue-600 hover:underline"
              >
                ← Wróć do strony głównej
              </Link>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
