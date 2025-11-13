/**
 * RefreshButton - Button to trigger manual data synchronization
 *
 * Features:
 * - Disabled state with countdown when refresh not available (24h cooldown)
 * - Loading spinner during refresh
 * - Tooltip with information about rate limit
 * - Icon animation on click
 * - Integration with useManualRefresh hook
 *
 * Implements US-008: Manual data refresh
 */

'use client';

import React from 'react';
import { Button } from '@/components/ui/button';

// ========================================
// TYPES
// ========================================

interface RefreshButtonProps {
  onClick: () => void;
  canRefresh: boolean;
  isRefreshing: boolean;
  timeRemaining: string; // Human-readable (e.g., "22 godziny 15 minut")
}

// ========================================
// COMPONENT
// ========================================

export function RefreshButton({
  onClick,
  canRefresh,
  isRefreshing,
  timeRemaining,
}: RefreshButtonProps) {
  const isDisabled = !canRefresh || isRefreshing;

  return (
    <div className="relative group">
      <Button
        onClick={onClick}
        disabled={isDisabled}
        variant={isDisabled ? 'outline' : 'default'}
        size="default"
        className="flex items-center gap-2"
      >
        {/* Refresh Icon */}
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

        {/* Button Text */}
        <span className="hidden sm:inline">
          {isRefreshing
            ? 'Odświeżanie...'
            : canRefresh
            ? 'Odśwież dane'
            : `Dostępne za ${timeRemaining}`}
        </span>

        {/* Mobile: Icon only, show countdown in tooltip */}
        <span className="sm:hidden">
          {isRefreshing ? 'Odświeżanie' : canRefresh ? 'Odśwież' : timeRemaining.split(' ')[0]}
        </span>
      </Button>

      {/* Tooltip */}
      {!canRefresh && !isRefreshing && (
        <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover:block z-50">
          <div className="bg-gray-900 text-white text-xs rounded-lg px-3 py-2 whitespace-nowrap shadow-lg">
            <div className="font-semibold mb-1">Limit odświeżania: raz na 24h</div>
            <div className="text-gray-300">Następne odświeżanie za: {timeRemaining}</div>
            {/* Arrow */}
            <div className="absolute top-full left-1/2 -translate-x-1/2 -mt-px">
              <div className="border-4 border-transparent border-t-gray-900" />
            </div>
          </div>
        </div>
      )}

      {/* Success Tooltip */}
      {canRefresh && !isRefreshing && (
        <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover:block z-50">
          <div className="bg-gray-900 text-white text-xs rounded-lg px-3 py-2 whitespace-nowrap shadow-lg">
            <div>Pobierz najnowsze opinie z zewnętrznych źródeł</div>
            {/* Arrow */}
            <div className="absolute top-full left-1/2 -translate-x-1/2 -mt-px">
              <div className="border-4 border-transparent border-t-gray-900" />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
