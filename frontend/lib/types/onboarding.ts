/**
 * Onboarding types for the multi-step brand and review source setup flow
 * Matches US-003: Configuring the first source
 */

// ========================================
// ENUMS
// ========================================

/**
 * Onboarding step identifiers
 * Represents the 5-step wizard flow
 */
export enum OnboardingStep {
  BRAND_SETUP = 1,
  SOURCE_TYPE = 2,
  SOURCE_URL = 3,
  CONFIRMATION = 4,
  IMPORT_PROGRESS = 5,
}

/**
 * Source type enum (matching backend SourceType enum)
 */
export enum SourceType {
  GOOGLE = 'GOOGLE',
  FACEBOOK = 'FACEBOOK',
  TRUSTPILOT = 'TRUSTPILOT',
}

// ========================================
// STATE TYPES
// ========================================

/**
 * Brand creation data (step 1)
 */
export interface BrandData {
  name: string;
}

/**
 * Review source creation data (steps 2-3)
 */
export interface ReviewSourceData {
  sourceType: SourceType | null;
  profileUrl: string;
  externalProfileId: string;
}

/**
 * Import progress tracking (step 5)
 * Updated via polling during initial review import
 */
export interface ImportProgress {
  isImporting: boolean;
  progress: number; // 0-100
  status: string;
  reviewsImported: number;
  totalReviews: number;
}

/**
 * Complete onboarding state
 * Managed by useOnboarding hook
 */
export interface OnboardingState {
  currentStep: OnboardingStep;
  brand: BrandData;
  reviewSource: ReviewSourceData;
  isSubmitting: boolean;
  importProgress: ImportProgress;
  errors: Record<string, string>;
}

// ========================================
// STEP CONFIGURATION
// ========================================

/**
 * Step configuration for rendering
 */
export interface StepConfig {
  id: OnboardingStep;
  label: string;
  description: string;
  canSkip: boolean;
}

/**
 * Default step configurations
 */
export const STEP_CONFIGS: StepConfig[] = [
  {
    id: OnboardingStep.BRAND_SETUP,
    label: 'Marka',
    description: 'Podaj nazwę swojej marki',
    canSkip: false,
  },
  {
    id: OnboardingStep.SOURCE_TYPE,
    label: 'Platforma',
    description: 'Wybierz platformę z opiniami',
    canSkip: false,
  },
  {
    id: OnboardingStep.SOURCE_URL,
    label: 'Profil',
    description: 'Podaj link do profilu',
    canSkip: false,
  },
  {
    id: OnboardingStep.CONFIRMATION,
    label: 'Potwierdzenie',
    description: 'Sprawdź dane',
    canSkip: false,
  },
  {
    id: OnboardingStep.IMPORT_PROGRESS,
    label: 'Import',
    description: 'Importowanie opinii',
    canSkip: false,
  },
];

// ========================================
// VALIDATION TYPES
// ========================================

/**
 * Validation result for form fields
 */
export interface ValidationResult {
  isValid: boolean;
  error?: string;
  externalId?: string; // Extracted from URL
}

/**
 * Source validation error codes
 */
export enum SourceValidationError {
  INVALID_URL = 'INVALID_URL',
  DUPLICATE_SOURCE = 'DUPLICATE_SOURCE',
  LIMIT_REACHED = 'LIMIT_REACHED',
  UNSUPPORTED_PLATFORM = 'UNSUPPORTED_PLATFORM',
  EXTRACTION_FAILED = 'EXTRACTION_FAILED',
}

// ========================================
// ERROR HANDLING
// ========================================

/**
 * Onboarding error types
 */
export enum OnboardingErrorType {
  // Validation errors
  BRAND_NAME_REQUIRED = 'BRAND_NAME_REQUIRED',
  BRAND_NAME_TOO_LONG = 'BRAND_NAME_TOO_LONG',
  SOURCE_TYPE_REQUIRED = 'SOURCE_TYPE_REQUIRED',
  INVALID_URL = 'INVALID_URL',

  // API errors
  BRAND_ALREADY_EXISTS = 'BRAND_ALREADY_EXISTS',
  SOURCE_LIMIT_REACHED = 'SOURCE_LIMIT_REACHED',
  DUPLICATE_SOURCE = 'DUPLICATE_SOURCE',

  // Network errors
  NETWORK_ERROR = 'NETWORK_ERROR',
  TIMEOUT_ERROR = 'TIMEOUT_ERROR',
  SERVER_ERROR = 'SERVER_ERROR',
}

/**
 * Polish error messages for onboarding
 */
export const ONBOARDING_ERROR_MESSAGES: Record<OnboardingErrorType, string> = {
  BRAND_NAME_REQUIRED: 'Nazwa marki jest wymagana',
  BRAND_NAME_TOO_LONG: 'Nazwa marki może mieć maksymalnie 255 znaków',
  SOURCE_TYPE_REQUIRED: 'Wybierz typ źródła opinii',
  INVALID_URL: 'Podany adres URL jest nieprawidłowy',
  BRAND_ALREADY_EXISTS: 'Marka została już utworzona dla tego konta',
  SOURCE_LIMIT_REACHED:
    'Plan darmowy pozwala na dodanie 1 źródła. Przejdź na plan premium aby dodać więcej.',
  DUPLICATE_SOURCE: 'To źródło zostało już dodane',
  NETWORK_ERROR: 'Błąd połączenia. Sprawdź połączenie internetowe.',
  TIMEOUT_ERROR: 'Przekroczono czas oczekiwania. Spróbuj ponownie.',
  SERVER_ERROR: 'Wystąpił błąd serwera. Spróbuj ponownie później.',
};

// ========================================
// INITIAL STATE
// ========================================

/**
 * Initial onboarding state
 */
export const INITIAL_ONBOARDING_STATE: OnboardingState = {
  currentStep: OnboardingStep.BRAND_SETUP,
  brand: { name: '' },
  reviewSource: {
    sourceType: null,
    profileUrl: '',
    externalProfileId: '',
  },
  isSubmitting: false,
  importProgress: {
    isImporting: false,
    progress: 0,
    status: '',
    reviewsImported: 0,
    totalReviews: 0,
  },
  errors: {},
};

// ========================================
// HELPER TYPES
// ========================================

/**
 * Source type metadata for UI
 */
export interface SourceTypeMetadata {
  type: SourceType;
  displayName: string;
  description: string;
  iconName: string;
  isRecommended: boolean;
  exampleUrl: string;
}

/**
 * Source type metadata for all platforms
 */
export const SOURCE_TYPE_METADATA: Record<SourceType, SourceTypeMetadata> = {
  [SourceType.GOOGLE]: {
    type: SourceType.GOOGLE,
    displayName: 'Google',
    description: 'Opinie z Google Maps i Google Business Profile',
    iconName: 'google',
    isRecommended: true,
    exampleUrl: 'https://www.google.com/maps/place/Nazwa+Firmy',
  },
  [SourceType.FACEBOOK]: {
    type: SourceType.FACEBOOK,
    displayName: 'Facebook',
    description: 'Opinie ze strony firmowej Facebook',
    iconName: 'facebook',
    isRecommended: false,
    exampleUrl: 'https://www.facebook.com/twoja-firma',
  },
  [SourceType.TRUSTPILOT]: {
    type: SourceType.TRUSTPILOT,
    displayName: 'Trustpilot',
    description: 'Opinie z profilu Trustpilot',
    iconName: 'trustpilot',
    isRecommended: false,
    exampleUrl: 'https://www.trustpilot.com/review/twojadomena.pl',
  },
};
