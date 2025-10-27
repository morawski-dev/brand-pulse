# Plan API REST BrandPulse

## 1. Przegląd

To API REST zapewnia usługi backendowe dla aplikacji SaaS BrandPulse, umożliwiając małym i średnim firmom monitorowanie i analizowanie opinii klientów z wielu źródeł (Google, Facebook, Trustpilot). API przestrzega zasad RESTful, używa uwierzytelniania JWT i jest zoptymalizowane pod względem wydajności dzięki cache'owaniu i wstępnie obliczonym agregatom.

**Bazowy URL:** `https://api.brandpulse.io` (produkcja) lub `http://localhost:8080` (development)

**Wersja API:** v1 (zawarta w ścieżce: `/api/v1/...`)

**Uwierzytelnianie:** Token JWT Bearer (z wyjątkiem publicznych endpointów uwierzytelniania)

**Typ zawartości:** `application/json`

## 2. Zasoby

| Zasób | Tabela bazodanowa | Opis |
|----------|---------------|-------------|
| Uwierzytelnianie | users | Rejestracja użytkownika, logowanie, odzyskiwanie hasła |
| Użytkownicy | users | Zarządzanie kontem użytkownika |
| Marki | brands | Encje marek/firm zarządzane przez użytkowników |
| Źródła opinii | review_sources | Skonfigurowane źródła opinii (Google, Facebook, Trustpilot) |
| Opinie | reviews | Indywidualne opinie klientów |
| Dashboard | dashboard_aggregates, ai_summaries | Zagregowane metryki i wnioski AI |
| Zadania synchronizacji | sync_jobs | Operacje synchronizacji opinii |
| Aktywność użytkownika | user_activity_log | Śledzenie aktywności użytkownika do analityki |

## 3. Endpointy uwierzytelniania

### 3.1. Rejestracja nowego użytkownika

**US-001: Rejestracja nowego użytkownika**

```http
POST /api/auth/register
```

**Treść żądania:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "confirmPassword": "SecurePass123!"
}
```

**Walidacja:**
- `email`: Wymagane, prawidłowy format email, unikalny w systemie
- `password`: Wymagane, minimum 8 znaków, musi zawierać wielką literę, małą literę, cyfrę, znak specjalny
- `confirmPassword`: Wymagane, musi pasować do hasła

**Odpowiedź sukcesu (201 Created):**
```json
{
  "userId": 1,
  "email": "user@example.com",
  "planType": "FREE",
  "maxSourcesAllowed": 1,
  "emailVerified": false,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": "2025-01-20T12:00:00Z",
  "createdAt": "2025-01-19T12:00:00Z"
}
```

**Odpowiedzi błędów:**
- `400 Bad Request`: Nieprawidłowy format email, hasła się nie zgadzają, hasło zbyt słabe
  ```json
  {
    "code": "VALIDATION_ERROR",
    "message": "Walidacja nie powiodła się",
    "errors": [
      {
        "field": "email",
        "message": "Nieprawidłowy format email"
      }
    ]
  }
  ```
- `409 Conflict`: Email już zarejestrowany
  ```json
  {
    "code": "EMAIL_ALREADY_EXISTS",
    "message": "Konto z tym adresem email już istnieje"
  }
  ```

**Logika biznesowa:**
- Hashowanie hasła przy użyciu BCrypt (minimum 10 rund)
- Ustawienie `plan_type='FREE'`, `max_sources_allowed=1`
- Generowanie tokenu JWT z 60-minutowym wygaśnięciem
- Logowanie aktywności: `USER_REGISTERED`
- Automatyczne logowanie użytkownika (natychmiastowy zwrot tokenu)

---

### 3.2. Logowanie użytkownika

**US-002: Logowanie do systemu**

```http
POST /api/auth/login
```

**Treść żądania:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Odpowiedź sukcesu (200 OK):**
```json
{
  "userId": 1,
  "email": "user@example.com",
  "planType": "FREE",
  "maxSourcesAllowed": 1,
  "emailVerified": false,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": "2025-01-20T12:00:00Z"
}
```

**Odpowiedzi błędów:**
- `401 Unauthorized`: Nieprawidłowe dane logowania
  ```json
  {
    "code": "INVALID_CREDENTIALS",
    "message": "Nieprawidłowy email lub hasło"
  }
  ```
- `403 Forbidden`: Konto niezweryfikowane (jeśli wymagana jest weryfikacja email)
  ```json
  {
    "code": "EMAIL_NOT_VERIFIED",
    "message": "Proszę zweryfikować email przed zalogowaniem",
    "verificationRequired": true
  }
  ```

**Logika biznesowa:**
- Walidacja danych logowania względem zahashowanego hasła
- Generowanie tokenu JWT zawierającego: userId, email, planType, maxSourcesAllowed
- Logowanie aktywności: `LOGIN`

---

### 3.3. Żądanie resetowania hasła

```http
POST /api/auth/forgot-password
```

**Treść żądania:**
```json
{
  "email": "user@example.com"
}
```

**Odpowiedź sukcesu (200 OK):**
```json
{
  "message": "Jeśli konto z tym adresem email istnieje, link do resetowania hasła został wysłany"
}
```

**Logika biznesowa:**
- Generowanie unikalnego `password_reset_token` (UUID)
- Ustawienie `password_reset_expires_at` na NOW() + 1 godzina
- Wysłanie emaila z linkiem resetującym: `https://app.brandpulse.io/reset-password?token={token}`
- Zawsze zwracaj sukces (bezpieczeństwo: nie ujawniaj czy email istnieje)

---

### 3.4. Resetowanie hasła

```http
POST /api/auth/reset-password
```

**Treść żądania:**
```json
{
  "token": "abc123def456...",
  "newPassword": "NewSecurePass123!",
  "confirmPassword": "NewSecurePass123!"
}
```

**Odpowiedź sukcesu (200 OK):**
```json
{
  "message": "Hasło zostało zresetowane pomyślnie. Możesz teraz zalogować się nowym hasłem."
}
```

**Odpowiedzi błędów:**
- `400 Bad Request`: Hasła się nie zgadzają, słabe hasło
- `401 Unauthorized`: Nieprawidłowy lub wygasły token
  ```json
  {
    "code": "INVALID_TOKEN",
    "message": "Token resetowania hasła jest nieprawidłowy lub wygasł"
  }
  ```

**Logika biznesowa:**
- Weryfikacja czy token istnieje i nie wygasł
- Hashowanie nowego hasła BCryptem
- Aktualizacja `password_hash`, czyszczenie `password_reset_token` i `password_reset_expires_at`
- Unieważnienie wszystkich istniejących tokenów JWT dla tego użytkownika (opcjonalne zabezpieczenie)

---

### 3.5. Wylogowanie

```http
POST /api/auth/logout
```

**Nagłówki:**
```
Authorization: Bearer {token}
```

**Odpowiedź sukcesu (200 OK):**
```json
{
  "message": "Wylogowano pomyślnie"
}
```

**Logika biznesowa:**
- Logowanie aktywności: `LOGOUT`
- Dodanie tokenu do czarnej listy (jeśli implementujemy unieważnianie tokenów)
- Klient powinien usunąć token z local storage

---

## 4. Endpointy użytkowników

### 4.1. Pobierz profil bieżącego użytkownika

```http
GET /api/users/me
```

**Nagłówki:**
```
Authorization: Bearer {token}
```

**Odpowiedź sukcesu (200 OK):**
```json
{
  "userId": 1,
  "email": "user@example.com",
  "planType": "FREE",
  "maxSourcesAllowed": 1,
  "emailVerified": true,
  "createdAt": "2025-01-19T12:00:00Z",
  "updatedAt": "2025-01-19T12:00:00Z"
}
```

**Odpowiedzi błędów:**
- `401 Unauthorized`: Brak tokenu lub nieprawidłowy token

---

### 4.2. Aktualizuj profil użytkownika

```http
PATCH /api/users/me
```

**Treść żądania:**
```json
{
  "email": "newemail@example.com"
}
```

**Odpowiedź sukcesu (200 OK):**
```json
{
  "userId": 1,
  "email": "newemail@example.com",
  "emailVerified": false,
  "updatedAt": "2025-01-20T10:30:00Z"
}
```

**Logika biznesowa:**
- Jeśli email został zmieniony, ustaw `email_verified=false` i wyślij email weryfikacyjny
- Aktualizacja emaila wymaga ponownej weryfikacji

---

## 5. Endpointy marek

### 5.1. Utwórz markę

**US-003: Konfiguracja pierwszego źródła (Krok 1)**

```http
POST /api/brands
```

**Nagłówki:**
```
Authorization: Bearer {token}
```

**Treść żądania:**
```json
{
  "name": "Moja sieć restauracji"
}
```

**Walidacja:**
- `name`: Wymagane, 1-255 znaków

**Odpowiedź sukcesu (201 Created):**
```json
{
  "brandId": 1,
  "userId": 1,
  "name": "Moja sieć restauracji",
  "sourceCount": 0,
  "lastManualRefreshAt": null,
  "createdAt": "2025-01-19T12:00:00Z",
  "updatedAt": "2025-01-19T12:00:00Z"
}
```

**Odpowiedzi błędów:**
- `400 Bad Request`: Nieprawidłowa nazwa
- `409 Conflict`: Użytkownik ma już markę (ograniczenie MVP)
  ```json
  {
    "code": "BRAND_LIMIT_EXCEEDED",
    "message": "MVP wspiera jedną markę na konto użytkownika"
  }
  ```

