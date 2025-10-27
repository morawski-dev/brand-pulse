# BrandPulse UI Architecture

## 1. UI Structure Overview

BrandPulse will be a Single Page Application (SPA) built with React and TypeScript, utilizing shadcn/ui and Tailwind CSS. The UI architecture is based on a modular feature-based structure with a clear separation between public views (pre-auth) and protected application views (post-auth). The navigation system will be adaptive - sidebar for desktop, bottom navigation for mobile. A key element will be progressive feature disclosure through an onboarding wizard and contextual tooltips.

## 2. Views List

### Public Views (Pre-Authentication)

#### Landing Page
- **View Path:** `/`
- **Main Goal:** Present product value and convert visitors
- **Key Information to Display:**
  - Hero section with value proposition
  - List of key features
  - Business benefits section
  - Demo preview with sample data
  - Pricing preview (freemium model)
  - CTA for registration and demo
- **Key View Components:**
  - Hero banner with animated metrics
  - Feature cards with icons
  - Interactive demo preview
  - Testimonials carousel (future)
  - Sticky CTA bar on mobile
- **UX, Accessibility and Security Considerations:**
  - Lazy loading for images
  - Semantic HTML for SEO
  - ARIA labels for interactive elements
  - CSP headers for external resources
  - Minimum 44px touch targets on mobile

#### Login Page
- **View Path:** `/login`
- **Main Goal:** Authenticate existing users
- **Key Information to Display:**
  - Login form (email, password)
  - Link to password recovery
  - Link to registration
  - "Remember me" option
  - Demo mode information
- **Key View Components:**
  - Form with real-time validation
  - Password visibility toggle
  - Loading state during login
  - Error toast for invalid credentials
  - Success redirect handler
- **UX, Accessibility and Security Considerations:**
  - Autofocus on first field
  - Keyboard navigation support
  - Clear error messages
  - Rate limiting feedback
  - Secure password handling (no autocomplete in public places)

#### Registration Page
- **View Path:** `/register`
- **Main Goal:** Create new user account
- **Key Information to Display:**
  - Registration form (email, password, repeat password)
  - Password strength indicator
  - Consents (terms of service, privacy policy)
  - Free plan information
  - Link to login
- **Key View Components:**
  - Multi-field form with progressive disclosure
  - Password strength meter
  - Checkbox agreements
  - Submit button with loading state
  - Success modal with next steps
- **UX, Accessibility and Security Considerations:**
  - Client-side validation with debouncing
  - Clear password requirements
  - GDPR-compliant consent collection
  - Email verification process
  - Prevention of duplicate accounts

#### Password Recovery Page
- **View Path:** `/forgot-password`
- **Main Goal:** Initialize password recovery process
- **Key Information to Display:**
  - Email field
  - Process instructions
  - Return to login link
  - Success/error feedback
- **Key View Components:**
  - Simple email form
  - Loading indicator
  - Success message component
  - Rate limit warning
- **UX, Accessibility and Security Considerations:**
  - Clear instructions
  - Rate limiting (max 3 attempts per hour)
  - Generic success message (security)
  - Email verification before sending

#### Demo Mode
- **View Path:** `/demo`
- **Main Goal:** Interactive presentation of application capabilities
- **Key Information to Display:**
  - Sample dashboard with demo data
  - All features active (read-only)
  - Floating tutorial tooltips
  - CTA to registration
- **Key View Components:**
  - Full dashboard layout
  - Demo data generator
  - Interactive tour overlay
  - Persistent CTA banner
- **UX, Accessibility and Security Considerations:**
  - No real data exposure
  - Clear "Demo Mode" indicators
  - Limited session duration (30 min)
  - Conversion tracking

### Protected Views (Post-Authentication)

#### Onboarding Wizard
- **View Path:** `/onboarding`
- **Main Goal:** Quick setup of first review source
- **Key Information to Display:**
  - Step 1: Brand name and type
  - Step 2: Source type selection (Google/Facebook/Trustpilot)
  - Step 3: Source URL input
  - Step 4: Verification and data import
  - Progress indicator
- **Key View Components:**
  - Multi-step form wizard
  - Progress bar
  - Source type selector cards
  - URL input with validation
  - Import progress tracker
  - Skip option (for later configuration)
- **UX, Accessibility and Security Considerations:**
  - Clear step indicators
  - Back navigation between steps
  - Save progress in localStorage
  - URL validation before submit
  - Clear error messages for invalid URLs

