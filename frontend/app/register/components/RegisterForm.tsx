/**
 * RegisterForm - Main registration form component
 *
 * Orchestrates entire registration flow:
 * - Email and password inputs with validation
 * - GDPR consent checkboxes
 * - Password strength indicator
 * - API integration and error handling
 * - Success modal and redirect
 *
 * Structure:
 * - Form header (title + subtitle)
 * - Email field
 * - Password field (with strength meter & requirements)
 * - Confirm password field
 * - Consent checkboxes
 * - Free plan info box
 * - Error alert (conditional)
 * - Submit button
 * - Success modal (conditional)
 */

'use client';

import Link from 'next/link';
import { useRegisterForm } from '../hooks/useRegisterForm';
import { EmailInput } from './EmailInput';
import { PasswordInput } from './PasswordInput';
import { ConsentCheckboxes } from './ConsentCheckboxes';
import { FreePlanInfoBox } from './FreePlanInfoBox';
import { ErrorAlert } from './ErrorAlert';
import { SubmitButton } from './SubmitButton';
import { SuccessModal } from './SuccessModal';

// ========================================
// COMPONENT
// ========================================

export function RegisterForm() {
  const {
    formData,
    errors,
    isSubmitting,
    isValid,
    apiError,
    showSuccessModal,
    handleFieldChange,
    handleBlur,
    handleSubmit,
    setShowSuccessModal,
  } = useRegisterForm();

  return (
    <>
      {/* Main Form */}
      <form onSubmit={handleSubmit} className="space-y-6" noValidate>
        {/* Form Header */}
        <div className="space-y-2 text-center">
          <h2 className="text-3xl font-bold tracking-tight text-gray-900">
            Utwórz konto
          </h2>
          <p className="text-sm text-gray-600">
            Zacznij monitorować opinie za darmo
          </p>
        </div>

        {/* Email Input */}
        <EmailInput
          value={formData.email}
          error={errors.email}
          onChange={(value) => handleFieldChange('email', value)}
          onBlur={() => handleBlur('email')}
          disabled={isSubmitting}
        />

        {/* Password Input (with strength meter and requirements) */}
        <PasswordInput
          id="password"
          label="Hasło"
          value={formData.password}
          error={errors.password}
          onChange={(value) => handleFieldChange('password', value)}
          onBlur={() => handleBlur('password')}
          disabled={isSubmitting}
          showStrengthMeter={true}
          showRequirements={true}
          autoComplete="new-password"
        />

        {/* Confirm Password Input (without strength meter) */}
        <PasswordInput
          id="confirmPassword"
          label="Powtórz hasło"
          value={formData.confirmPassword}
          error={errors.confirmPassword}
          onChange={(value) => handleFieldChange('confirmPassword', value)}
          onBlur={() => handleBlur('confirmPassword')}
          disabled={isSubmitting}
          showStrengthMeter={false}
          showRequirements={false}
          autoComplete="new-password"
        />

        {/* GDPR Consent Checkboxes */}
        <ConsentCheckboxes
          termsAccepted={formData.termsAccepted}
          privacyAccepted={formData.privacyAccepted}
          error={errors.consents}
          onChange={(field, value) => {
            handleFieldChange(
              field === 'terms' ? 'termsAccepted' : 'privacyAccepted',
              value
            );
          }}
          disabled={isSubmitting}
        />

        {/* Free Plan Info Box */}
        <FreePlanInfoBox />

        {/* API Error Alert (conditional) */}
        {apiError && (
          <ErrorAlert
            error={apiError}
            onDismiss={() => setShowSuccessModal(false)}
          />
        )}

        {/* Submit Button */}
        <SubmitButton isLoading={isSubmitting} isDisabled={!isValid} />

        {/* Login Link */}
        <div className="text-center">
          <p className="text-sm text-gray-600">
            Masz już konto?{' '}
            <Link
              href="/login"
              className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
            >
              Zaloguj się
            </Link>
          </p>
        </div>
      </form>

      {/* Success Modal (conditional) */}
      <SuccessModal
        isOpen={showSuccessModal}
        userData={null} // Will be populated from API response if needed
        onContinue={() => setShowSuccessModal(false)}
      />
    </>
  );
}
