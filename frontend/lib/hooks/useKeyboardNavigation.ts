/**
 * useKeyboardNavigation Hook - Keyboard shortcuts for review list navigation
 *
 * Shortcuts:
 * - J (or ArrowDown): Move to next review
 * - K (or ArrowUp): Move to previous review
 * - Escape: Clear selection
 *
 * Features:
 * - Auto-scroll selected review into view
 * - Prevents navigation when typing in input/textarea
 * - Returns selectedIndex for UI highlighting
 *
 * Usage:
 * ```typescript
 * const { selectedIndex, clearSelection } = useKeyboardNavigation({
 *   itemCount: reviews.length,
 *   onSelect: (index) => console.log('Selected review:', index),
 * });
 * ```
 */

import { useState, useEffect, useCallback } from 'react';

// ========================================
// TYPES
// ========================================

interface UseKeyboardNavigationOptions {
  itemCount: number; // Total number of items in the list
  onSelect?: (index: number) => void; // Callback when item is selected
  disabled?: boolean; // Disable keyboard navigation
}

interface UseKeyboardNavigationReturn {
  selectedIndex: number | null; // Currently selected item index
  clearSelection: () => void; // Manually clear selection
  selectNext: () => void; // Programmatically select next item
  selectPrevious: () => void; // Programmatically select previous item
}

// ========================================
// CONSTANTS
// ========================================

// Elements where keyboard shortcuts should be disabled
const IGNORED_ELEMENTS = ['INPUT', 'TEXTAREA', 'SELECT'];

// ========================================
// HOOK IMPLEMENTATION
// ========================================

export function useKeyboardNavigation(
  options: UseKeyboardNavigationOptions
): UseKeyboardNavigationReturn {
  const { itemCount, onSelect, disabled = false } = options;

  const [selectedIndex, setSelectedIndex] = useState<number | null>(null);

  // ========================================
  // CLEAR SELECTION
  // ========================================

  const clearSelection = useCallback(() => {
    setSelectedIndex(null);
  }, []);

  // ========================================
  // SELECT NEXT ITEM
  // ========================================

  const selectNext = useCallback(() => {
    if (disabled || itemCount === 0) return;

    setSelectedIndex((prev) => {
      // First selection
      if (prev === null) {
        onSelect?.(0);
        return 0;
      }

      // Already at last item
      if (prev >= itemCount - 1) {
        return prev;
      }

      // Move to next
      const next = prev + 1;
      onSelect?.(next);
      return next;
    });
  }, [itemCount, disabled, onSelect]);

  // ========================================
  // SELECT PREVIOUS ITEM
  // ========================================

  const selectPrevious = useCallback(() => {
    if (disabled || itemCount === 0) return;

    setSelectedIndex((prev) => {
      // No selection yet - start from last
      if (prev === null) {
        const last = itemCount - 1;
        onSelect?.(last);
        return last;
      }

      // Already at first item
      if (prev <= 0) {
        return prev;
      }

      // Move to previous
      const previous = prev - 1;
      onSelect?.(previous);
      return previous;
    });
  }, [itemCount, disabled, onSelect]);

  // ========================================
  // SCROLL SELECTED ITEM INTO VIEW
  // ========================================

  useEffect(() => {
    if (selectedIndex === null) return;

    // Find the selected review card by data attribute
    const selectedElement = document.querySelector(
      `[data-review-index="${selectedIndex}"]`
    );

    if (selectedElement) {
      selectedElement.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      });
    }
  }, [selectedIndex]);

  // ========================================
  // RESET SELECTION WHEN ITEM COUNT CHANGES
  // ========================================

  useEffect(() => {
    // Clear selection if it's out of bounds
    if (selectedIndex !== null && selectedIndex >= itemCount) {
      setSelectedIndex(null);
    }
  }, [itemCount, selectedIndex]);

  // ========================================
  // KEYBOARD EVENT HANDLER
  // ========================================

  useEffect(() => {
    if (disabled) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      // Ignore if user is typing in an input field
      const target = event.target as HTMLElement;
      if (IGNORED_ELEMENTS.includes(target.tagName)) {
        return;
      }

      // J or ArrowDown - Next item
      if (event.key === 'j' || event.key === 'J' || event.key === 'ArrowDown') {
        event.preventDefault();
        selectNext();
      }

      // K or ArrowUp - Previous item
      if (event.key === 'k' || event.key === 'K' || event.key === 'ArrowUp') {
        event.preventDefault();
        selectPrevious();
      }

      // Escape - Clear selection
      if (event.key === 'Escape') {
        event.preventDefault();
        clearSelection();
      }
    };

    window.addEventListener('keydown', handleKeyDown);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [disabled, selectNext, selectPrevious, clearSelection]);

  // ========================================
  // RETURN
  // ========================================

  return {
    selectedIndex,
    clearSelection,
    selectNext,
    selectPrevious,
  };
}
