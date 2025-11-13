/**
 * FiltersBar - Main filter container with dropdowns and refresh button
 *
 * Features:
 * - FilterDropdowns on the left
 * - RefreshButton on the right
 * - FilterChips below (active filters)
 * - Responsive layout
 * - Integration with useReviewFilters and useManualRefresh
 */

'use client';

import React from 'react';
import { FilterDropdowns } from './FilterDropdowns';
import { FilterChips } from './FilterChips';
import type { ReviewFilterState } from '@/lib/types/review';

// ========================================
// TYPES
// ========================================

interface FiltersBarProps {
  filters: ReviewFilterState;
  onFiltersChange: (filters: ReviewFilterState) => void;
  onFilterRemove: (key: keyof ReviewFilterState, value?: any) => void;
  onClearAllFilters: () => void;
  // Refresh props (will be used when RefreshButton is implemented)
  onRefresh?: () => void;
  canRefresh?: boolean;
  isRefreshing?: boolean;
  timeRemaining?: string;
}

// ========================================
// COMPONENT
// ========================================

export function FiltersBar({
  filters,
  onFiltersChange,
  onFilterRemove,
  onClearAllFilters,
  onRefresh,
  canRefresh = true,
  isRefreshing = false,
  timeRemaining = '',
}: FiltersBarProps) {
  return (
    <div className="space-y-4">
      {/* Top Bar: Filters + Refresh */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        {/* Filter Dropdowns */}
        <div className="flex-1">
          <FilterDropdowns filters={filters} onChange={onFiltersChange} />
        </div>

        {/* Refresh Button Placeholder */}
        {onRefresh && (
          <div className="flex-shrink-0">
            <button
              onClick={onRefresh}
              disabled={!canRefresh || isRefreshing}
              className={`flex items-center gap-2 rounded-lg border px-4 py-2 text-sm font-medium shadow-sm transition-all ${
                !canRefresh || isRefreshing
                  ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                  : 'bg-white text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500'
              }`}
              title={!canRefresh ? `Dostępne za: ${timeRemaining}` : 'Odśwież dane'}
            >
              <svg
                className={`h-4 w-4 ${isRefreshing ? 'animate-spin' : ''}`}
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
              <span className="hidden sm:inline">
                {isRefreshing ? 'Odświeżanie...' : canRefresh ? 'Odśwież dane' : timeRemaining}
              </span>
            </button>
          </div>
        )}
      </div>

      {/* Active Filter Chips */}
      <FilterChips
        filters={filters}
        onRemove={onFilterRemove}
        onClearAll={onClearAllFilters}
      />
    </div>
  );
}
