--liquibase formatted sql

--changeset tanettrimas:10 splitStatements:false runOnChange:false
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION generate_feedback_channel_external_id() RETURNS varchar AS $$
DECLARE
    chars     text    := 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    bytes     bytea;
    candidate varchar;
BEGIN
    LOOP
        bytes := gen_random_bytes(4);
        candidate := '';
        FOR i IN 0..3 LOOP
            candidate := candidate || substr(chars, 1 + (get_byte(bytes, i) % 36), 1);
        END LOOP;
        EXIT WHEN NOT EXISTS (SELECT 1 FROM feedback_channel WHERE external_id = candidate);
    END LOOP;
    RETURN candidate;
END;
$$ LANGUAGE plpgsql;

UPDATE feedback_channel SET external_id = generate_feedback_channel_external_id();

ALTER TABLE feedback_channel ALTER COLUMN external_id SET DEFAULT generate_feedback_channel_external_id();
