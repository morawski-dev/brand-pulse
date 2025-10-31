/**
 * Validation functions for registration form
 * All validation messages are in Polish as per MVP requirements
 */

import {
  ConsentState,
  PasswordStrength,
  PasswordStrengthResult,
} from '@/lib/types/auth';
import { ValidationResult } from '@/lib/types/api';

// ========================================
// EMAIL VALIDATION
// ========================================

/**
 * Validates email field
 *
 * Rules:
 * 1. Required: Cannot be empty
 * 2. Format: Must match email regex pattern
 *
 * @param email - Email address to validate
 * @returns Error message or undefined if valid
 */
export function validateEmail(email: string): ValidationResult {
  // Rule 1: Required
  if (!email || email.trim() === '') {
    return 'Email jest wymagany';
  }

  // Rule 2: Format validation
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    return 'Nieprawidłowy format email';
  }

  return undefined;
}

// ========================================
// PASSWORD VALIDATION
// ========================================

/**
 * Validates password field
 *
 * Rules (must match backend validation in RegisterRequest.java):
 * 1. Minimum 8 characters
 * 2. At least one uppercase letter (A-Z)
 * 3. At least one lowercase letter (a-z)
 * 4. At least one digit (0-9)
 * 5. At least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)
 *
 * @param password - Password to validate
 * @returns Error message or undefined if valid
 */
export function validatePassword(password: string): ValidationResult {
  // Rule 1: Minimum 8 characters
  if (password.length < 8) {
    return 'Hasło musi mieć minimum 8 znaków';
  }

  // Rule 2: Uppercase letter
  if (!/[A-Z]/.test(password)) {
    return 'Hasło musi zawierać wielką literę';
  }

  // Rule 3: Lowercase letter
  if (!/[a-z]/.test(password)) {
    return 'Hasło musi zawierać małą literę';
  }

  // Rule 4: Digit
  if (!/[0-9]/.test(password)) {
    return 'Hasło musi zawierać cyfrę';
  }

  // Rule 5: Special character
  if (!/[!@#$%^&*()_+\-=\[\]{}|;:,.<>?]/.test(password)) {
    return 'Hasło musi zawierać znak specjalny';
  }

  return undefined;
}

// ========================================
// CONFIRM PASSWORD VALIDATION
// ========================================

/**
 * Validates password confirmation field
 *
 * Rules:
 * 1. Required: Cannot be empty
 * 2. Match: Must be identical to password
 *
 * @param password - Original password
 * @param confirmPassword - Password confirmation
 * @returns Error message or undefined if valid
 */
export function validateConfirmPassword(
  password: string,
  confirmPassword: string
): ValidationResult {
  // Rule 1: Required
  if (!confirmPassword || confirmPassword.trim() === '') {
    return 'Potwierdzenie hasła jest wymagane';
  }

  // Rule 2: Match
  if (password !== confirmPassword) {
    return 'Hasła nie są zgodne';
  }

  return undefined;
}

// ========================================
// CONSENT VALIDATION
// ========================================

/**
 * Validates GDPR consent checkboxes
 *
 * Rules:
 * 1. Terms: Must be accepted
 * 2. Privacy: Must be accepted
 *
 * @param consents - Consent state
 * @returns Error message or undefined if valid
 */
export function validateConsents(consents: ConsentState): ValidationResult {
  // Both must be accepted for GDPR compliance
  if (!consents.termsAccepted || !consents.privacyAccepted) {
    return 'Musisz zaakceptować regulamin i politykę prywatności';
  }

  return undefined;
}

// ========================================
// PASSWORD STRENGTH CALCULATION
// ========================================

/**
 * Calculates password strength based on multiple criteria
 *
 * Scoring system (0-5 points):
 * - 1 point: Length >= 8 characters
 * - 1 point: Length >= 12 characters
 * - 1 point: Contains both uppercase and lowercase
 * - 1 point: Contains digit
 * - 1 point: Contains special character
 *
 * Strength mapping:
 * - 0-2 points: weak (czerwony)
 * - 3 points: fair (pomarańczowy)
 * - 4 points: good (żółty)
 * - 5 points: strong (zielony)
 *
 * @param password - Password to evaluate
 * @returns Password strength result with score and feedback
 */
export function calculatePasswordStrength(
  password: string
): PasswordStrengthResult {
  let score = 0;

  // Criterion 1: Minimum length
  if (password.length >= 8) score++;

  // Criterion 2: Good length
  if (password.length >= 12) score++;

  // Criterion 3: Mixed case
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score++;

  // Criterion 4: Contains digit
  if (/[0-9]/.test(password)) score++;

  // Criterion 5: Contains special character
  if (/[!@#$%^&*()_+\-=\[\]{}|;:,.<>?]/.test(password)) score++;

  // Map score to strength level
  let strength: PasswordStrength;
  if (score <= 2) {
    strength = 'weak';
  } else if (score === 3) {
    strength = 'fair';
  } else if (score === 4) {
    strength = 'good';
  } else {
    strength = 'strong';
  }

  return {
    strength,
    score,
    feedback: getStrengthLabel(strength),
  };
}

/**
 * Maps password strength to Polish display label
 *
 * @param strength - Password strength level
 * @returns Polish label for display
 */
export function getStrengthLabel(strength: PasswordStrength): string {
  const labels: Record<PasswordStrength, string> = {
    weak: 'Słabe',
    fair: 'Średnie',
    good: 'Dobre',
    strong: 'Silne',
  };
  return labels[strength];
}

// ========================================
// PASSWORD REQUIREMENTS LIST
// ========================================

/**
 * Generates list of password requirements with their current status
 * Used for real-time visual feedback in UI
 *
 * @param password - Current password value
 * @returns Array of requirements with isMet flags
 */
export function getPasswordRequirements(password: string) {
  return [
    {
      id: 'length',
      label: 'Minimum 8 znaków',
      isMet: password.length >= 8,
    },
    {
      id: 'uppercase',
      label: 'Wielka litera (A-Z)',
      isMet: /[A-Z]/.test(password),
    },
    {
      id: 'lowercase',
      label: 'Mała litera (a-z)',
      isMet: /[a-z]/.test(password),
    },
    {
      id: 'number',
      label: 'Cyfra (0-9)',
      isMet: /[0-9]/.test(password),
    },
    {
      id: 'special',
      label: 'Znak specjalny (!@#$...)',
      isMet: /[!@#$%^&*()_+\-=\[\]{}|;:,.<>?]/.test(password),
    },
  ];
}