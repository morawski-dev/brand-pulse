/**
 * DashboardLayout - Main layout component for dashboard page
 *
 * Structure:
 * - Sticky header (DashboardHeader)
 * - Sidebar (desktop only, hidden on mobile)
 * - Main content area
 * - Bottom navigation (mobile only, hidden on desktop)
 *
 * Responsive breakpoints:
 * - Mobile: < 768px (md)
 * - Desktop: >= 768px (md)
 */

'use client';

import React from 'react';
import type { BrandResponse } from '@/lib/types/brand';
import type { ReviewSourceSummary } from '@/lib/types/dashboard';
import { LocationSelector } from './LocationSelector';

// ========================================
// TYPES
// ========================================

interface DashboardLayoutProps {
  children: React.ReactNode;
  brand: BrandResponse | null;
  sources: ReviewSourceSummary[];
  selectedLocation: 'all' | number;
  onLocationChange: (locationId: 'all' | number) => void;
}

// ========================================
// COMPONENT
// ========================================

export function DashboardLayout({
  children,
  brand,
  sources,
  selectedLocation,
  onLocationChange,
}: DashboardLayoutProps) {
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50">
      {/* Header - Sticky */}
      <header className="sticky top-0 z-50 border-b bg-white/80 backdrop-blur-sm">
        <div className="container mx-auto px-4">
          <div className="flex h-16 items-center justify-between gap-2">
            {/* Logo */}
            <div className="flex items-center gap-2 sm:gap-3 min-w-0">
              <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-blue-600 to-purple-600">
                <span className="text-xl font-bold text-white">BP</span>
              </div>
              <div className="flex flex-col min-w-0">
                <span className="text-base sm:text-lg font-bold text-gray-900 truncate">BrandPulse</span>
                {brand && (
                  <span className="text-xs text-gray-500 truncate">{brand.name}</span>
                )}
              </div>
            </div>

            {/* Location Selector - Desktop */}
            <div className="hidden md:flex items-center gap-4 flex-shrink-0">
              <LocationSelector
                sources={sources}
                selectedLocation={selectedLocation}
                onChange={onLocationChange}
              />
            </div>

            {/* User Menu */}
            <div className="flex items-center gap-2 flex-shrink-0">
              <button className="flex h-10 w-10 items-center justify-center rounded-full bg-gray-200 hover:bg-gray-300 transition-colors">
                <span className="text-sm font-semibold text-gray-700">U</span>
              </button>
            </div>
          </div>

          {/* Location Selector - Mobile (below header) */}
          <div className="md:hidden py-3 border-t">
            <LocationSelector
              sources={sources}
              selectedLocation={selectedLocation}
              onChange={onLocationChange}
            />
          </div>
        </div>
      </header>

      {/* Main Layout */}
      <div className="container mx-auto px-4 py-6 pb-24 md:pb-6">
        <div className="flex gap-6">
          {/* Sidebar - Desktop only */}
          <aside className="hidden md:block w-64 shrink-0">
            <div className="sticky top-24 space-y-4">
              {/* Navigation */}
              <nav className="rounded-lg border bg-white p-4 shadow-sm">
                <div className="space-y-2">
                  <div className="rounded-md bg-blue-50 px-3 py-2 text-sm font-medium text-blue-700">
                    Dashboard
                  </div>
                  <div className="px-3 py-2 text-sm text-gray-600 hover:bg-gray-50 rounded-md cursor-pointer">
                    Źródła
                  </div>
                  <div className="px-3 py-2 text-sm text-gray-600 hover:bg-gray-50 rounded-md cursor-pointer">
                    Ustawienia
                  </div>
                  <div className="px-3 py-2 text-sm text-gray-600 hover:bg-gray-50 rounded-md cursor-pointer">
                    Pomoc
                  </div>
                </div>
              </nav>

              {/* Quick Stats */}
              <div className="rounded-lg border bg-white p-4 shadow-sm">
                <h3 className="text-sm font-semibold text-gray-900 mb-3">
                  Szybki dostęp
                </h3>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-gray-600">Źródła aktywne:</span>
                    <span className="font-medium">{sources.filter(s => s.isActive).length}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Ostatnia sync:</span>
                    <span className="font-medium text-xs">
                      {brand?.lastManualRefreshAt
                        ? new Date(brand.lastManualRefreshAt).toLocaleDateString('pl-PL')
                        : 'Nigdy'}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </aside>

          {/* Main Content */}
          <main className="flex-1 min-w-0">
            {children}
          </main>
        </div>
      </div>

      {/* Bottom Navigation - Mobile only */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 z-50 border-t bg-white shadow-lg">
        <div className="flex justify-around p-2">
          <button className="flex flex-col items-center gap-1 p-2 text-blue-600">
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
            </svg>
            <span className="text-xs font-medium">Dashboard</span>
          </button>
          <button className="flex flex-col items-center gap-1 p-2 text-gray-600">
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
            <span className="text-xs">Źródła</span>
          </button>
          <button className="flex flex-col items-center gap-1 p-2 text-gray-600">
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <span className="text-xs">Ustawienia</span>
          </button>
        </div>
      </nav>
    </div>
  );
}
