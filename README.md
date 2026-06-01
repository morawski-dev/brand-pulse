# BrandPulse

> Monitor and analyze customer reviews from multiple sources with AI-powered sentiment analysis

**BrandPulse** is a SaaS web application that helps small and medium-sized service businesses (restaurants, hotels, beauty salons, etc.) aggregate, monitor, and analyze customer reviews from multiple platforms including Google, Facebook, and Trustpilot. The system uses AI to perform sentiment analysis and provides actionable insights through an intuitive dashboard.

**Business model:** Freemium — the free plan allows monitoring of one review source.

## Key Features

- **Multi-Source Review Aggregation** — collect reviews in one place (Google first; Facebook & Trustpilot planned)
- **AI-Powered Sentiment Analysis** — automatic classification as positive / negative / neutral
- **Smart Summarization** — AI-generated text summary per source
- **Interactive Dashboard** — filter reviews by source, sentiment, and star rating; "All locations" vs single-source view
- **Manual Corrections** — override AI sentiment classifications
- **Automated Data Refresh** — daily sync (3:00 AM CET) + manual refresh (24h cooldown)
- **Freemium Gate** — free tier limited to one review source

## Current Status

This is an actively developed MVP. High-level state:

| Area | Status |
|---|---|
| Auth (register / login / JWT) | ✅ Implemented (end-to-end) |
| Onboarding wizard (5 steps) | ✅ Implemented |
| Review import jobs (manual + daily CRON 3 AM) | ✅ Implemented (background poller) |
| Dashboard (metrics, sentiment %, AI summary, reviews) | ✅ Implemented |
| AI sentiment + summaries (OpenRouter) | ✅ Implemented **with graceful fallback** to a rating heuristic / local summary when no API key is configured |
| Google review fetching | ✅ **Google Places API (New)** via Place ID when `GOOGLE_PLACES_API_KEY` is set (else mock). ⚠️ Places returns **at most ~5 reviews/place, no history** — so the "90-day import" is not achievable without Business Profile |
| Facebook / Trustpilot | ⏳ Phase 2 |
| Weekly email reports | ⏳ Phase 2 |

## Tech Stack

### Backend (`backend/`)
- **Java 21** + **Spring Boot 3.3.6**
- **PostgreSQL 16** + **Spring Data JPA / Hibernate**
- **Liquibase** — schema migrations (enabled, `V001`–`V012`)
- **Spring Security** + **JWT** (jjwt) — stateless auth
- **springdoc-openapi** — Swagger UI / OpenAPI
- **Caffeine** — caching · **Spring Scheduling** — background sync jobs · **Spring Mail** — reports (Phase 2)
- **Lombok**

### Frontend (`frontend/`)
- **Next.js 14** (App Router) + **React 18** + **TypeScript 5**
- **Tailwind CSS 3** + **Radix UI** (shadcn/ui-style components) + **lucide-react** + **sonner**
- **Jest** + **Testing Library**

### External Integrations
- **OpenRouter.ai** — multiple models (OpenAI, Anthropic, Google) for per-review sentiment classification and source summarization. Works without a key via a local fallback (rating-based sentiment + statistics-based summary). Default model `anthropic/claude-sonnet-4.6`.
- **Google Places API (New)** — fetches reviews by Place ID (Place Details). Works without a key via mock data. Limited to ~5 reviews per place (no history) — the only option without a Google Business Profile.

### Infrastructure
- **Docker Compose** — full local stack (PostgreSQL + backend + frontend)
- **GitHub Actions** — CI (backend `verify`, frontend `lint` + `build`) and AWS Copilot deploy (`.github/workflows/main.yml`)

## Prerequisites

- **Java 21** (the build targets Java 21 — JDK 21 is required for the backend; newer JDKs may break Lombok/MapStruct annotation processing)
- **Maven** — provided via the Maven Wrapper (`backend/mvnw` / `backend/mvnw.cmd`)
- **Node.js 20+** and **npm** (for the frontend)
- **Docker** + **Docker Compose**
- **Git**

## Getting Started

### Option A — Full stack with Docker (quickest)

```bash
docker compose up -d            # starts postgres + backend + frontend
docker compose ps               # verify all are up
```

- Backend: http://localhost:8080
- Frontend: http://localhost:3000
- PostgreSQL: localhost:5432 (db `brandpulse`, user `brandpulse_user`, pass `brandpulse_pass`)

> **Note:** the frontend Docker image is built with `NEXT_PUBLIC_API_URL=http://backend:8080`, which is only reachable inside the Docker network. Because API calls are made **client-side** from the browser, the containerized frontend cannot reach the backend from your host browser. For local UI work use **Option B** (frontend dev server), or rebuild the frontend image with a host-reachable API URL. Fixing this for real deployments is a tracked follow-up.

### Option B — Backend in Docker, frontend in dev mode (recommended for UI work)

```bash
# 1. Start backend + database
docker compose up -d postgres backend

# 2. Run the frontend dev server (uses frontend/.env.local -> NEXT_PUBLIC_API_URL=http://localhost:8080)
cd frontend
npm install
npm run dev                     # http://localhost:3000
```

CORS on the backend allows `http://localhost:3000`, so the dev server talks to the backend directly.

### Option C — Run the backend from source (without its container)

