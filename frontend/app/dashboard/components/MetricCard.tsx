/**
 * MetricCard - Single metric display card
 *
 * Features:
 * - Icon + label + value display
 * - Optional trend indicator (up/down/neutral)
 * - Responsive sizing
 * - Loading skeleton support
 *
 * Used in MetricsSection to display key metrics:
 * - Average rating
 * - Total reviews
 * - Positive sentiment %
 * - Negative sentiment %
 */

'use client';

import React from 'react';
import { Card } from '@/components/ui/card';

// ========================================
// TYPES
// ========================================

interface MetricCardProps {
  label: string;
  value: string | number;
  icon: React.ReactNode;
  trend?: {
    value: number;
    direction: 'up' | 'down' | 'neutral';
  };
  isLoading?: boolean;
}

// ========================================
// COMPONENT
// ========================================

export function MetricCard({
  label,
  value,
  icon,
  trend,
  isLoading = false,
}: MetricCardProps) {
  if (isLoading) {
    return (
      <Card className="p-6">
        <div className="space-y-3">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-lg bg-gray-200 relative overflow-hidden">
              <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/60 to-transparent" />
            </div>
            <div className="flex-1">
              <div className="h-4 w-24 rounded bg-gray-200 relative overflow-hidden">
                <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/60 to-transparent" />
              </div>
            </div>
          </div>
          <div className="h-8 w-20 rounded bg-gray-200 relative overflow-hidden">
            <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/60 to-transparent" />
          </div>
        </div>
      </Card>
    );
  }

  return (
    <Card className="p-6 transition-all duration-200 hover:shadow-lg hover:-translate-y-0.5">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          {/* Icon + Label */}
          <div className="flex items-center gap-3 mb-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-gradient-to-br from-blue-50 to-purple-50 text-blue-600">
              {icon}
            </div>
            <div className="text-sm font-medium text-gray-600">
              {label}
            </div>
          </div>

          {/* Value */}
          <div className="text-3xl font-bold text-gray-900">
            {value}
          </div>
        </div>

        {/* Trend Indicator */}
        {trend && (
          <div className={`flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium ${
            trend.direction === 'up'
              ? 'bg-green-100 text-green-700'
              : trend.direction === 'down'
              ? 'bg-red-100 text-red-700'
              : 'bg-gray-100 text-gray-700'
          }`}>
            {trend.direction === 'up' && (
              <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 10l7-7m0 0l7 7m-7-7v18" />
              </svg>
            )}
            {trend.direction === 'down' && (
              <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 14l-7 7m0 0l-7-7m7 7V3" />
              </svg>
            )}
            {trend.direction === 'neutral' && (
              <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 12h14" />
              </svg>
            )}
            <span>{Math.abs(trend.value)}%</span>
          </div>
        )}
      </div>
    </Card>
  );
}
