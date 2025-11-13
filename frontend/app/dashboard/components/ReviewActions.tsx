/**
 * ReviewActions - Dropdown menu for review actions
 *
 * Features:
 * - Edit sentiment dropdown with 3 options
 * - Checkmark for currently selected sentiment
 * - Disabled state during update
 * - Integration with useSentimentUpdate
 *
 * Implements US-007: Manual sentiment correction
 */

'use client';

import React, { useState } from 'react';
import { Sentiment } from '@/lib/types/common';

// ========================================
// TYPES
// ========================================

interface ReviewActionsProps {
  reviewId: number;
  currentSentiment: Sentiment;
  onChange: (reviewId: number, newSentiment: Sentiment) => void;
  isUpdating?: boolean;
}

// ========================================
// SENTIMENT OPTIONS
// ========================================

const sentimentOptions = [
  { value: Sentiment.POSITIVE, label: 'Pozytywny', icon: '👍', color: 'text-green-600' },
  { value: Sentiment.NEGATIVE, label: 'Negatywny', icon: '👎', color: 'text-red-600' },
  { value: Sentiment.NEUTRAL, label: 'Neutralny', icon: '➖', color: 'text-gray-600' },
];

// ========================================
// COMPONENT
// ========================================

export function ReviewActions({
  reviewId,
  currentSentiment,
  onChange,
  isUpdating = false,
}: ReviewActionsProps) {
  const [isOpen, setIsOpen] = useState(false);

  const handleSelect = (sentiment: Sentiment) => {
    if (sentiment === currentSentiment) {
      // Already selected, do nothing
      setIsOpen(false);
      return;
    }

    onChange(reviewId, sentiment);
    setIsOpen(false);
  };

  return (
    <div className="relative">
      {/* Trigger Button */}
      <button
        onClick={() => !isUpdating && setIsOpen(!isOpen)}
        disabled={isUpdating}
        className={`flex items-center justify-center rounded-lg p-2 transition-colors ${
          isUpdating
            ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
            : 'hover:bg-gray-100 text-gray-600 hover:text-gray-900'
        }`}
        title="Zmień sentyment"
      >
        {isUpdating ? (
          <svg className="h-5 w-5 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
        ) : (
          <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
          </svg>
        )}
      </button>

      {/* Dropdown Menu */}
      {isOpen && !isUpdating && (
        <>
          {/* Backdrop */}
          <div
            className="fixed inset-0 z-10"
            onClick={() => setIsOpen(false)}
          />

          {/* Menu */}
          <div className="absolute right-0 bottom-full mb-2 z-20 w-48 rounded-lg border bg-white shadow-lg">
            <div className="p-2">
              <div className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase">
                Zmień sentyment
              </div>
              {sentimentOptions.map((option) => (
                <button
                  key={option.value}
                  onClick={() => handleSelect(option.value)}
                  disabled={option.value === currentSentiment}
                  className={`flex w-full items-center gap-3 rounded-md px-3 py-2 text-left text-sm transition-colors ${
                    option.value === currentSentiment
                      ? 'bg-gray-50 cursor-default'
                      : 'hover:bg-gray-50'
                  }`}
                >
                  <span className="text-lg">{option.icon}</span>
                  <span className={`flex-1 font-medium ${option.color}`}>
                    {option.label}
                  </span>
                  {option.value === currentSentiment && (
                    <svg className="h-4 w-4 text-gray-600" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                    </svg>
                  )}
                </button>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
