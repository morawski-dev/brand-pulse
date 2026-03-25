/**
 * Unit tests for dashboard API query-param helpers.
 */

import {
  buildSentimentParam,
  buildRatingParam,
  formatLocalDate,
} from '../dashboard';
import { Sentiment, Rating } from '@/lib/types/common';

describe('buildSentimentParam', () => {
  it('returns undefined for an empty set', () => {
    expect(buildSentimentParam(new Set())).toBeUndefined();
  });

  it('joins selected sentiments with commas', () => {
    const set = new Set<Sentiment>([Sentiment.POSITIVE, Sentiment.NEGATIVE]);
    expect(buildSentimentParam(set)).toBe('POSITIVE,NEGATIVE');
  });

  it('serializes a single sentiment', () => {
    expect(buildSentimentParam(new Set<Sentiment>([Sentiment.NEUTRAL]))).toBe('NEUTRAL');
  });
});

describe('buildRatingParam', () => {
  it('returns undefined for an empty set', () => {
    expect(buildRatingParam(new Set())).toBeUndefined();
  });

  it('joins selected ratings with commas', () => {
    const set = new Set<Rating>([1, 5]);
    expect(buildRatingParam(set)).toBe('1,5');
  });
});

describe('formatLocalDate', () => {
  it('formats a date as YYYY-MM-DD with zero padding', () => {
    // Local-time components avoid timezone flakiness.
    expect(formatLocalDate(new Date(2026, 0, 5))).toBe('2026-01-05');
    expect(formatLocalDate(new Date(2026, 11, 31))).toBe('2026-12-31');
  });
});
