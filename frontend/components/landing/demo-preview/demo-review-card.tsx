'use client';

import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import type { DemoReviewCardProps } from '@/lib/types/landing';
import { formatReviewDate, getSentimentColor } from '@/lib/utils/demo-helpers';
import { Star, Facebook, Globe } from 'lucide-react';
import { cn } from '@/lib/utils';

// Source icon mapping
const SOURCE_ICONS = {
  google: Globe,
  facebook: Facebook,
  trustpilot: Star
} as const;

// Source display names
const SOURCE_NAMES = {
  google: 'Google',
  facebook: 'Facebook',
  trustpilot: 'Trustpilot'
} as const;

/**
 * DemoReviewCard - Single review card in demo preview
 * Shows source icon, date, rating stars, sentiment badge, content, and author
 */
export function DemoReviewCard({ review }: DemoReviewCardProps) {
  const SourceIcon = SOURCE_ICONS[review.source];

  // Generate star rating array
  const stars = Array.from({ length: 5 }, (_, i) => i + 1);

  return (
    <Card className="overflow-hidden transition-all duration-200 hover:shadow-md">
      <CardContent className="p-4">
        {/* Header: source icon, name, date, and rating */}
        <div className="mb-3 flex items-start justify-between gap-3">
          <div className="flex items-center gap-2">
            <div className="rounded-full bg-muted p-1.5">
              <SourceIcon className="h-4 w-4 text-muted-foreground" />
            </div>
            <div className="flex flex-col">
              <span className="text-sm font-medium text-foreground">
                {SOURCE_NAMES[review.source]}
              </span>
              <span className="text-xs text-muted-foreground">
                {formatReviewDate(review.date)}
              </span>
            </div>
          </div>

          {/* Star rating */}
          <div className="flex items-center gap-0.5">
            {stars.map((star) => (
              <Star
                key={star}
                className={cn(
                  'h-4 w-4',
                  star <= review.rating
                    ? 'fill-yellow-400 text-yellow-400'
                    : 'text-gray-300'
                )}
              />
            ))}
          </div>
        </div>

        {/* Sentiment badge */}
        <div className="mb-3">
          <Badge
            className={cn(
              'capitalize',
              getSentimentColor(review.sentiment)
            )}
          >
            {review.sentiment}
          </Badge>
        </div>

        {/* Review content */}
        <p className="mb-3 text-sm leading-relaxed text-foreground">
          {review.content}
        </p>

        {/* Author */}
        <p className="text-xs font-medium text-muted-foreground">
          {review.author}
        </p>
      </CardContent>
    </Card>
  );
}