---

### 5.2. Pobierz marki użytkownika

```http
GET /api/brands
```

**Nagłówki:**
```
Authorization: Bearer {token}
```

**Odpowiedź sukcesu (200 OK):**
```json
{
  "brands": [
    {
      "brandId": 1,
      "name": "Moja sieć restauracji",
      "sourceCount": 2,
      "lastManualRefreshAt": "2025-01-19T08:00:00Z",
      "createdAt": "2025-01-19T12:00:00Z",
      "updatedAt": "2025-01-19T12:00:00Z"
    }
  ]
}
```

**Logika biznesowa:**
- Filtrowanie marek według uwierzytelnionego użytkownika (JWT userId)
- Wykluczenie soft-deleted marek (`deleted_at IS NULL`)
- Uwzględnienie liczby aktywnych źródeł opinii

---

### 5.3. Pobierz markę po ID

```http
GET /api/brands/{brandId}
```

**Odpowiedź sukcesu (200 OK):**
```json
{
  "brandId": 1,
  "name": "Moja sieć restauracji",
  "sourceCount": 2,
  "lastManualRefreshAt": "2025-01-19T08:00:00Z",
  "sources": [
    {
      "sourceId": 1,
      "sourceType": "GOOGLE",
      "profileUrl": "https://www.google.com/maps/place/...",
      "isActive": true,
      "lastSyncAt": "2025-01-20T03:00:00Z",
      "lastSyncStatus": "SUCCESS"
    }
  ],
  "createdAt": "2025-01-19T12:00:00Z",
  "updatedAt": "2025-01-19T12:00:00Z"
}
```

**Odpowiedzi błędów:**
- `403 Forbidden`: Użytkownik nie jest właścicielem tej marki
- `404 Not Found`: Marka nie istnieje

---

### 5.4. Aktualizuj markę

```http
PATCH /api/brands/{brandId}
```

**Treść żądania:**
```json
{
  "name": "Zaktualizowana nazwa sieci restauracji"
}
```

**Odpowiedź sukcesu (200 OK):**
```json
{
  "brandId": 1,
  "name": "Zaktualizowana nazwa sieci restauracji",
  "updatedAt": "2025-01-20T10:45:00Z"
}
```

---

### 5.5. Usuń markę

```http
DELETE /api/brands/{brandId}
```

**Odpowiedź sukcesu (204 No Content)**

**Logika biznesowa:**
- Soft delete: Ustawienie `deleted_at=NOW()`
- Kaskada: Wszystkie review_sources, reviews, dashboard_aggregates są również soft-deleted (ON DELETE CASCADE)
- Hard delete po 90 dniach (zadanie w tle)

---

## 6. Endpointy źródeł opinii

### 6.1. Utwórz źródło opinii

**US-003: Konfiguracja pierwszego źródła (Krok 2)**
**US-009: Ograniczenie planu Free**

```http
POST /api/brands/{brandId}/review-sources
```

**Treść żądania:**
```json
{
  "sourceType": "GOOGLE",
  "profileUrl": "https://www.google.com/maps/place/My+Restaurant/@50.0647,19.9450,17z/...",
  "externalProfileId": "ChIJN1t_tDeuEmsRUsoyG83frY4",
  "authMethod": "API",
  "credentialsEncrypted": {
    "apiKey": "encrypted_value_here"
  }
}
```

**Walidacja:**
- `sourceType`: Wymagane, musi być 'GOOGLE', 'FACEBOOK' lub 'TRUSTPILOT'
- `profileUrl`: Wymagane, prawidłowy format URL
- `externalProfileId`: Wymagane, unikalne per brand+sourceType
- `authMethod`: Wymagane, musi być 'API' lub 'SCRAPING'

**Odpowiedź sukcesu (201 Created):**
```json
{
  "sourceId": 1,
  "brandId": 1,
  "sourceType": "GOOGLE",
  "profileUrl": "https://www.google.com/maps/place/...",
  "externalProfileId": "ChIJN1t_tDeuEmsRUsoyG83frY4",
  "authMethod": "API",
  "isActive": true,
  "lastSyncAt": null,
  "lastSyncStatus": null,
  "nextScheduledSyncAt": "2025-01-20T03:00:00Z",
  "importJobId": 42,
  "importStatus": "IN_PROGRESS",
  "createdAt": "2025-01-19T12:00:00Z"
}
```

**Odpowiedzi błędów:**
- `400 Bad Request`: Nieprawidłowy sourceType, nieprawidłowy URL
- `403 Forbidden`: Użytkownik nie jest właścicielem marki lub przekroczono limit planu
  ```json
  {
    "code": "PLAN_LIMIT_EXCEEDED",
    "message": "Plan darmowy pozwala na 1 źródło opinii. Płatne plany wkrótce.",
    "currentCount": 1,
    "maxAllowed": 1,
    "planType": "FREE"
  }
  ```
- `409 Conflict`: Duplikat źródła (ta sama marka+sourceType+externalProfileId)
  ```json
  {
    "code": "DUPLICATE_SOURCE",
    "message": "To źródło opinii jest już skonfigurowane dla Twojej marki"
  }
  ```

**Logika biznesowa:**
1. Sprawdź plan użytkownika: Policz istniejące aktywne źródła dla marki
2. Jeśli liczba >= `max_sources_allowed`, zwróć 403 z komunikatem o aktualizacji planu
3. Zaszyfruj dane dostępowe używając AES-256 przed zapisem
4. Utwórz `sync_job` z `job_type='INITIAL'`, `status='PENDING'`
5. Uruchom zadanie w tle do importu opinii z ostatnich 90 dni
6. Zwróć `importJobId` do śledzenia postępu
7. Logowanie aktywności: `SOURCE_ADDED`, następnie `FIRST_SOURCE_CONFIGURED_SUCCESSFULLY` jeśli pierwsze źródło

---

### 6.2. Listuj źródła opinii dla marki

```http
GET /api/brands/{brandId}/review-sources
```

**Odpowiedź sukcesu (200 OK):**
```json
{
  "sources": [
    {
      "sourceId": 1,
      "sourceType": "GOOGLE",
      "profileUrl": "https://www.google.com/maps/place/...",
      "externalProfileId": "ChIJN1t_tDeuEmsRUsoyG83frY4",
      "isActive": true,
      "lastSyncAt": "2025-01-20T03:00:00Z",
      "lastSyncStatus": "SUCCESS",
      "nextScheduledSyncAt": "2025-01-21T03:00:00Z",
      "createdAt": "2025-01-19T12:00:00Z"
    },
    {
      "sourceId": 2,
      "sourceType": "FACEBOOK",
      "profileUrl": "https://www.facebook.com/myrestaurant",
      "externalProfileId": "123456789",
      "isActive": true,
      "lastSyncAt": "2025-01-20T03:00:15Z",
      "lastSyncStatus": "FAILED",
      "lastSyncError": "Przekroczono limit API",
      "nextScheduledSyncAt": "2025-01-21T03:00:00Z",
      "createdAt": "2025-01-19T14:00:00Z"
    }
  ]
}
```

**Logika biznesowa:**
- Filtruj po brandId i `deleted_at IS NULL`
- Nigdy nie zwracaj pola `credentials_encrypted` w odpowiedziach API (bezpieczeństwo)

---

### 6.3. Pobierz źródło opinii po ID

```http
GET /api/brands/{brandId}/review-sources/{sourceId}
```

**Odpowiedź sukcesu (200 OK):**
```json
{
  "sourceId": 1,
  "brandId": 1,
  "sourceType": "GOOGLE",
  "profileUrl": "https://www.google.com/maps/place/...",
  "externalProfileId": "ChIJN1t_tDeuEmsRUsoyG83frY4",
  "authMethod": "API",
  "isActive": true,
  "lastSyncAt": "2025-01-20T03:00:00Z",
  "lastSyncStatus": "SUCCESS",
  "nextScheduledSyncAt": "2025-01-21T03:00:00Z",
  "createdAt": "2025-01-19T12:00:00Z",
  "updatedAt": "2025-01-20T03:00:00Z"
}
```

**Odpowiedzi błędów:**
- `403 Forbidden`: Użytkownik nie jest właścicielem tej marki/źródła
- `404 Not Found`: Źródło nie istnieje

---

### 6.4. Aktualizuj źródło opinii

```http
PATCH /api/brands/{brandId}/review-sources/{sourceId}
```

**Treść żądania:**
```json
{
  "isActive": false,
  "profileUrl": "https://www.google.com/maps/place/updated-url"
}
```

**Odpowiedź sukcesu (200 OK):**
```json
{
  "sourceId": 1,
  "isActive": false,
  "updatedAt": "2025-01-20T11:00:00Z"
}
```

**Logika biznesowa:**
- Ustawienie `isActive=false` wstrzymuje automatyczne synchronizacje
- Aktualizacja URL lub danych dostępowych może wywołać zadanie walidacyjne

---

### 6.5. Usuń źródło opinii

```http
DELETE /api/brands/{brandId}/review-sources/{sourceId}
```

**Odpowiedź sukcesu (204 No Content)**

**Logika biznesowa:**
- Soft delete: Ustawienie `deleted_at=NOW()`
- Usunięcia kaskadowe reviews, dashboard_aggregates, ai_summaries, sync_jobs
- Logowanie aktywności: `SOURCE_DELETED`

---

