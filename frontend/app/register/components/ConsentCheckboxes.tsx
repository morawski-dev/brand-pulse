/**
 * ConsentCheckboxes - GDPR consent checkboxes
 *
 * Displays two required checkboxes:
 * - Terms of Service acceptance
 * - Privacy Policy acceptance
 *
 * Features:
 * - Links to terms and privacy pages (open in new tab)
 * - Error message display
 * - GDPR compliant (not pre-checked)
 * - Accessible labels and ARIA attributes
 *
 * Validation:
 * - Both checkboxes must be checked before form submission
 */

'use client';

import Link from 'next/link';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';

// ========================================
// TYPES
// ========================================

interface ConsentCheckboxesProps {
  termsAccepted: boolean;
  privacyAccepted: boolean;
  error?: string;
  onChange: (field: 'terms' | 'privacy', value: boolean) => void;
  disabled?: boolean;
}

// ========================================
// COMPONENT
// ========================================

export function ConsentCheckboxes({
  termsAccepted,
  privacyAccepted,
  error,
  onChange,
  disabled = false,
}: ConsentCheckboxesProps) {
  return (
    <div className="space-y-4">
      {/* Terms of Service checkbox */}
      <div className="flex items-start gap-3">
        <Checkbox
          id="terms"
          checked={termsAccepted}
          onCheckedChange={(checked) =>
            onChange('terms', checked === true)
          }
          disabled={disabled}
          className="mt-1"
          aria-invalid={!!error}
        />
        <Label
          htmlFor="terms"
          className="text-sm leading-relaxed text-gray-700"
        >
          Akceptuję{' '}
          <Link
            href="/terms"
            target="_blank"
            rel="noopener noreferrer"
            className="font-medium text-blue-600 underline hover:text-blue-700"
          >
            regulamin serwisu
          </Link>
        </Label>
      </div>

      {/* Privacy Policy checkbox */}
      <div className="flex items-start gap-3">
        <Checkbox
          id="privacy"
          checked={privacyAccepted}
          onCheckedChange={(checked) =>
            onChange('privacy', checked === true)
          }
          disabled={disabled}
          className="mt-1"
          aria-invalid={!!error}
        />
        <Label
          htmlFor="privacy"
          className="text-sm leading-relaxed text-gray-700"
        >
          Akceptuję{' '}
          <Link
            href="/privacy"
            target="_blank"
            rel="noopener noreferrer"
            className="font-medium text-blue-600 underline hover:text-blue-700"
          >
            politykę prywatności
          </Link>
        </Label>
      </div>

      {/* Error message */}
      {error && (
        <p className="text-sm font-medium text-red-600" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
