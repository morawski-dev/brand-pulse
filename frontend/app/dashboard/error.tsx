/**
 * Dashboard Error Boundary
 * Catches errors in dashboard route and displays fallback UI
 */

'use client';

import { useEffect } from 'react';
import { Button } from '@/components/ui/button';

export default function DashboardError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Log error to console (extend to send to error tracking service)
    console.error('Dashboard error:', error);
  }, [error]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 via-white to-purple-50 p-4">
      <div className="max-w-md w-full">
        <div className="rounded-lg border bg-white p-8 shadow-lg text-center space-y-4">
          {/* Icon */}
          <div className="flex justify-center">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-red-100">
              <svg
                className="h-8 w-8 text-red-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                />
              </svg>
            </div>
          </div>

          {/* Title */}
          <h2 className="text-xl font-semibold text-gray-900">
            Wystąpił błąd w Dashboard
          </h2>

          {/* Description */}
          <p className="text-sm text-gray-600">
            Nie udało się załadować panelu. Spróbuj odświeżyć stronę lub
            skontaktuj się z pomocą techniczną, jeśli problem będzie się
            powtarzał.
          </p>

          {/* Error Details (dev mode only) */}
          {process.env.NODE_ENV === 'development' && (
            <details className="mt-4 text-left">
              <summary className="cursor-pointer text-xs text-gray-500 hover:text-gray-700">
                Szczegóły błędu (tryb deweloperski)
              </summary>
              <pre className="mt-2 text-xs bg-gray-100 p-2 rounded overflow-x-auto">
                {error.message}
              </pre>
              {error.digest && (
                <p className="mt-1 text-xs text-gray-500">
                  Error digest: {error.digest}
                </p>
              )}
            </details>
          )}

          {/* Actions */}
          <div className="flex gap-2 justify-center">
            <Button onClick={reset} variant="outline">
              <svg
                className="h-4 w-4 mr-2"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                />
              </svg>
              Spróbuj ponownie
            </Button>

            <Button onClick={() => window.location.reload()} variant="default">
              <svg
                className="h-4 w-4 mr-2"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                />
              </svg>
              Odśwież stronę
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