#### Main Dashboard
- **View Path:** `/dashboard`
- **Main Goal:** Central hub for browsing and managing reviews
- **Key Information to Display:**
  - Metrics summary (average rating, sentiment, trends)
  - AI-generated insights
  - Filters (source, sentiment, rating, date)
  - Review list with infinite scroll
  - Quick actions for each review
- **Key View Components:**
  - Sticky header with brand selector
  - Metrics cards grid
  - AI summary card
  - Filter bar with chips
  - Virtualized review list
  - Review card with actions
  - Empty states
  - Loading skeletons
- **UX, Accessibility and Security Considerations:**
  - Virtualization for performance (>100 reviews)
  - Keyboard shortcuts (J/K for navigation)
  - ARIA live regions for updates
  - Optimistic UI for sentiment changes
  - Deep linking through URL params

#### Review Detail Modal
- **View Path:** Modal overlay on `/dashboard`
- **Main Goal:** Detailed view of single review
- **Key Information to Display:**
  - Full review content
  - Metadata (author, date, source)
  - AI sentiment with confidence score
  - Sentiment correction option
  - Link to original review
- **Key View Components:**
  - Modal with backdrop
  - Review content viewer
  - Sentiment selector
  - Action buttons
  - Navigation arrows (prev/next)
- **UX, Accessibility and Security Considerations:**
  - Focus trap in modal
  - ESC key to close
  - Swipe gestures on mobile
  - Maintain scroll position

#### Sources Management
- **View Path:** `/sources`
- **Main Goal:** Manage review sources
- **Key Information to Display:**
  - List of active sources
  - Health status of each source
  - Last synchronization
  - Review count per source
  - Management options
- **Key View Components:**
  - Source cards grid
  - Health indicator badges
  - Sync button with rate limiting
  - Add source button (with paywall for 2+)
  - Edit/delete dropdowns
  - Error state alerts
- **UX, Accessibility and Security Considerations:**
  - Clear sync status indicators
  - Confirmation modals for delete
  - Rate limit countdown timer
  - Paywall modal for premium features

#### Settings Hub
- **View Path:** `/settings/*`
- **Main Goal:** Central settings management
- **Key Information to Display:**
  - Profile settings
  - Email notifications
  - Brand configuration
  - Security settings
  - Billing info (future)
- **Key View Components:**
  - Settings sidebar navigation
  - Form sections with auto-save
  - Toggle switches
  - Save indicators
  - Danger zone for account deletion
- **UX, Accessibility and Security Considerations:**
  - Auto-save with debouncing
  - Clear success feedback
  - Two-factor auth setup (future)
  - Data export options

#### Email Report Settings
- **View Path:** `/settings/reports`
- **Main Goal:** Configure weekly reports
- **Key Information to Display:**
  - Report frequency
  - Day of week and time
  - Data scope in report
  - Preview of last report
- **Key View Components:**
  - Schedule selector
  - Content checkboxes
  - Preview modal
  - Test email button
- **UX, Accessibility and Security Considerations:**
  - Timezone handling
  - Email delivery confirmation
  - Unsubscribe link compliance

### Special States

#### Empty States
- **Main Goal:** Inform and guide user when data is missing
- **Variants:**
  - No sources configured
  - No reviews yet
  - No results for filters
  - Import in progress
- **Key Components:**
  - Illustration
  - Descriptive message
  - CTA button
  - Help links

#### Error Pages
- **Main Goal:** Graceful error handling
- **Variants:**
  - 404 Not Found
  - 500 Server Error
  - 403 Forbidden
  - Network offline
- **Key Components:**
  - Error illustration
  - User-friendly message
  - Recovery actions
  - Support contact

## 3. User Journey Map

### New User Flow (First-Time Experience)
1. **Discovery:** Landing Page → reads value proposition → clicks "Try Demo"
2. **Trial:** Demo Mode → explores features → clicks "Create Account"
3. **Registration:** Registration Page → fills form → confirms email
4. **Onboarding:**
   - Step 1: Provides brand name
   - Step 2: Selects Google as first source
   - Step 3: Pastes link to Google Business Profile
   - Step 4: Observes data import (progress bar)
5. **First Value:** Dashboard with first reviews → tutorial tooltips → filters negative
6. **Engagement:** Corrects wrong sentiment → checks AI insights → configures email report

### Returning User Flow (Daily Operations)
1. **Quick Access:** Login → Dashboard (bookmark)
2. **Daily Review:**
   - Checks new reviews (badge notification)
   - Sorts by date
   - Filters negative (quick filter)
   - Reads AI summary
