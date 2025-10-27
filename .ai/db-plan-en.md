# BrandPulse Database Schema

## 1. Tables

### users
Stores user account information for authentication and authorization.

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

**Indexes:**
- `idx_users_email` ON (email) WHERE deleted_at IS NULL
- `idx_users_verification_token` ON (verification_token) WHERE verification_token IS NOT NULL
- `idx_users_password_reset_token` ON (password_reset_token) WHERE password_reset_token IS NOT NULL

---

### brands
Represents a brand/company that a user manages. MVP limits to 1 brand per user, but schema supports 1:N for future scalability.

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

**Indexes:**
- `idx_brands_user_id` ON (user_id) WHERE deleted_at IS NULL
- `idx_brands_last_manual_refresh` ON (last_manual_refresh_at) WHERE deleted_at IS NULL

---

### review_sources
Stores configured review sources (Google, Facebook, Trustpilot) for each brand.

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

**Indexes:**
- `idx_review_sources_brand_id` ON (brand_id) WHERE deleted_at IS NULL
- `idx_review_sources_next_sync` ON (next_scheduled_sync_at) WHERE is_active = TRUE AND deleted_at IS NULL
- `idx_review_sources_active` ON (brand_id, is_active) WHERE deleted_at IS NULL

---

### reviews
Stores individual reviews fetched from various sources.

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

**Indexes:**
- `idx_reviews_source_id` ON (review_source_id) WHERE deleted_at IS NULL
- `idx_reviews_published_at` ON (published_at DESC) WHERE deleted_at IS NULL
- `idx_reviews_sentiment` ON (review_source_id, sentiment) WHERE deleted_at IS NULL
- `idx_reviews_rating` ON (review_source_id, rating) WHERE deleted_at IS NULL
- `idx_reviews_negative` ON (review_source_id, published_at DESC) WHERE rating <= 2 AND deleted_at IS NULL (partial index for US-005)
- `idx_reviews_content_hash` ON (content_hash) WHERE deleted_at IS NULL
- `idx_reviews_composite_filter` ON (review_source_id, sentiment, rating, published_at DESC) WHERE deleted_at IS NULL

---

### sentiment_changes
Audit table tracking all sentiment modifications for accuracy analysis and model improvement.

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

**Indexes:**
- `idx_sentiment_changes_review_id` ON (review_id)
- `idx_sentiment_changes_user_id` ON (changed_by_user_id) WHERE changed_by_user_id IS NOT NULL
- `idx_sentiment_changes_reason` ON (change_reason, changed_at DESC)

---

### dashboard_aggregates
Pre-calculated aggregates for fast dashboard loading (<4s requirement).

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

**Indexes:**
- `idx_dashboard_aggregates_source_date` ON (review_source_id, date DESC)
- `idx_dashboard_aggregates_last_calc` ON (last_calculated_at)

---

### ai_summaries
Stores AI-generated text summaries for review sources.

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

**Indexes:**
- `idx_ai_summaries_source_id` ON (review_source_id, generated_at DESC)
- `idx_ai_summaries_valid` ON (review_source_id) WHERE valid_until > NOW()

---

### email_reports
Tracks weekly email reports sent to users for engagement metrics.

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

**Indexes:**
- `idx_email_reports_user_id` ON (user_id, sent_at DESC)
- `idx_email_reports_period` ON (period_start, period_end)
- `idx_email_reports_opened` ON (opened_at) WHERE opened_at IS NOT NULL

---

### user_activity_log
Tracks user activities for success metrics (Time to Value, Activation, Retention).

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

**Indexes:**
- `idx_user_activity_user_id` ON (user_id, occurred_at DESC)
- `idx_user_activity_type` ON (activity_type, occurred_at DESC)
- `idx_user_activity_registration` ON (user_id, occurred_at) WHERE activity_type = 'USER_REGISTERED'
- `idx_user_activity_first_source` ON (user_id, occurred_at) WHERE activity_type = 'FIRST_SOURCE_CONFIGURED_SUCCESSFULLY'

