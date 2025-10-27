# Architektura UI dla BrandPulse

## 1. Przegląd struktury UI

BrandPulse będzie Single Page Application (SPA) zbudowaną w React z TypeScript, wykorzystującą shadcn/ui i Tailwind CSS. Architektura UI opiera się na modułowej strukturze feature-based z wyraźnym podziałem między publicznymi widokami (pre-auth) a chronionymi widokami aplikacji (post-auth). System nawigacji będzie adaptacyjny - sidebar dla desktop, bottom navigation dla mobile. Kluczowym elementem będzie progresywne ujawnianie funkcjonalności poprzez onboarding wizard i contextual tooltips.

## 2. Lista widoków

### Widoki publiczne (Pre-Authentication)

#### Landing Page
- **Ścieżka widoku:** `/`
- **Główny cel:** Prezentacja wartości produktu i konwersja odwiedzających
- **Kluczowe informacje do wyświetlenia:**
  - Hero section z value proposition
  - Lista kluczowych funkcjonalności
  - Sekcja korzyści dla biznesu
  - Demo preview z przykładowymi danymi
  - Pricing preview (freemium model)
  - CTA do rejestracji i demo
- **Kluczowe komponenty widoku:**
  - Hero banner z animowanymi metrykami
  - Feature cards z ikonami
  - Interactive demo preview
  - Testimonials carousel (przyszłość)
  - Sticky CTA bar na mobile
- **UX, dostępność i względy bezpieczeństwa:**
  - Lazy loading dla obrazów
  - Semantic HTML dla SEO
  - ARIA labels dla interaktywnych elementów
  - CSP headers dla external resources
  - Minimum 44px touch targets na mobile

#### Login Page
- **Ścieżka widoku:** `/login`
- **Główny cel:** Autentykacja istniejących użytkowników
- **Kluczowe informacje do wyświetlenia:**
  - Formularz logowania (email, hasło)
  - Link do odzyskiwania hasła
  - Link do rejestracji
  - Opcja "Zapamiętaj mnie"
  - Informacja o demo mode
- **Kluczowe komponenty widoku:**
  - Form z walidacją real-time
  - Password visibility toggle
  - Loading state podczas logowania
  - Error toast dla błędnych danych
  - Success redirect handler
- **UX, dostępność i względy bezpieczeństwa:**
  - Autofocus na pierwszym polu
  - Keyboard navigation support
  - Clear error messages
  - Rate limiting feedback
  - Secure password handling (no autocomplete w publicznych miejscach)

#### Registration Page
- **Ścieżka widoku:** `/register`
- **Główny cel:** Utworzenie nowego konta użytkownika
- **Kluczowe informacje do wyświetlenia:**
  - Formularz rejestracji (email, hasło, powtórz hasło)
  - Password strength indicator
  - Zgody (regulamin, polityka prywatności)
  - Info o darmowym planie
  - Link do logowania
- **Kluczowe komponenty widoku:**
  - Multi-field form z progressive disclosure
  - Password strength meter
  - Checkbox agreements
  - Submit button z loading state
  - Success modal z next steps
- **UX, dostępność i względy bezpieczeństwa:**
  - Client-side validation z debouncing
  - Clear password requirements
  - GDPR-compliant consent collection
  - Email verification process
  - Prevention of duplicate accounts

#### Password Recovery Page
- **Ścieżka widoku:** `/forgot-password`
- **Główny cel:** Inicjalizacja procesu odzyskiwania hasła
- **Kluczowe informacje do wyświetlenia:**
  - Pole email
  - Instrukcje procesu
  - Link powrotu do logowania
  - Success/error feedback
- **Kluczowe komponenty widoku:**
  - Simple email form
  - Loading indicator
  - Success message component
  - Rate limit warning
- **UX, dostępność i względy bezpieczeństwa:**
  - Clear instructions
  - Rate limiting (max 3 attempts per hour)
  - Generic success message (security)
  - Email verification before sending

#### Demo Mode
- **Ścieżka widoku:** `/demo`
- **Główny cel:** Interaktywna prezentacja możliwości aplikacji
- **Kluczowe informacje do wyświetlenia:**
  - Przykładowy dashboard z danymi demo
  - Wszystkie funkcje aktywne (read-only)
  - Floating tutorial tooltips
  - CTA do rejestracji
- **Kluczowe komponenty widoku:**
  - Full dashboard layout
  - Demo data generator
  - Interactive tour overlay
  - Persistent CTA banner
- **UX, dostępność i względy bezpieczeństwa:**
  - No real data exposure
  - Clear "Demo Mode" indicators
  - Limited session duration (30 min)
  - Conversion tracking

### Widoki chronione (Post-Authentication)

#### Onboarding Wizard
- **Ścieżka widoku:** `/onboarding`
- **Główny cel:** Szybka konfiguracja pierwszego źródła opinii
- **Kluczowe informacje do wyświetlenia:**
  - Step 1: Nazwa i typ marki
  - Step 2: Wybór typu źródła (Google/Facebook/Trustpilot)
  - Step 3: Podanie URL źródła
  - Step 4: Weryfikacja i import danych
  - Progress indicator
