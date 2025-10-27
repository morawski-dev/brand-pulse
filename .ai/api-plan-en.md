# BrandPulse REST API Plan

## 1. Overview

This REST API provides backend services for the BrandPulse SaaS application, enabling small and medium-sized businesses to monitor and analyze customer reviews from multiple sources (Google, Facebook, Trustpilot). The API follows RESTful principles, uses JWT authentication, and is optimized for performance with caching and pre-calculated aggregates.

**Base URL:** `https://api.brandpulse.io` (production) or `http://localhost:8080` (development)

**API Version:** v1 (included in path: `/api/v1/...`)

**Authentication:** JWT Bearer token (except for public authentication endpoints)

**Content Type:** `application/json`

## 2. Resources

| Resource | Database Table | Description |
|----------|---------------|-------------|
| Authentication | users | User registration, login, password recovery |
| Users | users | User account management |
| Brands | brands | Brand/company entities managed by users |
| Review Sources | review_sources | Configured review sources (Google, Facebook, Trustpilot) |
| Reviews | reviews | Individual customer reviews |
| Dashboard | dashboard_aggregates, ai_summaries | Aggregated metrics and AI insights |
| Sync Jobs | sync_jobs | Review synchronization operations |
| User Activity | user_activity_log | User activity tracking for analytics |

## 3. Authentication Endpoints

### 3.1. Register New User

**US-001: New User Registration**

```http
POST /api/auth/register
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "confirmPassword": "SecurePass123!"
}
```

**Validation:**
- `email`: Required, valid email format, unique in system
- `password`: Required, minimum 8 characters, must contain uppercase, lowercase, number, special character
- `confirmPassword`: Required, must match password

**Success Response (201 Created):**
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

**Error Responses:**
- `400 Bad Request`: Invalid email format, passwords don't match, password too weak
  ```json
  {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "errors": [
      {
        "field": "email",
        "message": "Invalid email format"
      }
    ]
  }
  ```
- `409 Conflict`: Email already registered
  ```json
  {
    "code": "EMAIL_ALREADY_EXISTS",
    "message": "An account with this email already exists"
  }
  ```

**Business Logic:**
- Hash password using BCrypt (minimum 10 rounds)
- Set `plan_type='FREE'`, `max_sources_allowed=1`
- Generate JWT token with 60-minute expiration
- Log activity: `USER_REGISTERED`
- Auto-login user (return token immediately)

---

### 3.2. User Login

**US-002: System Login**

