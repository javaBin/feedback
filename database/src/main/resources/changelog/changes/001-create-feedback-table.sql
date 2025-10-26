--liquibase formatted sql

--changeset tanettrimas:1
CREATE TABLE feedback_channel (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    speakers TEXT[] NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--changeset tanettrimas:2
CREATE TABLE rating_type (
    id BIGSERIAL PRIMARY KEY,
    channel_id BIGINT NOT NULL REFERENCES feedback_channel(id) ON DELETE CASCADE,
    rating_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(channel_id, rating_name)
);

--changeset tanettrimas:3
CREATE TABLE feedback (
    id BIGSERIAL PRIMARY KEY,
    channel_id BIGINT NOT NULL REFERENCES feedback_channel(id),
    detailed_comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--changeset tanettrimas:4
CREATE TABLE feedback_rating (
    id BIGSERIAL PRIMARY KEY,
    feedback_id BIGINT NOT NULL REFERENCES feedback(id) ON DELETE CASCADE,
    rating_type_id BIGINT NOT NULL REFERENCES rating_type(id),
    rating_value INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(feedback_id, rating_type_id)
);

--changeset tanettrimas:5
CREATE INDEX idx_feedback_channel_id ON feedback(channel_id);

--changeset tanettrimas:6
CREATE INDEX idx_feedback_rating_feedback_id ON feedback_rating(feedback_id);

--changeset tanettrimas:7
CREATE INDEX idx_rating_type_channel_id ON rating_type(channel_id);
