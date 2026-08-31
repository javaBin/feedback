--liquibase formatted sql

--changeset tanettrimas:12
ALTER TABLE feedback_channel ADD COLUMN remote_id VARCHAR(255);
ALTER TABLE feedback_channel ADD CONSTRAINT uq_feedback_channel_remote_id UNIQUE (remote_id);
