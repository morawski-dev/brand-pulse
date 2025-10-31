/**
 * Terms of Service Page (PLACEHOLDER)
 *
 * Route: /terms
 * Public access
 *
 * TODO: Add actual terms of service content
 */

import { Metadata } from 'next';
import Link from 'next/link';

export const metadata: Metadata = {
  title: 'Regulamin serwisu - BrandPulse',
  description: 'Regulamin korzystania z serwisu BrandPulse',
};

export default function TermsPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto max-w-4xl px-4 py-12">
        <header className="mb-8">
          <Link href="/" className="flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-600">
              <span className="text-xl font-bold text-white">BP</span>
            </div>
            <span className="text-xl font-bold text-gray-900">BrandPulse</span>
          </Link>
        </header>

        <main className="rounded-lg bg-white p-8 shadow-sm">
          <h1 className="mb-6 text-3xl font-bold text-gray-900">
            Regulamin serwisu
          </h1>

          <div className="prose max-w-none">
            <p className="text-gray-600">
              Treść regulaminu będzie dostępna wkrótce.
            </p>

            <h2 className="mt-6 text-xl font-semibold text-gray-900">
              1. Postanowienia ogólne
            </h2>
            <p className="text-gray-600">
              [Placeholder - treść do uzupełnienia]
            </p>

            <h2 className="mt-6 text-xl font-semibold text-gray-900">
              2. Zasady korzystania z serwisu
            </h2>
            <p className="text-gray-600">
              [Placeholder - treść do uzupełnienia]
            </p>

            <h2 className="mt-6 text-xl font-semibold text-gray-900">
              3. Odpowiedzialność
            </h2>
            <p className="text-gray-600">
              [Placeholder - treść do uzupełnienia]
            </p>
          </div>

          <div className="mt-8">
            <Link
              href="/register"
              className="text-blue-600 hover:underline"
            >
              ← Wróć do rejestracji
            </Link>
          </div>
        </main>
      </div>
    </div>
  );
}
