-- V12: Mod config cache + download tasks + download locks

CREATE TABLE IF NOT EXISTS mod_config_cache (
    id              BIGINT PRIMARY KEY,
    workshop_id     VARCHAR(32) NOT NULL,
    version         VARCHAR(32) NOT NULL,
    mod_name        VARCHAR(256),
    config_json     JSON COMMENT 'modinfo.lua configuration_options 完整对象',
    steam_updated_at DATETIME COMMENT 'Steam API 返回的 time_updated',
    fetched_at      DATETIME,
    created_at      DATETIME,
    UNIQUE KEY uk_workshop_version (workshop_id, version),
    INDEX idx_workshop (workshop_id)
);

CREATE TABLE IF NOT EXISTS download_tasks (
    id              VARCHAR(36) PRIMARY KEY COMMENT 'UUID',
    workshop_id     VARCHAR(32) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/downloading/parsing/completed/failed',
    progress        INT DEFAULT 0 COMMENT '0-100',
    error_message   TEXT,
    retry_count     INT DEFAULT 0,
    max_retries     INT DEFAULT 3,
    started_at      DATETIME,
    completed_at    DATETIME,
    created_at      DATETIME,
    INDEX idx_workshop (workshop_id),
    INDEX idx_status (status)
);

CREATE TABLE IF NOT EXISTS download_locks (
    workshop_id     VARCHAR(32) PRIMARY KEY,
    locked_at       DATETIME NOT NULL,
    locked_by       VARCHAR(64) NOT NULL
);