```http
POST /api/auth/login
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Success Response (200 OK):**
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

**Error Responses:**
- `401 Unauthorized`: Invalid credentials
  ```json
  {
    "code": "INVALID_CREDENTIALS",
    "message": "Invalid email or password"
  }
  ```
- `403 Forbidden`: Account not verified (if email verification required)
  ```json
  {
    "code": "EMAIL_NOT_VERIFIED",
    "message": "Please verify your email before logging in",
    "verificationRequired": true
  }
  ```

**Business Logic:**
- Validate credentials against hashed password
- Generate JWT token containing: userId, email, planType, maxSourcesAllowed
- Log activity: `LOGIN`

---

### 3.3. Request Password Reset

```http
POST /api/auth/forgot-password
```

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Success Response (200 OK):**
```json
{
  "message": "If an account with this email exists, a password reset link has been sent"
}
```

**Business Logic:**
- Generate unique `password_reset_token` (UUID)
- Set `password_reset_expires_at` to NOW() + 1 hour
- Send email with reset link: `https://app.brandpulse.io/reset-password?token={token}`
- Always return success (security: don't reveal if email exists)

---

### 3.4. Reset Password

```http
POST /api/auth/reset-password
```

**Request Body:**
```json
{
  "token": "abc123def456...",
  "newPassword": "NewSecurePass123!",
  "confirmPassword": "NewSecurePass123!"
}
```

**Success Response (200 OK):**
```json
{
  "message": "Password reset successfully. You can now log in with your new password."
}
```

**Error Responses:**
- `400 Bad Request`: Passwords don't match, weak password
- `401 Unauthorized`: Invalid or expired token
  ```json
  {
    "code": "INVALID_TOKEN",
    "message": "Password reset token is invalid or has expired"
  }
  ```

**Business Logic:**
- Verify token exists and hasn't expired
- Hash new password with BCrypt
- Update `password_hash`, clear `password_reset_token` and `password_reset_expires_at`
- Invalidate all existing JWT tokens for this user (optional security measure)

---

### 3.5. Logout

```http
POST /api/auth/logout
```

**Headers:**
```
Authorization: Bearer {token}
```

**Success Response (200 OK):**
```json
{
  "message": "Logged out successfully"
}
```

**Business Logic:**
- Log activity: `LOGOUT`
- Add token to blacklist (if implementing token revocation)
- Client should delete token from local storage

---

## 4. User Endpoints

### 4.1. Get Current User Profile

```http
GET /api/users/me
```

**Headers:**
```
Authorization: Bearer {token}
```

**Success Response (200 OK):**
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

**Error Responses:**
- `401 Unauthorized`: Missing or invalid token

---

### 4.2. Update User Profile

```http
PATCH /api/users/me
```

**Request Body:**
```json
{
  "email": "newemail@example.com"
}
```

**Success Response (200 OK):**
```json
{
  "userId": 1,
  "email": "newemail@example.com",
  "emailVerified": false,
  "updatedAt": "2025-01-20T10:30:00Z"
}
```

**Business Logic:**
- If email changed, set `email_verified=false` and send verification email
- Updating email requires re-verification

---

## 5. Brand Endpoints

### 5.1. Create Brand

**US-003: Configuring First Source (Step 1)**

```http
POST /api/brands
```

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "name": "My Restaurant Chain"
}
```

**Validation:**
- `name`: Required, 1-255 characters

**Success Response (201 Created):**
```json
{
  "brandId": 1,
  "userId": 1,
  "name": "My Restaurant Chain",
  "sourceCount": 0,
  "lastManualRefreshAt": null,
  "createdAt": "2025-01-19T12:00:00Z",
  "updatedAt": "2025-01-19T12:00:00Z"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid name
- `409 Conflict`: User already has a brand (MVP limitation)
  ```json
  {
    "code": "BRAND_LIMIT_EXCEEDED",
    "message": "MVP supports one brand per user account"
  }
  ```

---

### 5.2. Get User's Brands

```http
GET /api/brands
```

**Headers:**
```
Authorization: Bearer {token}
```

**Success Response (200 OK):**
```json
{
  "brands": [
    {
      "brandId": 1,
      "name": "My Restaurant Chain",
      "sourceCount": 2,
      "lastManualRefreshAt": "2025-01-19T08:00:00Z",
      "createdAt": "2025-01-19T12:00:00Z",
      "updatedAt": "2025-01-19T12:00:00Z"
    }
  ]
}
```

**Business Logic:**
- Filter brands by authenticated user (JWT userId)
- Exclude soft-deleted brands (`deleted_at IS NULL`)
- Include count of active review sources

---

### 5.3. Get Brand by ID

```http
GET /api/brands/{brandId}
```

**Success Response (200 OK):**
```json
{
  "brandId": 1,
  "name": "My Restaurant Chain",
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

**Error Responses:**
- `403 Forbidden`: User doesn't own this brand
- `404 Not Found`: Brand doesn't exist

---

### 5.4. Update Brand

```http
PATCH /api/brands/{brandId}
```

**Request Body:**
```json
{
  "name": "Updated Restaurant Chain Name"
}
```

**Success Response (200 OK):**
```json
{
  "brandId": 1,
  "name": "Updated Restaurant Chain Name",
  "updatedAt": "2025-01-20T10:45:00Z"
}
```

---

### 5.5. Delete Brand

```http
DELETE /api/brands/{brandId}
```

**Success Response (204 No Content)**

**Business Logic:**
- Soft delete: Set `deleted_at=NOW()`
- Cascade: All review_sources, reviews, dashboard_aggregates are also soft-deleted (ON DELETE CASCADE)
- Hard delete after 90 days (background job)

---

## 6. Review Source Endpoints

### 6.1. Create Review Source

**US-003: Configuring First Source (Step 2)**
**US-009: Free Plan Limitation**

```http
POST /api/brands/{brandId}/review-sources
```

**Request Body:**
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

**Validation:**
- `sourceType`: Required, must be 'GOOGLE', 'FACEBOOK', or 'TRUSTPILOT'
- `profileUrl`: Required, valid URL format
- `externalProfileId`: Required, unique per brand+sourceType
- `authMethod`: Required, must be 'API' or 'SCRAPING'

**Success Response (201 Created):**
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

**Error Responses:**
- `400 Bad Request`: Invalid sourceType, invalid URL
- `403 Forbidden`: User doesn't own brand, or plan limit exceeded
  ```json
  {
    "code": "PLAN_LIMIT_EXCEEDED",
    "message": "Free plan allows 1 review source. Paid plans coming soon.",
    "currentCount": 1,
    "maxAllowed": 1,
    "planType": "FREE"
  }
  ```
- `409 Conflict`: Duplicate source (same brand+sourceType+externalProfileId)
  ```json
  {
    "code": "DUPLICATE_SOURCE",
    "message": "This review source is already configured for your brand"
  }
  ```

**Business Logic:**
1. Check user's plan: Count existing active sources for brand
2. If count >= `max_sources_allowed`, return 403 with plan upgrade message
3. Encrypt credentials using AES-256 before storing
4. Create `sync_job` with `job_type='INITIAL'`, `status='PENDING'`
5. Trigger background job to import last 90 days of reviews
6. Return `importJobId` for progress tracking
7. Log activity: `SOURCE_ADDED`, then `FIRST_SOURCE_CONFIGURED_SUCCESSFULLY` if first source

---

### 6.2. List Review Sources for Brand

```http
GET /api/brands/{brandId}/review-sources
```

**Success Response (200 OK):**
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
      "lastSyncError": "API rate limit exceeded",
      "nextScheduledSyncAt": "2025-01-21T03:00:00Z",
      "createdAt": "2025-01-19T14:00:00Z"
    }
  ]
}
```

**Business Logic:**
- Filter by brandId and `deleted_at IS NULL`
- Never return `credentials_encrypted` field in API responses (security)

---

### 6.3. Get Review Source by ID

```http
GET /api/brands/{brandId}/review-sources/{sourceId}
```

**Success Response (200 OK):**
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

**Error Responses:**
- `403 Forbidden`: User doesn't own this brand/source
- `404 Not Found`: Source doesn't exist

---

### 6.4. Update Review Source

```http
PATCH /api/brands/{brandId}/review-sources/{sourceId}
```

**Request Body:**
```json
{
  "isActive": false,
  "profileUrl": "https://www.google.com/maps/place/updated-url"
}
```

**Success Response (200 OK):**
```json
{
  "sourceId": 1,
  "isActive": false,
  "updatedAt": "2025-01-20T11:00:00Z"
}
```

**Business Logic:**
- Setting `isActive=false` pauses automatic syncs
- Updating URL or credentials may trigger validation job

---

### 6.5. Delete Review Source

```http
DELETE /api/brands/{brandId}/review-sources/{sourceId}
```

**Success Response (204 No Content)**

**Business Logic:**
- Soft delete: Set `deleted_at=NOW()`
- Cascade deletes reviews, dashboard_aggregates, ai_summaries, sync_jobs
- Log activity: `SOURCE_DELETED`

---

## 7. Review Endpoints

### 7.1. List Reviews

**US-004: Viewing Aggregated Reviews**
**US-005: Filtering Negative Reviews**
**US-006: Switching Between Locations**

```http
GET /api/brands/{brandId}/reviews
```

**Query Parameters:**
- `sourceId` (optional): Filter by specific review source. If omitted, returns reviews from all sources (aggregated view - US-006).
- `sentiment` (optional): Filter by sentiment. Values: `POSITIVE`, `NEGATIVE`, `NEUTRAL`. Can be comma-separated for multiple: `sentiment=NEGATIVE,NEUTRAL`.
- `rating` (optional): Filter by star rating. Values: `1`, `2`, `3`, `4`, `5`. Can be comma-separated: `rating=1,2` (US-005).
- `startDate` (optional): Filter reviews published after this date. Format: ISO 8601 (`2025-01-01T00:00:00Z`).
- `endDate` (optional): Filter reviews published before this date.
- `page` (optional): Page number (0-indexed). Default: `0`.
- `size` (optional): Items per page. Default: `20`, Max: `100`.
- `sort` (optional): Sort field and direction. Default: `publishedAt,desc`. Options: `publishedAt,asc`, `rating,desc`, etc.

**Example Request:**
```http
GET /api/brands/1/reviews?rating=1,2&sentiment=NEGATIVE&page=0&size=20&sort=publishedAt,desc
```

**Success Response (200 OK):**
```json
{
  "reviews": [
    {
      "reviewId": 123,
      "sourceId": 1,
      "sourceType": "GOOGLE",
      "externalReviewId": "ChdDSUhNMG9nS0VJQ0FnSURqMGZxT3ZRRRAB",
      "content": "Terrible service. Waited 45 minutes for cold food. Never coming back!",
      "authorName": "John Doe",
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
      "content": "Food was okay, but nothing special. Overpriced for what you get.",
      "authorName": "Jane Smith",
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

**Error Responses:**
- `400 Bad Request`: Invalid filter values, invalid date format
- `403 Forbidden`: User doesn't own this brand

**Business Logic:**
- Use `idx_reviews_composite_filter` index for multi-criteria filtering (performance)
- Use `idx_reviews_negative` partial index when filtering `rating<=2` (US-005 optimization)
- Filter `deleted_at IS NULL`
- Log activity: `VIEW_DASHBOARD` on first page load

**Performance:**
- Dashboard must load in <4 seconds (PRD requirement)
- Use Spring Cache with Caffeine (10-minute TTL)
- Pagination prevents loading thousands of reviews at once

---

### 7.2. Get Review by ID

```http
GET /api/brands/{brandId}/reviews/{reviewId}
```

**Success Response (200 OK):**
```json
{
  "reviewId": 123,
  "sourceId": 1,
  "sourceType": "GOOGLE",
  "externalReviewId": "ChdDSUhNMG9nS0VJQ0FnSURqMGZxT3ZRRRAB",
  "content": "Terrible service. Waited 45 minutes for cold food. Never coming back!",
  "contentHash": "a1b2c3d4e5f6...",
  "authorName": "John Doe",
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

**Error Responses:**
- `403 Forbidden`: User doesn't own the brand containing this review
- `404 Not Found`: Review doesn't exist

---

### 7.3. Update Review Sentiment

**US-007: Manual Sentiment Correction**

```http
PATCH /api/brands/{brandId}/reviews/{reviewId}/sentiment
```

**Request Body:**
```json
{
  "sentiment": "NEGATIVE"
}
```

**Validation:**
- `sentiment`: Required, must be 'POSITIVE', 'NEGATIVE', or 'NEUTRAL'

**Success Response (200 OK):**
```json
{
  "reviewId": 123,
  "sentiment": "NEGATIVE",
  "previousSentiment": "NEUTRAL",
  "updatedAt": "2025-01-19T10:00:00Z",
  "sentimentChangeId": 42
}
```

**Error Responses:**
- `400 Bad Request`: Invalid sentiment value
- `403 Forbidden`: User doesn't own this review
- `404 Not Found`: Review doesn't exist

**Business Logic:**
1. Verify user owns the brand containing this review
2. Record change in `sentiment_changes` table:
   - `old_sentiment`: Current review.sentiment
   - `new_sentiment`: Requested sentiment
   - `changed_by_user_id`: JWT userId
   - `change_reason`: 'USER_CORRECTION'
3. Update `reviews.sentiment` and `reviews.updated_at`
4. Invalidate cached dashboard aggregates for this review's source
5. Trigger async recalculation of `dashboard_aggregates` for affected date
6. Log activity: `SENTIMENT_CORRECTED`

**Cache Invalidation:**
- Clear cache key: `dashboard:brand:{brandId}`
- Clear cache key: `summary:source:{reviewSourceId}`

---

## 8. Dashboard Endpoints

### 8.1. Get Dashboard Summary

**US-004: Viewing Aggregated Reviews (Summary Section)**
**US-006: Switching Between Locations**

```http
GET /api/dashboard/summary
```

**Query Parameters:**
- `brandId` (required): Brand ID to get summary for
- `sourceId` (optional): Filter by specific source. If omitted, returns aggregated data for all sources (US-006 "All locations").
- `startDate` (optional): Summary start date. Default: 90 days ago.
- `endDate` (optional): Summary end date. Default: today.

**Example Request:**
```http
GET /api/dashboard/summary?brandId=1&sourceId=2&startDate=2024-10-20&endDate=2025-01-19
```

**Success Response (200 OK):**
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
    "text": "75% positive reviews. Customers consistently praise the speed of service and fresh ingredients. Main complaints focus on pricing (mentioned in 18 reviews) and limited parking availability (12 reviews). Recent trend: Increased positive feedback on new menu items introduced in December.",
    "modelUsed": "anthropic/claude-3-haiku",
    "generatedAt": "2025-01-19T03:05:00Z",
    "validUntil": "2025-01-20T03:05:00Z"
  },
  "recentNegativeReviews": [
    {
      "reviewId": 123,
      "rating": 1,
      "content": "Terrible service. Waited 45 minutes...",
      "publishedAt": "2025-01-18T14:30:00Z"
    },
    {
      "reviewId": 119,
      "rating": 2,
      "content": "Overpriced for portion sizes...",
      "publishedAt": "2025-01-17T11:20:00Z"
    }
  ],
  "lastUpdated": "2025-01-20T03:00:00Z"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid date format, endDate before startDate
- `403 Forbidden`: User doesn't own this brand
- `404 Not Found`: Brand or source doesn't exist

**Business Logic:**
- Pull data from `dashboard_aggregates` table (pre-calculated during CRON job)
- If `sourceId` provided, filter aggregates by that source only
- If `sourceId` omitted, SUM aggregates across all sources for the brand (US-006 "All locations")
- Fetch latest valid AI summary from `ai_summaries` WHERE `valid_until > NOW()`
- Include top 3 most recent negative reviews (rating <= 2) for quick access
- Use cached results (Spring Cache with Caffeine, 10-minute TTL)

**Performance Optimization:**
- Critical: Must load in <4 seconds (PRD requirement)
- Pre-calculation in `dashboard_aggregates` reduces query complexity
- Materialized view `mv_brand_aggregates` for "All locations" view (optional)
- Caching strategy: Cache key `dashboard:brand:{brandId}:source:{sourceId}`

---

### 8.2. Get AI Summary for Source

```http
GET /api/dashboard/summary/ai
```

**Query Parameters:**
- `sourceId` (required): Review source ID

**Success Response (200 OK):**
```json
{
  "summaryId": 15,
  "sourceId": 2,
  "text": "75% positive reviews. Customers consistently praise the speed of service and fresh ingredients. Main complaints focus on pricing (mentioned in 18 reviews) and limited parking availability (12 reviews). Recent trend: Increased positive feedback on new menu items introduced in December.",
  "modelUsed": "anthropic/claude-3-haiku",
  "tokenCount": 1243,
  "generatedAt": "2025-01-19T03:05:00Z",
  "validUntil": "2025-01-20T03:05:00Z"
}
```

**Business Logic:**
- Return latest summary WHERE `valid_until > NOW()`
- If no valid summary exists, trigger async generation and return 202 Accepted
- Cache with key: `summary:source:{sourceId}`

---

## 9. Sync Job Endpoints

### 9.1. Trigger Manual Sync

**US-008: Manual Data Refresh**

```http
POST /api/brands/{brandId}/sync
```

**Request Body (optional):**
```json
{
  "sourceId": 2
}
```

**Validation:**
- `sourceId` (optional): If provided, sync only this source. If omitted, sync all sources for the brand.

**Success Response (202 Accepted):**
```json
{
  "message": "Manual sync initiated successfully",
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

**Error Responses:**
- `403 Forbidden`: User doesn't own this brand
- `429 Too Many Requests`: Manual refresh already triggered in last 24 hours
  ```json
  {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Manual sync can only be triggered once per 24 hours",
    "lastManualRefreshAt": "2025-01-19T08:00:00Z",
    "nextAvailableAt": "2025-01-20T08:00:00Z",
    "hoursRemaining": 22
  }
  ```

**Business Logic:**
1. Check `brands.last_manual_refresh_at`
2. If `last_manual_refresh_at > (NOW() - INTERVAL '24 hours')`, return 429 with time remaining
3. Update `brands.last_manual_refresh_at = NOW()`
4. Create `sync_job(s)` with `job_type='MANUAL'`, `status='PENDING'`
5. Trigger async sync process for each source
6. Return job IDs for progress tracking
7. Log activity: `MANUAL_REFRESH_TRIGGERED`

**Rate Limiting:**
- 24-hour rolling window (not calendar day)
- Calculated: `last_manual_refresh_at + 24 hours`
- Button disabled on frontend until `nextAvailableAt`

---

### 9.2. Get Sync Job Status

```http
GET /api/sync-jobs/{jobId}
```

**Success Response (200 OK):**
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

**Status Values:**
- `PENDING`: Job queued, not started
- `IN_PROGRESS`: Currently fetching reviews
- `COMPLETED`: Successfully finished
- `FAILED`: Error occurred (see `errorMessage`)

**Error Responses:**
- `403 Forbidden`: User doesn't own the brand containing this source
- `404 Not Found`: Job doesn't exist

**Business Logic:**
- Used for polling during initial 90-day import (US-003)
- Used to track manual sync progress (US-008)
- Frontend can poll every 2-3 seconds until status != PENDING|IN_PROGRESS

---

### 9.3. List Sync Jobs for Source

```http
GET /api/brands/{brandId}/review-sources/{sourceId}/sync-jobs
```

**Query Parameters:**
- `page` (optional): Default 0
- `size` (optional): Default 20
- `status` (optional): Filter by status

**Success Response (200 OK):**
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

**Business Logic:**
- Ordered by `created_at DESC`
- Useful for debugging sync issues and monitoring import history

---

## 10. Activity Log Endpoints (Internal/Analytics)

### 10.1. Get User Activity Log

```http
GET /api/users/me/activity
```

**Query Parameters:**
- `activityType` (optional): Filter by type
- `startDate` (optional): Filter by date range
- `endDate` (optional): Filter by date range
- `page`, `size`: Pagination

**Success Response (200 OK):**
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

**Business Logic:**
- Used for success metrics calculation:
  - **Time to Value**: `USER_REGISTERED` → `FIRST_SOURCE_CONFIGURED_SUCCESSFULLY` < 10 minutes
  - **Activation**: Count `LOGIN` events in first 4 weeks >= 3
  - **Retention**: 35% of users log in 3+ times in first 4 weeks
- Stored in `user_activity_log` table
- Metadata JSONB column allows flexible event properties

**Activity Types:**
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

## 11. Email Report Endpoints (Future)

### 11.1. List Email Reports

```http
GET /api/users/me/email-reports
```

**Success Response (200 OK):**
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

**Business Logic:**
- Track email engagement for retention analysis
- Weekly reports sent automatically (background job)
- Opened/clicked tracked via email tracking pixels and UTM links

---

## 12. Authentication and Authorization

### 12.1. Authentication Mechanism

**Method:** JWT (JSON Web Token) Bearer authentication

**Token Structure:**
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

**Header Format:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Token Expiration:**
- Access Token: 60 minutes (configurable via `jwt.expiration-minutes`)
- Refresh Token: 7 days (future implementation)

**Token Storage:**
- Client: Local storage or HTTP-only cookie (recommended for security)
- Server: Stateless (no server-side session storage)
- Optional: Token blacklist for logout (Redis cache)

---

### 12.2. Authorization Rules

**Resource Ownership Validation:**
1. **Brands**: User can only access brands where `brand.user_id = JWT.userId`
2. **Review Sources**: User can access source if they own the parent brand
3. **Reviews**: User can access review if they own the parent brand → source chain
4. **Dashboard**: User can only see dashboard for their own brands

**Database Row-Level Security (RLS):**
- Primary: Spring Security checks in service layer
- Secondary: PostgreSQL RLS policies (defense-in-depth)
- Application sets session variable: `SET LOCAL app.current_user_id = {userId}`

**Public Endpoints (No Auth Required):**
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `GET /api/health`
- `GET /actuator/health`

**Protected Endpoints:**
- All other endpoints require valid JWT token
- Return 401 Unauthorized if token missing/invalid/expired
- Return 403 Forbidden if user lacks permission for resource

---

### 12.3. CORS Configuration

**Allowed Origins:**
- Development: `http://localhost:3000`
- Production: `https://app.brandpulse.io`

**Allowed Methods:**
- `GET`, `POST`, `PATCH`, `PUT`, `DELETE`, `OPTIONS`

**Allowed Headers:**
- `Authorization`, `Content-Type`, `Accept`

**Credentials:**
- Allowed (for cookies if using HTTP-only cookie auth)

---

## 13. Validation and Business Logic

### 13.1. Request Validation

**Technology:** Jakarta Bean Validation (JSR 380)

**Common Validations:**

#### User Registration
```java
@NotBlank(message = "Email is required")
@Email(message = "Invalid email format")
@Size(max = 255)
private String email;

@NotBlank(message = "Password is required")
@Size(min = 8, max = 100, message = "Password must be 8-100 characters")
@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
         message = "Password must contain uppercase, lowercase, number, and special character")
private String password;
```

#### Review Source
```java
@NotNull(message = "Source type is required")
@Enumerated(EnumType.STRING)
private SourceType sourceType; // GOOGLE, FACEBOOK, TRUSTPILOT

@NotBlank(message = "Profile URL is required")
@URL(message = "Invalid URL format")
private String profileUrl;

@NotBlank(message = "External profile ID is required")
@Size(max = 255)
private String externalProfileId;
```

#### Review Sentiment Update
```java
@NotNull(message = "Sentiment is required")
@Enumerated(EnumType.STRING)
private Sentiment sentiment; // POSITIVE, NEGATIVE, NEUTRAL
```

---

### 13.2. Business Logic Rules

#### Freemium Plan Limits (US-009)

**Rule:** Free plan users can configure maximum 1 review source

**Validation:**
```java
// In ReviewSourceService.createSource()
int activeSourceCount = reviewSourceRepository.countByBrandIdAndDeletedAtIsNull(brandId);
if (activeSourceCount >= user.getMaxSourcesAllowed()) {
    throw new PlanLimitExceededException(
        "Free plan allows " + user.getMaxSourcesAllowed() + " review source. Paid plans coming soon.",
        activeSourceCount,
        user.getMaxSourcesAllowed()
    );
}
```

**HTTP Response:** 403 Forbidden with plan upgrade message

---

#### Manual Refresh Rate Limiting (US-008)

**Rule:** Users can manually trigger sync once per 24 hours (rolling window)

**Validation:**
```java
// In BrandService.triggerManualSync()
LocalDateTime lastRefresh = brand.getLastManualRefreshAt();
if (lastRefresh != null && lastRefresh.isAfter(LocalDateTime.now().minusHours(24))) {
    LocalDateTime nextAvailable = lastRefresh.plusHours(24);
    long hoursRemaining = ChronoUnit.HOURS.between(LocalDateTime.now(), nextAvailable);
    throw new RateLimitExceededException(
        "Manual sync can only be triggered once per 24 hours",
        lastRefresh,
        nextAvailable,
        hoursRemaining
    );
}
```

**HTTP Response:** 429 Too Many Requests

---

#### Sentiment Correction Audit Trail (US-007)

**Rule:** All sentiment changes must be logged in `sentiment_changes` table

**Implementation:**
```java
// In ReviewService.updateSentiment()
@Transactional
public ReviewDTO updateSentiment(Long reviewId, Sentiment newSentiment, Long userId) {
    Review review = reviewRepository.findById(reviewId).orElseThrow();
    Sentiment oldSentiment = review.getSentiment();

    // Record change in audit table
    SentimentChange change = new SentimentChange();
    change.setReviewId(reviewId);
    change.setOldSentiment(oldSentiment);
    change.setNewSentiment(newSentiment);
    change.setChangedByUserId(userId);
    change.setChangeReason(ChangeReason.USER_CORRECTION);
    sentimentChangeRepository.save(change);

    // Update review
    review.setSentiment(newSentiment);
    reviewRepository.save(review);

    // Invalidate cache and trigger recalculation
    cacheManager.evict("dashboard:brand:" + review.getBrand().getId());
    dashboardAggregateService.recalculateForSource(review.getReviewSourceId());

    return mapToDTO(review);
}
```

**Success Metric:** 75% AI accuracy (measured via sentiment_changes table)

---

#### 90-Day Historical Import (PRD:28)

**Rule:** When creating new review source, import last 90 days of reviews

**Implementation:**
```java
// In ReviewSourceService.createSource()
@Transactional
public ReviewSourceDTO createSource(Long brandId, CreateSourceRequest request) {
    // Create source entity
    ReviewSource source = new ReviewSource();
    // ... set fields
    reviewSourceRepository.save(source);

    // Create sync job for initial import
    SyncJob job = new SyncJob();
    job.setReviewSourceId(source.getId());
    job.setJobType(JobType.INITIAL);
    job.setStatus(SyncStatus.PENDING);
    syncJobRepository.save(job);

    // Trigger async import (last 90 days)
    LocalDate startDate = LocalDate.now().minusDays(90);
    syncScheduler.scheduleImport(job.getId(), source, startDate, LocalDate.now());

    return mapToDTO(source, job.getId());
}
```

**Async Job:** Runs in background, updates `sync_job` status

---

#### Dashboard Aggregates Recalculation

**Rule:** Dashboard aggregates must be updated when:
- New reviews are imported (CRON or manual sync)
- User corrects sentiment (US-007)

**Implementation:**
```java
// In DashboardAggregateService.recalculateForSource()
@Transactional
public void recalculateForSource(Long reviewSourceId, LocalDate date) {
    // Aggregate reviews for this source on this date
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

**Performance:** Pre-calculation ensures <4s dashboard load time

---

#### Duplicate Review Prevention

**Rule:** Same review cannot be imported twice (same source + external ID)

**Database Constraint:**
```sql
UNIQUE (review_source_id, external_review_id)
```

**Application Handling:**
```java
// In ReviewImportService.importReview()
Optional<Review> existing = reviewRepository.findBySourceAndExternalId(sourceId, externalId);
if (existing.isPresent()) {
    // Update if content changed (based on content_hash)
    if (!existing.get().getContentHash().equals(newContentHash)) {
        existing.get().setContent(newContent);
        existing.get().setContentHash(newContentHash);
        reviewRepository.save(existing.get());
        syncJob.incrementReviewsUpdated();
    }
    // Skip if duplicate
    return;
}

// Create new review
Review newReview = new Review();
// ... set fields
reviewRepository.save(newReview);
syncJob.incrementReviewsNew();
```

---

#### AI Summary Caching

**Rule:** AI summaries are cached for 24 hours to reduce API costs

**Implementation:**
```java
// In AISummaryService.getSummaryForSource()
public AISummaryDTO getSummaryForSource(Long reviewSourceId) {
    // Check for valid cached summary
    Optional<AISummary> cached = aiSummaryRepository
        .findByReviewSourceIdAndValidUntilAfter(reviewSourceId, LocalDateTime.now());

    if (cached.isPresent()) {
        return mapToDTO(cached.get());
    }

    // Generate new summary
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

**Cost Optimization:** Reduces OpenRouter.ai API calls

---

## 14. Error Handling

### 14.1. Standard Error Response Format

All error responses follow this structure:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable error message",
  "timestamp": "2025-01-19T10:30:00Z",
  "path": "/api/brands/1/reviews",
  "details": {
    "additionalField": "additionalValue"
  }
}
```

---

### 14.2. HTTP Status Codes

| Status Code | Meaning | When to Use |
|-------------|---------|-------------|
| 200 OK | Success | GET, PATCH, PUT successful |
| 201 Created | Resource created | POST successful (new user, brand, source) |
| 202 Accepted | Request accepted, processing asynchronously | Manual sync triggered |
| 204 No Content | Success, no response body | DELETE successful |
| 400 Bad Request | Invalid request data | Validation errors, malformed JSON |
| 401 Unauthorized | Authentication required | Missing/invalid/expired token |
| 403 Forbidden | Insufficient permissions | User doesn't own resource, plan limit exceeded |
| 404 Not Found | Resource doesn't exist | Invalid ID in path |
| 409 Conflict | Resource conflict | Duplicate email, duplicate source |
| 422 Unprocessable Entity | Semantic validation error | Business rule violation |
| 429 Too Many Requests | Rate limit exceeded | Manual refresh < 24h, API throttling |
| 500 Internal Server Error | Server error | Unexpected exceptions |
| 502 Bad Gateway | External service error | Google API down, scraper failed |
| 503 Service Unavailable | Temporary unavailability | Maintenance mode, database down |

---

### 14.3. Common Error Codes

#### Authentication Errors
- `INVALID_CREDENTIALS`: Login failed
- `EMAIL_NOT_VERIFIED`: Account not activated
- `INVALID_TOKEN`: JWT invalid/expired
- `TOKEN_EXPIRED`: JWT expired (use refresh token)

#### Validation Errors
- `VALIDATION_ERROR`: Request validation failed (includes field-level errors)
- `INVALID_EMAIL_FORMAT`: Email format invalid
- `PASSWORD_TOO_WEAK`: Password doesn't meet requirements
- `PASSWORDS_DONT_MATCH`: Password confirmation mismatch

#### Resource Errors
- `RESOURCE_NOT_FOUND`: Requested resource doesn't exist
- `EMAIL_ALREADY_EXISTS`: Duplicate email during registration
- `DUPLICATE_SOURCE`: Review source already configured

#### Authorization Errors
- `ACCESS_DENIED`: User doesn't have permission
- `PLAN_LIMIT_EXCEEDED`: Free plan limit reached (US-009)
- `RATE_LIMIT_EXCEEDED`: Manual refresh rate limit (US-008)

#### Business Logic Errors
- `BRAND_LIMIT_EXCEEDED`: MVP: 1 brand per user
- `SYNC_IN_PROGRESS`: Cannot trigger sync while another running
- `INVALID_DATE_RANGE`: endDate before startDate

#### External Service Errors
- `GOOGLE_API_ERROR`: Google My Business API failure
- `AI_SERVICE_UNAVAILABLE`: OpenRouter.ai down
- `SCRAPER_FAILED`: Web scraping error

---

### 14.4. Validation Error Example

```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "invalid-email",
  "password": "weak",
  "confirmPassword": "weak123"
}
```

**Response (400 Bad Request):**
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "timestamp": "2025-01-19T10:30:00Z",
  "path": "/api/auth/register",
  "errors": [
    {
      "field": "email",
      "rejectedValue": "invalid-email",
      "message": "Invalid email format"
    },
    {
      "field": "password",
      "rejectedValue": "weak",
      "message": "Password must be 8-100 characters"
    },
    {
      "field": "confirmPassword",
      "rejectedValue": "weak123",
      "message": "Passwords must match"
    }
  ]
}
```

