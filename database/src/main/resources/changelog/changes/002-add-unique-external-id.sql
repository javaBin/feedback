--liquibase formatted sql

--changeset tanettrimas:8
ALTER TABLE feedback_channel ADD CONSTRAINT uq_feedback_channel_external_id UNIQUE (external_id);