---

### sync_jobs
Tracks synchronization jobs for monitoring and debugging.

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

**Indexes:**
- `idx_sync_jobs_source_id` ON (review_source_id, created_at DESC)
- `idx_sync_jobs_status` ON (status, created_at DESC) WHERE status IN ('PENDING', 'IN_PROGRESS')

---

## 2. Relationships

### Entity Relationship Diagram (Cardinality)

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
users (1) ──────< (N) sentiment_changes (optional, for tracking who changed)
```

**Key Relationships:**

1. **users → brands**: One-to-Many (1:N)
   - MVP enforces 1:1 at application level via `max_sources_allowed`
   - Schema supports future multi-brand plans
   - CASCADE DELETE: Deleting user removes all their brands

2. **brands → review_sources**: One-to-Many (1:N)
   - One brand can have multiple review sources (Google, Facebook, Trustpilot)
   - Free plan limits to 1 source via business logic
   - CASCADE DELETE: Deleting brand removes all sources

3. **review_sources → reviews**: One-to-Many (1:N)
   - Each review belongs to exactly one source
   - CASCADE DELETE: Deleting source removes all reviews
   - UNIQUE constraint on (review_source_id, external_review_id) prevents duplicates

4. **reviews → sentiment_changes**: One-to-Many (1:N)
   - Tracks complete history of sentiment modifications
   - CASCADE DELETE: Deleting review removes change history

5. **review_sources → dashboard_aggregates**: One-to-Many (1:N)
   - One aggregate record per source per day
   - Enables fast dashboard loading via pre-calculation

---

## 3. Database Constraints

### Uniqueness Constraints

1. **users.email**: Ensures no duplicate email addresses
2. **review_sources (brand_id, source_type, external_profile_id)**: Prevents duplicate source configurations
3. **reviews (review_source_id, external_review_id)**: Prevents duplicate reviews from same external source
4. **dashboard_aggregates (review_source_id, date)**: One aggregate per source per day

### Check Constraints

1. **users.plan_type**: Must be 'FREE' or 'PREMIUM'
2. **review_sources.source_type**: Must be 'GOOGLE', 'FACEBOOK', or 'TRUSTPILOT'
3. **review_sources.auth_method**: Must be 'API' or 'SCRAPING'
4. **reviews.rating**: Must be between 1 and 5
5. **reviews.sentiment**: Must be 'POSITIVE', 'NEGATIVE', or 'NEUTRAL'
6. **sentiment_changes (old_sentiment, new_sentiment, change_reason)**: Valid enum values

### Foreign Key Constraints

All foreign keys are defined with appropriate ON DELETE actions:
- **CASCADE**: Used where child records should be deleted with parent (brands → users, reviews → review_sources)
- **SET NULL**: Used for optional references (sentiment_changes.changed_by_user_id)
- **RESTRICT**: Default behavior where deletion should be prevented if dependents exist

---

## 4. Indexes Strategy

### Performance Optimization Indexes

**High-Priority Indexes (Critical for <4s dashboard requirement):**

1. **Composite Index for Dashboard Filtering** (US-004, US-005):
   ```sql
   idx_reviews_composite_filter ON (review_source_id, sentiment, rating, published_at DESC)
   ```
   - Optimizes multi-criteria filtering in dashboard
   - Supports "All locations" aggregated view

2. **Partial Index for Negative Reviews** (US-005):
   ```sql
   idx_reviews_negative ON (review_source_id, published_at DESC) WHERE rating <= 2
   ```
   - Optimizes most common use case: filtering 1-2 star reviews
   - Smaller index size = faster queries

3. **Dashboard Aggregates Lookup**:
   ```sql
   idx_dashboard_aggregates_source_date ON (review_source_id, date DESC)
   ```
   - Fast retrieval of pre-calculated metrics
   - Supports time-series queries

**User Activity Tracking Indexes (Success Metrics):**

4. **Time to Value Tracking**:
   ```sql
   idx_user_activity_registration ON (user_id, occurred_at) WHERE activity_type = 'USER_REGISTERED'
   idx_user_activity_first_source ON (user_id, occurred_at) WHERE activity_type = 'FIRST_SOURCE_CONFIGURED_SUCCESSFULLY'
   ```
   - Enables calculation of registration → first source configuration time
   - Supports 90% target: configuration within 10 minutes

5. **Activation and Retention Analysis**:
   ```sql
   idx_user_activity_user_id ON (user_id, occurred_at DESC)
   ```
   - Tracks login frequency for 35% retention metric (3 logins in 4 weeks)

**Operational Indexes:**

6. **CRON Job Scheduling**:
   ```sql
   idx_review_sources_next_sync ON (next_scheduled_sync_at) WHERE is_active = TRUE AND deleted_at IS NULL
   ```
   - Optimizes daily 3:00 AM CET synchronization job
   - Filters only active, non-deleted sources

7. **Manual Refresh Rate Limiting**:
   ```sql
   idx_brands_last_manual_refresh ON (last_manual_refresh_at) WHERE deleted_at IS NULL
   ```
   - Enforces 24-hour rolling window for manual refreshes (US-008)

8. **Email Report Engagement**:
   ```sql
   idx_email_reports_opened ON (opened_at) WHERE opened_at IS NOT NULL
   ```
   - Tracks email engagement for retention analysis

---

## 5. Materialized Views (Optional Performance Enhancement)

For "All locations" aggregated view, consider materialized view refreshed during CRON job:

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

**Refresh Strategy:**
- During daily CRON job (3:00 AM CET)
- After manual refresh completion
- CONCURRENT refresh to avoid blocking reads

---

## 6. PostgreSQL Row-Level Security (RLS) Policies

**Implementation Decision:** Hybrid approach for MVP
- **Primary security**: Spring Security with JWT (application layer)
- **Secondary defense**: PostgreSQL RLS (database layer) as defense-in-depth

**RLS Configuration:**

```sql
-- Enable RLS on multi-tenant tables
ALTER TABLE brands ENABLE ROW LEVEL SECURITY;
ALTER TABLE review_sources ENABLE ROW LEVEL SECURITY;
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE dashboard_aggregates ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_summaries ENABLE ROW LEVEL SECURITY;
ALTER TABLE email_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_activity_log ENABLE ROW LEVEL SECURITY;

