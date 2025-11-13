/**
 * ReviewsLoadingSkeleton - Loading skeleton for review list
 *
 * Features:
 * - Animated pulse effect
 * - Multiple skeleton cards (configurable count)
 * - Matches ReviewCard structure
 * - Responsive design
 */

'use client';

import React from 'react';
import { Card } from '@/components/ui/card';

// ========================================
// TYPES
// ========================================

interface ReviewsLoadingSkeletonProps {
  count?: number; // Number of skeleton cards (default: 5)
}

// ========================================
// COMPONENT
// ========================================

export function ReviewsLoadingSkeleton({ count = 5 }: ReviewsLoadingSkeletonProps) {
  return (
    <div className="space-y-4">
      {Array.from({ length: count }).map((_, index) => (
        <Card key={index} className="p-6">
          <div className="space-y-4">
            {/* Header: Author + Date + Source */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-full bg-gray-200 relative overflow-hidden">
                  <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/60 to-transparent" />
                </div>
                <div className="space-y-2">
                  <div className="h-4 w-32 rounded bg-gray-200 relative overflow-hidden">
                    <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/60 to-transparent" />
                  </div>
                  <div className="h-3 w-24 rounded bg-gray-200 relative overflow-hidden">
                    <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/60 to-transparent" />
                  </div>
                </div>
              </div>
              <div className="h-6 w-16 rounded bg-gray-200 relative overflow-hidden">
                <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/60 to-transparent" />
              </div>
            </div>

            {/* Rating */}
            <div className="flex items-center gap-1">
              {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="h-5 w-5 rounded bg-gray-200 relative overflow-hidden">
                  <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/60 to-transparent" />
                </div>
              ))}
            </div>

            {/* Content */}
            <div className="space-y-2">
              <div className="h-4 w-full rounded bg-gray-200 relative overflow-hidden">
                <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/60 to-transparent" />
              </div>
              <div className="h-4 w-full rounded bg-gray-200 relative overflow-hidden">
                <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/60 to-transparent" />
              </div>
              <div className="h-4 w-3/4 rounded bg-gray-200 relative overflow-hidden">
                <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/60 to-transparent" />
              </div>
            </div>

            {/* Footer: Sentiment + Actions */}
            <div className="flex items-center justify-between pt-2">
              <div className="h-6 w-24 rounded-full bg-gray-200 relative overflow-hidden">
                <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/60 to-transparent" />
              </div>
              <div className="h-8 w-8 rounded bg-gray-200 relative overflow-hidden">
                <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/60 to-transparent" />
              </div>
            </div>
          </div>
        </Card>
      ))}
    </div>
  );
}
