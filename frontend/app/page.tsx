import { HeroSection } from '@/components/landing/hero-section';
import { FeaturesSection } from '@/components/landing/features-section';
import { BenefitsSection } from '@/components/landing/benefits-section';
import { DemoPreview } from '@/components/landing/demo-preview/demo-preview';
import { PricingSection } from '@/components/landing/pricing-section';
import { CTASection } from '@/components/landing/cta-section';
import { StickyMobileCTA } from '@/components/landing/sticky-mobile-cta';
import {
  HERO_METRICS,
  FEATURES,
  BENEFITS,
  MOCK_REVIEWS,
  PRICING_PLANS
} from '@/lib/constants/landing-content';

/**
 * Landing Page - Main marketing page for BrandPulse
 * Features: Hero, Features, Benefits, Interactive Demo, Pricing, Final CTA
 */
export default function LandingPage() {
  return (
    <main className="flex min-h-screen flex-col">
      {/* Hero Section with animated metrics */}
      <HeroSection metrics={HERO_METRICS} />

      {/* Key Features Section */}
      <FeaturesSection features={FEATURES} />

      {/* Business Benefits Section */}
      <BenefitsSection benefits={BENEFITS} />

      {/* Interactive Demo Preview */}
      <DemoPreview mockReviews={MOCK_REVIEWS} />

      {/* Pricing Plans Section */}
      <PricingSection plans={PRICING_PLANS} />

      {/* Final Call-to-Action Section */}
      <CTASection />

      {/* Sticky Mobile CTA (visible only on mobile after scroll) */}
      <StickyMobileCTA threshold={300} />
    </main>
  );
}