-- V8: Server management + DST clusters + deploy tasks

CREATE TABLE IF NOT EXISTS servers (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    name            VARCHAR(64) NOT NULL,
    host            VARCHAR(255) NOT NULL,
    port            INT DEFAULT 22,
    username        VARCHAR(64) NOT NULL DEFAULT 'root',
    auth_type       VARCHAR(16) DEFAULT 'password',
    password        VARCHAR(255),
    ssh_key_id      BIGINT,
    tags            JSON,
    sort_order      INT DEFAULT 0,
    os_info         VARCHAR(255),
    cpu_cores       INT,
    mem_gb          DECIMAL(6,1),
    disk_gb         DECIMAL(8,1),
    steamcmd_installed TINYINT DEFAULT 0,
    dst_version     VARCHAR(32),
    status          VARCHAR(16) DEFAULT 'unknown',
    last_test_at    DATETIME,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted_at      BIGINT DEFAULT 0,
    INDEX idx_server_user (user_id)
);

CREATE TABLE IF NOT EXISTS ssh_keys (
    id                  BIGINT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    name                VARCHAR(64) NOT NULL,
    public_key          TEXT NOT NULL,
    private_key_encrypted TEXT NOT NULL,
    key_type            VARCHAR(16) DEFAULT 'ED25519',
    created_at          DATETIME
);

CREATE TABLE IF NOT EXISTS dst_clusters (
    id              BIGINT PRIMARY KEY,
    server_id       BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    name            VARCHAR(64) NOT NULL,
    display_name    VARCHAR(128),
    game_mode       VARCHAR(32) DEFAULT 'survival',
    max_players     INT DEFAULT 6,
    password        VARCHAR(64),
    cluster_token   TEXT,
    master_port     INT DEFAULT 10999,
    steam_port      INT DEFAULT 8766,
    has_caves       TINYINT DEFAULT 0,
    status          VARCHAR(16) DEFAULT 'stopped',
    player_count    INT DEFAULT 0,
    day_count       INT DEFAULT 0,
    season          VARCHAR(16),
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted_at      BIGINT DEFAULT 0,
    INDEX idx_dst_server (server_id)
);

CREATE TABLE IF NOT EXISTS dst_backups (
    id              BIGINT PRIMARY KEY,
    cluster_id      BIGINT NOT NULL,
    backup_name     VARCHAR(128) NOT NULL,
    file_size       BIGINT DEFAULT 0,
    backup_type     VARCHAR(16) DEFAULT 'manual',
    created_at      DATETIME
);

CREATE TABLE IF NOT EXISTS deploy_tasks (
    id              VARCHAR(36) PRIMARY KEY,
    server_id       BIGINT NOT NULL,
    cluster_name    VARCHAR(64),
    user_id         BIGINT NOT NULL,
    status          VARCHAR(16) NOT NULL,
    current_step    VARCHAR(32),
    step_detail     TEXT,
    started_at      DATETIME,
    completed_at    DATETIME,
    last_heartbeat_at DATETIME,
    error_message   TEXT,
    created_at      DATETIME
);