## 7. Endpointy opinii

### 7.1. Listuj opinie

**US-004: Przeglądanie zagregowanych opinii**
**US-005: Filtrowanie negatywnych opinii**
**US-006: Przełączanie między lokalizacjami**

```http
GET /api/brands/{brandId}/reviews
```

**Parametry zapytania:**
- `sourceId` (opcjonalny): Filtruj po konkretnym źródle opinii. Jeśli pominięty, zwraca opinie ze wszystkich źródeł (widok zagregowany - US-006).
- `sentiment` (opcjonalny): Filtruj po sentymencie. Wartości: `POSITIVE`, `NEGATIVE`, `NEUTRAL`. Może być rozdzielone przecinkami: `sentiment=NEGATIVE,NEUTRAL`.
- `rating` (opcjonalny): Filtruj po ocenie gwiazdkowej. Wartości: `1`, `2`, `3`, `4`, `5`. Może być rozdzielone przecinkami: `rating=1,2` (US-005).
- `startDate` (opcjonalny): Filtruj opinie opublikowane po tej dacie. Format: ISO 8601 (`2025-01-01T00:00:00Z`).
- `endDate` (opcjonalny): Filtruj opinie opublikowane przed tą datą.
- `page` (opcjonalny): Numer strony (indeksowanie od 0). Domyślnie: `0`.
- `size` (opcjonalny): Elementów na stronę. Domyślnie: `20`, Maksymalnie: `100`.
- `sort` (opcjonalny): Pole sortowania i kierunek. Domyślnie: `publishedAt,desc`. Opcje: `publishedAt,asc`, `rating,desc`, itp.

**Przykładowe żądanie:**
```http
GET /api/brands/1/reviews?rating=1,2&sentiment=NEGATIVE&page=0&size=20&sort=publishedAt,desc
```

**Odpowiedź sukcesu (200 OK):**
```json
{
  "reviews": [
    {
      "reviewId": 123,
      "sourceId": 1,
      "sourceType": "GOOGLE",
      "externalReviewId": "ChdDSUhNMG9nS0VJQ0FnSURqMGZxT3ZRRRAB",
      "content": "Okropna obsługa. Czekałem 45 minut na zimne jedzenie. Nigdy więcej!",
      "authorName": "Jan Kowalski",
      "rating": 1,
      "sentiment": "NEGATIVE",
      "sentimentConfidence": 0.9876,
      "publishedAt": "2025-01-18T14:30:00Z",
      "fetchedAt": "2025-01-19T03:00:00Z",
      "createdAt": "2025-01-19T03:00:15Z"
    },
    {
      "reviewId": 124,
      "sourceId": 2,
      "sourceType": "FACEBOOK",
      "externalReviewId": "987654321",
      "content": "Jedzenie było w porządku, ale nic szczególnego. Zawyżone ceny jak na to co dostajemy.",
      "authorName": "Anna Nowak",
      "rating": 2,
      "sentiment": "NEUTRAL",
      "sentimentConfidence": 0.6543,
      "publishedAt": "2025-01-17T19:15:00Z",
      "fetchedAt": "2025-01-18T03:00:00Z",
      "createdAt": "2025-01-18T03:00:22Z"
    }
  ],
  "pagination": {
    "currentPage": 0,
    "pageSize": 20,
    "totalItems": 47,
    "totalPages": 3,
    "hasNext": true,
    "hasPrevious": false
  },
  "filters": {
    "sourceId": null,
    "sentiment": ["NEGATIVE"],
    "rating": [1, 2],
    "startDate": null,
    "endDate": null
  }
}
```

**Odpowiedzi błędów:**
- `400 Bad Request`: Nieprawidłowe wartości filtrów, nieprawidłowy format daty
- `403 Forbidden`: Użytkownik nie jest właścicielem tej marki

**Logika biznesowa:**
- Użyj indeksu `idx_reviews_composite_filter` do filtrowania wielokryterialnego (wydajność)
- Użyj częściowego indeksu `idx_reviews_negative` przy filtrowaniu `rating<=2` (optymalizacja US-005)
- Filtruj `deleted_at IS NULL`
- Logowanie aktywności: `VIEW_DASHBOARD` przy pierwszym załadowaniu strony

**Wydajność:**
- Dashboard musi się załadować w <4 sekundy (wymóg PRD)
- Użyj Spring Cache z Caffeine (10-minutowy TTL)
- Paginacja zapobiega ładowaniu tysięcy opinii na raz

---

### 7.2. Pobierz opinię po ID

```http
GET /api/brands/{brandId}/reviews/{reviewId}
```

**Odpowiedź sukcesu (200 OK):**
```json
{
  "reviewId": 123,
  "sourceId": 1,
  "sourceType": "GOOGLE",
  "externalReviewId": "ChdDSUhNMG9nS0VJQ0FnSURqMGZxT3ZRRRAB",
  "content": "Okropna obsługa. Czekałem 45 minut na zimne jedzenie. Nigdy więcej!",
  "contentHash": "a1b2c3d4e5f6...",
  "authorName": "Jan Kowalski",
  "rating": 1,
  "sentiment": "NEGATIVE",
  "sentimentConfidence": 0.9876,
  "publishedAt": "2025-01-18T14:30:00Z",
  "fetchedAt": "2025-01-19T03:00:00Z",
  "createdAt": "2025-01-19T03:00:15Z",
  "updatedAt": "2025-01-19T03:00:15Z",
  "sentimentChangeHistory": [
    {
      "changedAt": "2025-01-19T10:00:00Z",
      "oldSentiment": "NEUTRAL",
      "newSentiment": "NEGATIVE",
      "changeReason": "USER_CORRECTION",
      "changedByUserId": 1
    },
    {
      "changedAt": "2025-01-19T03:00:15Z",
      "oldSentiment": null,
      "newSentiment": "NEUTRAL",
      "changeReason": "AI_INITIAL",
      "changedByUserId": null
    }
  ]
}
```

**Odpowiedzi błędów:**
- `403 Forbidden`: Użytkownik nie jest właścicielem marki zawierającej tę opinię
- `404 Not Found`: Opinia nie istnieje

---

### 7.3. Aktualizuj sentyment opinii

**US-007: Manualna korekta sentymentu**

```http
PATCH /api/brands/{brandId}/reviews/{reviewId}/sentiment
```

**Treść żądania:**
```json
{
  "sentiment": "NEGATIVE"
}
```

**Walidacja:**
- `sentiment`: Wymagane, musi być 'POSITIVE', 'NEGATIVE' lub 'NEUTRAL'

**Odpowiedź sukcesu (200 OK):**
```json
{
  "reviewId": 123,
  "sentiment": "NEGATIVE",
  "previousSentiment": "NEUTRAL",
  "updatedAt": "2025-01-19T10:00:00Z",
  "sentimentChangeId": 42
}
```

**Odpowiedzi błędów:**
- `400 Bad Request`: Nieprawidłowa wartość sentymentu
- `403 Forbidden`: Użytkownik nie jest właścicielem tej opinii
- `404 Not Found`: Opinia nie istnieje

**Logika biznesowa:**
1. Zweryfikuj, że użytkownik jest właścicielem marki zawierającej tę opinię
2. Zapisz zmianę w tabeli `sentiment_changes`:
   - `old_sentiment`: Obecny review.sentiment
   - `new_sentiment`: Żądany sentyment
   - `changed_by_user_id`: JWT userId
   - `change_reason`: 'USER_CORRECTION'
3. Zaktualizuj `reviews.sentiment` i `reviews.updated_at`
4. Unieważnij cache'owane agregaty dashboardu dla źródła tej opinii
5. Wywołaj asynchroniczne przeliczenie `dashboard_aggregates` dla danej daty
6. Logowanie aktywności: `SENTIMENT_CORRECTED`

**Unieważnianie cache:**
- Wyczyść klucz cache: `dashboard:brand:{brandId}`
- Wyczyść klucz cache: `summary:source:{reviewSourceId}`

---

## 8. Endpointy dashboardu

### 8.1. Pobierz podsumowanie dashboardu

**US-004: Przeglądanie zagregowanych opinii (Sekcja podsumowania)**
**US-006: Przełączanie między lokalizacjami**

```http
GET /api/dashboard/summary
```

**Parametry zapytania:**
- `brandId` (wymagane): ID marki do podsumowania
- `sourceId` (opcjonalny): Filtruj po konkretnym źródle. Jeśli pominięty, zwraca zagregowane dane dla wszystkich źródeł (US-006 "Wszystkie lokalizacje").
- `startDate` (opcjonalny): Data początkowa podsumowania. Domyślnie: 90 dni temu.
- `endDate` (opcjonalny): Data końcowa podsumowania. Domyślnie: dziś.

**Przykładowe żądanie:**
```http
GET /api/dashboard/summary?brandId=1&sourceId=2&startDate=2024-10-20&endDate=2025-01-19
```

