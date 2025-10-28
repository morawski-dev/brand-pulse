import type { DemoReview, DemoFilters, DemoStats } from '@/lib/types/landing';

/**
 * Filters reviews based on the provided filter criteria
 * @param reviews - Array of demo reviews to filter
 * @param filters - Filter criteria (source, sentiment, rating)
 * @returns Filtered array of reviews
 */
export function filterReviews(
  reviews: DemoReview[],
  filters: DemoFilters
): DemoReview[] {
  return reviews.filter((review) => {
    // Filter by source
    if (filters.source !== 'all' && review.source !== filters.source) {
      return false;
    }

    // Filter by sentiment
    if (filters.sentiment !== 'all' && review.sentiment !== filters.sentiment) {
      return false;
    }

    // Filter by rating
    if (filters.rating === '1-2' && review.rating > 2) {
      return false;
    }
    if (filters.rating === '3' && review.rating !== 3) {
      return false;
    }
    if (filters.rating === '4-5' && review.rating < 4) {
      return false;
    }

    return true;
  });
}

/**
 * Calculates statistics for a set of reviews
 * @param reviews - Array of reviews to analyze
 * @returns Statistics object with average rating, total count, and sentiment distribution
 */
export function calculateDemoStats(reviews: DemoReview[]): DemoStats {
  // Handle empty array
  if (reviews.length === 0) {
    return {
      averageRating: 0,
      totalReviews: 0,
      sentimentDistribution: {
        positive: 0,
        negative: 0,
        neutral: 0
      }
    };
  }

  // Calculate average rating
  const totalRating = reviews.reduce((sum, review) => sum + review.rating, 0);
  const averageRating = totalRating / reviews.length;

  // Calculate sentiment distribution
  const sentimentCounts = reviews.reduce(
    (counts, review) => {
      counts[review.sentiment]++;
      return counts;
    },
    { positive: 0, negative: 0, neutral: 0 }
  );

  const sentimentDistribution = {
    positive: Math.round((sentimentCounts.positive / reviews.length) * 100),
    negative: Math.round((sentimentCounts.negative / reviews.length) * 100),
    neutral: Math.round((sentimentCounts.neutral / reviews.length) * 100)
  };

  return {
    averageRating: Math.round(averageRating * 10) / 10, // Round to 1 decimal place
    totalReviews: reviews.length,
    sentimentDistribution
  };
}

/**
 * Formats a date string to a readable format
 * @param dateString - ISO 8601 date string
 * @returns Formatted date string (e.g., "Jan 20, 2025")
 */
export function formatReviewDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  });
}

/**
 * Gets the appropriate color class for a sentiment badge
 * @param sentiment - Review sentiment
 * @returns Tailwind CSS classes for the sentiment badge
 */
export function getSentimentColor(
  sentiment: DemoReview['sentiment']
): string {
  const colors = {
    positive: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300',
    negative: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300',
    neutral: 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300'
  };

  return colors[sentiment];
}

/**
 * Gets the appropriate icon name for a review source
 * @param source - Review source
 * @returns Icon identifier string
 */
export function getSourceIcon(source: DemoReview['source']): string {
  const icons = {
    google: 'google',
    facebook: 'facebook',
    trustpilot: 'star'
  };

  return icons[source];
}
