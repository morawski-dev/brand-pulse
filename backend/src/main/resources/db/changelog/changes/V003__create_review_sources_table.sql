--liquibase formatted sql

--changeset brandpulse:5
--comment: Create review_sources table - stores configured review sources for each brand
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

--changeset brandpulse:6
--comment: Create indexes for review_sources table
CREATE INDEX idx_review_sources_brand_id ON review_sources(brand_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_review_sources_next_sync ON review_sources(next_scheduled_sync_at) WHERE is_active = TRUE AND deleted_at IS NULL;
CREATE INDEX idx_review_sources_active ON review_sources(brand_id, is_active) WHERE deleted_at IS NULL;

--rollback DROP TABLE review_sources CASCADE;
