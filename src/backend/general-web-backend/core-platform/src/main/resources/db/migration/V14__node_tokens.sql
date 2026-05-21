-- V14: Node Agent bootstrap tokens
CREATE TABLE IF NOT EXISTS node_tokens (
    id           BIGINT PRIMARY KEY,
    server_id    BIGINT NOT NULL COMMENT 'FK to servers',
    token_hash   VARCHAR(128) NOT NULL COMMENT 'SHA-256(token)',
    token_prefix VARCHAR(12) NOT NULL COMMENT 'Display prefix dsn-xxxxxxxx',
    status       TINYINT NOT NULL DEFAULT 1 COMMENT '1:normal 0:disabled',
    last_used_at DATETIME NULL,
    created_at   DATETIME,
    deleted_at   BIGINT NOT NULL DEFAULT 0,
    INDEX idx_server (server_id),
    INDEX idx_hash (token_hash)
);