---

## 15. Performance and Caching

### 15.1. Caching Strategy

**Technology:** Spring Cache with Caffeine

**Cache Configuration:**
- TTL: 10 minutes (configurable)
- Max Size: 500 entries
- Eviction: LRU (Least Recently Used)

**Cached Endpoints:**

| Cache Key | Endpoint | TTL | Invalidation Trigger |
|-----------|----------|-----|---------------------|
| `dashboard:brand:{brandId}` | GET /api/dashboard/summary | 10 min | New reviews imported, sentiment corrected |
| `summary:source:{sourceId}` | GET /api/dashboard/summary/ai | 24 hours | Manual invalidation only (AI summaries have built-in validity) |
| `reviews:brand:{brandId}:filters:{hash}` | GET /api/brands/{brandId}/reviews | 5 min | New reviews imported |

**Cache Invalidation:**
```java
// After sentiment correction
cacheManager.evict("dashboard:brand:" + brandId);
cacheManager.evict("reviews:brand:" + brandId + ":filters:*");

// After sync job completion
cacheManager.evict("dashboard:brand:" + brandId);
cacheManager.evict("summary:source:" + sourceId);
```

---

### 15.2. Database Query Optimization

**Index Usage:**
1. `idx_reviews_composite_filter` - Multi-criteria filtering (source, sentiment, rating, date)
2. `idx_reviews_negative` - Partial index for rating <= 2 (US-005 optimization)
3. `idx_dashboard_aggregates_source_date` - Fast aggregate lookup
4. `idx_user_activity_registration` - Time to Value metric calculation

