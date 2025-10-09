# Tech Stack Overview

## Frontend
- **Next.js 14** enables building fast, efficient websites and applications with minimal JavaScript.
- **React 18** provides interactivity where needed.
- **TypeScript 5** ensures static typing and better IDE support.
- **Tailwind 3** allows convenient application styling.
- **Shadcn/ui** offers an accessible React component library that we’ll use as the foundation of our UI.

## Backend
- **Java 21** for business logic.
- **Spring Boot 3** as the framework for building REST APIs.
- **PostgreSQL** as the database.
- **Spring Web** – REST API (including WebClient for external services).
- **Spring Data JPA** – data layer.
- **PostgreSQL Driver** – database driver.
- **Validation (Jakarta)** – DTO validation (the MVP includes many forms).
- **Spring Security** – basic authentication (token/JWT to be added as a library).
- **Spring Boot Actuator** – health/metrics for CI/CD and monitoring.
- **Scheduling** – CRON 3:00 CET for refreshing reviews.
- **Liquibase** – database schema migrations.
- **Spring Cache (with Caffeine as provider)** – fast dashboard (<4 s).
- **Mail** – weekly email reports.
- **Lombok** – reduces boilerplate code.
- **Spring Boot DevTools** – convenient local development.
- **OpenAPI/Swagger:** springdoc-openapi-starter-webmvc-ui
- **JWT:** jjwt-api, jjwt-impl, jjwt-jackson (or auth0/java-jwt)
- **MapStruct:** mapping between DTO ↔ entity
- **Micrometer + Prometheus:** metrics for monitoring
- **Spring Retry:** resilience against flaky APIs (scraping / external)
- **Testcontainers:** postgresql, junit-jupiter (stable integration tests)
- **Feign** (optional, instead of plain WebClient): simpler HTTP client
- **OAuth2 Client** (for official Google Business Profile API)

## AI
- Communication with models via the **Openrouter.ai** service.
- Access to a wide range of models (OpenAI, Anthropic, Google, and many others), allowing us to find a solution that provides high efficiency and low cost.
- Supports setting financial limits for API keys.

## CI/CD and Hosting
- **GitHub Actions** for creating CI/CD pipelines.
- **AWS** for hosting the application using docker-compose and Docker images.  
