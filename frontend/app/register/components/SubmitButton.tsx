/**
 * SubmitButton - Form submission button with loading state
 *
 * Features:
 * - Loading spinner during submission
 * - Disabled state when form is invalid or submitting
 * - Text changes: "Zarejestruj się" → "Rejestrowanie..."
 * - Full width on mobile, auto width on desktop
 *
 * States:
 * - Default: enabled, clickable
 * - Loading: disabled, spinner visible
 * - Invalid: disabled, no spinner
 */

'use client';

import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';

// ========================================
// TYPES
// ========================================

interface SubmitButtonProps {
  isLoading: boolean;
  isDisabled: boolean;
  onClick?: () => void; // Optional, form onSubmit handles this
}

// ========================================
// COMPONENT
// ========================================

export function SubmitButton({
  isLoading,
  isDisabled,
  onClick,
}: SubmitButtonProps) {
  return (
    <Button
      type="submit"
      size="lg"
      className="w-full"
      disabled={isDisabled || isLoading}
      onClick={onClick}
    >
      {/* Loading spinner */}
      {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}

      {/* Button text */}
      {isLoading ? 'Rejestrowanie...' : 'Zarejestruj się'}
    </Button>
  );
}
