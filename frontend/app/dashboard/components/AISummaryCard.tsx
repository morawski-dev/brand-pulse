/**
 * AISummaryCard - AI-generated summary display card
 *
 * Features:
 * - Display AI-generated text summary
 * - Show model used and generation timestamp
 * - Loading skeleton
 * - Empty state when no summary available
 * - Validity expiration indicator
 *
 * Uses data from DashboardSummaryResponse.aiSummary
 */

'use client';

import React from 'react';
import { Card } from '@/components/ui/card';
import type { AISummaryResponse } from '@/lib/types/dashboard';

// ========================================
// TYPES
// ========================================

interface AISummaryCardProps {
  aiSummary: AISummaryResponse | null;
  isLoading?: boolean;
}

// ========================================
// HELPER FUNCTIONS
// ========================================

function formatTimestamp(timestamp: string): string {
  const date = new Date(timestamp);
  return date.toLocaleDateString('pl-PL', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function isExpiringSoon(validUntil: string): boolean {
  const expiryDate = new Date(validUntil);
  const now = new Date();
  const hoursRemaining = (expiryDate.getTime() - now.getTime()) / (1000 * 60 * 60);
  return hoursRemaining < 2; // Less than 2 hours
}

// ========================================
// COMPONENT
// ========================================

export function AISummaryCard({ aiSummary, isLoading = false }: AISummaryCardProps) {
  // Loading state
  if (isLoading) {
    return (
      <Card className="p-6">
        <div className="space-y-4">
          <div className="flex items-center gap-2">
            <div className="h-5 w-5 rounded bg-gray-200 relative overflow-hidden">
              <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/60 to-transparent" />
            </div>
            <div className="h-5 w-32 rounded bg-gray-200 relative overflow-hidden">
              <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/60 to-transparent" />
            </div>
          </div>
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
          <div className="flex items-center gap-4 pt-2">
            <div className="h-3 w-24 rounded bg-gray-200 relative overflow-hidden">
              <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/60 to-transparent" />
            </div>
            <div className="h-3 w-32 rounded bg-gray-200 relative overflow-hidden">
              <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/60 to-transparent" />
            </div>
          </div>
        </div>
      </Card>
    );
  }

  // Empty state
  if (!aiSummary) {
    return (
      <Card className="p-6">
        <div className="flex items-center gap-3 mb-4">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-gradient-to-br from-purple-50 to-blue-50">
            <span className="text-xl">✨</span>
          </div>
          <h3 className="text-lg font-semibold text-gray-900">
            Podsumowanie AI
          </h3>
        </div>
        <div className="text-center py-8">
          <div className="text-gray-400 mb-2">
            <svg className="h-12 w-12 mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
            </svg>
          </div>
          <p className="text-sm text-gray-500">
            Brak podsumowania AI
          </p>
          <p className="text-xs text-gray-400 mt-1">
            Podsumowanie zostanie wygenerowane po zebraniu większej ilości opinii
          </p>
        </div>
      </Card>
    );
  }

  // Check if expiring soon
  const expiringSoon = isExpiringSoon(aiSummary.validUntil);

  return (
    <Card className="p-6 transition-all duration-200 hover:shadow-lg hover:-translate-y-0.5">
      {/* Header */}
      <div className="flex items-center gap-3 mb-4">
        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-gradient-to-br from-purple-50 to-blue-50">
          <span className="text-xl">✨</span>
        </div>
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-gray-900">
            Podsumowanie AI
          </h3>
          {expiringSoon && (
            <div className="flex items-center gap-1 text-xs text-amber-600 mt-0.5">
              <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span>Wkrótce wygaśnie</span>
            </div>
          )}
        </div>
      </div>

      {/* Summary Text */}
      <div className="prose prose-sm max-w-none">
        <p className="text-gray-700 leading-relaxed">
          {aiSummary.text}
        </p>
      </div>

      {/* Footer */}
      <div className="mt-4 pt-4 border-t flex items-center justify-between text-xs text-gray-500">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-1">
            <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
            <span>Model: {aiSummary.modelUsed}</span>
          </div>
          <div className="flex items-center gap-1">
            <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>Wygenerowano: {formatTimestamp(aiSummary.generatedAt)}</span>
          </div>
        </div>
        <div className={`text-xs ${expiringSoon ? 'text-amber-600 font-medium' : 'text-gray-500'}`}>
          Ważne do: {formatTimestamp(aiSummary.validUntil)}
        </div>
      </div>
    </Card>
  );
}
