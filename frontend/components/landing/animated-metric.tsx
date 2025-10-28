'use client';

import { useScrollAnimation } from '@/hooks/use-scroll-animation';
import { useCountUp } from '@/hooks/use-count-up';
import type { AnimatedMetricProps } from '@/lib/types/landing';
import { cn } from '@/lib/utils';

/**
 * AnimatedMetric - Component displaying animated counting metric
 * Animation triggers when element is visible in viewport
 */
export function AnimatedMetric({
  value,
  label,
  suffix = '',
  animationDuration = 2000,
  delay = 0
}: AnimatedMetricProps) {
  const { ref, isInView } = useScrollAnimation({ threshold: 0.2 });

  // Delay animation start for staggered effect
  const shouldStart = isInView && delay === 0;

  // Handle delayed animations
  const [delayedStart, setDelayedStart] = React.useState(false);

  React.useEffect(() => {
    if (isInView && delay > 0) {
      const timer = setTimeout(() => {
        setDelayedStart(true);
      }, delay);

      return () => clearTimeout(timer);
    }
  }, [isInView, delay]);

  const { currentValue } = useCountUp({
    end: value,
    duration: animationDuration,
    start: delay > 0 ? delayedStart : shouldStart
  });

  return (
    <div
      ref={ref as React.RefObject<HTMLDivElement>}
      className={cn(
        'flex flex-col items-center gap-2 transition-opacity duration-700',
        isInView ? 'opacity-100' : 'opacity-0'
      )}
    >
      <div className="flex items-baseline gap-1">
        <span className="text-4xl font-bold tracking-tight text-primary md:text-5xl">
          {currentValue}
        </span>
        {suffix && (
          <span className="text-3xl font-bold text-primary md:text-4xl">
            {suffix}
          </span>
        )}
      </div>
      <p className="text-center text-sm text-muted-foreground md:text-base">
        {label}
      </p>
    </div>
  );
}

// Add React import for useEffect/useState
import * as React from 'react';