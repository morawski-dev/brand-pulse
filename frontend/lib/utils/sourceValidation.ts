/**
 * Source validation utilities
 * Functions for validating review source URLs and extracting profile IDs
 *
 * Supported platforms:
 * - Google (Maps and Business Profile)
 * - Facebook (Pages)
 * - Trustpilot
 */

import { SourceType, ValidationResult } from '@/lib/types/onboarding';

// ========================================
// URL REGEX PATTERNS
// ========================================

/**
 * URL regex patterns for each source type
 * Used for client-side validation before API call
 */
const URL_PATTERNS: Record<SourceType, RegExp> = {
  [SourceType.GOOGLE]:
    /^https?:\/\/(www\.)?google\.(com|pl|de|fr|es|it|co\.uk)\/(maps\/place\/|business\/)/i,
  [SourceType.FACEBOOK]:
    /^https?:\/\/(www\.)?(facebook|fb)\.com\/(pages\/|pg\/|[a-zA-Z0-9._-]+\/?)/i,
  [SourceType.TRUSTPILOT]:
    /^https?:\/\/(www\.)?trustpilot\.(com|pl|de|fr|es|it|co\.uk)\/review\/[a-zA-Z0-9.-]+/i,
};

/**
 * Extraction regex patterns for external profile IDs
 * More specific patterns for extracting IDs from valid URLs
 */
const EXTRACTION_PATTERNS: Record<
  SourceType,
  { pattern: RegExp; group: number }
