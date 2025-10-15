--liquibase formatted sql

--changeset brandpulse:15
--comment: Create email_reports table - tracks weekly email reports sent to users
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

--changeset brandpulse:16
--comment: Create indexes for email_reports table
CREATE INDEX idx_email_reports_user_id ON email_reports(user_id, sent_at DESC);
CREATE INDEX idx_email_reports_period ON email_reports(period_start, period_end);
CREATE INDEX idx_email_reports_opened ON email_reports(opened_at) WHERE opened_at IS NOT NULL;

--rollback DROP TABLE email_reports CASCADE;
