'use client';

import type { DemoStatsProps } from '@/lib/types/landing';
import { Card, CardContent } from '@/components/ui/card';
import { Star } from 'lucide-react';
import { cn } from '@/lib/utils';

/**
 * DemoStats - Panel with aggregated statistics
 * Shows average rating, total reviews, and sentiment distribution
 */
export function DemoStats({ stats }: DemoStatsProps) {
  const { averageRating, totalReviews, sentimentDistribution } = stats;

  // Generate stars for average rating (filled and half-filled)
  const fullStars = Math.floor(averageRating);
  const hasHalfStar = averageRating % 1 >= 0.5;
  const emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);

  return (
    <div className="grid gap-4 md:grid-cols-3">
      {/* Average Rating */}
      <Card>
        <CardContent className="flex flex-col items-center justify-center p-6">
          <div className="mb-2 flex items-baseline gap-1">
            <span className="text-4xl font-bold text-foreground">
              {averageRating.toFixed(1)}
            </span>
            <span className="text-lg text-muted-foreground">/5</span>
          </div>
          <div className="mb-2 flex items-center gap-0.5">
            {Array.from({ length: fullStars }).map((_, i) => (
              <Star key={`full-${i}`} className="h-4 w-4 fill-yellow-400 text-yellow-400" />
            ))}
            {hasHalfStar && (
              <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" style={{ clipPath: 'inset(0 50% 0 0)' }} />
            )}
            {Array.from({ length: emptyStars }).map((_, i) => (
              <Star key={`empty-${i}`} className="h-4 w-4 text-gray-300" />
            ))}
          </div>
          <p className="text-sm text-muted-foreground">Average Rating</p>
        </CardContent>
      </Card>

      {/* Total Reviews */}
      <Card>
        <CardContent className="flex flex-col items-center justify-center p-6">
          <div className="mb-2 text-4xl font-bold text-foreground">
            {totalReviews}
          </div>
          <p className="text-sm text-muted-foreground">Total Reviews</p>
        </CardContent>
      </Card>

      {/* Sentiment Distribution */}
      <Card>
        <CardContent className="flex flex-col gap-3 p-6">
          <p className="text-center text-sm font-medium text-muted-foreground">
            Sentiment Distribution
          </p>
          <div className="space-y-2">
            {/* Positive */}
            <div className="flex items-center gap-2">
              <span className="w-16 text-xs text-muted-foreground">Positive</span>
              <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
                <div
                  className="h-full bg-green-500 transition-all duration-500"
                  style={{ width: `${sentimentDistribution.positive}%` }}
                />
              </div>
              <span className="w-10 text-right text-xs font-medium text-foreground">
                {sentimentDistribution.positive}%
              </span>
            </div>

            {/* Neutral */}
            <div className="flex items-center gap-2">
              <span className="w-16 text-xs text-muted-foreground">Neutral</span>
              <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
                <div
                  className="h-full bg-gray-400 transition-all duration-500"
                  style={{ width: `${sentimentDistribution.neutral}%` }}
                />
              </div>
              <span className="w-10 text-right text-xs font-medium text-foreground">
                {sentimentDistribution.neutral}%
              </span>
            </div>

            {/* Negative */}
            <div className="flex items-center gap-2">
              <span className="w-16 text-xs text-muted-foreground">Negative</span>
              <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
                <div
                  className="h-full bg-red-500 transition-all duration-500"
                  style={{ width: `${sentimentDistribution.negative}%` }}
                />
              </div>
              <span className="w-10 text-right text-xs font-medium text-foreground">
                {sentimentDistribution.negative}%
              </span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}