/**
 * ReviewsEmptyState - Empty state component for review list
 *
 * Features:
 * - Two variants: no reviews at all vs no reviews after filtering
 * - Contextual messaging and CTA
 * - Icon illustration
 * - Action buttons (Add source, Clear filters)
 */

'use client';

import React from 'react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';

// ========================================
// TYPES
// ========================================

interface ReviewsEmptyStateProps {
  hasActiveFilters: boolean;
  onClearFilters: () => void;
  onAddSource?: () => void;
}

// ========================================
// COMPONENT
// ========================================

export function ReviewsEmptyState({
  hasActiveFilters,
  onClearFilters,
  onAddSource,
}: ReviewsEmptyStateProps) {
  // Variant 1: No reviews after filtering
  if (hasActiveFilters) {
    return (
      <Card className="p-12">
        <div className="flex flex-col items-center justify-center text-center space-y-4">
          {/* Icon */}
          <div className="flex h-20 w-20 items-center justify-center rounded-full bg-gray-100">
            <svg
              className="h-10 w-10 text-gray-400"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"
              />
            </svg>
          </div>

          {/* Message */}
          <div className="space-y-2">
            <h3 className="text-xl font-semibold text-gray-900">
              Brak opinii spełniających kryteria
            </h3>
            <p className="text-sm text-gray-500 max-w-md">
              Nie znaleziono opinii pasujących do wybranych filtrów. Spróbuj zmienić
              kryteria wyszukiwania lub wyczyść filtry, aby zobaczyć wszystkie opinie.
            </p>
          </div>

          {/* CTA Button */}
          <Button onClick={onClearFilters} variant="default">
            <svg className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
            Wyczyść filtry
          </Button>
        </div>
      </Card>
    );
  }

  // Variant 2: No reviews at all
  return (
    <Card className="p-12">
      <div className="flex flex-col items-center justify-center text-center space-y-4">
        {/* Icon */}
        <div className="flex h-20 w-20 items-center justify-center rounded-full bg-blue-50">
          <svg
            className="h-10 w-10 text-blue-600"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"
            />
          </svg>
        </div>

        {/* Message */}
        <div className="space-y-2">
          <h3 className="text-xl font-semibold text-gray-900">
            Nie masz jeszcze żadnych opinii
          </h3>
          <p className="text-sm text-gray-500 max-w-md">
            Aby zobaczyć opinie klientów, dodaj źródło opinii (Google, Facebook, Trustpilot).
            System automatycznie pobierze opinie z ostatnich 90 dni.
          </p>
        </div>

        {/* CTA Button */}
        {onAddSource && (
          <Button onClick={onAddSource} variant="default">
            <svg className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 4v16m8-8H4"
              />
            </svg>
            Dodaj źródło opinii
          </Button>
        )}
      </div>
    </Card>
  );
}