**Odpowiedź sukcesu (200 OK):**
```json
{
  "brandId": 1,
  "sourceId": 2,
  "sourceName": "GOOGLE",
  "period": {
    "startDate": "2024-10-20",
    "endDate": "2025-01-19"
  },
  "metrics": {
    "totalReviews": 247,
    "averageRating": 4.12,
    "sentimentDistribution": {
      "positive": 182,
      "negative": 31,
      "neutral": 34,
      "positivePercentage": 73.68,
      "negativePercentage": 12.55,
      "neutralPercentage": 13.77
    },
    "ratingDistribution": {
      "1": 12,
      "2": 19,
      "3": 34,
      "4": 78,
      "5": 104
    }
  },
  "aiSummary": {
    "summaryId": 15,
    "text": "75% pozytywnych opinii. Klienci konsekwentnie chwalą szybkość obsługi i świeże składniki. Główne skargi dotyczą cen (wspomniane w 18 opiniach) i ograniczonej dostępności parkingów (12 opinii). Najnowszy trend: Zwiększona pozytywna opinia o nowych pozycjach menu wprowadzonych w grudniu.",
    "modelUsed": "anthropic/claude-3-haiku",
    "generatedAt": "2025-01-19T03:05:00Z",
    "validUntil": "2025-01-20T03:05:00Z"
  },
  "recentNegativeReviews": [
    {
      "reviewId": 123,
      "rating": 1,
      "content": "Okropna obsługa. Czekałem 45 minut...",
      "publishedAt": "2025-01-18T14:30:00Z"
    },
    {
      "reviewId": 119,
      "rating": 2,
      "content": "Zawyżone ceny za wielkość porcji...",
      "publishedAt": "2025-01-17T11:20:00Z"
    }
  ],
  "lastUpdated": "2025-01-20T03:00:00Z"
}
```

**Odpowiedzi błędów:**
- `400 Bad Request`: Nieprawidłowy format daty, endDate przed startDate
- `403 Forbidden`: Użytkownik nie jest właścicielem tej marki
- `404 Not Found`: Marka lub źródło nie istnieje

**Logika biznesowa:**
- Pobieraj dane z tabeli `dashboard_aggregates` (wstępnie obliczone podczas zadania CRON)
- Jeśli podano `sourceId`, filtruj agregaty tylko dla tego źródła
- Jeśli pominięto `sourceId`, SUMUJ agregaty ze wszystkich źródeł dla marki (US-006 "Wszystkie lokalizacje")
- Pobierz najnowsze prawidłowe podsumowanie AI z `ai_summaries` WHERE `valid_until > NOW()`
- Uwzględnij 3 najnowsze negatywne opinie (rating <= 2) dla szybkiego dostępu
- Użyj cache'owanych wyników (Spring Cache z Caffeine, 10-minutowy TTL)

**Optymalizacja wydajności:**
- Krytyczne: Musi się załadować w <4 sekundy (wymóg PRD)
- Wstępne obliczenia w `dashboard_aggregates` redukują złożoność zapytań
- Zmaterializowany widok `mv_brand_aggregates` dla widoku "Wszystkie lokalizacje" (opcjonalne)
- Strategia cache'owania: Klucz cache `dashboard:brand:{brandId}:source:{sourceId}`

---

### 8.2. Pobierz podsumowanie AI dla źródła

```http
GET /api/dashboard/summary/ai
```

**Parametry zapytania:**
- `sourceId` (wymagane): ID źródła opinii

**Odpowiedź sukcesu (200 OK):**
```json
{
  "summaryId": 15,
  "sourceId": 2,
  "text": "75% pozytywnych opinii. Klienci konsekwentnie chwalą szybkość obsługi i świeże składniki. Główne skargi dotyczą cen (wspomniane w 18 opiniach) i ograniczonej dostępności parkingów (12 opinii). Najnowszy trend: Zwiększona pozytywna opinia o nowych pozycjach menu wprowadzonych w grudniu.",
  "modelUsed": "anthropic/claude-3-haiku",
  "tokenCount": 1243,
  "generatedAt": "2025-01-19T03:05:00Z",
  "validUntil": "2025-01-20T03:05:00Z"
}
```

**Logika biznesowa:**
- Zwróć najnowsze podsumowanie WHERE `valid_until > NOW()`
- Jeśli nie istnieje prawidłowe podsumowanie, wywołaj asynchroniczne generowanie i zwróć 202 Accepted
- Cache z kluczem: `summary:source:{sourceId}`

---

## 9. Endpointy zadań synchronizacji

### 9.1. Wywołaj manualną synchronizację

**US-008: Manualne odświeżanie danych**

```http
POST /api/brands/{brandId}/sync
```

**Treść żądania (opcjonalna):**
```json
{
  "sourceId": 2
}
```

**Walidacja:**
- `sourceId` (opcjonalny): Jeśli podany, synchronizuj tylko to źródło. Jeśli pominięty, synchronizuj wszystkie źródła dla marki.

**Odpowiedź sukcesu (202 Accepted):**
```json
{
  "message": "Manualna synchronizacja rozpoczęta pomyślnie",
  "jobs": [
    {
      "jobId": 78,
      "sourceId": 1,
      "sourceType": "GOOGLE",
      "jobType": "MANUAL",
      "status": "PENDING",
      "createdAt": "2025-01-19T10:30:00Z"
    },
    {
      "jobId": 79,
      "sourceId": 2,
      "sourceType": "FACEBOOK",
      "jobType": "MANUAL",
      "status": "PENDING",
      "createdAt": "2025-01-19T10:30:00Z"
    }
  ],
  "nextManualSyncAvailableAt": "2025-01-20T10:30:00Z"
}
```

**Odpowiedzi błędów:**
- `403 Forbidden`: Użytkownik nie jest właścicielem tej marki
- `429 Too Many Requests`: Manualne odświeżanie już wywołane w ciągu ostatnich 24 godzin
  ```json
  {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Manualna synchronizacja może być wywołana tylko raz na 24 godziny",
    "lastManualRefreshAt": "2025-01-19T08:00:00Z",
    "nextAvailableAt": "2025-01-20T08:00:00Z",
    "hoursRemaining": 22
  }
  ```

**Logika biznesowa:**
1. Sprawdź `brands.last_manual_refresh_at`
2. Jeśli `last_manual_refresh_at > (NOW() - INTERVAL '24 hours')`, zwróć 429 z pozostałym czasem
3. Zaktualizuj `brands.last_manual_refresh_at = NOW()`
4. Utwórz `sync_job(s)` z `job_type='MANUAL'`, `status='PENDING'`
5. Wywołaj asynchroniczny proces synchronizacji dla każdego źródła
6. Zwróć ID zadań do śledzenia postępu
7. Logowanie aktywności: `MANUAL_REFRESH_TRIGGERED`

**Ograniczenie częstotliwości:**
- 24-godzinne okno kroczące (nie dzień kalendarzowy)
- Obliczane: `last_manual_refresh_at + 24 godziny`
- Przycisk wyłączony na froncie do `nextAvailableAt`

---

### 9.2. Pobierz status zadania synchronizacji

```http
GET /api/sync-jobs/{jobId}
```

**Odpowiedź sukcesu (200 OK):**
```json
{
  "jobId": 78,
  "sourceId": 1,
  "sourceType": "GOOGLE",
  "jobType": "MANUAL",
  "status": "COMPLETED",
  "startedAt": "2025-01-19T10:30:05Z",
  "completedAt": "2025-01-19T10:31:42Z",
  "reviewsFetched": 15,
  "reviewsNew": 8,
  "reviewsUpdated": 7,
  "errorMessage": null,
  "createdAt": "2025-01-19T10:30:00Z"
}
```

**Wartości statusu:**
- `PENDING`: Zadanie w kolejce, nierozpoczęte
- `IN_PROGRESS`: Obecnie pobieranie opinii
- `COMPLETED`: Zakończone pomyślnie
- `FAILED`: Wystąpił błąd (patrz `errorMessage`)

**Odpowiedzi błędów:**
- `403 Forbidden`: Użytkownik nie jest właścicielem marki zawierającej to źródło
- `404 Not Found`: Zadanie nie istnieje

**Logika biznesowa:**
- Używane do odpytywania podczas początkowego 90-dniowego importu (US-003)
- Używane do śledzenia postępu manualnej synchronizacji (US-008)
- Frontend może odpytywać co 2-3 sekundy do czasu status != PENDING|IN_PROGRESS

---

### 9.3. Listuj zadania synchronizacji dla źródła

```http
GET /api/brands/{brandId}/review-sources/{sourceId}/sync-jobs
```

**Parametry zapytania:**
- `page` (opcjonalny): Domyślnie 0
- `size` (opcjonalny): Domyślnie 20
- `status` (opcjonalny): Filtruj po statusie

**Odpowiedź sukcesu (200 OK):**
```json
{
  "jobs": [
    {
      "jobId": 78,
      "jobType": "MANUAL",
      "status": "COMPLETED",
      "startedAt": "2025-01-19T10:30:05Z",
      "completedAt": "2025-01-19T10:31:42Z",
      "reviewsFetched": 15,
      "reviewsNew": 8
    },
    {
      "jobId": 65,
      "jobType": "SCHEDULED",
      "status": "COMPLETED",
      "startedAt": "2025-01-19T03:00:00Z",
      "completedAt": "2025-01-19T03:02:15Z",
      "reviewsFetched": 23,
      "reviewsNew": 12
    }
  ],
  "pagination": {
    "currentPage": 0,
    "totalItems": 42,
    "totalPages": 3
  }
}
```

**Logika biznesowa:**
- Uporządkowane według `created_at DESC`
- Przydatne do debugowania problemów z synchronizacją i monitorowania historii importu

---

## 10. Endpointy dziennika aktywności (Wewnętrzne/Analityka)

### 10.1. Pobierz dziennik aktywności użytkownika

```http
GET /api/users/me/activity
```

