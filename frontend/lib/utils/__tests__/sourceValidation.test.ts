/**
 * Unit tests for source URL validation / extraction utilities.
 */

import {
  validateSourceUrl,
  extractExternalId,
  validateAndExtract,
  getExampleUrl,
  getSourceDisplayName,
  isRecommendedSource,
  getAllSourceTypes,
} from '../sourceValidation';
import { SourceType } from '@/lib/types/onboarding';

describe('validateSourceUrl', () => {
  it('rejects empty URL', () => {
    const result = validateSourceUrl('', SourceType.GOOGLE);
    expect(result.isValid).toBe(false);
    expect(result.error).toBe('Adres URL jest wymagany');
  });

  it('accepts a valid Google Maps place URL', () => {
    expect(
      validateSourceUrl('https://www.google.com/maps/place/My+Business', SourceType.GOOGLE).isValid
    ).toBe(true);
  });

  it('accepts a valid Trustpilot review URL', () => {
    expect(
      validateSourceUrl('https://www.trustpilot.com/review/example.com', SourceType.TRUSTPILOT).isValid
    ).toBe(true);
  });

  it('rejects a URL that does not match the selected platform', () => {
    const result = validateSourceUrl('https://facebook.com/test', SourceType.GOOGLE);
    expect(result.isValid).toBe(false);
    expect(result.error).toContain('Google');
  });

  it('rejects a Trustpilot URL without /review/ path', () => {
    expect(
      validateSourceUrl('https://www.trustpilot.com/categories/x', SourceType.TRUSTPILOT).isValid
    ).toBe(false);
  });
});

describe('extractExternalId', () => {
  it('extracts the Google place name (decodeURIComponent keeps + literal)', () => {
    expect(
      extractExternalId('https://www.google.com/maps/place/My+Restaurant', SourceType.GOOGLE)
    ).toBe('My+Restaurant');
  });

  it('percent-decodes the extracted Google place name', () => {
    expect(
      extractExternalId('https://www.google.com/maps/place/My%20Restaurant', SourceType.GOOGLE)
    ).toBe('My Restaurant');
  });

  it('extracts a Facebook page handle', () => {
    expect(
      extractExternalId('https://www.facebook.com/mybusiness', SourceType.FACEBOOK)
    ).toBe('mybusiness');
  });

  it('extracts a Trustpilot domain', () => {
    expect(
      extractExternalId('https://www.trustpilot.com/review/example.com', SourceType.TRUSTPILOT)
    ).toBe('example.com');
  });

  it('returns null for an empty URL', () => {
    expect(extractExternalId('', SourceType.GOOGLE)).toBeNull();
  });

  it('returns null when the pattern does not match', () => {
    expect(extractExternalId('https://www.google.com/maps', SourceType.GOOGLE)).toBeNull();
  });
});

describe('validateAndExtract', () => {
  it('returns isValid + externalId for a valid URL', () => {
    const result = validateAndExtract(
      'https://www.google.com/maps/place/Test',
      SourceType.GOOGLE
    );
    expect(result.isValid).toBe(true);
    expect(result.externalId).toBe('Test');
  });

  it('propagates a format error for an invalid URL', () => {
    const result = validateAndExtract('invalid-url', SourceType.GOOGLE);
    expect(result.isValid).toBe(false);
    expect(result.error).toBeDefined();
  });
});

describe('helpers', () => {
  it('returns a non-empty example URL for each source type', () => {
    getAllSourceTypes().forEach((type) => {
      expect(getExampleUrl(type)).toMatch(/^https?:\/\//);
    });
  });

  it('maps source types to display names', () => {
    expect(getSourceDisplayName(SourceType.GOOGLE)).toBe('Google');
    expect(getSourceDisplayName(SourceType.FACEBOOK)).toBe('Facebook');
    expect(getSourceDisplayName(SourceType.TRUSTPILOT)).toBe('Trustpilot');
  });

  it('marks only Google as recommended for MVP', () => {
    expect(isRecommendedSource(SourceType.GOOGLE)).toBe(true);
    expect(isRecommendedSource(SourceType.FACEBOOK)).toBe(false);
  });

  it('returns all three source types', () => {
    expect(getAllSourceTypes()).toEqual([
      SourceType.GOOGLE,
      SourceType.FACEBOOK,
      SourceType.TRUSTPILOT,
    ]);
  });
});
