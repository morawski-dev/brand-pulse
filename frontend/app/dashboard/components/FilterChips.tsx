/**
 * FilterChips - Display active filters as removable chips
 *
 * Features:
 * - Show each active filter value as a chip
 * - X button to remove individual filter
 * - "Clear all" button when multiple filters active
 * - Color-coded by filter type
 * - Integration with useReviewFilters
 */

'use client';

import React from 'react';
import { Badge } from '@/components/ui/badge';
import type { ReviewFilterState } from '@/lib/types/review';
import { Sentiment, Rating } from '@/lib/types/common';

// ========================================
// TYPES
// ========================================

interface FilterChipsProps {
  filters: ReviewFilterState;
  onRemove: (key: keyof ReviewFilterState, value?: any) => void;
  onClearAll: () => void;
}

// ========================================
// HELPER FUNCTIONS
// ========================================

function getSentimentLabel(sentiment: Sentiment): string {
  switch (sentiment) {
    case Sentiment.POSITIVE:
      return 'Pozytywny';
    case Sentiment.NEGATIVE:
      return 'Negatywny';
    case Sentiment.NEUTRAL:
      return 'Neutralny';
  }
}

function getSentimentIcon(sentiment: Sentiment): string {
  switch (sentiment) {
    case Sentiment.POSITIVE:
      return '👍';
    case Sentiment.NEGATIVE:
      return '👎';
    case Sentiment.NEUTRAL:
      return '➖';
  }
}

function formatDate(date: Date): string {
  return date.toLocaleDateString('pl-PL', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

// ========================================
// COMPONENT
// ========================================

export function FilterChips({ filters, onRemove, onClearAll }: FilterChipsProps) {
  // Count active filters
  const activeCount =
    filters.sentiment.size +
    filters.rating.size +
    (filters.startDate ? 1 : 0) +
    (filters.endDate ? 1 : 0);

  // No active filters
  if (activeCount === 0) {
    return null;
  }

  return (
    <div className="flex flex-wrap items-center gap-2">
      <span className="text-sm text-gray-600">Aktywne filtry:</span>

      {/* Sentiment Chips */}
      {Array.from(filters.sentiment).map((sentiment) => (
        <Badge
          key={`sentiment-${sentiment}`}
          variant="secondary"
          className="flex items-center gap-1.5 pl-2 pr-1 py-1 bg-purple-100 text-purple-700 hover:bg-purple-200"
        >
          <span className="text-sm">{getSentimentIcon(sentiment)}</span>
          <span className="text-xs font-medium">{getSentimentLabel(sentiment)}</span>
          <button
            onClick={() => onRemove('sentiment', sentiment)}
            className="ml-1 rounded-full p-0.5 hover:bg-purple-300 transition-colors"
          >
            <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </Badge>
      ))}

      {/* Rating Chips */}
      {Array.from(filters.rating).map((rating) => (
        <Badge
          key={`rating-${rating}`}
          variant="secondary"
          className="flex items-center gap-1.5 pl-2 pr-1 py-1 bg-yellow-100 text-yellow-700 hover:bg-yellow-200"
        >
          <div className="flex items-center gap-0.5">
            {Array.from({ length: rating as number }).map((_, i) => (
              <svg
                key={i}
                className="h-3 w-3"
                fill="currentColor"
                viewBox="0 0 20 20"
              >
                <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
              </svg>
            ))}
          </div>
          <button
            onClick={() => onRemove('rating', rating)}
            className="ml-1 rounded-full p-0.5 hover:bg-yellow-300 transition-colors"
          >
            <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </Badge>
      ))}

      {/* Start Date Chip */}
      {filters.startDate && (
        <Badge
          variant="secondary"
          className="flex items-center gap-1.5 pl-2 pr-1 py-1 bg-blue-100 text-blue-700 hover:bg-blue-200"
        >
          <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <span className="text-xs font-medium">Od: {formatDate(filters.startDate)}</span>
          <button
            onClick={() => onRemove('startDate')}
            className="ml-1 rounded-full p-0.5 hover:bg-blue-300 transition-colors"
          >
            <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </Badge>
      )}

      {/* End Date Chip */}
      {filters.endDate && (
        <Badge
          variant="secondary"
          className="flex items-center gap-1.5 pl-2 pr-1 py-1 bg-blue-100 text-blue-700 hover:bg-blue-200"
        >
          <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <span className="text-xs font-medium">Do: {formatDate(filters.endDate)}</span>
          <button
            onClick={() => onRemove('endDate')}
            className="ml-1 rounded-full p-0.5 hover:bg-blue-300 transition-colors"
          >
            <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </Badge>
      )}

      {/* Clear All Button */}
      {activeCount > 1 && (
        <button
          onClick={onClearAll}
          className="text-xs text-gray-600 hover:text-gray-900 font-medium underline"
        >
          Wyczyść wszystkie
        </button>
      )}
    </div>
  );
}
