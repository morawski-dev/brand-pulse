// Hero Section Types
export interface HeroMetric {
  value: number; // Final value to display
  label: string; // Metric label (e.g., "users configure source")
  suffix?: string; // Suffix after number (%, +, min, etc.)
  animationDuration?: number; // Animation duration in ms (default: 2000)
}

// Features Types
export interface Feature {
  id: string; // Unique identifier
  icon: string; // Icon name from Lucide (e.g., "BarChart3")
  title: string; // Feature title (e.g., "Review Aggregation")
  description: string; // Feature description (1-2 sentences)
}

export interface FeatureCardProps {
  feature: Feature;
  index?: number; // Index for staggered animations
}

// Benefits Types
export interface Benefit {
  id: string;
  title: string; // Benefit title (e.g., "Save Time")
  description: string; // Benefit description (2-3 sentences)
  targetAudience: 'small-business' | 'chain' | 'all'; // Target group
  icon?: string; // Optional icon name from Lucide
}

export interface BenefitCardProps {
  benefit: Benefit;
}

// Demo Preview Types
export interface DemoReview {
  id: string; // Unique identifier
  source: 'google' | 'facebook' | 'trustpilot'; // Review source
  rating: 1 | 2 | 3 | 4 | 5; // Star rating
  sentiment: 'positive' | 'negative' | 'neutral'; // AI-assigned sentiment
  content: string; // Review content
  author: string; // Author name (or "Google User")
  date: string; // Date in ISO 8601 format
}

export interface DemoFilters {
  source: 'all' | 'google' | 'facebook' | 'trustpilot';
  sentiment: 'all' | 'positive' | 'negative' | 'neutral';
  rating: 'all' | '1-2' | '3' | '4-5';
}

export interface DemoStats {
  averageRating: number; // Average rating (1-5, with 0.1 precision)
  totalReviews: number; // Review count
  sentimentDistribution: {
    positive: number; // Positive percentage (0-100)
    negative: number; // Negative percentage (0-100)
    neutral: number; // Neutral percentage (0-100)
  };
}

// Pricing Types
export interface PricingPlan {
  id: string; // Unique plan identifier
  name: string; // Plan name (e.g., "Free", "Pro")
  price: number | 'custom'; // Price: 0 for free, number for paid, 'custom' for enterprise
  period?: 'month' | 'year'; // Billing period (if price is number)
  features: string[]; // Feature list (bullet points)
  limitations?: string[]; // Limitations list (optionally, e.g., "1 review source")
  highlighted?: boolean; // Whether plan is highlighted (visual emphasis)
  ctaText: string; // CTA button text (e.g., "Start Free", "Coming Soon")
  ctaAction: 'register' | 'contact' | 'coming-soon'; // CTA action
}

export interface PricingCardProps {
  plan: PricingPlan;
  onCTAClick: (planId: string, action: PricingPlan['ctaAction']) => void;
}

// Helper Types for Hooks
export interface UseScrollAnimationReturn {
  ref: React.RefObject<HTMLElement>;
  isInView: boolean;
}

export interface UseStickyPositionReturn {
  isVisible: boolean;
  scrollY: number;
}

export interface UseCountUpReturn {
  currentValue: number;
  isAnimating: boolean;
}

// Component Props Types
export interface AnimatedMetricProps {
  value: number;
  label: string;
  suffix?: string;
  animationDuration?: number;
  delay?: number; // staggered animations
}

export interface HeroSectionProps {
  metrics: HeroMetric[];
}

export interface FeaturesSectionProps {
  features: Feature[];
}

export interface BenefitsSectionProps {
  benefits: Benefit[];
}

export interface DemoPreviewProps {
  mockReviews: DemoReview[];
}

export interface DemoControlsProps {
  activeFilters: DemoFilters;
  onFilterChange: (filterType: keyof DemoFilters, value: string) => void;
}

export interface DemoStatsProps {
  stats: DemoStats;
}

export interface DemoReviewListProps {
  reviews: DemoReview[];
  maxVisible?: number; // default: 5
}

export interface DemoReviewCardProps {
  review: DemoReview;
}

export interface PricingSectionProps {
  plans: PricingPlan[];
}

export interface StickyMobileCTAProps {
  threshold?: number; // scroll threshold in px (default: 300)
}

export interface NavigationProps {
  variant?: 'default' | 'transparent'; // for different backgrounds
}
