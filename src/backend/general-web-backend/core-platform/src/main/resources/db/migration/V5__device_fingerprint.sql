CREATE TABLE IF NOT EXISTS user_devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_hash VARCHAR(128) NOT NULL,
    device_name VARCHAR(256) DEFAULT NULL,
    ip_address VARCHAR(64) DEFAULT NULL,
    user_agent VARCHAR(512) DEFAULT NULL,
    first_seen_at DATETIME NOT NULL,
    last_seen_at DATETIME NOT NULL,
    created_date DATE NOT NULL,
    is_trusted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_user_devices_user (user_id),
    INDEX idx_user_devices_date (created_date),
    UNIQUE KEY uk_user_device (user_id, device_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
