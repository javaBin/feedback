--liquibase formatted sql

--changeset tanettrimas:13
UPDATE feedback_channel SET is_open = false where is_open = true;