**Parametry zapytania:**
- `activityType` (opcjonalny): Filtruj po typie
- `startDate` (opcjonalny): Filtruj po zakresie dat
- `endDate` (opcjonalny): Filtruj po zakresie dat
- `page`, `size`: Paginacja

**Odpowiedź sukcesu (200 OK):**
```json
{
  "activities": [
    {
      "activityId": 123,
      "activityType": "SENTIMENT_CORRECTED",
      "occurredAt": "2025-01-19T10:00:00Z",
      "metadata": {
        "reviewId": 123,
        "oldSentiment": "NEUTRAL",
        "newSentiment": "NEGATIVE"
      }
    },
    {
      "activityId": 122,
      "activityType": "VIEW_DASHBOARD",
      "occurredAt": "2025-01-19T09:45:00Z",
      "metadata": {
        "brandId": 1
      }
    },
    {
      "activityType": "LOGIN",
      "occurredAt": "2025-01-19T09:44:30Z",
      "metadata": null
    }
  ],
  "pagination": {
    "currentPage": 0,
    "totalItems": 87
  }
}
```

**Logika biznesowa:**
- Używane do obliczania metryk sukcesu:
  - **Time to Value**: `USER_REGISTERED` → `FIRST_SOURCE_CONFIGURED_SUCCESSFULLY` < 10 minut
  - **Aktywacja**: Liczba zdarzeń `LOGIN` w pierwszych 4 tygodniach >= 3
  - **Retencja**: 35% użytkowników loguje się 3+ razy w pierwszych 4 tygodniach
- Przechowywane w tabeli `user_activity_log`
- Kolumna JSONB metadanych pozwala na elastyczne właściwości zdarzeń

**Typy aktywności:**
- `USER_REGISTERED`
- `LOGIN`, `LOGOUT`
- `VIEW_DASHBOARD`
- `FILTER_APPLIED`
- `SENTIMENT_CORRECTED`
- `SOURCE_CONFIGURED`
- `SOURCE_ADDED`
- `SOURCE_DELETED`
- `MANUAL_REFRESH_TRIGGERED`
- `FIRST_SOURCE_CONFIGURED_SUCCESSFULLY`

---

## 11. Endpointy raportów emailowych (Przyszłość)

### 11.1. Listuj raporty emailowe

```http
GET /api/users/me/email-reports
```

**Odpowiedź sukcesu (200 OK):**
```json
{
  "reports": [
    {
      "reportId": 15,
      "reportType": "WEEKLY_SUMMARY",
      "sentAt": "2025-01-19T06:00:00Z",
      "openedAt": "2025-01-19T08:30:00Z",
      "clickedAt": "2025-01-19T08:31:15Z",
      "periodStart": "2025-01-12",
      "periodEnd": "2025-01-18",
      "reviewsCount": 23,
      "newNegativeCount": 4,
      "deliveryStatus": "DELIVERED"
    }
  ]
}
```

**Logika biznesowa:**
- Śledź zaangażowanie w emaile dla analizy retencji
- Raporty tygodniowe wysyłane automatycznie (zadanie w tle)
- Otwarcie/kliknięcie śledzone poprzez piksele śledzące email i linki UTM

---

## 12. Uwierzytelnianie i autoryzacja

### 12.1. Mechanizm uwierzytelniania

**Metoda:** Uwierzytelnianie JWT (JSON Web Token) Bearer

**Struktura tokenu:**
```json
{
  "sub": "1",
  "email": "user@example.com",
  "planType": "FREE",
  "maxSourcesAllowed": 1,
  "iat": 1642598400,
  "exp": 1642602000
}
```

**Format nagłówka:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Wygaśnięcie tokenu:**
- Access Token: 60 minut (konfigurowalne przez `jwt.expiration-minutes`)
- Refresh Token: 7 dni (przyszła implementacja)

**Przechowywanie tokenu:**
- Klient: Local storage lub HTTP-only cookie (zalecane ze względów bezpieczeństwa)
- Serwer: Bezstanowy (brak przechowywania sesji po stronie serwera)
- Opcjonalnie: Czarna lista tokenów dla wylogowania (cache Redis)

---

### 12.2. Reguły autoryzacji

**Walidacja własności zasobów:**
1. **Marki**: Użytkownik może uzyskać dostęp tylko do marek gdzie `brand.user_id = JWT.userId`
2. **Źródła opinii**: Użytkownik może uzyskać dostęp do źródła jeśli jest właścicielem marki nadrzędnej
3. **Opinie**: Użytkownik może uzyskać dostęp do opinii jeśli jest właścicielem łańcucha marka → źródło nadrzędne
4. **Dashboard**: Użytkownik może zobaczyć tylko dashboard dla swoich marek

**Zabezpieczenie na poziomie wierszy bazy danych (RLS):**
- Podstawowe: Sprawdzenia Spring Security w warstwie serwisowej
- Dodatkowe: Polityki RLS PostgreSQL (obrona w głębi)
- Aplikacja ustawia zmienną sesyjną: `SET LOCAL app.current_user_id = {userId}`

**Publiczne endpointy (bez wymaganego uwierzytelniania):**
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `GET /api/health`
- `GET /actuator/health`

**Chronione endpointy:**
- Wszystkie pozostałe endpointy wymagają prawidłowego tokenu JWT
- Zwróć 401 Unauthorized jeśli token brakuje/jest nieprawidłowy/wygasł
- Zwróć 403 Forbidden jeśli użytkownik nie ma uprawnień do zasobu

---

### 12.3. Konfiguracja CORS

**Dozwolone originy:**
- Development: `http://localhost:3000`
- Produkcja: `https://app.brandpulse.io`

**Dozwolone metody:**
- `GET`, `POST`, `PATCH`, `PUT`, `DELETE`, `OPTIONS`

**Dozwolone nagłówki:**
- `Authorization`, `Content-Type`, `Accept`

**Credentials:**
- Dozwolone (dla cookies jeśli używane HTTP-only cookie auth)

---

## 13. Walidacja i logika biznesowa

### 13.1. Walidacja żądań

**Technologia:** Jakarta Bean Validation (JSR 380)

**Typowe walidacje:**

#### Rejestracja użytkownika
```java
@NotBlank(message = "Email jest wymagany")
@Email(message = "Nieprawidłowy format email")
@Size(max = 255)
private String email;

@NotBlank(message = "Hasło jest wymagane")
@Size(min = 8, max = 100, message = "Hasło musi mieć 8-100 znaków")
@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
         message = "Hasło musi zawierać wielką literę, małą literę, cyfrę i znak specjalny")
private String password;
```

#### Źródło opinii
```java
@NotNull(message = "Typ źródła jest wymagany")
@Enumerated(EnumType.STRING)
private SourceType sourceType; // GOOGLE, FACEBOOK, TRUSTPILOT

@NotBlank(message = "URL profilu jest wymagany")
@URL(message = "Nieprawidłowy format URL")
private String profileUrl;

@NotBlank(message = "ID profilu zewnętrznego jest wymagane")
@Size(max = 255)
private String externalProfileId;
```

#### Aktualizacja sentymentu opinii
```java
@NotNull(message = "Sentyment jest wymagany")
@Enumerated(EnumType.STRING)
private Sentiment sentiment; // POSITIVE, NEGATIVE, NEUTRAL
```

---

### 13.2. Reguły logiki biznesowej

#### Limity planu Freemium (US-009)

**Reguła:** Użytkownicy planu darmowego mogą skonfigurować maksymalnie 1 źródło opinii

**Walidacja:**
```java
// W ReviewSourceService.createSource()
int activeSourceCount = reviewSourceRepository.countByBrandIdAndDeletedAtIsNull(brandId);
if (activeSourceCount >= user.getMaxSourcesAllowed()) {
    throw new PlanLimitExceededException(
        "Plan darmowy pozwala na " + user.getMaxSourcesAllowed() + " źródło opinii. Płatne plany wkrótce.",
        activeSourceCount,
        user.getMaxSourcesAllowed()
    );
}
```

**Odpowiedź HTTP:** 403 Forbidden z komunikatem o ulepszeniu planu

---

#### Ograniczenie częstotliwości manualnego odświeżania (US-008)

**Reguła:** Użytkownicy mogą ręcznie wywołać synchronizację raz na 24 godziny (okno kroczące)

**Walidacja:**
```java
// W BrandService.triggerManualSync()
LocalDateTime lastRefresh = brand.getLastManualRefreshAt();
if (lastRefresh != null && lastRefresh.isAfter(LocalDateTime.now().minusHours(24))) {
    LocalDateTime nextAvailable = lastRefresh.plusHours(24);
    long hoursRemaining = ChronoUnit.HOURS.between(LocalDateTime.now(), nextAvailable);
    throw new RateLimitExceededException(
        "Manualna synchronizacja może być wywołana tylko raz na 24 godziny",
        lastRefresh,
        nextAvailable,
        hoursRemaining
    );
}
```

**Odpowiedź HTTP:** 429 Too Many Requests

---

#### Ślad audytowy korekty sentymentu (US-007)

**Reguła:** Wszystkie zmiany sentymentu muszą być logowane w tabeli `sentiment_changes`

