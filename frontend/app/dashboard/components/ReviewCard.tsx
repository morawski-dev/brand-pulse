/**
 * ReviewCard - Single review display card
 *
 * Features:
 * - Display all review information (author, date, rating, content, sentiment)
 * - Expand/collapse for long content (> 300 characters)
 * - Sentiment badge with edit action
 * - Optimistic update overlay during sentiment change
 * - Highlight state for keyboard navigation
 * - Source type icon
 *
 * Implements US-004, US-007
 */

'use client';

import React, { useState } from 'react';
import { Card } from '@/components/ui/card';
import { SentimentBadge } from './SentimentBadge';
import { ReviewActions } from './ReviewActions';
import type { ReviewViewModel } from '@/lib/types/review';
import type { Sentiment, SourceType } from '@/lib/types/common';

// ========================================
// TYPES
// ========================================

interface ReviewCardProps {
  review: ReviewViewModel;
  onSentimentChange: (reviewId: number, newSentiment: Sentiment) => void;
  isSelected?: boolean; // For keyboard navigation
}

// ========================================
// CONSTANTS
// ========================================

const CONTENT_COLLAPSE_THRESHOLD = 300; // Characters

// ========================================
// HELPER FUNCTIONS
// ========================================

function getSourceIcon(sourceType: SourceType): string {
  switch (sourceType) {
    case 'GOOGLE':
      return '🔍';
    case 'FACEBOOK':
      return '📘';
    case 'TRUSTPILOT':
      return '⭐';
    default:
      return '📍';
  }
}

function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString('pl-PL', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

// ========================================
// COMPONENT
// ========================================

export function ReviewCard({
  review,
  onSentimentChange,
  isSelected = false,
}: ReviewCardProps) {
  const [isExpanded, setIsExpanded] = useState(false);

  // Check if content should be collapsible
  const shouldCollapse = review.content.length > CONTENT_COLLAPSE_THRESHOLD;
  const displayContent =
    shouldCollapse && !isExpanded
      ? review.content.substring(0, CONTENT_COLLAPSE_THRESHOLD) + '...'
      : review.content;

  return (
    <Card
      className={`p-6 transition-all ${
        isSelected
          ? 'ring-2 ring-blue-500 shadow-md'
          : 'hover:shadow-md'
      } ${review.isOptimisticUpdate ? 'relative' : ''}`}
    >
      {/* Optimistic Update Overlay */}
      {review.isOptimisticUpdate && (
        <div className="absolute inset-0 bg-white/80 backdrop-blur-sm rounded-lg flex items-center justify-center z-10">
          <div className="flex items-center gap-2 text-gray-600">
            <svg className="h-5 w-5 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
            <span className="text-sm font-medium">Aktualizowanie...</span>
          </div>
        </div>
      )}

      <div className="space-y-4">
        {/* Header: Author + Date + Source */}
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            {/* Author Avatar */}
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-blue-100 to-purple-100 flex-shrink-0">
              <span className="text-sm font-semibold text-blue-700">
                {review.authorName.charAt(0).toUpperCase()}
              </span>
            </div>

            {/* Author Info */}
            <div>
              <div className="font-medium text-gray-900">
                {review.authorName}
              </div>
              <div className="flex items-center gap-2 text-xs text-gray-500">
                <span>{formatDate(review.publishedAt)}</span>
                <span>•</span>
                <span className="flex items-center gap-1">
                  <span>{getSourceIcon(review.sourceType)}</span>
                  <span>{review.sourceType}</span>
                </span>
              </div>
            </div>
          </div>

          {/* Rating Stars */}
          <div className="flex items-center gap-0.5 flex-shrink-0">
            {Array.from({ length: 5 }).map((_, i) => (
              <svg
                key={i}
                className={`h-5 w-5 ${
                  i < review.rating ? 'text-yellow-400' : 'text-gray-300'
                }`}
                fill="currentColor"
                viewBox="0 0 20 20"
              >
                <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
              </svg>
            ))}
          </div>
        </div>

        {/* Review Content */}
        <div className="prose prose-sm max-w-none">
          <p className="text-gray-700 leading-relaxed whitespace-pre-wrap">
            {displayContent}
          </p>

          {/* Expand/Collapse Button */}
          {shouldCollapse && (
            <button
              onClick={() => setIsExpanded(!isExpanded)}
              className="text-sm text-blue-600 hover:text-blue-700 font-medium mt-2"
            >
              {isExpanded ? 'Zwiń' : 'Rozwiń'}
            </button>
          )}
        </div>

        {/* Footer: Sentiment Badge + Actions */}
        <div className="flex items-center justify-between pt-2 border-t">
          <SentimentBadge
            sentiment={review.sentiment}
            confidence={review.sentimentConfidence}
            showConfidence={false}
          />

          <ReviewActions
            reviewId={review.reviewId}
            currentSentiment={review.sentiment}
            onChange={onSentimentChange}
            isUpdating={review.isOptimisticUpdate}
          />
        </div>

        {/* Previous Sentiment Indicator (during optimistic update) */}
        {review.isOptimisticUpdate && review.previousSentiment && (
          <div className="text-xs text-gray-500 italic">
            Poprzedni sentyment: {review.previousSentiment}
          </div>
        )}
      </div>
    </Card>
  );
}
