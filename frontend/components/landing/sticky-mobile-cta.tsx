'use client';

import { Button } from '@/components/ui/button';
import { useStickyPosition } from '@/hooks/use-sticky-position';
import type { StickyMobileCTAProps } from '@/lib/types/landing';
import { ArrowRight } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { cn } from '@/lib/utils';

/**
 * StickyMobileCTA - Mobile-only sticky CTA bar at bottom of screen
 * Shows after scrolling past threshold, hidden on desktop
 */
export function StickyMobileCTA({ threshold = 300 }: StickyMobileCTAProps) {
  const { isVisible } = useStickyPosition({ threshold });
  const router = useRouter();

  const handleStartFree = () => {
    router.push('/register');
  };

  return (
    <div
      className={cn(
        'fixed bottom-0 left-0 right-0 z-50 md:hidden',
        'border-t border-border bg-background/95 backdrop-blur-sm shadow-lg',
        'transition-transform duration-300',
        isVisible ? 'translate-y-0' : 'translate-y-full'
      )}
    >
      <div className="container mx-auto px-4 py-3">
        <Button
          size="lg"
          onClick={handleStartFree}
          className="w-full group"
        >
          Start Free
          <ArrowRight className="ml-2 h-4 w-4 transition-transform group-hover:translate-x-1" />
        </Button>
      </div>
    </div>
  );
}