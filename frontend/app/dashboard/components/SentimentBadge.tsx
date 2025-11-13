/**
 * SentimentBadge - Display review sentiment with icon
 *
 * Features:
 * - Color-coded badge (green/red/gray)
 * - Icon for each sentiment (thumbs up/down, minus)
 * - Optional confidence score display
 * - Responsive sizing
 */

'use client';

import React from 'react';
import { Badge } from '@/components/ui/badge';
import { Sentiment } from '@/lib/types/common';

// ========================================
// TYPES
// ========================================

interface SentimentBadgeProps {
  sentiment: Sentiment;
  confidence?: number; // 0-1 confidence score
  showConfidence?: boolean;
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

function getSentimentColor(sentiment: Sentiment): string {
  switch (sentiment) {
    case Sentiment.POSITIVE:
      return 'bg-green-100 text-green-700 border-green-200';
    case Sentiment.NEGATIVE:
      return 'bg-red-100 text-red-700 border-red-200';
    case Sentiment.NEUTRAL:
      return 'bg-gray-100 text-gray-700 border-gray-200';
  }
}

// ========================================
// COMPONENT
// ========================================

export function SentimentBadge({
  sentiment,
  confidence,
  showConfidence = false,
}: SentimentBadgeProps) {
  return (
    <Badge
      variant="outline"
      className={`flex items-center gap-1.5 px-3 py-1 border ${getSentimentColor(sentiment)}`}
    >
      <span className="text-base">{getSentimentIcon(sentiment)}</span>
      <span className="text-xs font-medium">{getSentimentLabel(sentiment)}</span>
      {showConfidence && confidence !== undefined && (
        <span className="text-xs opacity-75">
          ({Math.round(confidence * 100)}%)
        </span>
      )}
    </Badge>
  );
}
