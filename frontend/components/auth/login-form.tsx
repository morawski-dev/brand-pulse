/**
 * LoginForm - Main login form component
 *
 * Features:
 * - Email and password inputs with validation
 * - Remember me checkbox
 * - Password visibility toggle
 * - Error handling and display
 * - Loading states
 * - Links to forgot-password and register
 *
 * Uses:
 * - useLoginForm hook for state management
 * - shadcn/ui components for UI
 */

'use client';

import React, { useRef, useEffect } from 'react';
import Link from 'next/link';
import { Eye, EyeOff, Loader2, AlertCircle, Info } from 'lucide-react';
import { useLoginForm } from '@/lib/hooks/useLoginForm';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Alert, AlertDescription } from '@/components/ui/alert';

// ========================================
// MAIN COMPONENT
// ========================================

export function LoginForm() {
  const {
    formData,
    errors,
    isLoading,
    showPassword,
    handleEmailChange,
    handlePasswordChange,
    handleRememberMeChange,
    togglePasswordVisibility,
    handleSubmit,
    validateEmail,
    isFormValid,
  } = useLoginForm();

  // Auto-focus email input on mount
  const emailInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    emailInputRef.current?.focus();
  }, []);

  // Check if demo mode is enabled
  const isDemoMode = process.env.NEXT_PUBLIC_DEMO_MODE === 'true';

  return (
    <div className="space-y-6">
      {/* Demo Mode Alert */}
      {isDemoMode && (
        <Alert>
          <Info className="h-4 w-4" />
          <AlertDescription>
            <strong>Tryb demo:</strong> Email: demo@brandpulse.io, Hasło: Demo123!
          </AlertDescription>
        </Alert>
      )}

      {/* General Error Alert */}
      {errors.general && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{errors.general}</AlertDescription>
        </Alert>
      )}

      {/* Login Form */}
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        {/* Email Input */}
        <div className="space-y-2">
          <Label htmlFor="email">
            Email <span className="text-red-500">*</span>
          </Label>
          <Input
            ref={emailInputRef}
            id="email"
            type="email"
            placeholder="twoj.email@example.com"
            value={formData.email}
            onChange={e => handleEmailChange(e.target.value)}
            onBlur={e => {
              const error = validateEmail(e.target.value);
              if (error && formData.email) {
                // Only show error if field has been touched and has value
              }
            }}
            disabled={isLoading}
            className={errors.email ? 'border-red-500 focus-visible:ring-red-500' : ''}
            aria-invalid={!!errors.email}
            aria-describedby={errors.email ? 'email-error' : undefined}
            autoComplete="email"
          />
          {errors.email && (
            <p id="email-error" className="flex items-center gap-1 text-sm text-red-500">
              <AlertCircle className="h-3 w-3" />
              {errors.email}
            </p>
          )}
        </div>

        {/* Password Input */}
        <div className="space-y-2">
          <Label htmlFor="password">
            Hasło <span className="text-red-500">*</span>
          </Label>
          <div className="relative">
            <Input
              id="password"
              type={showPassword ? 'text' : 'password'}
              placeholder="••••••••"
              value={formData.password}
              onChange={e => handlePasswordChange(e.target.value)}
              disabled={isLoading}
              className={errors.password ? 'border-red-500 pr-10 focus-visible:ring-red-500' : 'pr-10'}
              aria-invalid={!!errors.password}
              aria-describedby={errors.password ? 'password-error' : undefined}
              autoComplete="current-password"
            />
            <button
              type="button"
              onClick={togglePasswordVisibility}
              disabled={isLoading}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 disabled:opacity-50"
              aria-label={showPassword ? 'Ukryj hasło' : 'Pokaż hasło'}
            >
              {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>
          {errors.password && (
            <p id="password-error" className="flex items-center gap-1 text-sm text-red-500">
              <AlertCircle className="h-3 w-3" />
              {errors.password}
            </p>
          )}
        </div>

        {/* Remember Me & Forgot Password Row */}
        <div className="flex items-center justify-between">
          {/* Remember Me Checkbox */}
          <div className="flex items-center space-x-2">
            <Checkbox
              id="rememberMe"
              checked={formData.rememberMe}
              onCheckedChange={handleRememberMeChange}
              disabled={isLoading}
            />
            <Label
              htmlFor="rememberMe"
              className="text-sm font-normal leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
            >
              Zapamiętaj mnie
            </Label>
          </div>

          {/* Forgot Password Link */}
          <Link
            href="/forgot-password"
            className="text-sm font-medium text-blue-600 hover:underline"
            tabIndex={isLoading ? -1 : 0}
          >
            Zapomniałeś hasła?
          </Link>
        </div>

        {/* Submit Button */}
        <Button
          type="submit"
          className="w-full"
          disabled={isLoading || !isFormValid}
          size="lg"
        >
          {isLoading ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" />
              Logowanie...
            </>
          ) : (
            'Zaloguj'
          )}
        </Button>

        {/* Register Link */}
        <div className="text-center text-sm text-gray-600">
          Nie masz konta?{' '}
          <Link
            href="/register"
            className="font-medium text-blue-600 hover:underline"
            tabIndex={isLoading ? -1 : 0}
          >
            Zarejestruj się
          </Link>
        </div>
      </form>
    </div>
  );
}
