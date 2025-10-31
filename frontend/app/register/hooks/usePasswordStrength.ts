/**
 * usePasswordStrength - Real-time password strength calculator hook
 *
 * Calculates password strength and requirement status as user types
 * Used in PasswordInput component for visual feedback
 *
 * Usage:
 * ```tsx
 * const { strength, requirements } = usePasswordStrength(password);
 * ```
 */

'use client';

import { useMemo } from 'react';
import { UsePasswordStrengthReturn } from '@/lib/types/api';
import {
  calculatePasswordStrength,
  getPasswordRequirements,
} from '../utils/validation';

// ========================================
// HOOK IMPLEMENTATION
// ========================================

/**
 * Calculates password strength and requirements in real-time
 *
 * Performance:
 * - Uses useMemo to prevent unnecessary recalculations
 * - Only recalculates when password changes
 *
 * @param password - Current password value from form
 * @returns Strength result and requirements list
 */
export function usePasswordStrength(password: string): UsePasswordStrengthReturn {
  /**
   * Calculate password strength (0-5 score)
   * Memoized to prevent recalculation on every render
   */
  const strength = useMemo(() => {
    return calculatePasswordStrength(password);
  }, [password]);

  /**
   * Generate requirements list with isMet flags
   * Memoized to prevent array recreation on every render
   */
  const requirements = useMemo(() => {
    return getPasswordRequirements(password);
  }, [password]);

  return {
    strength,
    requirements,
  };
}