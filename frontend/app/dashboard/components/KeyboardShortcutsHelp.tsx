/**
 * KeyboardShortcutsHelp - Displays available keyboard shortcuts
 *
 * Shows a small help tooltip or card with available shortcuts
 * Can be triggered by pressing '?' key
 */

'use client';

import React, { useState, useEffect } from 'react';
import { Card } from '@/components/ui/card';

// ========================================
// COMPONENT
// ========================================

export function KeyboardShortcutsHelp() {
  const [isOpen, setIsOpen] = useState(false);

  // ========================================
  // KEYBOARD EVENT HANDLER
  // ========================================

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // Toggle help with '?' key
      if (event.key === '?' && !isInputElement(event.target)) {
        event.preventDefault();
        setIsOpen((prev) => !prev);
      }

      // Close with Escape
      if (event.key === 'Escape' && isOpen) {
        event.preventDefault();
        setIsOpen(false);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen]);

  // ========================================
  // RENDER
  // ========================================

  return (
    <>
      {/* Trigger Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="fixed bottom-4 right-4 z-40 flex h-10 w-10 items-center justify-center rounded-full bg-gray-800 text-white shadow-lg hover:bg-gray-700 transition-colors"
        title="Skróty klawiszowe (naciśnij ?)"
      >
        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
      </button>

      {/* Help Modal */}
      {isOpen && (
        <>
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-black/50 z-50"
            onClick={() => setIsOpen(false)}
          />

          {/* Modal Card */}
          <Card className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-full max-w-md p-6 shadow-2xl">
            {/* Header */}
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">
                ⌨️ Skróty klawiszowe
              </h3>
              <button
                onClick={() => setIsOpen(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            </div>

            {/* Shortcuts List */}
            <div className="space-y-3">
              <ShortcutRow keys={['J', '↓']} description="Następna opinia" />
              <ShortcutRow keys={['K', '↑']} description="Poprzednia opinia" />
              <ShortcutRow keys={['Esc']} description="Wyczyść zaznaczenie" />
              <ShortcutRow keys={['?']} description="Pokaż/ukryj skróty" />
            </div>

            {/* Footer */}
            <div className="mt-6 pt-4 border-t text-xs text-gray-500 text-center">
              Skróty nie działają podczas pisania w polach tekstowych
            </div>
          </Card>
        </>
      )}
    </>
  );
}

// ========================================
// SHORTCUT ROW COMPONENT
// ========================================

function ShortcutRow({ keys, description }: { keys: string[]; description: string }) {
  return (
    <div className="flex items-center justify-between">
      <div className="flex gap-2">
        {keys.map((key, index) => (
          <React.Fragment key={key}>
            <kbd className="px-2 py-1 text-xs font-semibold text-gray-800 bg-gray-100 border border-gray-300 rounded">
              {key}
            </kbd>
            {index < keys.length - 1 && (
              <span className="text-gray-400 text-sm">lub</span>
            )}
          </React.Fragment>
        ))}
      </div>
      <span className="text-sm text-gray-600">{description}</span>
    </div>
  );
}

// ========================================
// HELPER FUNCTIONS
// ========================================

function isInputElement(target: EventTarget | null): boolean {
  if (!target) return false;
  const element = target as HTMLElement;
  return ['INPUT', 'TEXTAREA', 'SELECT'].includes(element.tagName);
}
