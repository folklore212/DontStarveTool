CREATE TABLE IF NOT EXISTS scheduled_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_type VARCHAR(64) NOT NULL COMMENT 'PHYSICAL_DELETE_USER',
    task_key VARCHAR(128) NOT NULL COMMENT 'dedup key: e.g. user:{userId}',
    payload_json TEXT NOT NULL COMMENT 'JSON payload — use TEXT for flexibility with complex task data',
    execute_at DATETIME NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=PENDING, 1=RUNNING, 2=COMPLETED, 3=FAILED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    executed_at DATETIME DEFAULT NULL,
    error_message VARCHAR(512) DEFAULT NULL,
    INDEX idx_st_status_exec (status, execute_at),
    INDEX idx_st_task_type (task_type),
    UNIQUE KEY uk_st_task_key (task_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
