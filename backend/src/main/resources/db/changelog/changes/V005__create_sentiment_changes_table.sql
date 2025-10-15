--liquibase formatted sql

--changeset brandpulse:9
--comment: Create sentiment_changes table - audit table tracking all sentiment modifications
CREATE TABLE sentiment_changes (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    old_sentiment VARCHAR(20) NOT NULL CHECK (old_sentiment IN ('POSITIVE', 'NEGATIVE', 'NEUTRAL')),
    new_sentiment VARCHAR(20) NOT NULL CHECK (new_sentiment IN ('POSITIVE', 'NEGATIVE', 'NEUTRAL')),
    changed_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    change_reason VARCHAR(30) NOT NULL CHECK (change_reason IN ('AI_INITIAL', 'USER_CORRECTION', 'AI_REANALYSIS')),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

--changeset brandpulse:10
--comment: Create indexes for sentiment_changes table
CREATE INDEX idx_sentiment_changes_review_id ON sentiment_changes(review_id);
CREATE INDEX idx_sentiment_changes_user_id ON sentiment_changes(changed_by_user_id) WHERE changed_by_user_id IS NOT NULL;
CREATE INDEX idx_sentiment_changes_reason ON sentiment_changes(change_reason, changed_at DESC);

--rollback DROP TABLE sentiment_changes CASCADE;
