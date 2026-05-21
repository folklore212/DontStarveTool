package com.iccuu.general_web_backend.core.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartitionMaintenanceScheduler {

    private final DataSource dataSource;

    @Scheduled(cron = "0 0 3 1 * ?", zone = "Asia/Shanghai")
    public void maintainPartitions() {
        log.info("Starting monthly partition maintenance");
        try (Connection conn = dataSource.getConnection()) {
            CallableStatement stmt = conn.prepareCall("{CALL add_next_month_partitions()}");
            stmt.execute();
            log.info("Partition maintenance completed via stored procedure");
        } catch (Exception e) {
            log.error("Partition maintenance failed. Verify MySQL Event Scheduler or run manually.", e);
        }
    }
}