-- Policy for brands: Users can only access their own brands
CREATE POLICY brands_user_isolation ON brands
    FOR ALL
    USING (user_id = current_setting('app.current_user_id')::BIGINT);

-- Policy for review_sources: Access via brand ownership
CREATE POLICY review_sources_user_isolation ON review_sources
    FOR ALL
    USING (
        brand_id IN (
            SELECT id FROM brands 
            WHERE user_id = current_setting('app.current_user_id')::BIGINT
            AND deleted_at IS NULL
        )
    );

-- Policy for reviews: Access via review_source → brand → user chain
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

-- Policy for dashboard_aggregates: Access via review_source ownership
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

-- Policy for email_reports: Users can only access their own reports
CREATE POLICY email_reports_user_isolation ON email_reports
    FOR ALL
    USING (user_id = current_setting('app.current_user_id')::BIGINT);

-- Policy for user_activity_log: Users can only access their own logs
CREATE POLICY user_activity_log_user_isolation ON user_activity_log
    FOR ALL
    USING (user_id = current_setting('app.current_user_id')::BIGINT);
```

**Spring Security Integration:**

```java
// Set user context for RLS in transaction
@Transactional
public void executeWithUserContext(Long userId, Runnable operation) {
    jdbcTemplate.execute("SET LOCAL app.current_user_id = " + userId);
    operation.run();
}
```

---

## 7. Data Integrity and Validation

### Database-Level Validation

1. **Content Deduplication**:
   - `content_hash` column in reviews table
   - Generated as SHA-256 hash of review content
   - Enables detection of duplicate content across different external IDs

2. **Timestamp Consistency**:
   - All timestamps use `TIMESTAMP WITH TIME ZONE`
   - Handles timezone conversion for CRON job (3:00 AM CET) and user display
   - Supports global expansion beyond Poland

3. **Soft Delete Pattern**:
   - `deleted_at` column on all major entities
   - Enables audit trail and data recovery
   - Filtered in indexes using `WHERE deleted_at IS NULL`

4. **Audit Columns**:
   - `created_at`: Record creation timestamp
   - `updated_at`: Last modification timestamp (updated by application or trigger)
   - Enables change tracking and debugging

### Triggers for Automatic Updates

```sql
-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to relevant tables
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

