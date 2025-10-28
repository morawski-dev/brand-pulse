'use client';

import { useEffect, useState } from 'react';
import type { UseCountUpReturn } from '@/lib/types/landing';

/**
 * Custom hook for animating a number counter
 * @param params - Configuration for the count-up animation
 * @param params.end - Target value to count up to
 * @param params.duration - Animation duration in milliseconds (default: 2000)
 * @param params.start - Whether to start the animation (trigger)
 * @returns Object with current value and animation status
 */
export function useCountUp({
  end,
  duration = 2000,
  start = false
}: {
  end: number;
  duration?: number;
  start: boolean;
}): UseCountUpReturn {
  const [currentValue, setCurrentValue] = useState(0);
  const [isAnimating, setIsAnimating] = useState(false);

  useEffect(() => {
    // Don't start if trigger is false
    if (!start) {
      return;
    }

    // Check for reduced motion preference
    const prefersReducedMotion =
      typeof window !== 'undefined' &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    if (prefersReducedMotion) {
      // Skip animation, show final value immediately
      setCurrentValue(end);
      return;
    }

    setIsAnimating(true);
    const startTime = Date.now();
    let animationFrameId: number;

    const animate = () => {
      const now = Date.now();
      const progress = Math.min((now - startTime) / duration, 1);

      // Easing function: easeOutCubic for smooth deceleration
      const easeOutCubic = 1 - Math.pow(1 - progress, 3);

      // Update current value
      const newValue = Math.floor(easeOutCubic * end);
      setCurrentValue(newValue);

      if (progress < 1) {
        // Continue animation
        animationFrameId = requestAnimationFrame(animate);
      } else {
        // Animation complete
        setCurrentValue(end); // Ensure final value is exact
        setIsAnimating(false);
      }
    };

    animationFrameId = requestAnimationFrame(animate);

    // Cleanup
    return () => {
      if (animationFrameId) {
        cancelAnimationFrame(animationFrameId);
      }
    };
  }, [start, end, duration]);

  return { currentValue, isAnimating };
}