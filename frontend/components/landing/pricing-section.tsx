'use client';

import { useRouter } from 'next/navigation';
import type { PricingSectionProps, PricingPlan } from '@/lib/types/landing';
import { PricingCard } from './pricing-card';

/**
 * PricingSection - Section displaying pricing plans
 * Handles CTA actions for different plan types
 */
export function PricingSection({ plans }: PricingSectionProps) {
  const router = useRouter();

  const handleCTAClick = (planId: string, action: PricingPlan['ctaAction']) => {
    switch (action) {
      case 'register':
        router.push('/register');
        break;
      case 'contact':
        // Future: open contact modal or navigate to contact page
        console.log('Contact sales for plan:', planId);
        break;
      case 'coming-soon':
        // Show notification that plan is coming soon
        console.log('Plan coming soon:', planId);
        break;
      default:
        break;
    }
  };

  return (
    <section id="pricing-section" className="w-full py-16 md:py-24">
      <div className="container mx-auto px-4 md:px-6">
        {/* Section Header */}
        <div className="mb-12 text-center">
          <h2 className="mb-4 text-3xl font-bold tracking-tight text-foreground md:text-4xl">
            Simple, Transparent Pricing
          </h2>
          <p className="mx-auto max-w-2xl text-lg text-muted-foreground">
            Start free with one review source. Upgrade when you're ready to scale.
          </p>
        </div>

        {/* Pricing Cards Grid */}
        <div className="mx-auto grid max-w-5xl gap-8 md:grid-cols-2">
          {plans.map((plan) => (
            <PricingCard
              key={plan.id}
              plan={plan}
              onCTAClick={handleCTAClick}
            />
          ))}
        </div>

        {/* Additional Info */}
        <div className="mt-12 text-center">
          <p className="text-sm text-muted-foreground">
            All plans include AI sentiment analysis and daily automatic sync.
            <br />
            No credit card required for the free plan.
          </p>
        </div>
      </div>
    </section>
  );
}