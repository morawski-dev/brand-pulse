/**
 * Onboarding helper utilities
 * Functions for managing onboarding state persistence and validation
 */

import { OnboardingState, INITIAL_ONBOARDING_STATE } from '@/lib/types/onboarding';

// ========================================
// CONSTANTS
// ========================================

/**
 * LocalStorage key for onboarding progress
 */
const ONBOARDING_STORAGE_KEY = 'brandpulse_onboarding_progress';

/**
 * Storage version for migration handling
 */
const STORAGE_VERSION = 1;

// ========================================
// STORAGE INTERFACE
// ========================================

interface StoredOnboardingData {
  version: number;
  timestamp: number;
  state: OnboardingState;
}

// ========================================
// SAVE FUNCTIONS
// ========================================

/**
 * Save onboarding progress to localStorage
 * Allows user to resume if they close the tab
 *
 * @param state - Current onboarding state
 *
 * @example
 * ```tsx
 * // In useOnboarding hook
 * useEffect(() => {
 *   if (state.currentStep > OnboardingStep.BRAND_SETUP) {
 *     saveOnboardingProgress(state);
 *   }
 * }, [state]);
 * ```
 */
export function saveOnboardingProgress(state: OnboardingState): void {
  // Don't save if user hasn't started
  if (
    state.currentStep === INITIAL_ONBOARDING_STATE.currentStep &&
    !state.brand.name
  ) {
    return;
  }

  try {
    const data: StoredOnboardingData = {
      version: STORAGE_VERSION,
      timestamp: Date.now(),
      state,
    };

    localStorage.setItem(ONBOARDING_STORAGE_KEY, JSON.stringify(data));
  } catch (error) {
    console.error('Failed to save onboarding progress:', error);
    // Don't throw - localStorage can fail (quota, privacy mode, etc.)
  }
}

// ========================================
// LOAD FUNCTIONS
// ========================================

/**
 * Load onboarding progress from localStorage
 * Returns null if no saved progress or if expired
 *
 * @param maxAgeHours - Maximum age of saved progress in hours (default: 24)
 * @returns Saved onboarding state or null
 *
 * @example
 * ```tsx
 * // In useOnboarding hook initialization
 * const savedState = loadOnboardingProgress();
 * const [state, setState] = useState(savedState || INITIAL_ONBOARDING_STATE);
 * ```
 */
export function loadOnboardingProgress(
  maxAgeHours: number = 24
): OnboardingState | null {
  try {
    const saved = localStorage.getItem(ONBOARDING_STORAGE_KEY);
    if (!saved) {
      return null;
    }

    const data: StoredOnboardingData = JSON.parse(saved);

    // Check version
    if (data.version !== STORAGE_VERSION) {
      console.warn('Onboarding storage version mismatch, clearing...');
      clearOnboardingProgress();
      return null;
    }

    // Check age
    const ageMs = Date.now() - data.timestamp;
    const maxAgeMs = maxAgeHours * 60 * 60 * 1000;
    if (ageMs > maxAgeMs) {
      console.log('Onboarding progress expired, clearing...');
      clearOnboardingProgress();
      return null;
    }

    return data.state;
  } catch (error) {
    console.error('Failed to load onboarding progress:', error);
    clearOnboardingProgress();
    return null;
  }
}

// ========================================
// CLEAR FUNCTIONS
// ========================================

/**
 * Clear onboarding progress from localStorage
 * Should be called after successful completion or manual clear
 *
 * @example
 * ```tsx
 * // After successful onboarding completion
 * clearOnboardingProgress();
 * router.push('/dashboard');
 * ```
 */
export function clearOnboardingProgress(): void {
  try {
    localStorage.removeItem(ONBOARDING_STORAGE_KEY);
  } catch (error) {
    console.error('Failed to clear onboarding progress:', error);
  }
}

// ========================================
// VALIDATION FUNCTIONS
// ========================================

/**
 * Check if onboarding state is valid for resuming
 * Validates that required data exists for current step
 *
 * @param state - Onboarding state to validate
 * @returns True if state is valid for resuming
 */
export function isOnboardingStateValid(state: OnboardingState): boolean {
  try {
    // Check brand name if past first step
    if (state.currentStep > 1 && !state.brand.name) {
      return false;
    }

    // Check source type if past second step
    if (state.currentStep > 2 && !state.reviewSource.sourceType) {
      return false;
    }

    // Check URL and external ID if past third step
    if (state.currentStep > 3) {
      if (
        !state.reviewSource.profileUrl ||
        !state.reviewSource.externalProfileId
      ) {
        return false;
      }
    }

    return true;
  } catch (error) {
    console.error('Error validating onboarding state:', error);
    return false;
  }
}

// ========================================
// ANALYTICS HELPERS
// ========================================

/**
 * Calculate time spent on current step
 * Useful for analytics and optimization
 *
 * @param startTime - Step start timestamp
 * @returns Time spent in seconds
 */
export function calculateTimeSpent(startTime: number): number {
  return Math.floor((Date.now() - startTime) / 1000);
}

/**
 * Calculate total onboarding time
 *
 * @param startTime - Onboarding start timestamp
 * @returns Total time in seconds
 */
export function calculateTotalTime(startTime: number): number {
  return Math.floor((Date.now() - startTime) / 1000);
}

/**
 * Format time duration for display
 *
 * @param seconds - Duration in seconds
 * @returns Formatted string (e.g., "2m 30s")
 */
export function formatDuration(seconds: number): string {
  if (seconds < 60) {
    return `${seconds}s`;
  }

  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;

  if (remainingSeconds === 0) {
    return `${minutes}m`;
  }

  return `${minutes}m ${remainingSeconds}s`;
}

// ========================================
// STEP VALIDATION
// ========================================

/**
 * Check if user can proceed to next step
 * Validates current step data before allowing navigation
 *
 * @param state - Current onboarding state
 * @returns True if can proceed, false otherwise
 */
export function canProceedToNextStep(state: OnboardingState): boolean {
  switch (state.currentStep) {
    case 1: // BRAND_SETUP
      return state.brand.name.trim().length > 0;

    case 2: // SOURCE_TYPE
      return state.reviewSource.sourceType !== null;

    case 3: // SOURCE_URL
      return (
        state.reviewSource.profileUrl.trim().length > 0 &&
        state.reviewSource.externalProfileId.trim().length > 0
      );

    case 4: // CONFIRMATION
      return true; // Always can proceed from confirmation

    case 5: // IMPORT_PROGRESS
      return false; // Cannot manually proceed from import

    default:
      return false;
  }
}

// ========================================
// ERROR HELPERS
// ========================================

/**
 * Get user-friendly error message for common onboarding errors
 *
 * @param error - Error object or message
 * @returns Polish error message
 */
export function getOnboardingErrorMessage(error: unknown): string {
  if (typeof error === 'string') {
    return error;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.';
}
