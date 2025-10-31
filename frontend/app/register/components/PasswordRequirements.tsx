/**
 * PasswordRequirements - Password validation requirements checklist
 *
 * Displays real-time list of password requirements with visual indicators:
 * - ✓ Green checkmark when requirement is met
 * - ✗ Gray X when requirement is not met
 */

'use client';

import { Check, X } from 'lucide-react';
import { PasswordRequirement } from '@/lib/types/auth';
import { cn } from '@/lib/utils';

// ========================================
// TYPES
// ========================================

interface PasswordRequirementsProps {
  requirements: PasswordRequirement[];
}

// ========================================
// COMPONENT
// ========================================

export function PasswordRequirements({ requirements }: PasswordRequirementsProps) {
  return (
    <div className="space-y-2">
      <p className="text-sm font-medium text-gray-700">Wymagania hasła:</p>
      <ul className="space-y-1.5">
        {requirements.map((requirement) => (
          <li
            key={requirement.id}
            className="flex items-center gap-2 text-sm"
          >
            {/* Icon: checkmark or X */}
            {requirement.isMet ? (
              <Check className="h-4 w-4 flex-shrink-0 text-green-600" />
            ) : (
              <X className="h-4 w-4 flex-shrink-0 text-gray-400" />
            )}

            {/* Requirement text */}
            <span
              className={cn(
                'transition-colors duration-200',
                requirement.isMet ? 'text-green-700' : 'text-gray-600'
              )}
            >
              {requirement.label}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
