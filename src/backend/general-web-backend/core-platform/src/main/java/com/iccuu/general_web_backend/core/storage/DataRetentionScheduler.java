package com.iccuu.general_web_backend.core.storage;

import com.iccuu.general_web_backend.common.constant.Constants;
import com.iccuu.general_web_backend.module.apikey.mapper.ApiKeyMapper;
import com.iccuu.general_web_backend.module.audit.mapper.AuditLogMapper;
import com.iccuu.general_web_backend.module.auth.mapper.LoginLogMapper;
import com.iccuu.general_web_backend.module.oauth.mapper.OAuthClientMapper;
import com.iccuu.general_web_backend.module.user.mapper.UserAuthMapper;
import com.iccuu.general_web_backend.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataRetentionScheduler {

    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;
    private final OAuthClientMapper oauthClientMapper;
    private final ApiKeyMapper apiKeyMapper;
    private final AuditLogMapper auditLogMapper;
    private final LoginLogMapper loginLogMapper;

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 0 4 * * ?")
    public void purgeSoftDeletedRecords() {
        long cutoff = System.currentTimeMillis() - Constants.SOFT_DELETE_RETENTION_MS;
        log.info("Starting soft-deleted records purge, cutoff: {}", cutoff);

        int users = deletePhysically("users", "user_id", cutoff);
        int userAuths = deletePhysically("user_auths", "id", cutoff);
        int oauthClients = deletePhysically("oauth_clients", "id", cutoff);
        int apiKeys = deletePhysically("api_keys", "id", cutoff);

        log.info("Data retention: purged {} users, {} user_auths, {} oauth_clients, {} api_keys soft-deleted records",
                users, userAuths, oauthClients, apiKeys);
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void purgeOldPartitions() {
        LocalDate cutoff = LocalDate.now().minusMonths(Constants.LOG_RETENTION_MONTHS);
        String partitionName = "p" + cutoff.format(DateTimeFormatter.ofPattern("yyyyMM"));
        log.info("Starting partition purge, dropping partition: {}", partitionName);

        for (String table : new String[]{"audit_logs", "login_logs"}) {
            try {
                jdbcTemplate.execute("ALTER TABLE " + table + " DROP PARTITION IF EXISTS " + partitionName);
                log.info("Dropped partition {} from table {}", partitionName, table);
            } catch (Exception e) {
                log.warn("Failed to drop partition {} from table {}: {}", partitionName, table, e.getMessage());
            }
        }

        log.info("Partition purge completed for cutoff partition: {}", partitionName);
    }

    /**
     * Physically delete soft-deleted records older than cutoff.
     * Uses raw SQL to bypass MyBatis-Plus @TableLogic which would otherwise
     * convert DELETE into an UPDATE of the deletedAt column.
     */
    private int deletePhysically(String tableName, String idColumn, long cutoff) {
        String sql = "DELETE FROM " + tableName + " WHERE deleted_at > 0 AND deleted_at < ?";
        int count = jdbcTemplate.update(sql, cutoff);
        if (count > 0) {
            log.debug("Physically deleted {} records from {}", count, tableName);
        }
        return count;
    }
}
