'use client';

import { useEffect, useState } from 'react';
import type { UseStickyPositionReturn } from '@/lib/types/landing';

/**
 * Custom hook for tracking scroll position and showing/hiding sticky elements
 * @param params - Configuration options
 * @param params.threshold - Scroll position in pixels to trigger visibility (default: 300)
 * @returns Object with visibility state and current scroll position
 */
export function useStickyPosition({
  threshold = 300
}: { threshold?: number } = {}): UseStickyPositionReturn {
  const [scrollY, setScrollY] = useState(0);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    // Check if window is available (client-side only)
    if (typeof window === 'undefined') {
      return;
    }

    const handleScroll = () => {
      const currentScrollY = window.scrollY;
      setScrollY(currentScrollY);
      setIsVisible(currentScrollY > threshold);
    };

    // Set initial state
    handleScroll();

    // Add scroll event listener with passive flag for better performance
    window.addEventListener('scroll', handleScroll, { passive: true });

    // Cleanup
    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, [threshold]);

  return { isVisible, scrollY };
}