**Pagination:**
- Default page size: 20
- Max page size: 100
- Use cursor-based pagination for large datasets (future optimization)

**Query Performance Target:**
- Dashboard load: <4 seconds (PRD requirement)
- Review list: <2 seconds
- Single resource GET: <500ms

---

### 15.3. Async Processing

**Background Jobs:**
1. **Initial 90-Day Import** (US-003)
   - Trigger: After creating new review source
   - Duration: 30-120 seconds (depends on review count)
   - Updates: `sync_jobs` table status

2. **Daily CRON Sync** (PRD:29)
   - Schedule: 3:00 AM CET daily
   - Job type: `SCHEDULED`
   - Process: Fetch new reviews since last sync

3. **Dashboard Aggregate Recalculation**
   - Trigger: After sync completion, sentiment correction
   - Updates: `dashboard_aggregates` table

4. **AI Summary Generation**
   - Trigger: On-demand if no valid summary exists
   - Caching: 24-hour validity period

5. **Weekly Email Reports** (PRD:54)
   - Schedule: Sunday 6:00 AM CET
   - Content: New reviews, negative count, key metrics

---

## 16. API Versioning

**Strategy:** URL path versioning

**Current Version:** v1

**Format:** `/api/v1/{resource}`

**Example:**
```
GET /api/v1/brands/1/reviews
```

