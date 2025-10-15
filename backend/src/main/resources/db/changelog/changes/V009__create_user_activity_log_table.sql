--liquibase formatted sql

--changeset brandpulse:17
--comment: Create user_activity_log table - tracks user activities for success metrics
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

--changeset brandpulse:18
--comment: Create indexes for user_activity_log table
CREATE INDEX idx_user_activity_user_id ON user_activity_log(user_id, occurred_at DESC);
CREATE INDEX idx_user_activity_type ON user_activity_log(activity_type, occurred_at DESC);
CREATE INDEX idx_user_activity_registration ON user_activity_log(user_id, occurred_at) WHERE activity_type = 'USER_REGISTERED';
CREATE INDEX idx_user_activity_first_source ON user_activity_log(user_id, occurred_at) WHERE activity_type = 'FIRST_SOURCE_CONFIGURED_SUCCESSFULLY';

--rollback DROP TABLE user_activity_log CASCADE;
