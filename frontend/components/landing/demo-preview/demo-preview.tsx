'use client';

import { useState, useMemo } from 'react';
import type { DemoPreviewProps, DemoFilters } from '@/lib/types/landing';
import { filterReviews, calculateDemoStats } from '@/lib/utils/demo-helpers';
import { DemoControls } from './demo-controls';
import { DemoStats } from './demo-stats';
import { DemoReviewList } from './demo-review-list';

/**
 * DemoPreview - Interactive demo section with mock reviews
 * Features filtering by source, sentiment, and rating with real-time stats
 */
export function DemoPreview({ mockReviews }: DemoPreviewProps) {
  // Filter state
  const [activeFilters, setActiveFilters] = useState<DemoFilters>({
    source: 'all',
    sentiment: 'all',
    rating: 'all'
  });

  // Filter change handler
  const handleFilterChange = (
    filterType: keyof DemoFilters,
    value: string
  ) => {
    setActiveFilters((prev) => ({
      ...prev,
      [filterType]: value
    }));
  };

  // Computed filtered reviews
  const filteredReviews = useMemo(() => {
    return filterReviews(mockReviews, activeFilters);
  }, [mockReviews, activeFilters]);

  // Computed statistics
  const demoStats = useMemo(() => {
    return calculateDemoStats(filteredReviews);
  }, [filteredReviews]);

  return (
    <section
      id="demo-section"
      className="w-full py-16 md:py-24 bg-muted/30"
    >
      <div className="container mx-auto px-4 md:px-6">
        {/* Section Header */}
        <div className="mb-12 text-center">
          <h2 className="mb-4 text-3xl font-bold tracking-tight text-foreground md:text-4xl">
            See How It Works
          </h2>
          <p className="mx-auto max-w-2xl text-lg text-muted-foreground">
            Try our interactive demo. Filter reviews by source, sentiment, and rating to see how BrandPulse aggregates and analyzes your customer feedback.
          </p>
        </div>

        {/* Demo Interface */}
        <div className="mx-auto max-w-6xl space-y-8">
          {/* Filters */}
          <div className="rounded-lg border bg-card p-6 shadow-sm">
            <h3 className="mb-4 text-lg font-semibold text-foreground">
              Filter Reviews
            </h3>
            <DemoControls
              activeFilters={activeFilters}
              onFilterChange={handleFilterChange}
            />
          </div>

          {/* Statistics */}
          <DemoStats stats={demoStats} />

          {/* Reviews List */}
          <div>
            <h3 className="mb-4 text-lg font-semibold text-foreground">
              Reviews ({filteredReviews.length})
            </h3>
            <DemoReviewList reviews={filteredReviews} maxVisible={5} />
          </div>
        </div>
      </div>
    </section>
  );
}