## 8. Security Considerations

### Sensitive Data Protection

1. **Password Storage**:
   - `password_hash` column stores BCrypt-hashed passwords
   - Never store plain text passwords
   - Minimum 10 BCrypt rounds recommended

2. **API Credentials Encryption**:
   - `credentials_encrypted` JSONB column stores OAuth tokens, API keys
   - Encrypt at application level before storing
   - Use AES-256 with key stored in environment variable or secret manager

3. **PII and GDPR Compliance**:
   - Soft delete enables data recovery window
   - Hard delete after 90 days for compliance
   - Email encryption optional depending on requirements

### Authentication Token Management

4. **Password Reset Tokens**:
   - `password_reset_token` with expiration timestamp
   - Single-use tokens (cleared after successful reset)
   - Index on token for fast lookup

5. **Email Verification**:
   - `verification_token` for email confirmation
   - `email_verified` boolean flag
   - Enables two-factor authentication pathway

---

## 9. Performance Optimization Strategies

### Connection Pooling (HikariCP Configuration)

```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### Caching Strategy (Spring Cache + Caffeine)

**Cache Keys:**
1. Dashboard aggregates: `dashboard:brand:{brandId}`
2. AI summaries: `summary:source:{reviewSourceId}`
3. Review counts: `counts:source:{reviewSourceId}`

**Cache Invalidation:**
- On new review import
- On sentiment correction (US-007)
- On manual refresh (US-008)

### Query Optimization Guidelines

1. **Avoid N+1 Queries**: Use JOIN FETCH in JPA queries
2. **Pagination**: Implement cursor-based pagination for review list
3. **Projection Queries**: Select only needed columns for list views
4. **Batch Operations**: Use batch inserts for review imports (90-day history)

---

## 10. Data Archival and Retention

### Retention Policy

1. **Active Data**: Last 90 days (per PRD requirement)
2. **Historical Data**: 91-365 days (queryable but deprioritized)
3. **Archived Data**: >365 days (moved to cold storage or deleted)

### Archival Strategy

```sql
-- Partition reviews table by published_at (future optimization)
CREATE TABLE reviews_2024_q4 PARTITION OF reviews
    FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');

CREATE TABLE reviews_2025_q1 PARTITION OF reviews
    FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');
```

### Cleanup Jobs

```sql
-- Soft delete → hard delete after 90 days
DELETE FROM reviews 
WHERE deleted_at < NOW() - INTERVAL '90 days';

