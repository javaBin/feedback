--liquibase formatted sql

--changeset tanettrimas:9
UPDATE feedback_channel SET external_id = REPLACE(external_id, 'TEST-', '') WHERE id = 1;
