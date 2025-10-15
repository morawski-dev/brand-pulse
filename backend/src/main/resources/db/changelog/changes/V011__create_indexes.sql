--liquibase formatted sql

--changeset brandpulse:21
--comment: Additional indexes (primary indexes already created with tables)
-- All primary indexes have been created in their respective table migration files
-- This file is reserved for any future additional indexes

--rollback -- No rollback needed as no indexes created here
