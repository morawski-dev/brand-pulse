--liquibase formatted sql

--changeset brandpulse:7
--comment: Create reviews table - stores individual reviews fetched from various sources
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

--changeset brandpulse:8
--comment: Create indexes for reviews table
CREATE INDEX idx_reviews_source_id ON reviews(review_source_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_published_at ON reviews(published_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_sentiment ON reviews(review_source_id, sentiment) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_rating ON reviews(review_source_id, rating) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_negative ON reviews(review_source_id, published_at DESC) WHERE rating <= 2 AND deleted_at IS NULL;
CREATE INDEX idx_reviews_content_hash ON reviews(content_hash) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_composite_filter ON reviews(review_source_id, sentiment, rating, published_at DESC) WHERE deleted_at IS NULL;

--rollback DROP TABLE reviews CASCADE;
