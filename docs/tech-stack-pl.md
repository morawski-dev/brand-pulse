# Stos Technologiczny

## Frontend
- **Next.js 14** - pozwala na tworzenie szybkich, wydajnych stron i aplikacji z minimalną ilością JavaScript
- **React 18** - zapewni interaktywność tam, gdzie jest potrzebna
- **TypeScript 5** - dla statycznego typowania kodu i lepszego wsparcia IDE
- **Tailwind 3** - pozwala na wygodne stylowanie aplikacji
- **Shadcn/ui** - zapewnia bibliotekę dostępnych komponentów React, na których oprzemy UI

## Backend
- **Java 21** - dla logiki biznesowej
- **Spring Boot 3** - framework do budowy REST API
- **Baza danych:** PostgreSQL
- **Spring Web** - REST API (w tym WebClient do zewnętrznych usług)
- **Spring Data JPA** - warstwa danych
- **PostgreSQL Driver** - sterownik bazy
- **Validation (jakarta)** - walidacja DTO (MVP ma sporo formularzy)
- **Spring Security** - podstawy auth (token/JWT do dodania jako lib)
- **Spring Boot Actuator** - health/metrics do CI/CD i monitoringu
- **Scheduling** - CRON 3:00 CET do odświeżania opinii
- **Liquibase** - migracje schematu DB
- **Spring Cache (Caffeine)** - szybki dashboard \<4s
- **Mail** - cotygodniowe raporty e-mail
- **Lombok** - mniej boilerplate'u
- **Spring Boot DevTools** - wygodny dev (lokalnie)
- **OpenAPI/Swagger:** springdoc-openapi-starter-webmvc-ui
- **JWT:** jjwt-api, jjwt-impl, jjwt-jackson (lub auth0/java-jwt)
- **MapStruct:** mapowanie DTO ↔ entity
- **Micrometer + Prometheus:** metryki do monitoringu
- **Spring Retry:** odporność na flaky API (scraping / zewnętrzne)
- **Testcontainers:** postgresql, junit-jupiter (stabilne testy integracyjne)
- **Feign (opcjonalnie):** prostszy klient HTTP
- **OAuth2 Client:** przy oficjalnym Google Business Profile API

# AI
- Komunikacja z modelami przez Openrouter.ai
- Dostęp do szerokiej gamy modeli (OpenAI, Anthropic, Google i wiele innych), które pozwolą znaleźć rozwiązanie zapewniające wysoką efektywność i niskie koszta
- Pozwala na ustawianie limitów finansowych na klucze API

# CI/CD i Hosting
- **GitHub Actions** - pipeline'y CI/CD
- **AWS** - hostowanie aplikacji przez docker-compose i obrazy Docker