-- Archive old aggregates (optional)
INSERT INTO dashboard_aggregates_archive 
SELECT * FROM dashboard_aggregates 
WHERE date < CURRENT_DATE - INTERVAL '1 year';
```

---

## 11. Migration Strategy (Liquibase)

### Migration File Naming Convention

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

### Rollback Capability

Each migration should include:
1. Forward migration (UP)
2. Rollback script (DOWN)
3. Preconditions (e.g., table doesn't exist)
4. Validation queries

---

## 12. Design Decisions and Rationale

### 1. Future-Proof User-Brand Relationship (1:N Schema for 1:1 MVP)
**Decision**: Schema supports 1:N despite MVP limitation of 1 brand per user.
**Rationale**: 
- Avoids expensive schema refactoring when introducing premium multi-brand plans
- `max_sources_allowed` column enables gradual plan rollout
- Business logic enforcement in application layer provides flexibility

### 2. Composite Unique Constraint for Review Deduplication
**Decision**: `UNIQUE (review_source_id, external_review_id)` + `content_hash` column.
**Rationale**:
- Prevents duplicate imports during retries or manual refreshes
- Content hash detects same review with different external IDs
- Database-level guarantee stronger than application-level checks

### 3. Sentiment Changes Audit Table
**Decision**: Separate table for sentiment modification history.
**Rationale**:
- Enables accuracy measurement (75% agreement metric from PRD)
- Machine learning feedback loop: identify systematic AI errors
- Supports future features: "Undo" functionality, change notifications

### 4. Dashboard Aggregates Pre-Calculation
**Decision**: Dedicated table for pre-calculated metrics, refreshed during CRON + materialized views.
**Rationale**:
- Critical for <4 second dashboard loading requirement
- Reduces real-time computation on large datasets
- Enables efficient "All locations" aggregated view (US-006)

### 5. JSONB for Flexible Credentials Storage
**Decision**: `credentials_encrypted` JSONB column instead of separate columns.
**Rationale**:
- Each platform requires different auth data (OAuth tokens, API keys, scraping selectors)
- Avoids schema changes when adding Facebook/Trustpilot integrations
- PostgreSQL JSONB provides indexing and query capabilities

### 6. Soft Delete Pattern with Periodic Cleanup
**Decision**: `deleted_at` timestamp on all major entities + cleanup job.
**Rationale**:
- User experience: "Undo" deleted sources within grace period
- Compliance: 90-day retention for audit trail (GDPR right to erasure)
- Data integrity: Prevents cascading deletes during troubleshooting

### 7. Partial Index for Negative Reviews
**Decision**: `WHERE rating <= 2` partial index on reviews.
**Rationale**:
- US-005 user story: Filtering negative reviews is primary use case
- Smaller index = faster queries + reduced storage
- Aligns with business value: prioritizing response to negative feedback

### 8. Timezone-Aware Timestamps Throughout
**Decision**: All timestamps use `TIMESTAMP WITH TIME ZONE`.
**Rationale**:
- CRON job scheduled in CET but server may be in different timezone
- Future international expansion requires timezone handling
- Avoids DST (Daylight Saving Time) bugs

### 9. Activity Log for Product Metrics
**Decision**: Granular user_activity_log table instead of simple login tracking.
**Rationale**:
- Measures PRD success metrics: Time to Value, Activation, Retention
- Enables product analytics: feature usage, funnel conversion
- JSONB metadata column allows flexible event properties without schema changes

### 10. Hybrid Security Model (Spring Security + RLS)
**Decision**: Primary security in application, optional RLS as secondary layer.
**Rationale**:
- Spring Security sufficient for MVP (reduces complexity)
- PostgreSQL RLS provides defense-in-depth against SQL injection, logic errors
- Can be disabled in MVP and enabled in production for minimal performance impact

### 11. Sync Jobs Table for Operational Visibility
**Decision**: Separate table tracking each synchronization job.
**Rationale**:
- Debugging: Identify failed imports, flaky APIs
- Monitoring: Alert on consecutive failures
- Billing: Track API usage for cost analysis (unresolved PRD issue)

### 12. AI Summaries Table with Caching
**Decision**: Store AI-generated summaries with `valid_until` timestamp.
**Rationale**:
- Reduces OpenRouter.ai API costs: cache summaries for 24 hours
- Enables summary history: track how sentiment narrative evolves
- `model_used` + `token_count` columns support cost optimization analysis

---

## 13. Open Questions and Future Considerations

### 1. Multi-Location Physical Structure
**Question**: Does "All locations" mean multiple review sources per brand, or multiple physical locations?
**Impact**: May need separate `locations` table if brand has multiple physical addresses.
**MVP Decision**: Assume 1 review source = 1 location. Revisit in post-MVP.

### 2. API Cost Tracking Granularity
**Question**: Should we track costs per API call or per synchronization job?
**Current Approach**: `token_count` in ai_summaries, but no detailed cost tracking.
**Recommendation**: Add `api_costs` table post-MVP with columns: `service_provider`, `operation_type`, `cost_amount`, `timestamp`.

### 3. Scraping Infrastructure Tables
**Question**: Do we need tables for scraping job logs, proxy rotation, rate limiting?
**MVP Decision**: Start with error logging in `sync_jobs.error_message`. Add dedicated tables if scraping becomes primary method.

### 4. Review Response Workflow
**Question**: Future feature (out of scope for MVP) - should schema prepare for storing draft responses?
**Recommendation**: Add in Phase 2: `review_responses` table with FK to reviews, draft/published status.

### 5. Real-Time Notifications
**Question**: Email reports tracked, but PRD mentions future Slack/real-time alerts.
**Recommendation**: Add `notification_preferences` table in Phase 2 with channels (email, Slack, webhook).

### 6. Data Export and Reporting
**Question**: CSV/PDF export out of scope for MVP, but should we prepare for it?
**Recommendation**: Current schema sufficient. Add `export_jobs` table when implementing feature.

### 7. Advanced Analytics and Trends
**Question**: PRD excludes charts/trends for MVP. Will time-series tables be needed?
**Current Approach**: `dashboard_aggregates` by date supports basic trends.
**Recommendation**: Evaluate time-series extension (TimescaleDB) if trend analysis becomes core feature.

---

## 14. Testing Strategy

### Database Testing with Testcontainers

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
        // Test UNIQUE constraint on (review_source_id, external_review_id)
    }
    
    @Test
    void shouldEnforceSoftDeleteInQueries() {
        // Test that deleted_at IS NULL filters work correctly
    }
}
```

