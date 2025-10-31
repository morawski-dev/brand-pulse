/**
 * EmailInput - Email address input field with validation
 *
 * Features:
 * - Email format validation
 * - Error message display
 * - Autocomplete support
 * - Accessible labels and ARIA attributes
 *
 * Validation:
 * - Required field
 * - Valid email format (regex)
 * - Uniqueness checked by API on submit
 */

'use client';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { cn } from '@/lib/utils';

// ========================================
// TYPES
// ========================================

interface EmailInputProps {
  value: string;
  error?: string;
  onChange: (value: string) => void;
  onBlur?: () => void;
  disabled?: boolean;
}

// ========================================
// COMPONENT
// ========================================

export function EmailInput({
  value,
  error,
  onChange,
  onBlur,
  disabled = false,
}: EmailInputProps) {
  return (
    <div className="space-y-2">
      {/* Label */}
      <Label htmlFor="email" className="text-sm font-medium text-gray-900">
        Email
      </Label>

      {/* Input */}
      <Input
        id="email"
        type="email"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onBlur={onBlur}
        disabled={disabled}
        autoComplete="email"
        placeholder="twoj@email.com"
        className={cn(
          'transition-colors duration-200',
          error && 'border-red-500 focus-visible:ring-red-500'
        )}
        aria-invalid={!!error}
        aria-describedby={error ? 'email-error' : undefined}
      />

      {/* Error message */}
      {error && (
        <p
          id="email-error"
          className="text-sm font-medium text-red-600"
          role="alert"
        >
          {error}
        </p>
      )}
    </div>
  );
}