- **Kluczowe komponenty widoku:**
  - Multi-step form wizard
  - Progress bar
  - Source type selector cards
  - URL input z walidacją
  - Import progress tracker
  - Skip option (dla późniejszej konfiguracji)
- **UX, dostępność i względy bezpieczeństwa:**
  - Clear step indicators
  - Back navigation between steps
  - Save progress in localStorage
  - URL validation before submit
  - Clear error messages for invalid URLs

#### Main Dashboard
- **Ścieżka widoku:** `/dashboard`
- **Główny cel:** Centralny hub do przeglądania i zarządzania opiniami
- **Kluczowe informacje do wyświetlenia:**
  - Metrics summary (średnia ocena, sentyment, trendy)
  - AI-generated insights
  - Filtry (źródło, sentyment, ocena, data)
  - Lista opinii z infinite scroll
  - Quick actions dla każdej opinii
- **Kluczowe komponenty widoku:**
  - Sticky header z brand selector
  - Metrics cards grid
  - AI summary card
  - Filter bar z chips
  - Virtualized review list
  - Review card z actions
  - Empty states
  - Loading skeletons
- **UX, dostępność i względy bezpieczeństwa:**
  - Virtualization dla wydajności (>100 opinii)
  - Keyboard shortcuts (J/K dla nawigacji)
  - ARIA live regions dla updates
  - Optimistic UI dla sentiment changes
  - Deep linking przez URL params

#### Review Detail Modal
- **Ścieżka widoku:** Modal overlay na `/dashboard`
- **Główny cel:** Szczegółowy widok pojedynczej opinii
- **Kluczowe informacje do wyświetlenia:**
  - Pełna treść opinii
  - Metadata (autor, data, źródło)
  - Sentyment AI z confidence score
  - Opcja korekty sentymentu
  - Link do oryginalnej opinii
- **Kluczowe komponenty widoku:**
  - Modal with backdrop
  - Review content viewer
  - Sentiment selector
  - Action buttons
  - Navigation arrows (prev/next)
- **UX, dostępność i względy bezpieczeństwa:**
  - Focus trap w modalu
  - ESC key to close
  - Swipe gestures na mobile
  - Maintain scroll position

#### Sources Management
- **Ścieżka widoku:** `/sources`
- **Główny cel:** Zarządzanie źródłami opinii
- **Kluczowe informacje do wyświetlenia:**
  - Lista aktywnych źródeł
  - Health status każdego źródła
  - Ostatnia synchronizacja
  - Liczba opinii per źródło
  - Opcje zarządzania
- **Kluczowe komponenty widoku:**
  - Source cards grid
  - Health indicator badges
  - Sync button z rate limiting
  - Add source button (z paywall dla 2+)
  - Edit/delete dropdowns
  - Error state alerts
- **UX, dostępność i względy bezpieczeństwa:**
  - Clear sync status indicators
  - Confirmation modals dla delete
  - Rate limit countdown timer
  - Paywall modal dla premium features

#### Settings Hub
- **Ścieżka widoku:** `/settings/*`
- **Główny cel:** Centralne zarządzanie ustawieniami
- **Kluczowe informacje do wyświetlenia:**
  - Profile settings
  - Email notifications
  - Brand configuration
  - Security settings
  - Billing info (przyszłość)
- **Kluczowe komponenty widoku:**
  - Settings sidebar navigation
  - Form sections z auto-save
  - Toggle switches
  - Save indicators
  - Danger zone dla delete account
- **UX, dostępność i względy bezpieczeństwa:**
  - Auto-save z debouncing
  - Clear success feedback
  - Two-factor auth setup (przyszłość)
  - Data export options

#### Email Report Settings
- **Ścieżka widoku:** `/settings/reports`
- **Główny cel:** Konfiguracja cotygodniowych raportów
- **Kluczowe informacje do wyświetlenia:**
  - Częstotliwość raportów
  - Dzień tygodnia i godzina
  - Zakres danych w raporcie
  - Preview ostatniego raportu
- **Kluczowe komponenty widoku:**
  - Schedule selector
  - Content checkboxes
  - Preview modal
  - Test email button
- **UX, dostępność i względy bezpieczeństwa:**
  - Timezone handling
  - Email delivery confirmation
  - Unsubscribe link compliance

### Stany specjalne

#### Empty States
- **Główny cel:** Informowanie i kierowanie użytkownika gdy brak danych
- **Warianty:**
  - No sources configured
  - No reviews yet
  - No results for filters
  - Import in progress
- **Kluczowe komponenty:**
  - Ilustracja
  - Descriptive message
  - CTA button
  - Help links

#### Error Pages
- **Główny cel:** Graceful error handling
- **Warianty:**
  - 404 Not Found
  - 500 Server Error
  - 403 Forbidden
  - Network offline
