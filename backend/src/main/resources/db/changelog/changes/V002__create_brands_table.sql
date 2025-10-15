--liquibase formatted sql

--changeset brandpulse:3
--comment: Create brands table - represents a brand/company that a user manages
CREATE TABLE brands (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    last_manual_refresh_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

--changeset brandpulse:4
--comment: Create indexes for brands table
CREATE INDEX idx_brands_user_id ON brands(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_brands_last_manual_refresh ON brands(last_manual_refresh_at) WHERE deleted_at IS NULL;

--rollback DROP TABLE brands CASCADE;
