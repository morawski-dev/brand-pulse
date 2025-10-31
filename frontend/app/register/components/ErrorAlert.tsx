/**
 * ErrorAlert - API error display component
 *
 * Displays error messages from API responses with:
 * - Error icon
 * - Error message
 * - Dismiss button (X)
 *
 * Handles different error types:
 * - Validation errors (400)
 * - Conflict errors (409 - email exists)
 * - Network errors
 * - Server errors (500)
 */

'use client';

import { AlertCircle, X } from 'lucide-react';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { ApiError } from '@/lib/types/auth';

// ========================================
// TYPES
// ========================================

interface ErrorAlertProps {
  error: ApiError | null;
  onDismiss: () => void;
}

// ========================================
// COMPONENT
// ========================================

export function ErrorAlert({ error, onDismiss }: ErrorAlertProps) {
  if (!error) return null;

  return (
    <Alert variant="destructive" className="relative" role="alert" aria-live="assertive">
      {/* Error icon */}
      <AlertCircle className="h-4 w-4" />

      {/* Error message */}
      <AlertDescription className="pr-8">
        {error.message}
      </AlertDescription>

      {/* Dismiss button */}
      <Button
        variant="ghost"
        size="icon"
        className="absolute right-2 top-2 h-6 w-6"
        onClick={onDismiss}
        aria-label="Zamknij powiadomienie"
      >
        <X className="h-4 w-4" />
      </Button>
    </Alert>
  );
}
