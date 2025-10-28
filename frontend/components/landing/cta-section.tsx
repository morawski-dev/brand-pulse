'use client';

import { Button } from '@/components/ui/button';
import { ArrowRight } from 'lucide-react';
import { useRouter } from 'next/navigation';

/**
 * CTASection - Final call-to-action section before footer
 * Large, prominent CTA encouraging sign-up
 */
export function CTASection() {
  const router = useRouter();

  const handleStartFree = () => {
    router.push('/register');
  };

  return (
    <section className="relative w-full overflow-hidden bg-gradient-to-br from-primary to-primary/80 py-20 md:py-28">
      <div className="container relative mx-auto px-4 md:px-6">
        <div className="mx-auto max-w-3xl text-center">
          {/* Heading */}
          <h2 className="mb-6 text-3xl font-bold tracking-tight text-primary-foreground md:text-4xl lg:text-5xl">
            Ready to Take Control of Your Reviews?
          </h2>

          {/* Description */}
          <p className="mb-8 text-lg text-primary-foreground/90 md:text-xl">
            Join businesses already using BrandPulse to monitor and improve their online reputation.
            Start free today - no credit card required.
          </p>

          {/* CTA Button */}
          <Button
            size="lg"
            variant="secondary"
            onClick={handleStartFree}
            className="group shadow-lg"
          >
            Start Free Now
            <ArrowRight className="ml-2 h-4 w-4 transition-transform group-hover:translate-x-1" />
          </Button>

          {/* Additional info */}
          <p className="mt-6 text-sm text-primary-foreground/75">
            Free plan includes 1 review source • No credit card required • Cancel anytime
          </p>
        </div>
      </div>

      {/* Background decoration */}
      <div className="absolute inset-0 -z-10 overflow-hidden opacity-10">
        <div className="absolute right-0 top-1/2 h-96 w-96 -translate-y-1/2 rounded-full bg-white blur-3xl" />
        <div className="absolute left-0 top-1/2 h-96 w-96 -translate-y-1/2 rounded-full bg-white blur-3xl" />
      </div>
    </section>
  );
}