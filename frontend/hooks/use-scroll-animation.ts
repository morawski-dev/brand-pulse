'use client';

import { useEffect, useRef, useState } from 'react';
import type { UseScrollAnimationReturn } from '@/lib/types/landing';

/**
 * Custom hook for scroll-triggered animations using Intersection Observer
 * @param options - Configuration options for the observer
 * @param options.threshold - Percentage of element visibility to trigger (0-1)
 * @returns Object with ref to attach to element and isInView state
 */
export function useScrollAnimation(options?: {
  threshold?: number;
}): UseScrollAnimationReturn {
  const ref = useRef<HTMLElement>(null);
  const [isInView, setIsInView] = useState(false);

  useEffect(() => {
    // Check if IntersectionObserver is supported
    if (typeof IntersectionObserver === 'undefined') {
      // Fallback: always show animations if not supported
      setIsInView(true);
      return;
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        // Only set to true, never back to false (animation triggers once)
        if (entry.isIntersecting) {
          setIsInView(true);
        }
      },
      {
        threshold: options?.threshold || 0.1,
        rootMargin: '0px'
      }
    );

    const currentRef = ref.current;

    if (currentRef) {
      observer.observe(currentRef);
    }

    return () => {
      if (currentRef) {
        observer.unobserve(currentRef);
      }
      observer.disconnect();
    };
  }, [options?.threshold]);

  return { ref, isInView };
}