**Implementacja:**
```java
// W ReviewService.updateSentiment()
@Transactional
public ReviewDTO updateSentiment(Long reviewId, Sentiment newSentiment, Long userId) {
    Review review = reviewRepository.findById(reviewId).orElseThrow();
    Sentiment oldSentiment = review.getSentiment();

    // Zapisz zmianę w tabeli audytowej
    SentimentChange change = new SentimentChange();
    change.setReviewId(reviewId);
    change.setOldSentiment(oldSentiment);
    change.setNewSentiment(newSentiment);
    change.setChangedByUserId(userId);
    change.setChangeReason(ChangeReason.USER_CORRECTION);
    sentimentChangeRepository.save(change);

    // Zaktualizuj opinię
    review.setSentiment(newSentiment);
    reviewRepository.save(review);

    // Unieważnij cache i wywołaj przeliczenie
    cacheManager.evict("dashboard:brand:" + review.getBrand().getId());
    dashboardAggregateService.recalculateForSource(review.getReviewSourceId());

    return mapToDTO(review);
}
```

**Metryka sukcesu:** 75% dokładność AI (mierzona przez tabelę sentiment_changes)

---

#### Import historii 90-dniowej (PRD:28)

**Reguła:** Przy tworzeniu nowego źródła opinii, importuj opinie z ostatnich 90 dni

**Implementacja:**
```java
// W ReviewSourceService.createSource()
@Transactional
public ReviewSourceDTO createSource(Long brandId, CreateSourceRequest request) {
    // Utwórz encję źródła
    ReviewSource source = new ReviewSource();
    // ... ustaw pola
    reviewSourceRepository.save(source);

    // Utwórz zadanie synchronizacji dla początkowego importu
    SyncJob job = new SyncJob();
    job.setReviewSourceId(source.getId());
    job.setJobType(JobType.INITIAL);
    job.setStatus(SyncStatus.PENDING);
    syncJobRepository.save(job);

    // Wywołaj asynchroniczny import (ostatnie 90 dni)
    LocalDate startDate = LocalDate.now().minusDays(90);
    syncScheduler.scheduleImport(job.getId(), source, startDate, LocalDate.now());

    return mapToDTO(source, job.getId());
}
```

**Zadanie asynchroniczne:** Działa w tle, aktualizuje status `sync_job`

---

#### Przeliczanie agregatów dashboardu

**Reguła:** Agregaty dashboardu muszą być aktualizowane gdy:
- Nowe opinie są importowane (CRON lub manualna synchronizacja)
- Użytkownik koryguje sentyment (US-007)

**Implementacja:**
```java
// W DashboardAggregateService.recalculateForSource()
@Transactional
public void recalculateForSource(Long reviewSourceId, LocalDate date) {
    // Agreguj opinie dla tego źródła w tej dacie
    List<Review> reviews = reviewRepository.findBySourceAndDate(reviewSourceId, date);

    DashboardAggregate aggregate = new DashboardAggregate();
    aggregate.setReviewSourceId(reviewSourceId);
    aggregate.setDate(date);
    aggregate.setTotalReviews(reviews.size());
    aggregate.setAvgRating(reviews.stream().mapToInt(Review::getRating).average().orElse(0.0));
    aggregate.setPositiveCount((int) reviews.stream().filter(r -> r.getSentiment() == POSITIVE).count());
    aggregate.setNegativeCount((int) reviews.stream().filter(r -> r.getSentiment() == NEGATIVE).count());
    aggregate.setNeutralCount((int) reviews.stream().filter(r -> r.getSentiment() == NEUTRAL).count());
    aggregate.setLastCalculatedAt(LocalDateTime.now());

    dashboardAggregateRepository.save(aggregate);
}
```

**Wydajność:** Wstępne obliczenia zapewniają <4s czas ładowania dashboardu

---

#### Zapobieganie duplikatom opinii

**Reguła:** Ta sama opinia nie może być importowana dwukrotnie (to samo źródło + ID zewnętrzne)

**Ograniczenie bazodanowe:**
```sql
UNIQUE (review_source_id, external_review_id)
```

**Obsługa w aplikacji:**
```java
// W ReviewImportService.importReview()
Optional<Review> existing = reviewRepository.findBySourceAndExternalId(sourceId, externalId);
if (existing.isPresent()) {
    // Zaktualizuj jeśli zawartość się zmieniła (na podstawie content_hash)
    if (!existing.get().getContentHash().equals(newContentHash)) {
        existing.get().setContent(newContent);
        existing.get().setContentHash(newContentHash);
        reviewRepository.save(existing.get());
        syncJob.incrementReviewsUpdated();
    }
    // Pomiń jeśli duplikat
    return;
}

// Utwórz nową opinię
Review newReview = new Review();
// ... ustaw pola
reviewRepository.save(newReview);
syncJob.incrementReviewsNew();
```

---

#### Cache'owanie podsumowań AI

**Reguła:** Podsumowania AI są cache'owane przez 24 godziny aby zredukować koszty API

**Implementacja:**
```java
// W AISummaryService.getSummaryForSource()
public AISummaryDTO getSummaryForSource(Long reviewSourceId) {
    // Sprawdź cache'owane prawidłowe podsumowanie
    Optional<AISummary> cached = aiSummaryRepository
        .findByReviewSourceIdAndValidUntilAfter(reviewSourceId, LocalDateTime.now());

    if (cached.isPresent()) {
        return mapToDTO(cached.get());
    }

    // Wygeneruj nowe podsumowanie
    List<Review> recentReviews = reviewRepository
        .findBySourceIdOrderByPublishedAtDesc(reviewSourceId, PageRequest.of(0, 100));

    String summaryText = openRouterClient.generateSummary(recentReviews);

    AISummary summary = new AISummary();
    summary.setReviewSourceId(reviewSourceId);
    summary.setSummaryText(summaryText);
    summary.setModelUsed("anthropic/claude-3-haiku");
    summary.setValidUntil(LocalDateTime.now().plusHours(24));
    aiSummaryRepository.save(summary);

    return mapToDTO(summary);
}
```

**Optymalizacja kosztów:** Redukuje wywołania API OpenRouter.ai

---

## 14. Obsługa błędów

### 14.1. Standardowy format odpowiedzi błędu

Wszystkie odpowiedzi błędów mają następującą strukturę:

```json
{
  "code": "ERROR_CODE",
  "message": "Czytelny komunikat błędu",
  "timestamp": "2025-01-19T10:30:00Z",
  "path": "/api/brands/1/reviews",
  "details": {
    "additionalField": "additionalValue"
  }
}
```

---

### 14.2. Kody statusu HTTP

| Kod statusu | Znaczenie | Kiedy używać |
|-------------|---------|-------------|
| 200 OK | Sukces | GET, PATCH, PUT zakończone sukcesem |
| 201 Created | Zasób utworzony | POST zakończony sukcesem (nowy użytkownik, marka, źródło) |
| 202 Accepted | Żądanie zaakceptowane, przetwarzanie asynchroniczne | Wywołana manualna synchronizacja |
| 204 No Content | Sukces, brak treści odpowiedzi | DELETE zakończony sukcesem |
| 400 Bad Request | Nieprawidłowe dane żądania | Błędy walidacji, nieprawidłowy JSON |
| 401 Unauthorized | Wymagane uwierzytelnienie | Brak/nieprawidłowy/wygasły token |
| 403 Forbidden | Niewystarczające uprawnienia | Użytkownik nie jest właścicielem zasobu, przekroczono limit planu |
| 404 Not Found | Zasób nie istnieje | Nieprawidłowe ID w ścieżce |
| 409 Conflict | Konflikt zasobów | Duplikat email, duplikat źródła |
| 422 Unprocessable Entity | Błąd walidacji semantycznej | Naruszenie reguły biznesowej |
| 429 Too Many Requests | Przekroczono limit częstotliwości | Manualne odświeżanie < 24h, ograniczanie API |
| 500 Internal Server Error | Błąd serwera | Nieoczekiwane wyjątki |
| 502 Bad Gateway | Błąd usługi zewnętrznej | Niedostępne Google API, niepowodzenie scrapera |
| 503 Service Unavailable | Tymczasowa niedostępność | Tryb konserwacji, awaria bazy danych |

---

### 14.3. Typowe kody błędów

#### Błędy uwierzytelniania
- `INVALID_CREDENTIALS`: Logowanie nie powiodło się
- `EMAIL_NOT_VERIFIED`: Konto niezaktywowane
- `INVALID_TOKEN`: JWT nieprawidłowy/wygasły
- `TOKEN_EXPIRED`: JWT wygasł (użyj tokenu odświeżającego)

#### Błędy walidacji
- `VALIDATION_ERROR`: Walidacja żądania nie powiodła się (zawiera błędy na poziomie pól)
- `INVALID_EMAIL_FORMAT`: Nieprawidłowy format email
- `PASSWORD_TOO_WEAK`: Hasło nie spełnia wymagań
- `PASSWORDS_DONT_MATCH`: Niezgodność potwierdzenia hasła

#### Błędy zasobów
- `RESOURCE_NOT_FOUND`: Żądany zasób nie istnieje
- `EMAIL_ALREADY_EXISTS`: Duplikat email podczas rejestracji
- `DUPLICATE_SOURCE`: Źródło opinii już skonfigurowane

#### Błędy autoryzacji
- `ACCESS_DENIED`: Użytkownik nie ma uprawnień
- `PLAN_LIMIT_EXCEEDED`: Osiągnięto limit planu darmowego (US-009)
- `RATE_LIMIT_EXCEEDED`: Limit częstotliwości manualnego odświeżania (US-008)

