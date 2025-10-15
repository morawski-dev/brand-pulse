--liquibase formatted sql

--changeset brandpulse:19
--comment: Create sync_jobs table - tracks synchronization jobs for monitoring and debugging
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

--changeset brandpulse:20
--comment: Create indexes for sync_jobs table
CREATE INDEX idx_sync_jobs_source_id ON sync_jobs(review_source_id, created_at DESC);
CREATE INDEX idx_sync_jobs_status ON sync_jobs(status, created_at DESC) WHERE status IN ('PENDING', 'IN_PROGRESS');

--rollback DROP TABLE sync_jobs CASCADE;
