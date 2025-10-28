# BrandPulse Frontend

Landing Page aplikacji BrandPulse zbudowany w Next.js 14 z TypeScript, Tailwind CSS i Shadcn UI.

## Tech Stack

- **Next.js 14** - React framework z App Router
- **TypeScript** - typowanie statyczne
- **Tailwind CSS** - utility-first CSS framework
- **Shadcn UI** - komponenty UI oparte na Radix UI
- **Lucide React** - ikony

## Wymagania

- Node.js 18.17 lub nowszy
- npm lub yarn

## Instalacja

```bash
# Zainstaluj zależności
npm install

# Uruchom serwer deweloperski
npm run dev
```

Aplikacja będzie dostępna pod adresem [http://localhost:3000](http://localhost:3000).

## Dostępne komendy

```bash
# Serwer deweloperski z hot reload
npm run dev

# Produkcyjny build
npm run build

# Start produkcyjnej wersji
npm start

# Linting
npm run lint
```

## Struktura projektu

```
frontend/
├── app/                          # Next.js App Router
│   ├── layout.tsx               # Root layout z SEO metadata
│   ├── page.tsx                 # Landing Page (/)
│   └── globals.css              # Globalne style
│
├── components/
│   ├── landing/                 # Komponenty landing page
│   │   ├── hero-section.tsx    # Hero z animated metrics
│   │   ├── features-section.tsx # Grid funkcji produktu
│   │   ├── feature-card.tsx    # Karta pojedynczej funkcji
│   │   ├── benefits-section.tsx # Korzyści biznesowe
│   │   ├── benefit-card.tsx    # Karta korzyści
│   │   ├── pricing-section.tsx # Sekcja cennikowa
│   │   ├── pricing-card.tsx    # Karta planu cenowego
│   │   ├── cta-section.tsx     # Finalna sekcja CTA
│   │   ├── sticky-mobile-cta.tsx # Sticky CTA na mobile
│   │   ├── animated-metric.tsx  # Animowany licznik
│   │   └── demo-preview/       # Interaktywne demo
│   │       ├── demo-preview.tsx     # Główny kontener
│   │       ├── demo-controls.tsx    # Filtry
│   │       ├── demo-stats.tsx       # Statystyki
│   │       ├── demo-review-list.tsx # Lista recenzji
│   │       └── demo-review-card.tsx # Karta recenzji
│   │
│   ├── layout/
│   │   └── navigation.tsx       # Globalna nawigacja
│   │
│   └── ui/                      # Shadcn UI components
│       ├── button.tsx
│       ├── card.tsx
│       └── badge.tsx
│
├── lib/
│   ├── types/
│   │   └── landing.ts           # TypeScript typy
│   ├── constants/
│   │   └── landing-content.ts   # Mock data i content
│   ├── utils/
│   │   └── demo-helpers.ts      # Funkcje pomocnicze
│   └── utils.ts                 # cn() helper
│
├── hooks/
│   ├── use-scroll-animation.ts  # Intersection Observer
│   ├── use-count-up.ts          # Animacja licznika
│   └── use-sticky-position.ts   # Tracking scroll position
│
└── public/                      # Pliki statyczne
```

## Features Landing Page

### Sekcje

1. **Hero Section**
   - Główny value proposition
   - 2 CTA buttons (Start Free, See Demo)
   - 3 animated metrics z licznikami

2. **Features Section**
   - Grid 6 kart funkcji produktu
   - Ikony Lucide React
   - Staggered animations on scroll

3. **Benefits Section**
   - 3 korzyści biznesowe
   - Target audience badges (Small Business, Chain, All)

4. **Demo Preview** (Interaktywne)
   - Filtry: Source (Google/Facebook/Trustpilot), Sentiment, Rating
   - Real-time statistics (avg rating, total, sentiment %)
   - Lista 5 przykładowych recenzji
   - Empty state gdy brak wyników

5. **Pricing Section**
   - 2 plany: Free i Pro (Coming Soon)
   - Feature lists i limitations
   - "Most Popular" badge

6. **CTA Section**
   - Finalna zachęta do rejestracji
   - Gradient background

7. **Sticky Mobile CTA**
   - Pokazuje się po scrollu >300px
   - Tylko na mobile (<768px)

### Custom Hooks

#### useScrollAnimation
Intersection Observer do triggerowania animacji przy scrollu.

```typescript
const { ref, isInView } = useScrollAnimation({ threshold: 0.1 });
```

#### useCountUp
Animacja licznika od 0 do target value z easing.

```typescript
const { currentValue, isAnimating } = useCountUp({
  end: 90,
  duration: 2000,
  start: true
});
```

#### useStickyPosition
Tracking pozycji scrollu dla sticky elementów.

```typescript
const { isVisible, scrollY } = useStickyPosition({ threshold: 300 });
```

## SEO

### Metadata
- Title: "BrandPulse - Monitor Customer Reviews in One Place"
- Description z keywords
- Open Graph tags (Facebook/LinkedIn)
- Twitter Cards
- Canonical URL
- metadataBase dla proper URL resolution

### Structured Data
Schema.org JSON-LD:
- Type: SoftwareApplication
- Offers (Free plan)
- AggregateRating
- FeatureList

## Responsywność

Breakpoints (Tailwind):
- **Mobile**: < 768px (sm)
- **Tablet**: 768px - 1024px (md)
- **Desktop**: ≥ 1024px (lg)

Mobile-first design z responsive grid layouts.

## Accessibility

- Semantic HTML (`<main>`, `<section>`, `<article>`, `<nav>`)
- ARIA labels dla interactive elements
- Keyboard navigation support
- Focus indicators
- `prefers-reduced-motion` support w animacjach
- Touch targets ≥44px na mobile

## Performance

### Metrics (Lighthouse targets)
- LCP (Largest Contentful Paint): < 2.5s
- FID (First Input Delay): < 100ms
- CLS (Cumulative Layout Shift): < 0.1

### Optymalizacje
- Static Site Generation (SSG) dla landing page
- Next.js Image optimization (auto lazy load)
- Tree-shaking unused code
- CSS purging w production build
- Passive event listeners dla scroll

### Bundle Size
```
Route (app)              Size      First Load JS
┌ ○ /                    167 kB    263 kB
```

## Routing

- `/` - Landing Page (app/page.tsx)
- `/login` - Login page (TODO)
- `/register` - Registration page (TODO)

## Mock Data

Przykładowe dane znajdują się w `lib/constants/landing-content.ts`:
- `HERO_METRICS` - 3 metryki hero section
- `FEATURES` - 6 funkcji produktu
- `BENEFITS` - 3 korzyści biznesowe
- `MOCK_REVIEWS` - 12 przykładowych recenzji
- `PRICING_PLANS` - 2 plany cenowe

## Development Notes

### Icon Handling
Ikony Lucide React są przekazywane jako stringi (np. "BarChart3") i mapowane dynamicznie w komponentach:

```typescript
import * as Icons from 'lucide-react';
const Icon = (Icons as any)[iconName] || Icons.HelpCircle;
```

To rozwiązanie pozwala uniknąć błędu "Functions cannot be passed to Client Components".

### Client Components
Komponenty wymagające interaktywności muszą mieć directive `'use client'`:
- Wszystkie komponenty używające hooks (useState, useEffect, custom hooks)
- Komponenty z event handlers (onClick, onChange)
- Komponenty z animacjami

### Server Components
Domyślnie wszystkie komponenty w Next.js 14 są Server Components:
- `app/page.tsx` - landing page
- Sekcje bez interaktywności mogą być server components

## Future Enhancements

1. **A11y**
   - Pełny audit WCAG AA
   - Screen reader testing
   - Skip links

2. **Performance**
   - Image optimization (dodanie og-image.png)
   - Font optimization
   - Dynamic imports dla ciężkich komponentów

3. **Analytics**
   - Google Analytics 4
   - Event tracking dla CTA clicks
   - Heatmaps (Hotjar)

4. **Testing**
   - Unit tests (Vitest)
   - Component tests (React Testing Library)
   - E2E tests (Playwright)

5. **Content Management**
   - Integracja z CMS (Contentful/Sanity)
   - i18n support (multiple languages)

## Troubleshooting

### Build errors

```bash
# Clear cache i reinstall
rm -rf .next node_modules
npm install
npm run build
```

### Port already in use

```bash
# Windows
netstat -ano | findstr :3000
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:3000 | xargs kill -9
```

## Contributing

1. Create feature branch
2. Make changes
3. Run lint: `npm run lint`
4. Test build: `npm run build`
5. Create pull request

## License

Copyright © 2025 BrandPulse. All rights reserved.