'use client';

import { Button } from '@/components/ui/button';
import { AnimatedMetric } from './animated-metric';
import type { HeroSectionProps } from '@/lib/types/landing';
import { ArrowRight, PlayCircle } from 'lucide-react';
import { useRouter } from 'next/navigation';

/**
 * HeroSection - Main hero section with value proposition, CTAs, and animated metrics
 */
export function HeroSection({ metrics }: HeroSectionProps) {
  const router = useRouter();

  const handleStartFree = () => {
    router.push('/register');
  };

  const handleSeeDemo = () => {
    const demoSection = document.getElementById('demo-section');
    if (demoSection) {
      demoSection.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <section className="relative w-full overflow-hidden bg-gradient-to-b from-background to-muted/20 py-20 md:py-32">
      <div className="container mx-auto px-4 md:px-6">
        {/* Hero Content */}
        <div className="mx-auto max-w-4xl text-center">
          {/* Main Heading */}
          <h1 className="mb-6 text-4xl font-bold tracking-tight text-foreground md:text-5xl lg:text-6xl">
            Monitor Customer Reviews
            <br />
            <span className="bg-gradient-to-r from-primary to-primary/60 bg-clip-text text-transparent">
              All in One Place
            </span>
          </h1>

          {/* Description */}
          <p className="mx-auto mb-8 max-w-2xl text-lg leading-relaxed text-muted-foreground md:text-xl">
            BrandPulse aggregates reviews from Google, Facebook, and Trustpilot.
            Get AI-powered sentiment analysis and actionable insights to grow your business.
          </p>

          {/* CTA Buttons */}
          <div className="mb-16 flex flex-col gap-4 sm:flex-row sm:justify-center">
            <Button
              size="lg"
              onClick={handleStartFree}
              className="group"
            >
              Start Free
              <ArrowRight className="ml-2 h-4 w-4 transition-transform group-hover:translate-x-1" />
            </Button>
            <Button
              size="lg"
              variant="outline"
              onClick={handleSeeDemo}
              className="group"
            >
              <PlayCircle className="mr-2 h-4 w-4" />
              See Demo
            </Button>
          </div>

          {/* Animated Metrics */}
          <div className="grid grid-cols-1 gap-8 md:grid-cols-3 md:gap-12">
            {metrics.map((metric, index) => (
              <AnimatedMetric
                key={index}
                value={metric.value}
                label={metric.label}
                suffix={metric.suffix}
                animationDuration={metric.animationDuration}
                delay={index * 200} // Staggered animation
              />
            ))}
          </div>
        </div>
      </div>

      {/* Background decoration */}
      <div className="absolute inset-0 -z-10 overflow-hidden">
        <div className="absolute left-1/2 top-0 h-[500px] w-[500px] -translate-x-1/2 rounded-full bg-primary/5 blur-3xl" />
      </div>
    </section>
  );
}