```bash
docker compose up -d postgres   # database only

# Build & run the backend (JDK 21 required)
./backend/mvnw clean install -f backend/pom.xml
./backend/mvnw spring-boot:run -f backend/pom.xml
```

On Windows (PowerShell), set JDK 21 for the session if it isn't the default, e.g.:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Amazon Corretto\jdk21.0.4_7'
.\backend\mvnw.cmd spring-boot:run -f backend\pom.xml
```

### Verify

```bash
curl http://localhost:8080/actuator/health      # -> {"status":"UP", ...}
```

### (Optional) Configure AI and Google reviews

Both run with safe fallbacks out of the box (heuristic AI, mock Google reviews). To use the real services, copy `.env.example` to `.env` in the repo root (gitignored — docker-compose reads it automatically) and set:

```
# OpenRouter AI (https://openrouter.ai/keys) — without it: local fallback summary + rating heuristic
OPENROUTER_API_KEY=sk-or-...
# Optional model id (default anthropic/claude-sonnet-4.6); cheaper: anthropic/claude-3.5-haiku
# OPENROUTER_MODEL=anthropic/claude-sonnet-4.6

# Google Places API (New) — enable the API + billing in Google Cloud, create a key.
# Without it: mock review data. A review source's externalProfileId must be the Place ID.
GOOGLE_PLACES_API_KEY=AIza...
```

These map to Spring properties via env (`openrouter.api.key`, `google.places.api.key`). For a from-source run (Option C) export the same env vars in your shell. **Never commit real keys.**

**Using Google reviews:** find your business's **Place ID** with the [Place ID Finder](https://developers.google.com/maps/documentation/places/web-service/place-id), then paste it (e.g. `ChIJ...`) in the onboarding Google step. Note the hard Places API limit: **at most ~5 reviews per place, no history**.

## API Documentation

Interactive API docs (springdoc-openapi) are available when the backend is running:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/api-docs

Public endpoints (no auth): `/api/auth/register`, `/api/auth/login`, `/api/auth/forgot-password`, `/api/auth/reset-password`, `/actuator/**`, Swagger/OpenAPI. All other `/api/**` endpoints require a `Authorization: Bearer <JWT>` header.

Main endpoint groups: `/api/auth/*`, `/api/users/me`, `/api/brands`, `/api/brands/{brandId}/review-sources`, `/api/sources/{sourceId}/import-status`, `/api/brands/{brandId}/reviews`, `/api/brands/{brandId}/sync`, `/api/sync-jobs/{jobId}`, `/api/dashboard/summary`.

## Running Tests

```bash
# Backend (JDK 21). Integration tests use Testcontainers, so Docker must be running.
./backend/mvnw test -f backend/pom.xml
./backend/mvnw test -Dtest=ClassName -f backend/pom.xml
./backend/mvnw clean package -DskipTests -f backend/pom.xml

# Frontend
cd frontend
npm test
npm run lint
npm run build
```

> The full backend test suite is green (312 tests). Integration tests use Testcontainers PostgreSQL, so Docker must be running.

## Project Structure

```
brand-pulse/
├── backend/                         # Spring Boot backend (Java 21)
│   ├── src/main/java/com/morawski/dev/backend/
│   │   ├── config/      controller/   dto/        entity/
│   │   ├── repository/  service/      mapper/     security/
│   │   ├── scheduler/   exception/    util/
│   │   └── BackendApplication.java
│   ├── src/main/resources/
│   │   ├── db/changelog/            # Liquibase migrations (V001–V012)
│   │   └── application.properties
│   ├── src/test/                    # unit + Testcontainers integration tests
│   ├── Dockerfile
│   └── pom.xml / mvnw
├── frontend/                        # Next.js 14 app (App Router, TypeScript)
│   ├── app/                         # routes: /, login, register, onboarding, dashboard, ...
│   ├── components/  context/  hooks/  lib/        # UI, AuthContext, hooks, API client + types
│   ├── Dockerfile
│   └── package.json
├── docker-compose.yml               # postgres + backend + frontend
└── README.md
```

## Configuration

Backend config lives in `backend/src/main/resources/application.properties`:

- **Database** — PostgreSQL connection (overridable via `SPRING_DATASOURCE_*` env vars)
- **Liquibase** — enabled; migrations run on startup
- **Security/JWT** — `jwt.secret`, `jwt.expiration-minutes` (use a real secret in production)
- **CORS** — allows `http://localhost:3000` and `https://app.brandpulse.io`
- **Cache** — Caffeine (named caches: `dashboard`, `summaries`, `reviews`, `sources`, `brands`)
- **Mail** — SMTP placeholders (mail health contributor disabled in dev)
- **AI** — OpenRouter URL / key / model

Active Spring profile defaults to `dev` (the backend container sets it explicitly). Frontend API base URL: `frontend/.env.local` (`NEXT_PUBLIC_API_URL`).

## Success Metrics (MVP)

- **Time to Value:** 90% of users configure their first source within 10 minutes
- **Sentiment Accuracy:** ≥75% agreement between AI and manual classification
- **Activation Rate:** 60% of new users configure a source within 7 days
- **Retention:** 35% log in at least 3 times in the first 4 weeks

## License

Proprietary software. All rights reserved.

---

**Built with ❤️ for small and medium-sized businesses**