3. **Actions:**
   - Corrects sentiment where needed
   - Checks source health
   - Manual sync if needed
4. **Weekly:** Receives and reviews email report

### Source Management Flow
1. **Add Source:** Sources → Add Source → Select Type → Enter URL → Validate → Import
2. **Monitor:** Dashboard → Source Filter → View source-specific metrics
3. **Maintain:** Sources → Check health → Sync if needed → Edit if broken
4. **Scale:** Try add 2nd source → See paywall → (Future: upgrade to paid)

### Crisis Response Flow
1. **Alert:** Email notification about negative spike
2. **Investigate:** Login → Dashboard → Filter negative + recent
3. **Analyze:** Read reviews → Check AI insights → Identify patterns
4. **Action:** Export data → Share with team → Plan response

## 4. Layout and Navigation Structure

### Desktop Layout (≥768px)
```
┌─────────────────────────────────────────────────┐
│ Top Bar                                         │
│ [Logo] [Brand Selector]        [User] [Logout] │
├────────────┬────────────────────────────────────┤
│            │ Main Content Area                  │
│ Sidebar    │ ┌────────────────────────────────┐ │
│            │ │ Page Header                    │ │
│ Dashboard  │ ├────────────────────────────────┤ │
│ Sources    │ │                                │ │
│ Settings   │ │ Dynamic Content                │ │
│ Help       │ │                                │ │
│            │ └────────────────────────────────┘ │
└────────────┴────────────────────────────────────┘
```

### Mobile Layout (<768px)
```
┌─────────────────────────────────┐
│ Mobile Header                   │
│ [≡] [Logo]           [User]     │
├─────────────────────────────────┤
│                                 │
│ Main Content                    │
│ (Full width, vertical scroll)   │
│                                 │
├─────────────────────────────────┤
│ Bottom Navigation               │
│ [Dashboard][Sources][Settings]  │
└─────────────────────────────────┘
```

### Navigation Hierarchy
- **Level 1:** Dashboard (default), Sources, Settings
- **Level 2:** Settings → Profile, Reports, Brand, Security
- **Level 3:** Modals → Review Detail, Add Source, Paywall
- **Contextual:** Filters, Sort, Search (within views)

### Navigation Patterns
- **Deep linking:** All filter states in URL for bookmark/share
- **Breadcrumbs:** For nested settings views
- **Back navigation:** Consistent browser back button handling
- **Keyboard shortcuts:**
  - `G D` - Go to Dashboard
  - `G S` - Go to Sources
  - `/` - Focus search
  - `?` - Show shortcuts modal

## 5. Key Components

### Layout Components
- **AppShell:** Main container with sidebar/bottom nav
- **PageHeader:** Reusable header with title, actions, breadcrumbs
- **ContentContainer:** Responsive padding and max-width wrapper

### Data Display Components
- **MetricCard:** Display key metrics with trend
- **ReviewCard:** Single review with actions and metadata
- **SourceHealthCard:** Source status with sync controls
- **AIInsightCard:** Formatted AI summary with highlights

### Input Components
- **FilterBar:** Multi-select filters with chips
- **SearchInput:** With debouncing and clear button
- **DateRangePicker:** Date range selection for filters
- **SentimentSelector:** Radio group for sentiment override

### Feedback Components
- **LoadingSkeleton:** Placeholder during loading
- **EmptyState:** Illustration + message + CTA
- **ErrorBoundary:** Graceful error handling wrapper
- **Toast:** Non-blocking notifications
- **ProgressTracker:** Multi-step progress indicator

### Modal Components
- **ReviewDetailModal:** Full review display with actions
- **PaywallModal:** Upgrade prompt with pricing
- **ConfirmDialog:** Reusable confirmation modal
- **OnboardingWizard:** Multi-step guided setup

### Utility Components
- **InfiniteScroll:** Virtualized list for large datasets
- **Tooltip:** Contextual help tooltips
- **Badge:** Status indicators and counts
- **Dropdown:** Actions menu with keyboard support

### Charts & Visualizations
- **SentimentPieChart:** Sentiment distribution
- **RatingDistribution:** Rating histogram
- **TrendLine:** Trend chart over time (future)

Each component will be built with focus on:
- **Reusability:** Generic components with props configuration
- **Accessibility:** ARIA labels, keyboard navigation, focus management
- **Performance:** Memoization, lazy loading, code splitting
- **Responsiveness:** Mobile-first with breakpoint variants
- **Testability:** Single responsibility, mockable dependencies
