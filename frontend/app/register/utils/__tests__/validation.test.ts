/**
 * Unit tests for validation functions
 */

import {
  validateEmail,
  validatePassword,
  validateConfirmPassword,
  validateConsents,
  calculatePasswordStrength,
  getPasswordRequirements,
} from '../validation';

// ========================================
// EMAIL VALIDATION TESTS
// ========================================

describe('validateEmail', () => {
  it('returns error for empty email', () => {
    expect(validateEmail('')).toBe('Email jest wymagany');
    expect(validateEmail('   ')).toBe('Email jest wymagany');
  });

  it('returns error for invalid email format', () => {
    expect(validateEmail('invalid')).toBe('Nieprawidłowy format email');
    expect(validateEmail('invalid@')).toBe('Nieprawidłowy format email');
    expect(validateEmail('@example.com')).toBe('Nieprawidłowy format email');
    expect(validateEmail('test@')).toBe('Nieprawidłowy format email');
  });

  it('returns undefined for valid email', () => {
    expect(validateEmail('test@example.com')).toBeUndefined();
    expect(validateEmail('user.name@domain.co.uk')).toBeUndefined();
    expect(validateEmail('test+label@example.com')).toBeUndefined();
  });
});

// ========================================
// PASSWORD VALIDATION TESTS
// ========================================

describe('validatePassword', () => {
  it('returns error for password shorter than 8 characters', () => {
    expect(validatePassword('Short1!')).toBe('Hasło musi mieć minimum 8 znaków');
  });

  it('returns error for password without uppercase letter', () => {
    expect(validatePassword('lowercase1!')).toBe('Hasło musi zawierać wielką literę');
  });

  it('returns error for password without lowercase letter', () => {
    expect(validatePassword('UPPERCASE1!')).toBe('Hasło musi zawierać małą literę');
  });

  it('returns error for password without digit', () => {
    expect(validatePassword('NoDigits!')).toBe('Hasło musi zawierać cyfrę');
  });

  it('returns error for password without special character', () => {
    expect(validatePassword('NoSpecial1')).toBe('Hasło musi zawierać znak specjalny');
  });

  it('returns undefined for valid password', () => {
    expect(validatePassword('ValidPass1!')).toBeUndefined();
    expect(validatePassword('Str0ng@Password')).toBeUndefined();
    expect(validatePassword('Test123#')).toBeUndefined();
  });
});

// ========================================
// CONFIRM PASSWORD VALIDATION TESTS
// ========================================

describe('validateConfirmPassword', () => {
  it('returns error for empty confirmation', () => {
    expect(validateConfirmPassword('password', '')).toBe(
      'Potwierdzenie hasła jest wymagane'
    );
  });

  it('returns error when passwords do not match', () => {
    expect(validateConfirmPassword('Password1!', 'Different1!')).toBe(
      'Hasła nie są zgodne'
    );
  });

  it('returns undefined when passwords match', () => {
    expect(validateConfirmPassword('Password1!', 'Password1!')).toBeUndefined();
  });
});

// ========================================
// CONSENTS VALIDATION TESTS
// ========================================

describe('validateConsents', () => {
  it('returns error when terms not accepted', () => {
    expect(
      validateConsents({ termsAccepted: false, privacyAccepted: true })
    ).toBe('Musisz zaakceptować regulamin i politykę prywatności');
  });

  it('returns error when privacy not accepted', () => {
    expect(
      validateConsents({ termsAccepted: true, privacyAccepted: false })
    ).toBe('Musisz zaakceptować regulamin i politykę prywatności');
  });

  it('returns error when both not accepted', () => {
    expect(
      validateConsents({ termsAccepted: false, privacyAccepted: false })
    ).toBe('Musisz zaakceptować regulamin i politykę prywatności');
  });

  it('returns undefined when both accepted', () => {
    expect(
      validateConsents({ termsAccepted: true, privacyAccepted: true })
    ).toBeUndefined();
  });
});

// ========================================
// PASSWORD STRENGTH TESTS
// ========================================

describe('calculatePasswordStrength', () => {
  it('returns weak strength for short simple password', () => {
    const result = calculatePasswordStrength('abc');
    expect(result.strength).toBe('weak');
    expect(result.score).toBeLessThanOrEqual(2);
    expect(result.feedback).toBe('Słabe');
  });

  it('returns fair strength for password with 3 criteria', () => {
    const result = calculatePasswordStrength('Password1');
    expect(result.strength).toBe('fair');
    expect(result.score).toBe(3);
    expect(result.feedback).toBe('Średnie');
  });

  it('returns good strength for password with 4 criteria', () => {
    const result = calculatePasswordStrength('Password1!');
    expect(result.strength).toBe('good');
    expect(result.score).toBe(4);
    expect(result.feedback).toBe('Dobre');
  });

  it('returns strong strength for password with all criteria', () => {
    const result = calculatePasswordStrength('StrongPassword123!');
    expect(result.strength).toBe('strong');
    expect(result.score).toBe(5);
    expect(result.feedback).toBe('Silne');
  });
});

// ========================================
// PASSWORD REQUIREMENTS TESTS
// ========================================

describe('getPasswordRequirements', () => {
  it('returns all requirements as not met for empty password', () => {
    const requirements = getPasswordRequirements('');
    expect(requirements).toHaveLength(5);
    expect(requirements.every((req) => !req.isMet)).toBe(true);
  });

  it('marks length requirement as met for 8+ character password', () => {
    const requirements = getPasswordRequirements('12345678');
    const lengthReq = requirements.find((req) => req.id === 'length');
    expect(lengthReq?.isMet).toBe(true);
  });

  it('marks all requirements as met for valid password', () => {
    const requirements = getPasswordRequirements('ValidPass123!');
    expect(requirements.every((req) => req.isMet)).toBe(true);
  });

  it('correctly identifies missing requirements', () => {
    const requirements = getPasswordRequirements('lowercase123');
    const uppercaseReq = requirements.find((req) => req.id === 'uppercase');
    const specialReq = requirements.find((req) => req.id === 'special');

    expect(uppercaseReq?.isMet).toBe(false);
    expect(specialReq?.isMet).toBe(false);
  });
});