- **Kluczowe komponenty:**
  - Error illustration
  - User-friendly message
  - Recovery actions
  - Support contact

## 3. Mapa podróży użytkownika

### Przepływ nowego użytkownika (First-Time Experience)
1. **Discovery:** Landing Page → czyta wartość propozycji → klika "Wypróbuj demo"
2. **Trial:** Demo Mode → eksploruje funkcjonalności → klika "Załóż konto"
3. **Registration:** Registration Page → wypełnia formularz → potwierdza email
4. **Onboarding:** 
   - Step 1: Podaje nazwę marki
   - Step 2: Wybiera Google jako pierwsze źródło
   - Step 3: Wkleja link do Google Business Profile
   - Step 4: Obserwuje import danych (progress bar)
5. **First Value:** Dashboard z pierwszymi opiniami → tutorial tooltips → filtruje negatywne
6. **Engagement:** Koryguje błędny sentyment → sprawdza AI insights → konfiguruje raport email

### Przepływ powracającego użytkownika (Daily Operations)
1. **Quick Access:** Login → Dashboard (bookmark)
2. **Daily Review:**
   - Sprawdza nowe opinie (badge notification)
   - Sortuje po dacie
   - Filtruje negatywne (quick filter)
   - Czyta AI summary
3. **Actions:**
   - Koryguje sentyment gdzie potrzeba
   - Sprawdza source health
   - Ręczny sync jeśli potrzebny
4. **Weekly:** Otrzymuje i przegląda email report

### Przepływ zarządzania źródłami
1. **Add Source:** Sources → Add Source → Select Type → Enter URL → Validate → Import
2. **Monitor:** Dashboard → Source Filter → View source-specific metrics
3. **Maintain:** Sources → Check health → Sync if needed → Edit if broken
4. **Scale:** Try add 2nd source → See paywall → (Future: upgrade to paid)

### Przepływ reagowania na kryzys
1. **Alert:** Email notification o negative spike
2. **Investigate:** Login → Dashboard → Filter negative + recent
3. **Analyze:** Read reviews → Check AI insights → Identify patterns
4. **Action:** Export data → Share with team → Plan response

## 4. Układ i struktura nawigacji

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

### Hierarchia nawigacji
- **Poziom 1:** Dashboard (default), Sources, Settings
- **Poziom 2:** Settings → Profile, Reports, Brand, Security
- **Poziom 3:** Modals → Review Detail, Add Source, Paywall
- **Contextual:** Filters, Sort, Search (within views)

### Wzorce nawigacji
- **Deep linking:** Wszystkie stany filtrów w URL dla bookmark/share
- **Breadcrumbs:** Dla zagnieżdżonych widoków settings
- **Back navigation:** Spójna obsługa browser back button
- **Keyboard shortcuts:** 
  - `G D` - Go to Dashboard
  - `G S` - Go to Sources
  - `/` - Focus search
  - `?` - Show shortcuts modal

## 5. Kluczowe komponenty

### Layout Components
- **AppShell:** Główny container z sidebar/bottom nav
- **PageHeader:** Reusable header z title, actions, breadcrumbs
- **ContentContainer:** Responsive padding i max-width wrapper

### Data Display Components
- **MetricCard:** Wyświetlanie kluczowych metryk z trendem
- **ReviewCard:** Pojedyncza opinia z actions i metadata
- **SourceHealthCard:** Status źródła z sync controls
- **AIInsightCard:** Formatted AI summary z highlights

### Input Components
- **FilterBar:** Multi-select filters z chips
- **SearchInput:** Z debouncing i clear button
- **DateRangePicker:** Wybór zakresu dat dla filtrów
- **SentimentSelector:** Radio group dla sentiment override

### Feedback Components
- **LoadingSkeleton:** Placeholder podczas ładowania
- **EmptyState:** Ilustracja + message + CTA
- **ErrorBoundary:** Graceful error handling wrapper
- **Toast:** Non-blocking notifications
- **ProgressTracker:** Multi-step progress indicator

### Modal Components
- **ReviewDetailModal:** Full review display z actions
- **PaywallModal:** Upgrade prompt z pricing
- **ConfirmDialog:** Reusable confirmation modal
- **OnboardingWizard:** Multi-step guided setup

### Utility Components
- **InfiniteScroll:** Virtualized list dla dużych datasets
- **Tooltip:** Contextual help tooltips
- **Badge:** Status indicators i counts
- **Dropdown:** Actions menu z keyboard support

### Charts & Visualizations
- **SentimentPieChart:** Rozkład sentymentu
- **RatingDistribution:** Histogram ocen
- **TrendLine:** Wykres trendu w czasie (przyszłość)

Każdy komponent będzie zbudowany z myślą o:
- **Reusability:** Komponenty generyczne z props configuration
- **Accessibility:** ARIA labels, keyboard navigation, focus management
- **Performance:** Memoization, lazy loading, code splitting
- **Responsiveness:** Mobile-first z breakpoint variants
- **Testability:** Pojedyncza odpowiedzialność, mockable dependencies