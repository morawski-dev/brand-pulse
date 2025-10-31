/**
 * PasswordStrengthMeter - Visual password strength indicator
 *
 * Displays:
 * - Progress bar with color based on strength
 * - Text label (Słabe/Średnie/Dobre/Silne)
 *
 * Colors:
 * - weak: red (#ef4444)
 * - fair: orange (#f97316)
 * - good: yellow (#eab308)
 * - strong: green (#22c55e)
 */

'use client';

import { Progress } from '@/components/ui/progress';
import { PasswordStrengthResult } from '@/lib/types/auth';
import { cn } from '@/lib/utils';

// ========================================
// TYPES
// ========================================

interface PasswordStrengthMeterProps {
  strength: PasswordStrengthResult;
}

// ========================================
// COMPONENT
// ========================================

export function PasswordStrengthMeter({ strength }: PasswordStrengthMeterProps) {
  // Calculate progress percentage (0-100)
  const progress = (strength.score / 5) * 100;

  // Map strength to color classes
  const colorClasses = {
    weak: 'bg-red-500',
    fair: 'bg-orange-500',
    good: 'bg-yellow-500',
    strong: 'bg-green-500',
  };

  // Map strength to text color classes
  const textColorClasses = {
    weak: 'text-red-600',
    fair: 'text-orange-600',
    good: 'text-yellow-600',
    strong: 'text-green-600',
  };

  return (
    <div className="space-y-2">
      {/* Progress bar */}
      <div className="relative h-2 w-full overflow-hidden rounded-full bg-gray-200">
        <div
          className={cn(
            'h-full transition-all duration-300 ease-in-out',
            colorClasses[strength.strength]
          )}
          style={{ width: `${progress}%` }}
        />
      </div>

      {/* Strength label */}
      <p
        className={cn(
          'text-sm font-medium transition-colors duration-300',
          textColorClasses[strength.strength]
        )}
      >
        Siła hasła: {strength.feedback}
      </p>
    </div>
  );
}