**Future Versions:**
- Breaking changes: Introduce `/api/v2/...`
- Non-breaking changes: Add to v1 (new optional fields, new endpoints)

**Deprecation Policy:**
- Support previous version for 6 months after new version release
- Return `Deprecation` header: `Deprecation: Sun, 20 Jul 2025 00:00:00 GMT`
- Document migration guide in API docs

---

## 17. Rate Limiting

**Global Rate Limits:**
- Per User: 1000 requests / hour
- Per IP: 5000 requests / hour (unauthenticated)

**Endpoint-Specific Limits:**
- POST /api/auth/login: 5 requests / 15 minutes (brute-force protection)
- POST /api/auth/register: 3 requests / hour per IP
- POST /api/brands/{brandId}/sync: 1 request / 24 hours (business rule)

**Response Headers:**
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 987
X-RateLimit-Reset: 1642598400
```

**Rate Limit Exceeded (429):**
```json
{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Please try again later.",
  "retryAfter": 3600,
  "limit": 1000,
  "resetAt": "2025-01-19T11:00:00Z"
}
```

---

## 18. API Documentation

**Technology:** Swagger / OpenAPI 3.0

**Endpoints:**
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI Spec: `http://localhost:8080/api-docs`

**Features:**
- Interactive API explorer
- Request/response examples
- Authentication setup (JWT bearer)
- Try-it-out functionality