### Data Integrity Tests

1. **Foreign Key Cascade Tests**: Verify deleting brand removes review_sources and reviews
2. **Constraint Violation Tests**: Ensure CHECK constraints prevent invalid data
3. **Index Performance Tests**: Verify query plans use expected indexes
4. **RLS Policy Tests**: Confirm users cannot access other users' data

---

## 15. Monitoring and Observability

### Database Metrics (Spring Boot Actuator + Micrometer)

1. **Connection Pool Metrics**: Active connections, wait time, timeouts
2. **Query Performance**: Slow query log (>1s), query count by table
3. **Cache Hit Ratio**: Caffeine cache statistics
4. **Table Size Growth**: Monitor reviews and user_activity_log tables
5. **Index Usage**: Identify unused indexes for removal

### Alerts

1. **High Connection Pool Utilization**: >80% active connections
2. **Slow Queries**: Any query >2s (dashboard requirement is <4s total)
3. **Failed Sync Jobs**: >3 consecutive failures for any review_source
4. **Database Size**: >80% storage capacity
5. **Replication Lag**: If using read replicas (future consideration)

---

## Summary

This database schema provides a robust foundation for the BrandPulse MVP with:

✅ **Performance**: Pre-aggregation, strategic indexes, caching → <4s dashboard loading
✅ **Scalability**: 1:N relationships ready for premium plans, partitioning-ready structure
✅ **Security**: Multi-layered approach (Spring Security + optional RLS), encrypted credentials
✅ **Data Integrity**: Unique constraints, soft deletes, audit trails
✅ **Observability**: Activity logging, sync job tracking, email engagement metrics
✅ **Maintainability**: Liquibase migrations, clear naming conventions, comprehensive documentation

The schema balances MVP simplicity with future extensibility, avoiding premature optimization while ensuring key requirements (freemium limits, sentiment accuracy tracking, performance SLAs) are properly supported at the database level.
