/**
 * PasswordInput - Password input field with strength meter and requirements
 *
 * Features:
 * - Toggle password visibility (eye icon)
 * - Password strength meter (optional, for main password field)
 * - Password requirements checklist (optional, for main password field)
 * - Error message display
 * - Accessible labels and ARIA attributes
 *
 * Validation:
 * - Min 8 characters
 * - Uppercase + lowercase + digit + special character
 */

'use client';

import { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { PasswordStrengthMeter } from './PasswordStrengthMeter';
import { PasswordRequirements } from './PasswordRequirements';
import { usePasswordStrength } from '../hooks/usePasswordStrength';

// ========================================
// TYPES
// ========================================

interface PasswordInputProps {
  value: string;
  error?: string;
  onChange: (value: string) => void;
  onBlur?: () => void;
  disabled?: boolean;
  showStrengthMeter?: boolean; // true for main password, false for confirm
  showRequirements?: boolean; // true for main password, false for confirm
  label?: string; // "Hasło" or "Powtórz hasło"
  autoComplete?: string; // "new-password" or "new-password"
  id?: string; // HTML id attribute
}

// ========================================
// COMPONENT
// ========================================

export function PasswordInput({
  value,
  error,
  onChange,
  onBlur,
  disabled = false,
  showStrengthMeter = false,
  showRequirements = false,
  label = 'Hasło',
  autoComplete = 'new-password',
  id = 'password',
}: PasswordInputProps) {
  const [showPassword, setShowPassword] = useState(false);
  const { strength, requirements } = usePasswordStrength(value);

  /**
   * Toggle password visibility
   */
  const toggleVisibility = () => {
    setShowPassword((prev) => !prev);
  };

  return (
    <div className="space-y-3">
      {/* Label */}
      <Label htmlFor={id} className="text-sm font-medium text-gray-900">
        {label}
      </Label>

      {/* Input wrapper with toggle button */}
      <div className="relative">
        <Input
          id={id}
          type={showPassword ? 'text' : 'password'}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onBlur={onBlur}
          disabled={disabled}
          autoComplete={autoComplete}
          placeholder="••••••••"
          className={cn(
            'pr-10 transition-colors duration-200',
            error && 'border-red-500 focus-visible:ring-red-500'
          )}
          aria-invalid={!!error}
          aria-describedby={
            error
              ? `${id}-error`
              : showRequirements
              ? `${id}-requirements`
              : undefined
          }
        />

        {/* Toggle visibility button */}
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="absolute right-0 top-0 h-full px-3 hover:bg-transparent"
          onClick={toggleVisibility}
          disabled={disabled}
          aria-label={showPassword ? 'Ukryj hasło' : 'Pokaż hasło'}
        >
          {showPassword ? (
            <EyeOff className="h-4 w-4 text-gray-500" />
          ) : (
            <Eye className="h-4 w-4 text-gray-500" />
          )}
        </Button>
      </div>

      {/* Password strength meter (only for main password field) */}
      {showStrengthMeter && value.length > 0 && (
        <PasswordStrengthMeter strength={strength} />
      )}

      {/* Password requirements checklist (only for main password field) */}
      {showRequirements && (
        <div id={`${id}-requirements`}>
          <PasswordRequirements requirements={requirements} />
        </div>
      )}

      {/* Error message */}
      {error && (
        <p
          id={`${id}-error`}
          className="text-sm font-medium text-red-600"
          role="alert"
        >
          {error}
        </p>
      )}
    </div>
  );
}