**Documentation Annotations:**
```java
@Tag(name = "Brands", description = "Brand management endpoints")
@Operation(summary = "Create a new brand", description = "Creates a brand entity for the authenticated user")
@ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Brand created successfully"),
    @ApiResponse(responseCode = "409", description = "User already has a brand (MVP limitation)")
})
public ResponseEntity<BrandDTO> createBrand(@Valid @RequestBody CreateBrandRequest request) {
    // ...
}
```

---

## 19. Security Considerations

### 19.1. Password Security
- Hashing: BCrypt with minimum 10 rounds
- Never return `password_hash` in API responses
- Password reset tokens: UUID, 1-hour expiration, single-use

### 19.2. Data Encryption
- API credentials: AES-256 encryption at application level
- Never return `credentials_encrypted` in API responses
- Encryption key: Stored in environment variable or secret manager

### 19.3. SQL Injection Prevention
- Use JPA parameterized queries (never string concatenation)
- Spring Data JPA provides automatic protection
- PostgreSQL RLS as secondary defense layer

### 19.4. XSS Prevention
- Frontend: React escapes output by default
- Backend: Sanitize user input (review content, brand names)
- Content-Security-Policy header

### 19.5. CORS
- Whitelist only trusted origins
- Development: `http://localhost:3000`
- Production: `https://app.brandpulse.io`

