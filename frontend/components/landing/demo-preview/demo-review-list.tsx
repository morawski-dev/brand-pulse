'use client';

import type { DemoReviewListProps } from '@/lib/types/landing';
import { DemoReviewCard } from './demo-review-card';
import { Button } from '@/components/ui/button';
import { RotateCcw } from 'lucide-react';

/**
 * DemoReviewList - List of reviews with empty state
 * Shows up to maxVisible reviews from the filtered list
 */
export function DemoReviewList({ reviews, maxVisible = 5 }: DemoReviewListProps) {
  const visibleReviews = reviews.slice(0, maxVisible);

  // Empty state
  if (reviews.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-border bg-muted/30 py-12 px-6 text-center">
        <div className="mb-4 rounded-full bg-muted p-3">
          <RotateCcw className="h-6 w-6 text-muted-foreground" />
        </div>
        <h3 className="mb-2 text-lg font-semibold text-foreground">
          No reviews found
        </h3>
        <p className="mb-4 max-w-sm text-sm text-muted-foreground">
          No reviews match your current filter criteria. Try adjusting the filters above.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Review cards with staggered animation */}
      {visibleReviews.map((review, index) => (
        <div
          key={review.id}
          className="animate-in fade-in slide-in-from-bottom-2"
          style={{
            animationDelay: `${index * 50}ms`,
            animationFillMode: 'backwards'
          }}
        >
          <DemoReviewCard review={review} />
        </div>
      ))}

      {/* Show count if there are more reviews */}
      {reviews.length > maxVisible && (
        <div className="pt-2 text-center">
          <p className="text-sm text-muted-foreground">
            Showing {maxVisible} of {reviews.length} reviews
          </p>
        </div>
      )}
    </div>
  );
}