> = {
  [SourceType.GOOGLE]: {
    // Extract everything after "place/" until next "/" or end of string
    pattern: /maps\/place\/([^\/\?#]+)/i,
    group: 1,
  },
  [SourceType.FACEBOOK]: {
    // Extract page username or ID (skip "pages/" and "pg/" prefixes)
    pattern: /(?:facebook|fb)\.com\/(?:pages\/|pg\/)?([a-zA-Z0-9._-]+)/i,
    group: 1,
  },
  [SourceType.TRUSTPILOT]: {
    // Extract domain after "review/"
    pattern: /review\/([a-zA-Z0-9.-]+)/i,
    group: 1,
  },
};

// ========================================
// VALIDATION FUNCTIONS
// ========================================

/**
 * Validates source URL format
 *
 * Checks if URL matches the expected pattern for the given source type
 * Does NOT check if the profile actually exists (backend responsibility)
 *
 * @param url - URL to validate
 * @param sourceType - Source platform type
 * @returns Validation result with error message if invalid
 *
 * @example
 * validateSourceUrl('https://www.google.com/maps/place/Example', SourceType.GOOGLE)
 * // => { isValid: true }
 *
 * @example
 * validateSourceUrl('https://facebook.com/test', SourceType.GOOGLE)
 * // => { isValid: false, error: 'Nieprawidłowy format URL dla GOOGLE' }
 */
export function validateSourceUrl(
  url: string,
  sourceType: SourceType
): ValidationResult {
  // Empty URL
  if (!url || url.trim().length === 0) {
    return {
      isValid: false,
      error: 'Adres URL jest wymagany',
    };
  }

  // Check format
  const pattern = URL_PATTERNS[sourceType];
  if (!pattern.test(url)) {
    const platformName = getSourceDisplayName(sourceType);
    return {
      isValid: false,
      error: `Nieprawidłowy format URL dla ${platformName}. Sprawdź przykład.`,
    };
  }

  return { isValid: true };
}

/**
 * Extracts external profile ID from URL
 *
 * Uses platform-specific regex patterns to extract the unique identifier
 * Returns null if extraction fails
 *
 * @param url - Source URL
 * @param sourceType - Source platform type
 * @returns Extracted profile ID or null if failed
 *
 * @example
 * extractExternalId('https://www.google.com/maps/place/My+Restaurant', SourceType.GOOGLE)
 * // => 'My+Restaurant'
 *
 * @example
 * extractExternalId('https://www.facebook.com/mybusiness', SourceType.FACEBOOK)
 * // => 'mybusiness'
 *
 * @example
 * extractExternalId('https://www.trustpilot.com/review/example.com', SourceType.TRUSTPILOT)
 * // => 'example.com'
 */
export function extractExternalId(
  url: string,
  sourceType: SourceType
): string | null {
  if (!url || url.trim().length === 0) {
    return null;
  }

  try {
    const { pattern, group } = EXTRACTION_PATTERNS[sourceType];
    const match = url.match(pattern);

    if (!match || !match[group]) {
      return null;
    }

    // Decode URL encoding (e.g., My+Restaurant => My Restaurant)
    const extracted = decodeURIComponent(match[group]);

    // Clean up trailing slashes
    return extracted.replace(/\/+$/, '');
  } catch (error) {
    console.error('Failed to extract external ID:', error);
    return null;
  }
}

/**
 * Validates URL and extracts external ID in one call
 *
 * Convenience function that combines validation and extraction
 * Returns validation result with extracted ID if successful
 *
 * @param url - Source URL to validate
 * @param sourceType - Source platform type
 * @returns Validation result with extracted ID if valid
 *
 * @example
 * validateAndExtract('https://www.google.com/maps/place/Test', SourceType.GOOGLE)
 * // => { isValid: true, externalId: 'Test' }
 *
 * @example
 * validateAndExtract('invalid-url', SourceType.GOOGLE)
 * // => { isValid: false, error: 'Nieprawidłowy format URL...' }
 */
export function validateAndExtract(
  url: string,
  sourceType: SourceType
): ValidationResult {
  // First validate format
  const validation = validateSourceUrl(url, sourceType);
  if (!validation.isValid) {
    return validation;
  }

  // Then extract ID
  const externalId = extractExternalId(url, sourceType);
  if (!externalId) {
    return {
      isValid: false,
      error: 'Nie udało się wyodrębnić ID profilu z podanego URL',
    };
  }

  return {
    isValid: true,
    externalId,
  };
}

// ========================================
// HELPER FUNCTIONS
// ========================================

/**
 * Gets example URL for each source type
 *
 * Returns a properly formatted example URL that users can reference
 * Useful for placeholder text and help messages
 *
 * @param sourceType - Source platform type
 * @returns Example URL string
 *
 * @example
 * getExampleUrl(SourceType.GOOGLE)
 * // => 'https://www.google.com/maps/place/Nazwa+Firmy'
 */
export function getExampleUrl(sourceType: SourceType): string {
  const examples: Record<SourceType, string> = {
    [SourceType.GOOGLE]:
      'https://www.google.com/maps/place/Nazwa+Firmy/@50.123,19.456',
    [SourceType.FACEBOOK]: 'https://www.facebook.com/twoja-firma',
    [SourceType.TRUSTPILOT]:
      'https://www.trustpilot.com/review/twojadomena.pl',
  };

  return examples[sourceType];
}

/**
 * Gets platform display name (Polish)
 *
 * Returns localized display name for UI
 *
 * @param sourceType - Source platform type
 * @returns Polish display name
 *
 * @example
 * getSourceDisplayName(SourceType.GOOGLE)
 * // => 'Google'
 */
export function getSourceDisplayName(sourceType: SourceType): string {
  const names: Record<SourceType, string> = {
    [SourceType.GOOGLE]: 'Google',
    [SourceType.FACEBOOK]: 'Facebook',
    [SourceType.TRUSTPILOT]: 'Trustpilot',
  };

  return names[sourceType];
}

/**
 * Gets platform description (Polish)
 *
 * Returns short description for UI cards/tooltips
 *
 * @param sourceType - Source platform type
 * @returns Polish description
 *
 * @example
 * getSourceDescription(SourceType.GOOGLE)
 * // => 'Opinie z Google Maps i Google Business Profile'
 */
export function getSourceDescription(sourceType: SourceType): string {
  const descriptions: Record<SourceType, string> = {
    [SourceType.GOOGLE]: 'Opinie z Google Maps i Google Business Profile',
    [SourceType.FACEBOOK]: 'Opinie ze strony firmowej Facebook',
    [SourceType.TRUSTPILOT]: 'Opinie z profilu Trustpilot',
  };

  return descriptions[sourceType];
}

/**
 * Gets help text for URL input (Polish)
 *
 * Returns instructions on where to find the URL
 *
 * @param sourceType - Source platform type
 * @returns Polish help text
 */
export function getUrlHelpText(sourceType: SourceType): string {
  const helpTexts: Record<SourceType, string> = {
    [SourceType.GOOGLE]:
      'Skopiuj adres URL z Google Maps lub Google Business Profile',
    [SourceType.FACEBOOK]:
      'Skopiuj adres URL strony firmowej z paska adresu przeglądarki',
    [SourceType.TRUSTPILOT]:
      'Skopiuj adres URL profilu z Trustpilot (zawiera /review/)',
  };

  return helpTexts[sourceType];
}

/**
 * Checks if source type is recommended for MVP
 *
 * Google is the recommended platform due to API priority
 *
 * @param sourceType - Source platform type
 * @returns True if recommended
 */
export function isRecommendedSource(sourceType: SourceType): boolean {
  return sourceType === SourceType.GOOGLE;
}

/**
 * Gets all available source types
 *
 * Returns array of all supported source types
 * Useful for rendering source selection cards
 *
 * @returns Array of source types
 */
export function getAllSourceTypes(): SourceType[] {
  return [SourceType.GOOGLE, SourceType.FACEBOOK, SourceType.TRUSTPILOT];
}