### 19.6. HTTPS
- Enforce HTTPS in production
- HSTS header: `Strict-Transport-Security: max-age=31536000; includeSubDomains`

---

## 20. Monitoring and Logging

### 20.1. Actuator Endpoints

**Health Check:**
```http
GET /actuator/health
```

**Response:**
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

**Metrics (Prometheus Format):**
```http
GET /actuator/prometheus
```

**Key Metrics:**
- `http_server_requests_seconds`: Request duration
- `jvm_memory_used_bytes`: Memory usage
- `hikaricp_connections_active`: Database connections
- `cache_gets_total`: Cache hit/miss rate
- `sync_job_duration_seconds`: Sync job performance

---

### 20.2. Logging Strategy

**Log Levels:**
- ERROR: Exceptions, failed sync jobs, external API errors
- WARN: Rate limit reached, plan limit exceeded, slow queries (>2s)
- INFO: User registration, login, source configuration, sync completion
- DEBUG: Request/response details, cache operations (disabled in production)

**Structured Logging (JSON):**
```json
{
  "timestamp": "2025-01-19T10:30:00Z",
  "level": "INFO",
  "logger": "com.brandpulse.service.ReviewSourceService",
  "message": "Review source created successfully",
  "userId": 1,
  "brandId": 1,
  "sourceId": 1,
  "sourceType": "GOOGLE",
  "traceId": "abc123def456"
}
```

