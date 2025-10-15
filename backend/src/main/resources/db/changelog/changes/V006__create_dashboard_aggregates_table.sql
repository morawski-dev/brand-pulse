--liquibase formatted sql

--changeset brandpulse:11
--comment: Create dashboard_aggregates table - pre-calculated aggregates for fast dashboard loading
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

--changeset brandpulse:12
--comment: Create indexes for dashboard_aggregates table
CREATE INDEX idx_dashboard_aggregates_source_date ON dashboard_aggregates(review_source_id, date DESC);
CREATE INDEX idx_dashboard_aggregates_last_calc ON dashboard_aggregates(last_calculated_at);

--rollback DROP TABLE dashboard_aggregates CASCADE;
