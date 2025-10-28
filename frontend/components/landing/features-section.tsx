'use client';

import type { FeaturesSectionProps } from '@/lib/types/landing';
import { FeatureCard } from './feature-card';

/**
 * FeaturesSection - Section showcasing key product features
 * Features are displayed in a responsive grid with staggered animations
 */
export function FeaturesSection({ features }: FeaturesSectionProps) {
  return (
    <section id="features-section" className="w-full py-16 md:py-24">
      <div className="container mx-auto px-4 md:px-6">
        {/* Section Header */}
        <div className="mb-12 text-center">
          <h2 className="mb-4 text-3xl font-bold tracking-tight text-foreground md:text-4xl">
            Key Features
          </h2>
          <p className="mx-auto max-w-2xl text-lg text-muted-foreground">
            Everything you need to monitor, analyze, and act on customer feedback across multiple platforms.
          </p>
        </div>

        {/* Features Grid */}
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {features.map((feature, index) => (
            <FeatureCard
              key={feature.id}
              feature={feature}
              index={index}
            />
          ))}
        </div>
      </div>
    </section>
  );
}