#### Błędy logiki biznesowej
- `BRAND_LIMIT_EXCEEDED`: MVP: 1 marka na użytkownika
- `SYNC_IN_PROGRESS`: Nie można wywołać synchronizacji podczas innej działającej
- `INVALID_DATE_RANGE`: endDate przed startDate

#### Błędy usług zewnętrznych
- `GOOGLE_API_ERROR`: Niepowodzenie API Google My Business
- `AI_SERVICE_UNAVAILABLE`: Niedostępny OpenRouter.ai
- `SCRAPER_FAILED`: Błąd scrapowania web

---

### 14.4. Przykład błędu walidacji

```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "invalid-email",
  "password": "weak",
  "confirmPassword": "weak123"
}
```

**Odpowiedź (400 Bad Request):**
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Walidacja żądania nie powiodła się",
  "timestamp": "2025-01-19T10:30:00Z",
  "path": "/api/auth/register",
  "errors": [
    {
      "field": "email",
      "rejectedValue": "invalid-email",
      "message": "Nieprawidłowy format email"
    },
    {
      "field": "password",
      "rejectedValue": "weak",
      "message": "Hasło musi mieć 8-100 znaków"
    },
    {
      "field": "confirmPassword",
      "rejectedValue": "weak123",
      "message": "Hasła muszą się zgadzać"
    }
  ]
}
```

---

## 15. Wydajność i cache'owanie

### 15.1. Strategia cache'owania

**Technologia:** Spring Cache z Caffeine

**Konfiguracja cache:**
- TTL: 10 minut (konfigurowalne)
- Maksymalny rozmiar: 500 wpisów
- Eksmisja: LRU (Least Recently Used)

**Cache'owane endpointy:**

| Klucz cache | Endpoint | TTL | Wyzwalacz unieważnienia |
|-----------|----------|-----|------------------------|
| `dashboard:brand:{brandId}` | GET /api/dashboard/summary | 10 min | Nowe opinie importowane, sentyment skorygowany |
| `summary:source:{sourceId}` | GET /api/dashboard/summary/ai | 24 godziny | Tylko manualne unieważnienie (podsumowania AI mają wbudowaną ważność) |
| `reviews:brand:{brandId}:filters:{hash}` | GET /api/brands/{brandId}/reviews | 5 min | Nowe opinie importowane |

**Unieważnianie cache:**
```java
// Po korekcie sentymentu
cacheManager.evict("dashboard:brand:" + brandId);
cacheManager.evict("reviews:brand:" + brandId + ":filters:*");

// Po zakończeniu zadania synchronizacji
cacheManager.evict("dashboard:brand:" + brandId);
cacheManager.evict("summary:source:" + sourceId);
```

---

### 15.2. Optymalizacja zapytań bazodanowych

**Użycie indeksów:**
1. `idx_reviews_composite_filter` - Filtrowanie wielokryterialne (źródło, sentyment, ocena, data)
2. `idx_reviews_negative` - Indeks częściowy dla rating <= 2 (optymalizacja US-005)
3. `idx_dashboard_aggregates_source_date` - Szybkie wyszukiwanie agregatów
4. `idx_user_activity_registration` - Obliczanie metryki Time to Value

**Paginacja:**
- Domyślny rozmiar strony: 20
- Maksymalny rozmiar strony: 100
- Użyj paginacji opartej na kursorze dla dużych zbiorów danych (przyszła optymalizacja)

**Cel wydajności zapytań:**
- Ładowanie dashboardu: <4 sekundy (wymóg PRD)
- Lista opinii: <2 sekundy
- GET pojedynczego zasobu: <500ms

---

### 15.3. Przetwarzanie asynchroniczne

**Zadania w tle:**
1. **Początkowy import 90-dniowy** (US-003)
   - Wyzwalacz: Po utworzeniu nowego źródła opinii
   - Czas trwania: 30-120 sekund (zależnie od liczby opinii)
   - Aktualizacje: Status tabeli `sync_jobs`

2. **Codzienna synchronizacja CRON** (PRD:29)
   - Harmonogram: Codziennie o 3:00 CET
   - Typ zadania: `SCHEDULED`
   - Proces: Pobieranie nowych opinii od ostatniej synchronizacji

3. **Przeliczanie agregatów dashboardu**
   - Wyzwalacz: Po zakończeniu synchronizacji, korekcie sentymentu
   - Aktualizacje: Tabela `dashboard_aggregates`

4. **Generowanie podsumowań AI**
   - Wyzwalacz: Na żądanie jeśli nie istnieje prawidłowe podsumowanie
   - Cache'owanie: 24-godzinny okres ważności

5. **Tygodniowe raporty emailowe** (PRD:54)
   - Harmonogram: Niedziela 6:00 CET
   - Zawartość: Nowe opinie, liczba negatywnych, kluczowe metryki

---

## 16. Wersjonowanie API

**Strategia:** Wersjonowanie ścieżki URL

**Bieżąca wersja:** v1

**Format:** `/api/v1/{resource}`

**Przykład:**
```
GET /api/v1/brands/1/reviews
```

**Przyszłe wersje:**
- Zmiany łamiące: Wprowadzenie `/api/v2/...`
- Zmiany nieełamiące: Dodanie do v1 (nowe opcjonalne pola, nowe endpointy)

**Polityka deprecjacji:**
- Wspieranie poprzedniej wersji przez 6 miesięcy po wydaniu nowej wersji
- Zwracanie nagłówka `Deprecation`: `Deprecation: Sun, 20 Jul 2025 00:00:00 GMT`
- Dokumentowanie przewodnika migracji w dokumentacji API

---

## 17. Ograniczanie częstotliwości

**Globalne limity częstotliwości:**
- Na użytkownika: 1000 żądań / godzinę
- Na IP: 5000 żądań / godzinę (nieuwierzytelnione)

**Limity specyficzne dla endpointów:**
- POST /api/auth/login: 5 żądań / 15 minut (ochrona przed brute-force)
- POST /api/auth/register: 3 żądania / godzinę na IP
- POST /api/brands/{brandId}/sync: 1 żądanie / 24 godziny (reguła biznesowa)

**Nagłówki odpowiedzi:**
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 987
X-RateLimit-Reset: 1642598400
```

**Przekroczono limit częstotliwości (429):**
```json
{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Zbyt wiele żądań. Proszę spróbować później.",
  "retryAfter": 3600,
  "limit": 1000,
  "resetAt": "2025-01-19T11:00:00Z"
}
```

---

## 18. Dokumentacja API

**Technologia:** Swagger / OpenAPI 3.0

**Endpointy:**
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Specyfikacja OpenAPI: `http://localhost:8080/api-docs`

**Funkcje:**
- Interaktywny eksplorator API
- Przykłady żądań/odpowiedzi
- Konfiguracja uwierzytelniania (JWT bearer)
- Funkcja wypróbuj

**Adnotacje dokumentacji:**
```java
@Tag(name = "Brands", description = "Endpointy zarządzania markami")
@Operation(summary = "Utwórz nową markę", description = "Tworzy encję marki dla uwierzytelnionego użytkownika")
@ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Marka utworzona pomyślnie"),
    @ApiResponse(responseCode = "409", description = "Użytkownik ma już markę (ograniczenie MVP)")
})
public ResponseEntity<BrandDTO> createBrand(@Valid @RequestBody CreateBrandRequest request) {
    // ...
}
```

---

## 19. Kwestie bezpieczeństwa

### 19.1. Bezpieczeństwo haseł
- Hashowanie: BCrypt z minimum 10 rundami
- Nigdy nie zwracaj `password_hash` w odpowiedziach API
- Tokeny resetowania hasła: UUID, wygaśnięcie po 1 godzinie, jednorazowego użytku

### 19.2. Szyfrowanie danych
- Dane dostępowe API: Szyfrowanie AES-256 na poziomie aplikacji
- Nigdy nie zwracaj `credentials_encrypted` w odpowiedziach API
- Klucz szyfrowania: Przechowywany w zmiennej środowiskowej lub menedżerze sekretów

### 19.3. Zapobieganie SQL Injection
- Użyj sparametryzowanych zapytań JPA (nigdy konkatenacji stringów)
- Spring Data JPA zapewnia automatyczną ochronę
- PostgreSQL RLS jako dodatkowa warstwa obrony

### 19.4. Zapobieganie XSS
- Frontend: React domyślnie escapuje output
- Backend: Sanityzuj dane wejściowe użytkownika (zawartość opinii, nazwy marek)
- Nagłówek Content-Security-Policy

### 19.5. CORS
- Biała lista tylko zaufanych originów
- Development: `http://localhost:3000`
- Produkcja: `https://app.brandpulse.io`

### 19.6. HTTPS
- Wymuszaj HTTPS w produkcji
- Nagłówek HSTS: `Strict-Transport-Security: max-age=31536000; includeSubDomains`

---

## 20. Monitorowanie i logowanie

### 20.1. Endpointy Actuator

**Sprawdzanie stanu:**
```http
GET /actuator/health
```

