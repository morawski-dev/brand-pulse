# Schemat Bazy Danych BrandPulse

## 1. Tabele

### users
Przechowuje informacje o kontach użytkowników do uwierzytelniania i autoryzacji.

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    plan_type VARCHAR(20) NOT NULL DEFAULT 'FREE' CHECK (plan_type IN ('FREE', 'PREMIUM')),
    max_sources_allowed INTEGER NOT NULL DEFAULT 1,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_token VARCHAR(255),
    password_reset_token VARCHAR(255),
    password_reset_expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);
```

**Indeksy:**
- `idx_users_email` ON (email) WHERE deleted_at IS NULL
- `idx_users_verification_token` ON (verification_token) WHERE verification_token IS NOT NULL
- `idx_users_password_reset_token` ON (password_reset_token) WHERE password_reset_token IS NOT NULL

---

### brands
Reprezentuje markę/firmę, którą zarządza użytkownik. MVP ogranicza do 1 marki na użytkownika, ale schemat wspiera 1:N dla przyszłej skalowalności.

```sql
CREATE TABLE brands (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    last_manual_refresh_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);
```

**Indeksy:**
- `idx_brands_user_id` ON (user_id) WHERE deleted_at IS NULL
- `idx_brands_last_manual_refresh` ON (last_manual_refresh_at) WHERE deleted_at IS NULL

---

### review_sources
Przechowuje skonfigurowane źródła opinii (Google, Facebook, Trustpilot) dla każdej marki.

```sql
CREATE TABLE review_sources (
    id BIGSERIAL PRIMARY KEY,
    brand_id BIGINT NOT NULL REFERENCES brands(id) ON DELETE CASCADE,
    source_type VARCHAR(20) NOT NULL CHECK (source_type IN ('GOOGLE', 'FACEBOOK', 'TRUSTPILOT')),
    profile_url TEXT NOT NULL,
    external_profile_id VARCHAR(255) NOT NULL,
    auth_method VARCHAR(20) NOT NULL CHECK (auth_method IN ('API', 'SCRAPING')),
    credentials_encrypted JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_sync_at TIMESTAMP WITH TIME ZONE,
    last_sync_status VARCHAR(20) CHECK (last_sync_status IN ('SUCCESS', 'FAILED', 'IN_PROGRESS')),
    last_sync_error TEXT,
    next_scheduled_sync_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (brand_id, source_type, external_profile_id)
);
```

**Indeksy:**
- `idx_review_sources_brand_id` ON (brand_id) WHERE deleted_at IS NULL
- `idx_review_sources_next_sync` ON (next_scheduled_sync_at) WHERE is_active = TRUE AND deleted_at IS NULL
- `idx_review_sources_active` ON (brand_id, is_active) WHERE deleted_at IS NULL

---

### reviews
Przechowuje pojedyncze opinie pobrane z różnych źródeł.

```sql
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    review_source_id BIGINT NOT NULL REFERENCES review_sources(id) ON DELETE CASCADE,
    external_review_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    author_name VARCHAR(255),
    rating SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    sentiment VARCHAR(20) NOT NULL CHECK (sentiment IN ('POSITIVE', 'NEGATIVE', 'NEUTRAL')),
    sentiment_confidence DECIMAL(5,4),
    published_at TIMESTAMP WITH TIME ZONE NOT NULL,
    fetched_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (review_source_id, external_review_id)
);
```

**Indeksy:**
- `idx_reviews_source_id` ON (review_source_id) WHERE deleted_at IS NULL
- `idx_reviews_published_at` ON (published_at DESC) WHERE deleted_at IS NULL
- `idx_reviews_sentiment` ON (review_source_id, sentiment) WHERE deleted_at IS NULL
- `idx_reviews_rating` ON (review_source_id, rating) WHERE deleted_at IS NULL
- `idx_reviews_negative` ON (review_source_id, published_at DESC) WHERE rating <= 2 AND deleted_at IS NULL (indeks częściowy dla US-005)
- `idx_reviews_content_hash` ON (content_hash) WHERE deleted_at IS NULL
- `idx_reviews_composite_filter` ON (review_source_id, sentiment, rating, published_at DESC) WHERE deleted_at IS NULL

---

### sentiment_changes
Tabela audytowa śledząca wszystkie modyfikacje sentymentu dla analizy dokładności i ulepszania modelu.

```sql
CREATE TABLE sentiment_changes (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    old_sentiment VARCHAR(20) NOT NULL CHECK (old_sentiment IN ('POSITIVE', 'NEGATIVE', 'NEUTRAL')),
    new_sentiment VARCHAR(20) NOT NULL CHECK (new_sentiment IN ('POSITIVE', 'NEGATIVE', 'NEUTRAL')),
    changed_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    change_reason VARCHAR(30) NOT NULL CHECK (change_reason IN ('AI_INITIAL', 'USER_CORRECTION', 'AI_REANALYSIS')),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

**Indeksy:**
- `idx_sentiment_changes_review_id` ON (review_id)
- `idx_sentiment_changes_user_id` ON (changed_by_user_id) WHERE changed_by_user_id IS NOT NULL
- `idx_sentiment_changes_reason` ON (change_reason, changed_at DESC)

---

### dashboard_aggregates
Wstępnie obliczone agregaty dla szybkiego ładowania dashboardu (wymóg <4s).

```sql
CREATE TABLE dashboard_aggregates (
    id BIGSERIAL PRIMARY KEY,
    review_source_id BIGINT NOT NULL REFERENCES review_sources(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    total_reviews INTEGER NOT NULL DEFAULT 0,
    avg_rating DECIMAL(3,2),
    positive_count INTEGER NOT NULL DEFAULT 0,
    negative_count INTEGER NOT NULL DEFAULT 0,
    neutral_count INTEGER NOT NULL DEFAULT 0,
    last_calculated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (review_source_id, date)
);
```

**Indeksy:**
- `idx_dashboard_aggregates_source_date` ON (review_source_id, date DESC)
- `idx_dashboard_aggregates_last_calc` ON (last_calculated_at)

---

### ai_summaries
Przechowuje podsumowania tekstowe wygenerowane przez AI dla źródeł opinii.

```sql
CREATE TABLE ai_summaries (
    id BIGSERIAL PRIMARY KEY,
    review_source_id BIGINT NOT NULL REFERENCES review_sources(id) ON DELETE CASCADE,
    summary_text TEXT NOT NULL,
    model_used VARCHAR(100),
    token_count INTEGER,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    valid_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

**Indeksy:**
- `idx_ai_summaries_source_id` ON (review_source_id, generated_at DESC)
- `idx_ai_summaries_valid` ON (review_source_id) WHERE valid_until > NOW()

---

### email_reports
Śledzi cotygodniowe raporty e-mail wysyłane do użytkowników dla metryk zaangażowania.

```sql
CREATE TABLE email_reports (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    report_type VARCHAR(30) NOT NULL CHECK (report_type IN ('WEEKLY_SUMMARY')),
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    opened_at TIMESTAMP WITH TIME ZONE,
    clicked_at TIMESTAMP WITH TIME ZONE,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    reviews_count INTEGER NOT NULL DEFAULT 0,
    new_negative_count INTEGER NOT NULL DEFAULT 0,
    email_provider_message_id VARCHAR(255),
    delivery_status VARCHAR(20) CHECK (delivery_status IN ('SENT', 'DELIVERED', 'BOUNCED', 'FAILED'))
);
```

**Indeksy:**
- `idx_email_reports_user_id` ON (user_id, sent_at DESC)
- `idx_email_reports_period` ON (period_start, period_end)
- `idx_email_reports_opened` ON (opened_at) WHERE opened_at IS NOT NULL

---

### user_activity_log
Śledzi aktywności użytkowników dla metryk sukcesu (Time to Value, Aktywacja, Retencja).

```sql
CREATE TABLE user_activity_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL CHECK (activity_type IN (
        'USER_REGISTERED',
        'LOGIN',
        'LOGOUT',
        'VIEW_DASHBOARD',
        'FILTER_APPLIED',
        'SENTIMENT_CORRECTED',
        'SOURCE_CONFIGURED',
        'SOURCE_ADDED',
        'SOURCE_DELETED',
        'MANUAL_REFRESH_TRIGGERED',
        'FIRST_SOURCE_CONFIGURED_SUCCESSFULLY'
    )),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    metadata JSONB
);
```

**Indeksy:**
- `idx_user_activity_user_id` ON (user_id, occurred_at DESC)
- `idx_user_activity_type` ON (activity_type, occurred_at DESC)
- `idx_user_activity_registration` ON (user_id, occurred_at) WHERE activity_type = 'USER_REGISTERED'
- `idx_user_activity_first_source` ON (user_id, occurred_at) WHERE activity_type = 'FIRST_SOURCE_CONFIGURED_SUCCESSFULLY'

---

### sync_jobs
Śledzi zadania synchronizacji do monitorowania i debugowania.

```sql
CREATE TABLE sync_jobs (
    id BIGSERIAL PRIMARY KEY,
    review_source_id BIGINT NOT NULL REFERENCES review_sources(id) ON DELETE CASCADE,
    job_type VARCHAR(20) NOT NULL CHECK (job_type IN ('SCHEDULED', 'MANUAL', 'INITIAL')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    reviews_fetched INTEGER DEFAULT 0,
    reviews_new INTEGER DEFAULT 0,
    reviews_updated INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

**Indeksy:**
- `idx_sync_jobs_source_id` ON (review_source_id, created_at DESC)
- `idx_sync_jobs_status` ON (status, created_at DESC) WHERE status IN ('PENDING', 'IN_PROGRESS')

---

## 2. Relacje

### Diagram Relacji Encji (Kardinalność)

```
users (1) ──────< (N) brands
brands (1) ──────< (N) review_sources
review_sources (1) ──────< (N) reviews
review_sources (1) ──────< (N) dashboard_aggregates
review_sources (1) ──────< (N) ai_summaries
review_sources (1) ──────< (N) sync_jobs
reviews (1) ──────< (N) sentiment_changes
users (1) ──────< (N) email_reports
users (1) ──────< (N) user_activity_log
users (1) ──────< (N) sentiment_changes (opcjonalne, do śledzenia kto zmienił)
```

**Kluczowe Relacje:**

1. **users → brands**: Jeden-do-Wielu (1:N)
   - MVP wymusza 1:1 na poziomie aplikacji przez `max_sources_allowed`
   - Schemat wspiera przyszłe plany multi-brand
   - CASCADE DELETE: Usunięcie użytkownika usuwa wszystkie jego marki

2. **brands → review_sources**: Jeden-do-Wielu (1:N)
   - Jedna marka może mieć wiele źródeł opinii (Google, Facebook, Trustpilot)
   - Darmowy plan ogranicza do 1 źródła przez logikę biznesową
   - CASCADE DELETE: Usunięcie marki usuwa wszystkie źródła

3. **review_sources → reviews**: Jeden-do-Wielu (1:N)
   - Każda opinia należy do dokładnie jednego źródła
   - CASCADE DELETE: Usunięcie źródła usuwa wszystkie opinie
   - Ograniczenie UNIQUE na (review_source_id, external_review_id) zapobiega duplikatom

4. **reviews → sentiment_changes**: Jeden-do-Wielu (1:N)
   - Śledzi pełną historię modyfikacji sentymentu
   - CASCADE DELETE: Usunięcie opinii usuwa historię zmian

5. **review_sources → dashboard_aggregates**: Jeden-do-Wielu (1:N)
   - Jeden rekord agregatu na źródło na dzień
   - Umożliwia szybkie ładowanie dashboardu przez wstępne obliczenia

---

## 3. Ograniczenia Bazy Danych

### Ograniczenia Unikalności

1. **users.email**: Zapewnia brak duplikatów adresów e-mail
2. **review_sources (brand_id, source_type, external_profile_id)**: Zapobiega duplikacji konfiguracji źródeł
3. **reviews (review_source_id, external_review_id)**: Zapobiega duplikacji opinii z tego samego zewnętrznego źródła
4. **dashboard_aggregates (review_source_id, date)**: Jeden agregat na źródło na dzień

### Ograniczenia CHECK

1. **users.plan_type**: Musi być 'FREE' lub 'PREMIUM'
2. **review_sources.source_type**: Musi być 'GOOGLE', 'FACEBOOK' lub 'TRUSTPILOT'
3. **review_sources.auth_method**: Musi być 'API' lub 'SCRAPING'
4. **reviews.rating**: Musi być pomiędzy 1 a 5
5. **reviews.sentiment**: Musi być 'POSITIVE', 'NEGATIVE' lub 'NEUTRAL'
6. **sentiment_changes (old_sentiment, new_sentiment, change_reason)**: Poprawne wartości enum

### Ograniczenia Kluczy Obcych

Wszystkie klucze obce są zdefiniowane z odpowiednimi akcjami ON DELETE:
- **CASCADE**: Używane tam, gdzie rekordy potomne powinny być usunięte wraz z rodzicem (brands → users, reviews → review_sources)
- **SET NULL**: Używane dla opcjonalnych referencji (sentiment_changes.changed_by_user_id)
- **RESTRICT**: Domyślne zachowanie, gdzie usunięcie powinno być uniemożliwione jeśli istnieją zależności

---

## 4. Strategia Indeksów

### Indeksy Optymalizacji Wydajności

**Indeksy o Wysokim Priorytecie (Krytyczne dla wymogu <4s dashboardu):**

1. **Indeks Złożony dla Filtrowania Dashboardu** (US-004, US-005):
   ```sql
   idx_reviews_composite_filter ON (review_source_id, sentiment, rating, published_at DESC)
   ```
   - Optymalizuje filtrowanie wielokryterialne w dashboardzie
   - Wspiera zagregowany widok "Wszystkie lokalizacje"

2. **Indeks Częściowy dla Negatywnych Opinii** (US-005):
   ```sql
   idx_reviews_negative ON (review_source_id, published_at DESC) WHERE rating <= 2
   ```
   - Optymalizuje najczęstszy przypadek użycia: filtrowanie opinii 1-2 gwiazdkowych
   - Mniejszy rozmiar indeksu = szybsze zapytania

3. **Wyszukiwanie Agregatów Dashboardu**:
   ```sql
   idx_dashboard_aggregates_source_date ON (review_source_id, date DESC)
   ```
   - Szybkie pobieranie wstępnie obliczonych metryk
   - Wspiera zapytania szeregów czasowych

**Indeksy Śledzenia Aktywności Użytkownika (Metryki Sukcesu):**

4. **Śledzenie Time to Value**:
   ```sql
   idx_user_activity_registration ON (user_id, occurred_at) WHERE activity_type = 'USER_REGISTERED'
   idx_user_activity_first_source ON (user_id, occurred_at) WHERE activity_type = 'FIRST_SOURCE_CONFIGURED_SUCCESSFULLY'
   ```
   - Umożliwia obliczanie czasu rejestracja → pierwsza konfiguracja źródła
   - Wspiera cel 90%: konfiguracja w ciągu 10 minut

5. **Analiza Aktywacji i Retencji**:
   ```sql
   idx_user_activity_user_id ON (user_id, occurred_at DESC)
   ```
   - Śledzi częstotliwość logowania dla metryki 35% retencji (3 logowania w 4 tygodnie)

**Indeksy Operacyjne:**

6. **Harmonogramowanie Zadań CRON**:
   ```sql
   idx_review_sources_next_sync ON (next_scheduled_sync_at) WHERE is_active = TRUE AND deleted_at IS NULL
   ```
   - Optymalizuje codzienne zadanie synchronizacji o 3:00 CET
   - Filtruje tylko aktywne, nieusuniete źródła

7. **Ograniczanie Częstotliwości Ręcznego Odświeżania**:
   ```sql
   idx_brands_last_manual_refresh ON (last_manual_refresh_at) WHERE deleted_at IS NULL
   ```
   - Wymusza 24-godzinne okno kroczące dla ręcznych odświeżeń (US-008)

8. **Zaangażowanie w Raporty E-mail**:
   ```sql
   idx_email_reports_opened ON (opened_at) WHERE opened_at IS NOT NULL
   ```
   - Śledzi zaangażowanie e-mail dla analizy retencji

---

## 5. Widoki Zmaterializowane (Opcjonalne Zwiększenie Wydajności)

Dla zagregowanego widoku "Wszystkie lokalizacje", rozważ widok zmaterializowany odświeżany podczas zadania CRON:

```sql
CREATE MATERIALIZED VIEW mv_brand_aggregates AS
SELECT
    b.id AS brand_id,
    b.user_id,
    COUNT(DISTINCT r.id) AS total_reviews,
    AVG(r.rating) AS avg_rating,
    SUM(CASE WHEN r.sentiment = 'POSITIVE' THEN 1 ELSE 0 END) AS positive_count,
    SUM(CASE WHEN r.sentiment = 'NEGATIVE' THEN 1 ELSE 0 END) AS negative_count,
    SUM(CASE WHEN r.sentiment = 'NEUTRAL' THEN 1 ELSE 0 END) AS neutral_count,
    MAX(r.published_at) AS last_review_date
FROM brands b
LEFT JOIN review_sources rs ON rs.brand_id = b.id AND rs.deleted_at IS NULL
LEFT JOIN reviews r ON r.review_source_id = rs.id AND r.deleted_at IS NULL
WHERE b.deleted_at IS NULL
GROUP BY b.id, b.user_id;

CREATE UNIQUE INDEX idx_mv_brand_aggregates_brand_id ON mv_brand_aggregates(brand_id);
CREATE INDEX idx_mv_brand_aggregates_user_id ON mv_brand_aggregates(user_id);
```

**Strategia Odświeżania:**
- Podczas codziennego zadania CRON (3:00 CET)
- Po zakończeniu ręcznego odświeżania
- Odświeżanie CONCURRENT aby uniknąć blokowania odczytów

---

## 6. Polityki Row-Level Security (RLS) PostgreSQL

**Decyzja Implementacyjna:** Podejście hybrydowe dla MVP
- **Główne bezpieczeństwo**: Spring Security z JWT (warstwa aplikacji)
- **Drugorzędna obrona**: PostgreSQL RLS (warstwa bazy danych) jako obrona w głąb

**Konfiguracja RLS:**

```sql
-- Włącz RLS na tabelach wielodostępnych
ALTER TABLE brands ENABLE ROW LEVEL SECURITY;
ALTER TABLE review_sources ENABLE ROW LEVEL SECURITY;
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE dashboard_aggregates ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_summaries ENABLE ROW LEVEL SECURITY;
ALTER TABLE email_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_activity_log ENABLE ROW LEVEL SECURITY;

-- Polityka dla brands: Użytkownicy mogą uzyskać dostęp tylko do swoich marek
CREATE POLICY brands_user_isolation ON brands
    FOR ALL
    USING (user_id = current_setting('app.current_user_id')::BIGINT);

-- Polityka dla review_sources: Dostęp przez własność marki
CREATE POLICY review_sources_user_isolation ON review_sources
    FOR ALL
    USING (
        brand_id IN (
            SELECT id FROM brands
            WHERE user_id = current_setting('app.current_user_id')::BIGINT
            AND deleted_at IS NULL
        )
    );

-- Polityka dla reviews: Dostęp przez łańcuch review_source → brand → user
CREATE POLICY reviews_user_isolation ON reviews
    FOR ALL
    USING (
        review_source_id IN (
            SELECT rs.id FROM review_sources rs
            JOIN brands b ON b.id = rs.brand_id
            WHERE b.user_id = current_setting('app.current_user_id')::BIGINT
            AND b.deleted_at IS NULL
            AND rs.deleted_at IS NULL
        )
    );

-- Polityka dla dashboard_aggregates: Dostęp przez własność review_source
CREATE POLICY dashboard_aggregates_user_isolation ON dashboard_aggregates
    FOR ALL
    USING (
        review_source_id IN (
            SELECT rs.id FROM review_sources rs
            JOIN brands b ON b.id = rs.brand_id
            WHERE b.user_id = current_setting('app.current_user_id')::BIGINT
            AND b.deleted_at IS NULL
            AND rs.deleted_at IS NULL
        )
    );

-- Polityka dla email_reports: Użytkownicy mogą uzyskać dostęp tylko do swoich raportów
CREATE POLICY email_reports_user_isolation ON email_reports
    FOR ALL
    USING (user_id = current_setting('app.current_user_id')::BIGINT);

-- Polityka dla user_activity_log: Użytkownicy mogą uzyskać dostęp tylko do swoich logów
CREATE POLICY user_activity_log_user_isolation ON user_activity_log
    FOR ALL
    USING (user_id = current_setting('app.current_user_id')::BIGINT);
```

**Integracja Spring Security:**

```java
// Ustaw kontekst użytkownika dla RLS w transakcji
@Transactional
public void executeWithUserContext(Long userId, Runnable operation) {
    jdbcTemplate.execute("SET LOCAL app.current_user_id = " + userId);
    operation.run();
}
```

---

## 7. Integralność i Walidacja Danych

### Walidacja na Poziomie Bazy Danych

1. **Deduplikacja Treści**:
   - Kolumna `content_hash` w tabeli reviews
   - Generowana jako hash SHA-256 treści opinii
   - Umożliwia wykrywanie zduplikowanej treści w różnych zewnętrznych ID

2. **Spójność Znaczników Czasu**:
   - Wszystkie znaczniki czasu używają `TIMESTAMP WITH TIME ZONE`
   - Obsługuje konwersję stref czasowych dla zadania CRON (3:00 CET) i wyświetlania użytkownikowi
   - Wspiera globalną ekspansję poza Polskę

3. **Wzorzec Miękkiego Usuwania**:
   - Kolumna `deleted_at` na wszystkich głównych encjach
   - Umożliwia ścieżkę audytu i odzyskiwanie danych
   - Filtrowane w indeksach używając `WHERE deleted_at IS NULL`

4. **Kolumny Audytu**:
   - `created_at`: Znacznik czasu utworzenia rekordu
   - `updated_at`: Znacznik czasu ostatniej modyfikacji (aktualizowany przez aplikację lub trigger)
   - Umożliwia śledzenie zmian i debugowanie

### Triggery dla Automatycznych Aktualizacji

```sql
-- Auto-aktualizacja znacznika czasu updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Zastosuj do odpowiednich tabel
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_brands_updated_at BEFORE UPDATE ON brands
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_review_sources_updated_at BEFORE UPDATE ON review_sources
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reviews_updated_at BEFORE UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

---

## 8. Kwestie Bezpieczeństwa

### Ochrona Danych Wrażliwych

1. **Przechowywanie Haseł**:
   - Kolumna `password_hash` przechowuje hasła zahashowane BCrypt
   - Nigdy nie przechowuj haseł w postaci zwykłego tekstu
   - Zalecane minimum 10 rund BCrypt

2. **Szyfrowanie Poświadczeń API**:
   - Kolumna JSONB `credentials_encrypted` przechowuje tokeny OAuth, klucze API
   - Szyfruj na poziomie aplikacji przed zapisaniem
   - Użyj AES-256 z kluczem przechowywanym w zmiennej środowiskowej lub menedżerze sekretów

3. **PII i Zgodność GDPR**:
   - Miękkie usuwanie umożliwia okno odzyskiwania danych
   - Twarde usuwanie po 90 dniach dla zgodności
   - Szyfrowanie e-mail opcjonalne w zależności od wymagań

### Zarządzanie Tokenami Uwierzytelniania

4. **Tokeny Resetowania Hasła**:
   - `password_reset_token` z znacznikiem czasu wygaśnięcia
   - Tokeny jednorazowe (czyszczone po pomyślnym resecie)
   - Indeks na tokenie dla szybkiego wyszukiwania

5. **Weryfikacja E-mail**:
   - `verification_token` dla potwierdzenia e-mail
   - Flaga boolean `email_verified`
   - Umożliwia ścieżkę uwierzytelniania dwuskładnikowego

---

## 9. Strategie Optymalizacji Wydajności

### Pooling Połączeń (Konfiguracja HikariCP)

```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### Strategia Cachowania (Spring Cache + Caffeine)

**Klucze Cache:**
1. Agregaty dashboardu: `dashboard:brand:{brandId}`
2. Podsumowania AI: `summary:source:{reviewSourceId}`
3. Liczby opinii: `counts:source:{reviewSourceId}`

**Unieważnianie Cache:**
- Przy imporcie nowej opinii
- Przy korekcie sentymentu (US-007)
- Przy ręcznym odświeżeniu (US-008)

### Wytyczne Optymalizacji Zapytań

1. **Unikaj Zapytań N+1**: Użyj JOIN FETCH w zapytaniach JPA
2. **Paginacja**: Implementuj paginację opartą na kursorze dla listy opinii
3. **Zapytania Projekcyjne**: Wybieraj tylko potrzebne kolumny dla widoków list
4. **Operacje Wsadowe**: Użyj wsadowych wstawień dla importu opinii (90-dniowa historia)

---

## 10. Archiwizacja i Retencja Danych

### Polityka Retencji

1. **Dane Aktywne**: Ostatnie 90 dni (zgodnie z wymaganiem PRD)
2. **Dane Historyczne**: 91-365 dni (możliwe do odpytania, ale depriorytetyzowane)
3. **Dane Zarchiwizowane**: >365 dni (przeniesione do zimnego magazynu lub usunięte)

### Strategia Archiwizacji

```sql
-- Partycjonuj tabelę reviews według published_at (przyszła optymalizacja)
CREATE TABLE reviews_2024_q4 PARTITION OF reviews
    FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');

CREATE TABLE reviews_2025_q1 PARTITION OF reviews
    FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');
```

### Zadania Czyszczenia

```sql
-- Miękkie usuwanie → twarde usuwanie po 90 dniach
DELETE FROM reviews
WHERE deleted_at < NOW() - INTERVAL '90 days';

-- Archiwizuj stare agregaty (opcjonalnie)
INSERT INTO dashboard_aggregates_archive
SELECT * FROM dashboard_aggregates
WHERE date < CURRENT_DATE - INTERVAL '1 year';
```

---

## 11. Strategia Migracji (Liquibase)

### Konwencja Nazewnictwa Plików Migracji

```
V001__create_users_table.sql
V002__create_brands_table.sql
V003__create_review_sources_table.sql
V004__create_reviews_table.sql
V005__create_sentiment_changes_table.sql
V006__create_dashboard_aggregates_table.sql
V007__create_ai_summaries_table.sql
V008__create_email_reports_table.sql
V009__create_user_activity_log_table.sql
V010__create_sync_jobs_table.sql
V011__create_indexes.sql
V012__create_triggers.sql
V013__enable_rls_policies.sql
```

### Możliwość Wycofania

Każda migracja powinna zawierać:
1. Migrację do przodu (UP)
2. Skrypt wycofania (DOWN)
3. Warunki wstępne (np. tabela nie istnieje)
4. Zapytania walidacyjne

---

## 12. Decyzje Projektowe i Uzasadnienie

### 1. Relacja User-Brand Odporność na Przyszłość (Schemat 1:N dla MVP 1:1)
**Decyzja**: Schemat wspiera 1:N pomimo ograniczenia MVP do 1 marki na użytkownika.
**Uzasadnienie**:
- Unika kosztownego refaktoringu schematu przy wprowadzaniu planów premium multi-brand
- Kolumna `max_sources_allowed` umożliwia stopniowe wdrażanie planów
- Wymuszanie logiki biznesowej w warstwie aplikacji zapewnia elastyczność

### 2. Złożone Ograniczenie Unikalności dla Deduplikacji Opinii
**Decyzja**: `UNIQUE (review_source_id, external_review_id)` + kolumna `content_hash`.
**Uzasadnienie**:
- Zapobiega duplikacji importów podczas ponownych prób lub ręcznych odświeżeń
- Hash treści wykrywa tę samą opinię z różnymi zewnętrznymi ID
- Gwarancja na poziomie bazy danych silniejsza niż sprawdzanie na poziomie aplikacji

### 3. Tabela Audytu Zmian Sentymentu
**Decyzja**: Oddzielna tabela dla historii modyfikacji sentymentu.
**Uzasadnienie**:
- Umożliwia pomiar dokładności (metryka 75% zgodności z PRD)
- Pętla sprzężenia zwrotnego uczenia maszynowego: identyfikacja systematycznych błędów AI
- Wspiera przyszłe funkcje: funkcjonalność "Cofnij", powiadomienia o zmianach

### 4. Wstępne Obliczanie Agregatów Dashboardu
**Decyzja**: Dedykowana tabela dla wstępnie obliczonych metryk, odświeżana podczas CRON + widoki zmaterializowane.
**Uzasadnienie**:
- Krytyczne dla wymogu ładowania dashboardu <4 sekundy
- Redukuje obliczenia w czasie rzeczywistym na dużych zbiorach danych
- Umożliwia efektywny zagregowany widok "Wszystkie lokalizacje" (US-006)

### 5. JSONB dla Elastycznego Przechowywania Poświadczeń
**Decyzja**: Kolumna JSONB `credentials_encrypted` zamiast oddzielnych kolumn.
**Uzasadnienie**:
- Każda platforma wymaga różnych danych uwierzytelniających (tokeny OAuth, klucze API, selektory scrapingu)
- Unika zmian schematu przy dodawaniu integracji Facebook/Trustpilot
- PostgreSQL JSONB zapewnia możliwości indeksowania i odpytywania

### 6. Wzorzec Miękkiego Usuwania z Okresowym Czyszczeniem
**Decyzja**: Znacznik czasu `deleted_at` na wszystkich głównych encjach + zadanie czyszczenia.
**Uzasadnienie**:
- Doświadczenie użytkownika: "Cofnij" usunięte źródła w okresie karencji
- Zgodność: 90-dniowa retencja dla ścieżki audytu (GDPR prawo do usunięcia)
- Integralność danych: Zapobiega kaskadowym usunięciom podczas rozwiązywania problemów

### 7. Indeks Częściowy dla Negatywnych Opinii
**Decyzja**: Indeks częściowy `WHERE rating <= 2` na reviews.
**Uzasadnienie**:
- Historia użytkownika US-005: Filtrowanie negatywnych opinii to podstawowy przypadek użycia
- Mniejszy indeks = szybsze zapytania + zmniejszone przechowywanie
- Zgodne z wartością biznesową: priorytetyzacja odpowiedzi na negatywny feedback

### 8. Znaczniki Czasu z Obsługą Stref Czasowych
**Decyzja**: Wszystkie znaczniki czasu używają `TIMESTAMP WITH TIME ZONE`.
**Uzasadnienie**:
- Zadanie CRON zaplanowane w CET, ale serwer może być w innej strefie czasowej
- Przyszła międzynarodowa ekspansja wymaga obsługi stref czasowych
- Unika błędów DST (Daylight Saving Time - czas letni)

### 9. Log Aktywności dla Metryk Produktowych
**Decyzja**: Szczegółowa tabela user_activity_log zamiast prostego śledzenia logowania.
**Uzasadnienie**:
- Mierzy metryki sukcesu PRD: Time to Value, Aktywacja, Retencja
- Umożliwia analitykę produktową: użycie funkcji, konwersja lejka
- Kolumna metadanych JSONB pozwala na elastyczne właściwości zdarzeń bez zmian schematu

### 10. Hybrydowy Model Bezpieczeństwa (Spring Security + RLS)
**Decyzja**: Główne bezpieczeństwo w aplikacji, opcjonalne RLS jako drugorzędna warstwa.
**Uzasadnienie**:
- Spring Security wystarczające dla MVP (zmniejsza złożoność)
- PostgreSQL RLS zapewnia obronę w głąb przed SQL injection, błędami logiki
- Może być wyłączone w MVP i włączone w produkcji przy minimalnym wpływie na wydajność

### 11. Tabela Zadań Synchronizacji dla Widoczności Operacyjnej
**Decyzja**: Oddzielna tabela śledząca każde zadanie synchronizacji.
**Uzasadnienie**:
- Debugowanie: Identyfikacja nieudanych importów, chwiejnych API
- Monitorowanie: Alerty przy kolejnych niepowodzeniach
- Rozliczenia: Śledzenie użycia API dla analizy kosztów (nierozwiązana kwestia PRD)

### 12. Tabela Podsumowań AI z Cachowaniem
**Decyzja**: Przechowuj podsumowania generowane przez AI z znacznikiem czasu `valid_until`.
**Uzasadnienie**:
- Redukuje koszty API OpenRouter.ai: cache podsumowań na 24 godziny
- Umożliwia historię podsumowań: śledź jak ewoluuje narracja sentymentu
- Kolumny `model_used` + `token_count` wspierają analizę optymalizacji kosztów

---

## 13. Otwarte Pytania i Przyszłe Kwestie

### 1. Wielolokacyjna Struktura Fizyczna
**Pytanie**: Czy "Wszystkie lokalizacje" oznacza wiele źródeł opinii na markę, czy wiele fizycznych lokalizacji?
**Wpływ**: Może potrzebować oddzielnej tabeli `locations` jeśli marka ma wiele fizycznych adresów.
**Decyzja MVP**: Zakładamy 1 źródło opinii = 1 lokalizacja. Ponowna analiza po MVP.

### 2. Granularność Śledzenia Kosztów API
**Pytanie**: Czy powinniśmy śledzić koszty na wywołanie API czy na zadanie synchronizacji?
**Obecne Podejście**: `token_count` w ai_summaries, ale brak szczegółowego śledzenia kosztów.
**Rekomendacja**: Dodaj tabelę `api_costs` po MVP z kolumnami: `service_provider`, `operation_type`, `cost_amount`, `timestamp`.

### 3. Tabele Infrastruktury Scrapingu
**Pytanie**: Czy potrzebujemy tabel dla logów zadań scrapingu, rotacji proxy, limitowania prędkości?
**Decyzja MVP**: Rozpocznij od logowania błędów w `sync_jobs.error_message`. Dodaj dedykowane tabele jeśli scraping stanie się główną metodą.

### 4. Workflow Odpowiedzi na Opinie
**Pytanie**: Przyszła funkcja (poza zakresem MVP) - czy schemat powinien przygotować się na przechowywanie wersji roboczych odpowiedzi?
**Rekomendacja**: Dodaj w Fazie 2: tabela `review_responses` z FK do reviews, status draft/published.

### 5. Powiadomienia w Czasie Rzeczywistym
**Pytanie**: Raporty e-mail śledzone, ale PRD wspomina o przyszłych alertach Slack/w czasie rzeczywistym.
**Rekomendacja**: Dodaj tabelę `notification_preferences` w Fazie 2 z kanałami (email, Slack, webhook).

### 6. Eksport Danych i Raportowanie
**Pytanie**: Eksport CSV/PDF poza zakresem MVP, ale czy powinniśmy się do tego przygotować?
**Rekomendacja**: Obecny schemat wystarczający. Dodaj tabelę `export_jobs` przy implementacji funkcji.

### 7. Zaawansowana Analityka i Trendy
**Pytanie**: PRD wyklucza wykresy/trendy dla MVP. Czy tabele szeregów czasowych będą potrzebne?
**Obecne Podejście**: `dashboard_aggregates` według daty wspiera podstawowe trendy.
**Rekomendacja**: Oceń rozszerzenie szeregów czasowych (TimescaleDB) jeśli analiza trendów stanie się główną funkcją.

---

## 14. Strategia Testowania

### Testowanie Bazy Danych z Testcontainers

```java
@Testcontainers
@SpringBootTest
class ReviewRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("brandpulse_test")
        .withUsername("test")
        .withPassword("test");

    @Test
    void shouldPreventDuplicateReviews() {
        // Test ograniczenia UNIQUE na (review_source_id, external_review_id)
    }

    @Test
    void shouldEnforceSoftDeleteInQueries() {
        // Test że filtry deleted_at IS NULL działają poprawnie
    }
}
```

### Testy Integralności Danych

1. **Testy Kaskady Kluczy Obcych**: Weryfikuj, że usunięcie marki usuwa review_sources i reviews
2. **Testy Naruszenia Ograniczeń**: Upewnij się, że ograniczenia CHECK zapobiegają niepoprawnym danym
3. **Testy Wydajności Indeksów**: Weryfikuj, że plany zapytań używają oczekiwanych indeksów
4. **Testy Polityk RLS**: Potwierdź, że użytkownicy nie mogą uzyskać dostępu do danych innych użytkowników

---

## 15. Monitorowanie i Obserwowalność

### Metryki Bazy Danych (Spring Boot Actuator + Micrometer)

1. **Metryki Puli Połączeń**: Aktywne połączenia, czas oczekiwania, timeouty
2. **Wydajność Zapytań**: Logi wolnych zapytań (>1s), liczba zapytań według tabeli
3. **Współczynnik Trafień Cache**: Statystyki cache Caffeine
4. **Wzrost Rozmiaru Tabeli**: Monitoruj tabele reviews i user_activity_log
5. **Użycie Indeksów**: Identyfikuj nieużywane indeksy do usunięcia

### Alerty

1. **Wysokie Wykorzystanie Puli Połączeń**: >80% aktywnych połączeń
2. **Wolne Zapytania**: Jakiekolwiek zapytanie >2s (wymóg dashboardu to <4s łącznie)
3. **Nieudane Zadania Synchronizacji**: >3 kolejne niepowodzenia dla jakiegokolwiek review_source
4. **Rozmiar Bazy Danych**: >80% pojemności magazynu
5. **Opóźnienie Replikacji**: Jeśli używane są repliki odczytu (przyszła kwestia)

---

## Podsumowanie

Ten schemat bazy danych zapewnia solidny fundament dla MVP BrandPulse z:

✅ **Wydajność**: Pre-agregacja, strategiczne indeksy, cachowanie → ładowanie dashboardu <4s
✅ **Skalowalność**: Relacje 1:N gotowe na plany premium, struktura gotowa na partycjonowanie
✅ **Bezpieczeństwo**: Wielowarstwowe podejście (Spring Security + opcjonalne RLS), zaszyfrowane poświadczenia
✅ **Integralność Danych**: Ograniczenia unikalności, miękkie usuwanie, ścieżki audytu
✅ **Obserwowalność**: Logowanie aktywności, śledzenie zadań synchronizacji, metryki zaangażowania e-mail
✅ **Łatwość Utrzymania**: Migracje Liquibase, czyste konwencje nazewnictwa, kompleksowa dokumentacja

Schemat balansuje prostotę MVP z przyszłą rozszerzalnością, unikając przedwczesnej optymalizacji przy jednoczesnym zapewnieniu, że kluczowe wymagania (limity freemium, śledzenie dokładności sentymentu, SLA wydajności) są właściwie wspierane na poziomie bazy danych.