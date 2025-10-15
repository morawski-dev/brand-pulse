--liquibase formatted sql

--changeset brandpulse:22 splitStatements:false
--comment: Create trigger function for auto-updating updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

--changeset brandpulse:23
--comment: Apply updated_at trigger to users table
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

--changeset brandpulse:24
--comment: Apply updated_at trigger to brands table
CREATE TRIGGER update_brands_updated_at
    BEFORE UPDATE ON brands
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

--changeset brandpulse:25
--comment: Apply updated_at trigger to review_sources table
CREATE TRIGGER update_review_sources_updated_at
    BEFORE UPDATE ON review_sources
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

--changeset brandpulse:26
--comment: Apply updated_at trigger to reviews table
CREATE TRIGGER update_reviews_updated_at
    BEFORE UPDATE ON reviews
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

--rollback DROP TRIGGER IF EXISTS update_reviews_updated_at ON reviews;
--rollback DROP TRIGGER IF EXISTS update_review_sources_updated_at ON review_sources;
--rollback DROP TRIGGER IF EXISTS update_brands_updated_at ON brands;
--rollback DROP TRIGGER IF EXISTS update_users_updated_at ON users;
--rollback DROP FUNCTION IF EXISTS update_updated_at_column();
