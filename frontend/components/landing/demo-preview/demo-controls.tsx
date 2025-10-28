'use client';

import type { DemoControlsProps, DemoFilters } from '@/lib/types/landing';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

// Filter options configuration
const SOURCE_OPTIONS: Array<{ value: DemoFilters['source']; label: string }> = [
  { value: 'all', label: 'All Sources' },
  { value: 'google', label: 'Google' },
  { value: 'facebook', label: 'Facebook' },
  { value: 'trustpilot', label: 'Trustpilot' }
];

const SENTIMENT_OPTIONS: Array<{ value: DemoFilters['sentiment']; label: string }> = [
  { value: 'all', label: 'All Sentiments' },
  { value: 'positive', label: 'Positive' },
  { value: 'neutral', label: 'Neutral' },
  { value: 'negative', label: 'Negative' }
];

const RATING_OPTIONS: Array<{ value: DemoFilters['rating']; label: string }> = [
  { value: 'all', label: 'All Ratings' },
  { value: '4-5', label: '4-5 ★' },
  { value: '3', label: '3 ★' },
  { value: '1-2', label: '1-2 ★' }
];

/**
 * DemoControls - Filter controls for demo preview
 * Shows buttons for source, sentiment, and rating filters
 */
export function DemoControls({ activeFilters, onFilterChange }: DemoControlsProps) {
  return (
    <div className="space-y-4">
      {/* Source Filter */}
      <div>
        <label className="mb-2 block text-sm font-medium text-foreground">
          Source
        </label>
        <div className="flex flex-wrap gap-2">
          {SOURCE_OPTIONS.map((option) => (
            <Button
              key={option.value}
              variant={activeFilters.source === option.value ? 'default' : 'outline'}
              size="sm"
              onClick={() => onFilterChange('source', option.value)}
              className={cn(
                'transition-all',
                activeFilters.source === option.value && 'shadow-sm'
              )}
            >
              {option.label}
            </Button>
          ))}
        </div>
      </div>

      {/* Sentiment Filter */}
      <div>
        <label className="mb-2 block text-sm font-medium text-foreground">
          Sentiment
        </label>
        <div className="flex flex-wrap gap-2">
          {SENTIMENT_OPTIONS.map((option) => (
            <Button
              key={option.value}
              variant={activeFilters.sentiment === option.value ? 'default' : 'outline'}
              size="sm"
              onClick={() => onFilterChange('sentiment', option.value)}
              className={cn(
                'transition-all',
                activeFilters.sentiment === option.value && 'shadow-sm'
              )}
            >
              {option.label}
            </Button>
          ))}
        </div>
      </div>

      {/* Rating Filter */}
      <div>
        <label className="mb-2 block text-sm font-medium text-foreground">
          Rating
        </label>
        <div className="flex flex-wrap gap-2">
          {RATING_OPTIONS.map((option) => (
            <Button
              key={option.value}
              variant={activeFilters.rating === option.value ? 'default' : 'outline'}
              size="sm"
              onClick={() => onFilterChange('rating', option.value)}
              className={cn(
                'transition-all',
                activeFilters.rating === option.value && 'shadow-sm'
              )}
            >
              {option.label}
            </Button>
          ))}
        </div>
      </div>
    </div>
  );
}