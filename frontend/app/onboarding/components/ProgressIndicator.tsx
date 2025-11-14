/**
 * ProgressIndicator component
 * Visual progress indicator for the 5-step onboarding wizard
 *
 * Features:
 * - Horizontal stepper on desktop
 * - Vertical dots on mobile
 * - Highlights active step
 * - Shows completed steps with checkmark
 * - Responsive design
 */

'use client';

import { CheckCircle2 } from 'lucide-react';
import { OnboardingStep, STEP_CONFIGS } from '@/lib/types/onboarding';
import { cn } from '@/lib/utils';

// ========================================
// TYPES
// ========================================

export interface ProgressIndicatorProps {
  /**
   * Current active step (1-5)
   */
  currentStep: OnboardingStep;

  /**
   * Optional CSS class name
   */
  className?: string;
}

// ========================================
// COMPONENT
// ========================================

/**
 * Progress indicator for onboarding wizard
 *
 * Mobile: Shows vertical dots on the left
 * Desktop: Shows horizontal stepper with labels
 *
 * @example
 * ```tsx
 * <ProgressIndicator currentStep={OnboardingStep.SOURCE_TYPE} />
 * ```
 */
export function ProgressIndicator({
  currentStep,
  className,
}: ProgressIndicatorProps) {
  return (
    <>
      {/* Mobile: Vertical dots indicator */}
      <div className={cn('block md:hidden', className)}>
        <MobileProgress currentStep={currentStep} />
      </div>

      {/* Desktop: Horizontal stepper */}
      <div className={cn('hidden md:block', className)}>
        <DesktopProgress currentStep={currentStep} />
      </div>
    </>
  );
}

// ========================================
// MOBILE PROGRESS (Vertical Dots)
// ========================================

function MobileProgress({ currentStep }: { currentStep: OnboardingStep }) {
  return (
    <div className="flex items-center gap-2">
      {STEP_CONFIGS.map((step, index) => {
        const isActive = step.id === currentStep;
        const isCompleted = step.id < currentStep;

        return (
          <div key={step.id} className="flex items-center gap-2">
            {/* Dot */}
            <div
              className={cn(
                'h-2 w-2 rounded-full transition-all duration-200',
                {
                  'bg-blue-600 scale-125': isActive,
                  'bg-green-500': isCompleted,
                  'bg-gray-300': !isActive && !isCompleted,
                }
              )}
              aria-label={`Krok ${step.id}: ${step.label}`}
              aria-current={isActive ? 'step' : undefined}
            />

            {/* Connector line (except after last step) */}
            {index < STEP_CONFIGS.length - 1 && (
              <div
                className={cn(
                  'h-0.5 w-4 transition-colors duration-200',
                  isCompleted ? 'bg-green-500' : 'bg-gray-300'
                )}
              />
            )}
          </div>
        );
      })}
    </div>
  );
}

// ========================================
// DESKTOP PROGRESS (Horizontal Stepper)
// ========================================

function DesktopProgress({ currentStep }: { currentStep: OnboardingStep }) {
  return (
    <nav aria-label="Postęp onboardingu">
      <ol className="flex items-center justify-between">
        {STEP_CONFIGS.map((step, index) => {
          const isActive = step.id === currentStep;
          const isCompleted = step.id < currentStep;

          return (
            <li
              key={step.id}
              className={cn('flex flex-1 items-center', {
                'flex-1': index < STEP_CONFIGS.length - 1,
              })}
            >
              {/* Step content */}
              <div className="flex flex-col items-center gap-2">
                {/* Step circle */}
                <div
                  className={cn(
                    'flex h-10 w-10 items-center justify-center rounded-full border-2 transition-all duration-200',
                    {
                      'border-blue-600 bg-blue-600 text-white': isActive,
                      'border-green-500 bg-green-500 text-white': isCompleted,
                      'border-gray-300 bg-white text-gray-500':
                        !isActive && !isCompleted,
                    }
                  )}
                  aria-label={`Krok ${step.id}: ${step.label}`}
                  aria-current={isActive ? 'step' : undefined}
                >
                  {isCompleted ? (
                    <CheckCircle2 className="h-5 w-5" aria-hidden="true" />
                  ) : (
                    <span className="text-sm font-semibold">{step.id}</span>
                  )}
                </div>

                {/* Step label */}
                <div className="flex flex-col items-center text-center">
                  <span
                    className={cn('text-sm font-medium transition-colors', {
                      'text-blue-600': isActive,
                      'text-green-600': isCompleted,
                      'text-gray-500': !isActive && !isCompleted,
                    })}
                  >
                    {step.label}
                  </span>
                  <span
                    className={cn(
                      'text-xs transition-colors',
                      isActive ? 'text-gray-600' : 'text-gray-400'
                    )}
                  >
                    {step.description}
                  </span>
                </div>
              </div>

              {/* Connector line (except after last step) */}
              {index < STEP_CONFIGS.length - 1 && (
                <div
                  className={cn(
                    'mx-4 h-0.5 flex-1 transition-colors duration-200',
                    isCompleted ? 'bg-green-500' : 'bg-gray-300'
                  )}
                  aria-hidden="true"
                />
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
