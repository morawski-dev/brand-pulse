--liquibase formatted sql

--changeset brandpulse:13
--comment: Create ai_summaries table - stores AI-generated text summaries for review sources
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

--changeset brandpulse:14
--comment: Create indexes for ai_summaries table
CREATE INDEX idx_ai_summaries_source_id ON ai_summaries(review_source_id, generated_at DESC);
CREATE INDEX idx_ai_summaries_valid_until ON ai_summaries(review_source_id, valid_until);

--rollback DROP TABLE ai_summaries CASCADE;
