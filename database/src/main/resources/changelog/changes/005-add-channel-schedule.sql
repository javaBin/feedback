--liquibase formatted sql

--changeset tanettrimas:9
ALTER TABLE feedback_channel
    ADD COLUMN opens_at  TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    ADD COLUMN closes_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    ADD CONSTRAINT channel_schedule_valid
        CHECK (opens_at IS NULL OR closes_at IS NULL OR closes_at > opens_at);
