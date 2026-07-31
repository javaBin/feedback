--liquibase formatted sql

--changeset tanettrimas:8
ALTER TABLE feedback_channel ADD COLUMN is_open BOOLEAN NOT NULL DEFAULT FALSE;
