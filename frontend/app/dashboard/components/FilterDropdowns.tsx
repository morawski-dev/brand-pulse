/**
 * FilterDropdowns - Multi-select dropdowns for filtering reviews
 *
 * Features:
 * - Sentiment filter (POSITIVE, NEGATIVE, NEUTRAL)
 * - Rating filter (1-5 stars)
 * - Date range picker (startDate, endDate)
 * - Validation: startDate <= endDate
 * - Multi-select with checkboxes
 *
 * Integrates with useReviewFilters hook
 */

'use client';

import React, { useState } from 'react';
import type { ReviewFilterState } from '@/lib/types/review';
import { Sentiment, Rating } from '@/lib/types/common';
import { Checkbox } from '@/components/ui/checkbox';

// ========================================
// TYPES
// ========================================

interface FilterDropdownsProps {
  filters: ReviewFilterState;
  onChange: (filters: ReviewFilterState) => void;
}

// ========================================
// COMPONENT
// ========================================

export function FilterDropdowns({ filters, onChange }: FilterDropdownsProps) {
  const [openDropdown, setOpenDropdown] = useState<string | null>(null);
  const [dateError, setDateError] = useState<string | null>(null);

  // ========================================
  // SENTIMENT FILTER
  // ========================================

  const sentimentOptions = [
    { value: Sentiment.POSITIVE, label: 'Pozytywny', icon: '👍', color: 'text-green-600' },
    { value: Sentiment.NEGATIVE, label: 'Negatywny', icon: '👎', color: 'text-red-600' },
    { value: Sentiment.NEUTRAL, label: 'Neutralny', icon: '➖', color: 'text-gray-600' },
  ];

  const toggleSentiment = (sentiment: Sentiment) => {
    const newSet = new Set(filters.sentiment);
    if (newSet.has(sentiment)) {
      newSet.delete(sentiment);
    } else {
      newSet.add(sentiment);
    }
    onChange({ ...filters, sentiment: newSet });
  };

  // ========================================
  // RATING FILTER
  // ========================================

  const ratingOptions: Rating[] = [5, 4, 3, 2, 1];

  const toggleRating = (rating: Rating) => {
    const newSet = new Set(filters.rating);
    if (newSet.has(rating)) {
      newSet.delete(rating);
    } else {
      newSet.add(rating);
    }
    onChange({ ...filters, rating: newSet });
  };

  // ========================================
  // DATE FILTER
  // ========================================

  const handleDateChange = (type: 'start' | 'end', value: string) => {
    const newDate = value ? new Date(value) : null;

    const newFilters = {
      ...filters,
      [type === 'start' ? 'startDate' : 'endDate']: newDate,
    };

    // Validate dates
    if (newFilters.startDate && newFilters.endDate) {
      if (newFilters.startDate > newFilters.endDate) {
        setDateError('Data końcowa musi być późniejsza niż data początkowa');
        return;
      }
    }

    setDateError(null);
    onChange(newFilters);
  };

  // ========================================
  // RENDER
  // ========================================

  return (
    <div className="flex flex-wrap items-center gap-3">
      {/* Sentiment Dropdown */}
      <div className="relative">
        <button
          onClick={() => setOpenDropdown(openDropdown === 'sentiment' ? null : 'sentiment')}
          className="flex items-center gap-2 rounded-lg border bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>Sentyment</span>
          {filters.sentiment.size > 0 && (
            <span className="flex h-5 w-5 items-center justify-center rounded-full bg-blue-600 text-xs font-semibold text-white">
              {filters.sentiment.size}
            </span>
          )}
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </button>

        {openDropdown === 'sentiment' && (
          <>
            <div className="fixed inset-0 z-10" onClick={() => setOpenDropdown(null)} />
            <div className="absolute left-0 z-20 mt-2 w-64 rounded-lg border bg-white shadow-lg">
              <div className="p-3 space-y-2">
                {sentimentOptions.map((option) => (
                  <label
                    key={option.value}
                    className="flex items-center gap-3 rounded-md px-3 py-2 hover:bg-gray-50 cursor-pointer"
                  >
                    <Checkbox
                      checked={filters.sentiment.has(option.value)}
                      onCheckedChange={() => toggleSentiment(option.value)}
                    />
                    <span className="text-lg">{option.icon}</span>
                    <span className={`text-sm font-medium ${option.color}`}>
                      {option.label}
                    </span>
                  </label>
                ))}
              </div>
            </div>
          </>
        )}
      </div>

      {/* Rating Dropdown */}
      <div className="relative">
        <button
          onClick={() => setOpenDropdown(openDropdown === 'rating' ? null : 'rating')}
          className="flex items-center gap-2 rounded-lg border bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
            <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
          </svg>
          <span>Ocena</span>
          {filters.rating.size > 0 && (
            <span className="flex h-5 w-5 items-center justify-center rounded-full bg-blue-600 text-xs font-semibold text-white">
              {filters.rating.size}
            </span>
          )}
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </button>

        {openDropdown === 'rating' && (
          <>
            <div className="fixed inset-0 z-10" onClick={() => setOpenDropdown(null)} />
            <div className="absolute left-0 z-20 mt-2 w-64 rounded-lg border bg-white shadow-lg">
              <div className="p-3 space-y-2">
                {ratingOptions.map((rating) => (
                  <label
                    key={rating}
                    className="flex items-center gap-3 rounded-md px-3 py-2 hover:bg-gray-50 cursor-pointer"
                  >
                    <Checkbox
                      checked={filters.rating.has(rating)}
                      onCheckedChange={() => toggleRating(rating)}
                    />
                    <div className="flex items-center gap-1">
                      {Array.from({ length: rating }).map((_, i) => (
                        <svg
                          key={i}
                          className="h-4 w-4 text-yellow-400"
                          fill="currentColor"
                          viewBox="0 0 20 20"
                        >
                          <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                        </svg>
                      ))}
                    </div>
                    <span className="text-sm font-medium text-gray-700">
                      {rating} {rating === 1 ? 'gwiazdka' : 'gwiazdki'}
                    </span>
                  </label>
                ))}
              </div>
            </div>
          </>
        )}
      </div>

      {/* Date Range Picker */}
      <div className="relative">
        <button
          onClick={() => setOpenDropdown(openDropdown === 'date' ? null : 'date')}
          className="flex items-center gap-2 rounded-lg border bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <span>Zakres dat</span>
          {(filters.startDate || filters.endDate) && (
            <span className="flex h-5 w-5 items-center justify-center rounded-full bg-blue-600 text-xs font-semibold text-white">
              1
            </span>
          )}
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </button>

        {openDropdown === 'date' && (
          <>
            <div className="fixed inset-0 z-10" onClick={() => setOpenDropdown(null)} />
            <div className="absolute left-0 z-20 mt-2 w-80 rounded-lg border bg-white shadow-lg">
              <div className="p-4 space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Data początkowa
                  </label>
                  <input
                    type="date"
                    value={filters.startDate ? filters.startDate.toISOString().split('T')[0] : ''}
                    onChange={(e) => handleDateChange('start', e.target.value)}
                    className="w-full rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Data końcowa
                  </label>
                  <input
                    type="date"
                    value={filters.endDate ? filters.endDate.toISOString().split('T')[0] : ''}
                    onChange={(e) => handleDateChange('end', e.target.value)}
                    className="w-full rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                {dateError && (
                  <div className="text-sm text-red-600 flex items-center gap-2">
                    <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                    </svg>
                    {dateError}
                  </div>
                )}
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
