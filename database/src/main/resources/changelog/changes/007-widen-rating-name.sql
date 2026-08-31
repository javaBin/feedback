--liquibase formatted sql

--changeset tanettrimas:11
ALTER TABLE rating_type ALTER COLUMN rating_name TYPE VARCHAR(150);
