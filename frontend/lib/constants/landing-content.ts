import type {
  HeroMetric,
  Feature,
  Benefit,
  DemoReview,
  PricingPlan
} from '@/lib/types/landing';

// Hero Metrics
export const HERO_METRICS: HeroMetric[] = [
  {
    value: 90,
    label: 'users configure source in 10 min',
    suffix: '%',
    animationDuration: 2000
  },
  {
    value: 75,
    label: 'sentiment analysis accuracy',
    suffix: '%',
    animationDuration: 2200
  },
  {
    value: 3,
    label: 'review sources in one place',
    suffix: '+',
    animationDuration: 1800
  }
];

// Features
export const FEATURES: Feature[] = [
  {
    id: 'aggregation',
    icon: 'BarChart3',
    title: 'Review Aggregation',
    description: 'All reviews from Google, Facebook and Trustpilot in one unified dashboard.'
  },
  {
    id: 'ai-sentiment',
    icon: 'Brain',
    title: 'AI Sentiment Analysis',
    description: 'Automatic sentiment classification powered by advanced AI models for every review.'
  },
  {
    id: 'smart-filters',
    icon: 'Filter',
    title: 'Smart Filtering',
    description: 'Filter reviews by source, sentiment, rating, and date to find what matters most.'
  },
  {
    id: 'auto-sync',
    icon: 'RefreshCw',
    title: 'Automatic Sync',
    description: 'Daily automatic updates plus manual refresh option to keep your data current.'
  },
  {
    id: 'insights',
    icon: 'TrendingUp',
    title: 'Business Insights',
    description: 'Get actionable insights from aggregated data to improve your customer experience.'
  },
  {
    id: 'multi-location',
    icon: 'Users',
    title: 'Multi-Location Support',
    description: 'Monitor reviews across multiple business locations from a single dashboard.'
  }
];

// Benefits
export const BENEFITS: Benefit[] = [
  {
    id: 'save-time',
    title: 'Save Hours Every Week',
    description: 'Stop checking multiple platforms manually. Get all your customer feedback in one place, saving you 5+ hours weekly on review monitoring and analysis.',
    targetAudience: 'small-business',
    icon: 'Clock'
  },
  {
    id: 'understand-customers',
    title: 'Understand Your Customers Better',
    description: 'AI-powered sentiment analysis reveals what customers truly think about your business. Identify trends, spot issues early, and capitalize on positive feedback.',
    targetAudience: 'all',
    icon: 'Brain'
  },
  {
    id: 'grow-reputation',
    title: 'Grow Your Online Reputation',
    description: 'Track your reputation across all platforms, respond faster to reviews, and turn feedback into actionable improvements that drive business growth.',
    targetAudience: 'chain',
    icon: 'Target'
  }
];

// Mock Reviews for Demo
export const MOCK_REVIEWS: DemoReview[] = [
  {
    id: '1',
    source: 'google',
    rating: 5,
    sentiment: 'positive',
    content: 'Excellent service! The team was professional and attentive. Highly recommend this business to anyone looking for quality work.',
    author: 'Sarah Johnson',
    date: '2025-01-20T10:30:00Z'
  },
  {
    id: '2',
    source: 'facebook',
    rating: 4,
    sentiment: 'positive',
    content: 'Great experience overall. Quick response time and friendly staff. Would definitely come back again.',
    author: 'Michael Chen',
    date: '2025-01-19T14:15:00Z'
  },
  {
    id: '3',
    source: 'trustpilot',
    rating: 2,
    sentiment: 'negative',
    content: 'Service was okay but prices are too high compared to competitors. Expected more for what we paid.',
    author: 'Emma Wilson',
    date: '2025-01-18T09:45:00Z'
  },
  {
    id: '4',
    source: 'google',
    rating: 5,
    sentiment: 'positive',
    content: 'Outstanding! Best customer service I have experienced in years. The attention to detail is remarkable.',
    author: 'David Martinez',
    date: '2025-01-17T16:20:00Z'
  },
  {
    id: '5',
    source: 'facebook',
    rating: 3,
    sentiment: 'neutral',
    content: 'Average experience. Nothing special but nothing terrible either. Got the job done as expected.',
    author: 'Lisa Anderson',
    date: '2025-01-16T11:00:00Z'
  },
  {
    id: '6',
    source: 'google',
    rating: 1,
    sentiment: 'negative',
    content: 'Very disappointed with the service. Long wait times and unprofessional behavior from staff.',
    author: 'Robert Taylor',
    date: '2025-01-15T13:30:00Z'
  },
  {
    id: '7',
    source: 'trustpilot',
    rating: 5,
    sentiment: 'positive',
    content: 'Fantastic! Exceeded all my expectations. Will definitely recommend to friends and family.',
    author: 'Jennifer Lee',
    date: '2025-01-14T08:45:00Z'
  },
  {
    id: '8',
    source: 'google',
    rating: 4,
    sentiment: 'positive',
    content: 'Very good service. Small issue with timing but they resolved it quickly and professionally.',
    author: 'James Brown',
    date: '2025-01-13T15:10:00Z'
  },
  {
    id: '9',
    source: 'facebook',
    rating: 5,
    sentiment: 'positive',
    content: 'Amazing team! They went above and beyond to ensure everything was perfect. Truly impressed!',
    author: 'Amanda Garcia',
    date: '2025-01-12T10:20:00Z'
  },
  {
    id: '10',
    source: 'trustpilot',
    rating: 3,
    sentiment: 'neutral',
    content: 'Decent service but room for improvement. Would consider using them again if prices were better.',
    author: 'Christopher White',
    date: '2025-01-11T12:00:00Z'
  },
  {
    id: '11',
    source: 'google',
    rating: 5,
    sentiment: 'positive',
    content: 'Absolutely love this place! Staff is incredibly friendly and knowledgeable. Keep up the great work!',
    author: 'Maria Rodriguez',
    date: '2025-01-10T14:30:00Z'
  },
  {
    id: '12',
    source: 'facebook',
    rating: 2,
    sentiment: 'negative',
    content: 'Not satisfied with the outcome. Communication could be much better. Felt like just another customer.',
    author: 'Thomas Harris',
    date: '2025-01-09T09:15:00Z'
  }
];

// Pricing Plans
export const PRICING_PLANS: PricingPlan[] = [
  {
    id: 'free',
    name: 'Free',
    price: 0,
    period: 'month',
    features: [
      '1 review source (Google, Facebook, or Trustpilot)',
      'AI sentiment analysis',
      'Basic dashboard with filters',
      'Daily automatic sync',
      'Manual refresh (once per 24h)',
      'Email notifications'
    ],
    limitations: [
      'Limited to 1 review source',
      'Last 90 days of reviews only'
    ],
    highlighted: false,
    ctaText: 'Start Free',
    ctaAction: 'register'
  },
  {
    id: 'pro',
    name: 'Pro',
    price: 'custom',
    features: [
      'Unlimited review sources',
      'Advanced AI sentiment analysis',
      'Multi-location support',
      'Full review history',
      'Priority support',
      'Weekly email reports',
      'Custom integrations'
    ],
    highlighted: true,
    ctaText: 'Coming Soon',
    ctaAction: 'coming-soon'
  }
];