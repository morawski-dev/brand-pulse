/**
 * LocationSelector - Dropdown to switch between "All locations" and individual sources
 *
 * Features:
 * - Dropdown menu with all available sources
 * - "All locations" option for aggregated view
 * - Source type icon and status indicator
 * - Active state highlighting
 *
 * Implements US-006: Switching view between locations
 */

'use client';

import React, { useState } from 'react';
import type { ReviewSourceSummary } from '@/lib/types/dashboard';
import { SourceType } from '@/lib/types/common';

// ========================================
// TYPES
// ========================================

interface LocationSelectorProps {
  sources: ReviewSourceSummary[];
  selectedLocation: 'all' | number;
  onChange: (locationId: 'all' | number) => void;
}

// ========================================
// HELPER FUNCTIONS
// ========================================

function getSourceIcon(sourceType: SourceType): string {
  switch (sourceType) {
    case SourceType.GOOGLE:
      return '🔍';
    case SourceType.FACEBOOK:
      return '📘';
    case SourceType.TRUSTPILOT:
      return '⭐';
    default:
      return '📍';
  }
}

function getSourceLabel(sourceType: SourceType): string {
  switch (sourceType) {
    case SourceType.GOOGLE:
      return 'Google';
    case SourceType.FACEBOOK:
      return 'Facebook';
    case SourceType.TRUSTPILOT:
      return 'Trustpilot';
    default:
      return sourceType;
  }
}

// ========================================
// COMPONENT
// ========================================

export function LocationSelector({
  sources,
  selectedLocation,
  onChange,
}: LocationSelectorProps) {
  const [isOpen, setIsOpen] = useState(false);

  // Get currently selected label
  const selectedLabel =
    selectedLocation === 'all'
      ? 'Wszystkie lokalizacje'
      : sources.find((s) => s.sourceId === selectedLocation)
      ? `${getSourceLabel(sources.find((s) => s.sourceId === selectedLocation)!.sourceType)}`
      : 'Nieznane';

  const handleSelect = (locationId: 'all' | number) => {
    onChange(locationId);
    setIsOpen(false);
  };

  return (
    <div className="relative">
      {/* Trigger Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 rounded-lg border bg-white px-4 py-2.5 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px] w-full md:w-auto justify-between"
      >
        <div className="flex items-center gap-2">
          <span className="text-base">📍</span>
          <span className="truncate">{selectedLabel}</span>
        </div>
        <svg
          className={`h-4 w-4 transition-transform flex-shrink-0 ${isOpen ? 'rotate-180' : ''}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {/* Dropdown Menu */}
      {isOpen && (
        <>
          {/* Backdrop */}
          <div
            className="fixed inset-0 z-10"
            onClick={() => setIsOpen(false)}
          />

          {/* Menu */}
          <div className="absolute left-0 md:right-0 z-20 mt-2 w-full md:w-64 rounded-lg border bg-white shadow-lg max-h-[400px] overflow-y-auto">
            <div className="p-2">
              {/* All Locations Option */}
              <button
                onClick={() => handleSelect('all')}
                className={`flex w-full items-center gap-3 rounded-md px-3 py-2.5 text-left text-sm transition-colors min-h-[44px] ${
                  selectedLocation === 'all'
                    ? 'bg-blue-50 text-blue-700 font-medium'
                    : 'text-gray-700 hover:bg-gray-50'
                }`}
              >
                <span className="text-base">📍</span>
                <div className="flex-1">
                  <div>Wszystkie lokalizacje</div>
                  <div className="text-xs text-gray-500">
                    Widok zagregowany ({sources.length} {sources.length === 1 ? 'źródło' : 'źródeł'})
                  </div>
                </div>
                {selectedLocation === 'all' && (
                  <svg className="h-4 w-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                )}
              </button>

              {/* Divider */}
              {sources.length > 0 && (
                <div className="my-2 border-t" />
              )}

              {/* Individual Sources */}
              {sources.map((source) => (
                <button
                  key={source.sourceId}
                  onClick={() => handleSelect(source.sourceId)}
                  className={`flex w-full items-center gap-3 rounded-md px-3 py-2.5 text-left text-sm transition-colors min-h-[44px] ${
                    selectedLocation === source.sourceId
                      ? 'bg-blue-50 text-blue-700 font-medium'
                      : 'text-gray-700 hover:bg-gray-50'
                  }`}
                >
                  <span className="text-base">{getSourceIcon(source.sourceType)}</span>
                  <div className="flex-1">
                    <div>{getSourceLabel(source.sourceType)}</div>
                    <div className="flex items-center gap-2 text-xs text-gray-500">
                      {source.isActive ? (
                        <span className="flex items-center gap-1">
                          <span className="h-1.5 w-1.5 rounded-full bg-green-500" />
                          Aktywne
                        </span>
                      ) : (
                        <span className="flex items-center gap-1">
                          <span className="h-1.5 w-1.5 rounded-full bg-gray-400" />
                          Nieaktywne
                        </span>
                      )}
                      {source.lastSyncAt && (
                        <span>
                          • {new Date(source.lastSyncAt).toLocaleDateString('pl-PL', {
                            day: 'numeric',
                            month: 'short',
                          })}
                        </span>
                      )}
                    </div>
                  </div>
                  {selectedLocation === source.sourceId && (
                    <svg className="h-4 w-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                    </svg>
                  )}
                </button>
              ))}

              {/* Empty State */}
              {sources.length === 0 && (
                <div className="px-3 py-4 text-center text-sm text-gray-500">
                  Brak skonfigurowanych źródeł
                </div>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
