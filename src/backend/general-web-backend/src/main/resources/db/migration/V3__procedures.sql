-- ============================================================
-- V3: Partition maintenance stored procedure
--
-- NOTE: The event scheduler approach may fail on cloud MySQL databases
-- (e.g. AWS RDS, Alibaba Cloud RDS) that lack SUPER privilege.
-- For cloud DBs, either:
--   1. Run this procedure manually via cron/scheduler, OR
--   2. Use the Java PartitionMaintenanceScheduler which is the
--      recommended approach for production cloud deployments.
-- ============================================================

DELIMITER $$

CREATE PROCEDURE add_next_month_partitions()
BEGIN
    DECLARE v_last_boundary DATE;
    DECLARE v_p1_name VARCHAR(20);
    DECLARE v_p2_name VARCHAR(20);
    DECLARE v_p1_boundary DATE;
    DECLARE v_p2_boundary DATE;

    -- 从现有分区中动态推算下个边界（排除 MAXVALUE 的 p_future）
    SELECT MAX(STR_TO_DATE(PARTITION_DESCRIPTION, '%Y-%m-%d'))
    INTO v_last_boundary
    FROM information_schema.PARTITIONS
    WHERE TABLE_SCHEMA = 'auth_system'
      AND TABLE_NAME = 'login_logs'
      AND PARTITION_DESCRIPTION != 'MAXVALUE';

    IF v_last_boundary IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No existing partition boundary found';
    END IF;

    SET v_p1_boundary = DATE_ADD(v_last_boundary, INTERVAL 1 MONTH);
    SET v_p2_boundary = DATE_ADD(v_last_boundary, INTERVAL 2 MONTH);
    SET v_p1_name = CONCAT('p', DATE_FORMAT(DATE_SUB(v_p1_boundary, INTERVAL 1 MONTH), '%Y%m'));
    SET v_p2_name = CONCAT('p', DATE_FORMAT(DATE_SUB(v_p2_boundary, INTERVAL 1 MONTH), '%Y%m'));

    SET @sql = CONCAT(
        'ALTER TABLE login_logs REORGANIZE PARTITION p_future INTO (',
        'PARTITION ', v_p1_name, ' VALUES LESS THAN (''', v_p1_boundary, '''), ',
        'PARTITION ', v_p2_name, ' VALUES LESS THAN (''', v_p2_boundary, '''), ',
        'PARTITION p_future VALUES LESS THAN (MAXVALUE))'
    );
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

    SET @sql = CONCAT(
        'ALTER TABLE audit_logs REORGANIZE PARTITION p_future INTO (',
        'PARTITION ', v_p1_name, ' VALUES LESS THAN (''', v_p1_boundary, '''), ',
        'PARTITION ', v_p2_name, ' VALUES LESS THAN (''', v_p2_boundary, '''), ',
        'PARTITION p_future VALUES LESS THAN (MAXVALUE))'
    );
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
END$$

DELIMITER ;

-- ============================================================
-- Event scheduler (commented out — may fail on cloud DBs without SUPER privilege)
--
-- Run manually or rely on Java PartitionMaintenanceScheduler for cloud DBs.
-- ============================================================
-- CREATE EVENT IF NOT EXISTS evt_add_partitions
-- ON SCHEDULE EVERY 1 MONTH
-- STARTS '2026-05-01 02:00:00'
-- ON COMPLETION PRESERVE
-- ENABLE
-- COMMENT '每月自动为 login_logs/audit_logs 追加两个分区并保留 p_future'
-- DO CALL add_next_month_partitions();
