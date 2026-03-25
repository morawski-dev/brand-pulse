/**
 * Render tests for the SentimentBadge component.
 */

import { render, screen } from '@testing-library/react';
import { SentimentBadge } from '../SentimentBadge';
import { Sentiment } from '@/lib/types/common';

describe('SentimentBadge', () => {
  it('renders the Polish label and icon for a positive sentiment', () => {
    render(<SentimentBadge sentiment={Sentiment.POSITIVE} />);
    expect(screen.getByText('Pozytywny')).toBeInTheDocument();
    expect(screen.getByText('👍')).toBeInTheDocument();
  });

  it('renders the label for a negative sentiment', () => {
    render(<SentimentBadge sentiment={Sentiment.NEGATIVE} />);
    expect(screen.getByText('Negatywny')).toBeInTheDocument();
    expect(screen.getByText('👎')).toBeInTheDocument();
  });

  it('shows the confidence percentage when requested', () => {
    render(
      <SentimentBadge sentiment={Sentiment.NEUTRAL} confidence={0.95} showConfidence />
    );
    expect(screen.getByText('Neutralny')).toBeInTheDocument();
    expect(screen.getByText('(95%)')).toBeInTheDocument();
  });

  it('hides the confidence percentage by default', () => {
    render(<SentimentBadge sentiment={Sentiment.POSITIVE} confidence={0.95} />);
    expect(screen.queryByText('(95%)')).not.toBeInTheDocument();
  });
});