**Sensitive Data Filtering:**
- Never log passwords, tokens, encrypted credentials
- Mask email addresses: `u***@example.com`
- Mask review content in logs (GDPR compliance)

---

## 21. Testing Strategy

### 21.1. Unit Tests
- Service layer: Business logic validation
- Validators: DTO validation rules
- Mappers: Entity ↔ DTO conversion

### 21.2. Integration Tests
- Technology: Testcontainers (PostgreSQL)
- Scope: Repository layer, database constraints
- Tests: Foreign key cascades, unique constraints, RLS policies

### 21.3. API Tests
- Technology: Spring MockMvc or RestAssured
- Scope: Controller layer, request/response validation
- Tests: Authentication, authorization, error handling

### 21.4. End-to-End Tests
- Scenario: User registration → source configuration → dashboard view
- Tests: US-001 → US-003 → US-004 flow
- Metrics: Time to Value (<10 minutes)

---

## 22. Deployment and CI/CD

### 22.1. Environment Configuration

**Development:**
- Database: Docker Compose (PostgreSQL)
- API: `http://localhost:8080`
- Frontend: `http://localhost:3000`

**Staging:**
- Database: AWS RDS PostgreSQL
- API: `https://api-staging.brandpulse.io`
- Frontend: `https://staging.brandpulse.io`

**Production:**
- Database: AWS RDS PostgreSQL (Multi-AZ)
- API: `https://api.brandpulse.io`
- Frontend: `https://app.brandpulse.io`

---

### 22.2. CI/CD Pipeline (GitHub Actions)

**On Push to main:**
1. Run tests (unit + integration)
2. Build Docker image
3. Push to container registry
4. Deploy to staging
5. Run smoke tests
6. Manual approval for production
7. Deploy to production

**Docker Image:**
```dockerfile
FROM eclipse-temurin:21-jre-alpine
COPY target/backend.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

---

## 23. Future Enhancements

### 23.1. Phase 2 Features
- Facebook & Trustpilot integrations
- Weekly email reports
- Review response drafting (AI-assisted)
- Advanced analytics (trends, charts)

### 23.2. API Additions
- `POST /api/reviews/{reviewId}/response`: Generate AI response draft
- `GET /api/analytics/trends`: Time-series sentiment trends
- `POST /api/exports`: Export reviews to CSV/PDF
- `GET /api/notifications/preferences`: Notification settings

### 23.3. Performance Optimizations
- Cursor-based pagination for large datasets
- GraphQL endpoint for flexible data fetching
- Read replicas for analytics queries
- TimescaleDB extension for time-series data

---

## 24. Appendix

### 24.1. Sample User Journey (API Calls)

**New User Onboarding (US-001, US-003):**

```http
# 1. Register
POST /api/auth/register
{
  "email": "anna@salon.pl",
  "password": "SecurePass123!",
  "confirmPassword": "SecurePass123!"
}
# Response: 201 Created, includes JWT token

# 2. Create Brand
POST /api/brands
Authorization: Bearer {token}
{
  "name": "Anna's Beauty Salon"
}
# Response: 201 Created, brandId: 1

# 3. Add Google Review Source
POST /api/brands/1/review-sources
Authorization: Bearer {token}
{
  "sourceType": "GOOGLE",
  "profileUrl": "https://www.google.com/maps/place/...",
  "externalProfileId": "ChIJN1t_tDeuEmsRUsoyG83frY4",
  "authMethod": "API"
}
# Response: 201 Created, sourceId: 1, importJobId: 1

# 4. Poll Import Progress
GET /api/sync-jobs/1
Authorization: Bearer {token}
# Response: 200 OK, status: IN_PROGRESS
# (Poll every 3 seconds until status: COMPLETED)

# 5. View Dashboard
GET /api/dashboard/summary?brandId=1
Authorization: Bearer {token}
# Response: 200 OK, aggregated metrics + AI summary

# 6. View Reviews
GET /api/brands/1/reviews?page=0&size=20
Authorization: Bearer {token}
# Response: 200 OK, paginated review list
```

**Time to Value:** 5-8 minutes (target: <10 minutes)

---

### 24.2. Database Schema Reference

Full database schema documentation: `.ai/db-plan.md`

**Key Tables:**
- `users`: User accounts and plan info
- `brands`: User's brand entities
- `review_sources`: Configured sources (Google, Facebook, Trustpilot)
- `reviews`: Individual customer reviews
- `sentiment_changes`: Audit trail for sentiment corrections
- `dashboard_aggregates`: Pre-calculated dashboard metrics
- `ai_summaries`: AI-generated text summaries
- `sync_jobs`: Synchronization job tracking
- `user_activity_log`: User activity for analytics

---

### 24.3. Success Metrics Tracking

**Time to Value (90% < 10 minutes):**
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

**Activation Rate (60% within 7 days):**
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

**Retention (35% with 3+ logins in 4 weeks):**
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

## 25. Contact and Support

**API Issues:** Report via GitHub Issues or support@brandpulse.io
**API Version:** v1
**Last Updated:** 2025-01-19
**Documentation:** https://docs.brandpulse.io/api
