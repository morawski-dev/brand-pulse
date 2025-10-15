--liquibase formatted sql

--changeset brandpulse:1
--comment: Create users table for authentication and authorization
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

--changeset brandpulse:2
--comment: Create indexes for users table
CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_verification_token ON users(verification_token) WHERE verification_token IS NOT NULL;
CREATE INDEX idx_users_password_reset_token ON users(password_reset_token) WHERE password_reset_token IS NOT NULL;

--rollback DROP TABLE users CASCADE;
