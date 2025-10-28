'use client';

import type { BenefitsSectionProps } from '@/lib/types/landing';
import { BenefitCard } from './benefit-card';

/**
 * BenefitsSection - Section presenting business benefits
 * Focus on the "why" rather than the "what"
 */
export function BenefitsSection({ benefits }: BenefitsSectionProps) {
  return (
    <section id="benefits-section" className="w-full bg-muted/30 py-16 md:py-24">
      <div className="container mx-auto px-4 md:px-6">
        {/* Section Header */}
        <div className="mb-12 text-center">
          <h2 className="mb-4 text-3xl font-bold tracking-tight text-foreground md:text-4xl">
            Benefits for Your Business
          </h2>
          <p className="mx-auto max-w-2xl text-lg text-muted-foreground">
            Discover how BrandPulse helps businesses like yours save time, understand customers, and grow.
          </p>
        </div>

        {/* Benefits Grid */}
        <div className="mx-auto grid max-w-5xl gap-8 md:grid-cols-2 lg:grid-cols-3">
          {benefits.map((benefit) => (
            <BenefitCard key={benefit.id} benefit={benefit} />
          ))}
        </div>
      </div>
    </section>
  );
}