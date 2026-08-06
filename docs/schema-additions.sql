-- ============================================================================
-- Schema additions required by the DDD / Event Sourcing / Hexagonal migration
-- ============================================================================
-- application-prod.properties uses spring.jpa.hibernate.ddl-auto=validate, so
-- Hibernate will NOT create these automatically in production — run this
-- manually against the prod database first (dev already gets them for free
-- via ddl-auto=update). No existing table's schema changes except `chats`
-- (see the ALTER at the bottom): chats.chat_id drops its AUTO_INCREMENT,
-- because it's now minted up front by chat_id_sequences instead — this keeps
-- the column type (BIGINT) and every existing row completely untouched.

CREATE TABLE events (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_id   VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type     VARCHAR(255) NOT NULL,
    version        BIGINT NOT NULL,
    occurred_on    DATETIME NOT NULL,
    payload        TEXT NOT NULL,
    CONSTRAINT uk_events_aggregate_version UNIQUE (aggregate_id, version)
);

CREATE INDEX idx_events_aggregate_id ON events (aggregate_id);
CREATE INDEX idx_events_occurred_on ON events (occurred_on);

CREATE TABLE chat_id_sequences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY
);

-- IMPORTANT: run this against a copy first and verify row counts if this
-- database already has chats in it. AUTO_INCREMENT cannot simply be dropped
-- while rows already rely on it being self-assigning going forward — after
-- this ALTER, every new chat's id must come from chat_id_sequences.
ALTER TABLE chats MODIFY chat_id BIGINT NOT NULL;
