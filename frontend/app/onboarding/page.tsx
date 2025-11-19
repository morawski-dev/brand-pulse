/**
 * Onboarding Page - First-time user setup
 *
 * Route: /onboarding
 * Protected: Requires authentication
 *
 * Flow:
 * 1. User completes registration
 * 2. Redirected here automatically
 * 3. Configure first brand and review source (5-step wizard)
 * 4. Auto-redirect to /dashboard after import completes
 *
 * User Story: US-003 - Configuring the first source
 * Success Criteria: 90% of users configure within 10 minutes
 */

import { Metadata } from 'next';
import { OnboardingWizard } from './components/OnboardingWizard';

// ========================================
// METADATA
// ========================================

export const metadata: Metadata = {
  title: 'Konfiguracja konta - BrandPulse',
  description: 'Skonfiguruj swoją markę i pierwsze źródło opinii',
  robots: 'noindex, nofollow', // Don't index onboarding pages
};

// ========================================
// PAGE COMPONENT
// ========================================

/**
 * Onboarding page component
 *
 * Renders the 5-step wizard:
 * 1. Brand name setup
 * 2. Source type selection (Google/Facebook/Trustpilot)
 * 3. Source URL input with validation
 * 4. Confirmation and review
 * 5. Import progress with auto-redirect
 *
 * Note: This is a client component through OnboardingWizard
 * Authentication check happens in middleware or AuthContext
 */
export default function OnboardingPage() {
  return <OnboardingWizard />;
}
