package com.iccuu.general_web_backend.core.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes the 9-table cascade physical delete for GDPR "forget me".
 * Wrapped in {@code @Transactional} so a mid-operation failure rolls back
 * entirely, leaving the {@code scheduled_tasks} row intact for retry.
 *
 * @implNote Infrastructure layer — uses JdbcTemplate to bypass MyBatis-Plus
 *           {@code @TableLogic} soft-delete interceptor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PhysicalDeleteExecutor {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Cascade-deletes all user data. The user row is deleted last so that
     * retries can still complete partial cleanup. The caller (TaskPoller)
     * holds the {@code scheduled_tasks} lifetime — that row is NOT deleted here.
     */
    @Transactional
    public void executeCascadeDelete(Long userId) {
        log.info("Starting cascade physical delete for userId={}", userId);

        jdbcTemplate.update("DELETE FROM user_credentials_history WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM login_logs WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM audit_logs WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_mfa WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_devices WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_auths WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_profiles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", userId);

        log.info("Cascade physical delete completed for userId={}", userId);
    }
}