**Odpowiedź:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10485760,
        "exists": true
      }
    }
  }
}
```

**Metryki (format Prometheus):**
```http
GET /actuator/prometheus
```

**Kluczowe metryki:**
- `http_server_requests_seconds`: Czas trwania żądania
- `jvm_memory_used_bytes`: Użycie pamięci
- `hikaricp_connections_active`: Połączenia bazodanowe
- `cache_gets_total`: Współczynnik trafień/chybień cache
- `sync_job_duration_seconds`: Wydajność zadań synchronizacji

---

### 20.2. Strategia logowania

**Poziomy logów:**
- ERROR: Wyjątki, niepowodzenia zadań synchronizacji, błędy API zewnętrznych
- WARN: Osiągnięto limit częstotliwości, przekroczono limit planu, wolne zapytania (>2s)
- INFO: Rejestracja użytkownika, logowanie, konfiguracja źródła, zakończenie synchronizacji
- DEBUG: Szczegóły żądania/odpowiedzi, operacje cache (wyłączone w produkcji)

**Strukturalne logowanie (JSON):**
```json
{
  "timestamp": "2025-01-19T10:30:00Z",
  "level": "INFO",
  "logger": "com.brandpulse.service.ReviewSourceService",
  "message": "Źródło opinii utworzone pomyślnie",
  "userId": 1,
  "brandId": 1,
  "sourceId": 1,
  "sourceType": "GOOGLE",
  "traceId": "abc123def456"
}
```

**Filtrowanie wrażliwych danych:**
- Nigdy nie loguj haseł, tokenów, zaszyfrowanych danych dostępowych
- Maskuj adresy email: `u***@example.com`
- Maskuj zawartość opinii w logach (zgodność z RODO)

---

## 21. Strategia testowania

### 21.1. Testy jednostkowe
- Warstwa serwisowa: Walidacja logiki biznesowej
- Walidatory: Reguły walidacji DTO
- Mappery: Konwersja Entity ↔ DTO

### 21.2. Testy integracyjne
- Technologia: Testcontainers (PostgreSQL)
- Zakres: Warstwa repozytorium, ograniczenia bazodanowe
- Testy: Kaskady kluczy obcych, ograniczenia unikalności, polityki RLS

### 21.3. Testy API
- Technologia: Spring MockMvc lub RestAssured
- Zakres: Warstwa kontrolera, walidacja żądania/odpowiedzi
- Testy: Uwierzytelnianie, autoryzacja, obsługa błędów

### 21.4. Testy End-to-End
- Scenariusz: Rejestracja użytkownika → konfiguracja źródła → widok dashboardu
- Testy: Przepływ US-001 → US-003 → US-004
- Metryki: Time to Value (<10 minut)

---

## 22. Wdrożenie i CI/CD

### 22.1. Konfiguracja środowiska

**Development:**
- Baza danych: Docker Compose (PostgreSQL)
- API: `http://localhost:8080`
- Frontend: `http://localhost:3000`

**Staging:**
- Baza danych: AWS RDS PostgreSQL
- API: `https://api-staging.brandpulse.io`
- Frontend: `https://staging.brandpulse.io`

**Produkcja:**
- Baza danych: AWS RDS PostgreSQL (Multi-AZ)
- API: `https://api.brandpulse.io`
- Frontend: `https://app.brandpulse.io`

---

### 22.2. Pipeline CI/CD (GitHub Actions)

**Przy push do main:**
1. Uruchom testy (jednostkowe + integracyjne)
2. Zbuduj obraz Docker
3. Wypchnij do rejestru kontenerów
4. Wdróż na staging
5. Uruchom testy smoke
6. Manualne zatwierdzenie dla produkcji
7. Wdróż na produkcję

**Obraz Docker:**
```dockerfile
FROM eclipse-temurin:21-jre-alpine
COPY target/backend.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

---

## 23. Przyszłe ulepszenia

### 23.1. Funkcje fazy 2
- Integracje Facebook i Trustpilot
- Tygodniowe raporty emailowe
- Szkice odpowiedzi na opinie (wspomagane AI)
- Zaawansowana analityka (trendy, wykresy)

### 23.2. Dodatki do API
- `POST /api/reviews/{reviewId}/response`: Wygeneruj szkic odpowiedzi AI
- `GET /api/analytics/trends`: Trendy sentymentu w szeregu czasowym
- `POST /api/exports`: Eksport opinii do CSV/PDF
- `GET /api/notifications/preferences`: Ustawienia powiadomień

### 23.3. Optymalizacje wydajności
- Paginacja oparta na kursorze dla dużych zbiorów danych
- Endpoint GraphQL dla elastycznego pobierania danych
- Repliki do odczytu dla zapytań analitycznych
- Rozszerzenie TimescaleDB dla danych szeregów czasowych

---

## 24. Dodatek

### 24.1. Przykładowa ścieżka użytkownika (wywołania API)

**Onboarding nowego użytkownika (US-001, US-003):**

```http
# 1. Rejestracja
POST /api/auth/register
{
  "email": "anna@salon.pl",
  "password": "SecurePass123!",
  "confirmPassword": "SecurePass123!"
}
# Odpowiedź: 201 Created, zawiera token JWT

# 2. Utwórz markę
POST /api/brands
Authorization: Bearer {token}
{
  "name": "Salon Piękności Anny"
}
# Odpowiedź: 201 Created, brandId: 1

# 3. Dodaj źródło opinii Google
POST /api/brands/1/review-sources
Authorization: Bearer {token}
{
  "sourceType": "GOOGLE",
  "profileUrl": "https://www.google.com/maps/place/...",
  "externalProfileId": "ChIJN1t_tDeuEmsRUsoyG83frY4",
  "authMethod": "API"
}
# Odpowiedź: 201 Created, sourceId: 1, importJobId: 1

# 4. Odpytuj postęp importu
GET /api/sync-jobs/1
Authorization: Bearer {token}
# Odpowiedź: 200 OK, status: IN_PROGRESS
# (Odpytuj co 3 sekundy do czasu status: COMPLETED)

# 5. Wyświetl dashboard
GET /api/dashboard/summary?brandId=1
Authorization: Bearer {token}
# Odpowiedź: 200 OK, zagregowane metryki + podsumowanie AI

# 6. Wyświetl opinie
GET /api/brands/1/reviews?page=0&size=20
Authorization: Bearer {token}
# Odpowiedź: 200 OK, paginowana lista opinii
```

**Time to Value:** 5-8 minut (cel: <10 minut)

---

### 24.2. Odniesienie do schematu bazy danych

Pełna dokumentacja schematu bazy danych: `.ai/db-plan.md`

**Kluczowe tabele:**
- `users`: Konta użytkowników i informacje o planie
- `brands`: Encje marek użytkownika
- `review_sources`: Skonfigurowane źródła (Google, Facebook, Trustpilot)
- `reviews`: Indywidualne opinie klientów
- `sentiment_changes`: Ślad audytowy korekt sentymentu
- `dashboard_aggregates`: Wstępnie obliczone metryki dashboardu
- `ai_summaries`: Podsumowania tekstowe generowane przez AI
- `sync_jobs`: Śledzenie zadań synchronizacji
- `user_activity_log`: Aktywność użytkownika dla analityki

---

### 24.3. Śledzenie metryk sukcesu

**Time to Value (90% < 10 minut):**
```sql
SELECT
  user_id,
  MIN(CASE WHEN activity_type = 'USER_REGISTERED' THEN occurred_at END) AS registered_at,
  MIN(CASE WHEN activity_type = 'FIRST_SOURCE_CONFIGURED_SUCCESSFULLY' THEN occurred_at END) AS first_source_at,
  EXTRACT(EPOCH FROM (
    MIN(CASE WHEN activity_type = 'FIRST_SOURCE_CONFIGURED_SUCCESSFULLY' THEN occurred_at END) -
    MIN(CASE WHEN activity_type = 'USER_REGISTERED' THEN occurred_at END)
  )) / 60 AS minutes_to_value
FROM user_activity_log
GROUP BY user_id
HAVING MIN(CASE WHEN activity_type = 'FIRST_SOURCE_CONFIGURED_SUCCESSFULLY' THEN occurred_at END) IS NOT NULL;
```

**Wskaźnik aktywacji (60% w ciągu 7 dni):**
```sql
SELECT
  COUNT(DISTINCT CASE
    WHEN first_source_at <= registered_at + INTERVAL '7 days'
    THEN user_id
  END) * 100.0 / COUNT(DISTINCT user_id) AS activation_rate
FROM (
  SELECT
    user_id,
    MIN(CASE WHEN activity_type = 'USER_REGISTERED' THEN occurred_at END) AS registered_at,
    MIN(CASE WHEN activity_type = 'FIRST_SOURCE_CONFIGURED_SUCCESSFULLY' THEN occurred_at END) AS first_source_at
  FROM user_activity_log
  GROUP BY user_id
) subquery;
```

**Retencja (35% z 3+ logowaniami w 4 tygodnie):**
```sql
SELECT
  COUNT(DISTINCT CASE
    WHEN login_count >= 3
    THEN user_id
  END) * 100.0 / COUNT(DISTINCT user_id) AS retention_rate
FROM (
  SELECT
    user_id,
    MIN(CASE WHEN activity_type = 'USER_REGISTERED' THEN occurred_at END) AS registered_at,
    COUNT(CASE WHEN activity_type = 'LOGIN'
      AND occurred_at <= MIN(CASE WHEN activity_type = 'USER_REGISTERED' THEN occurred_at END) + INTERVAL '28 days'
      THEN 1
    END) AS login_count
  FROM user_activity_log
  GROUP BY user_id
) subquery;
```

---

## 25. Kontakt i wsparcie

**Problemy z API:** Zgłaszaj przez GitHub Issues lub support@brandpulse.io
**Wersja API:** v1
**Ostatnia aktualizacja:** 2025-01-19
**Dokumentacja:** https://docs.brandpulse.io/api