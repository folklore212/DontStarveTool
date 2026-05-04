package com.iccuu.general_web_backend.infrastructure.event;

import com.iccuu.general_web_backend.common.event.PasswordChangedEvent;
import com.iccuu.general_web_backend.common.event.UserLoggedInEvent;
import com.iccuu.general_web_backend.common.event.UserRegisteredEvent;
import com.iccuu.general_web_backend.module.audit.service.AuditLogService;
import com.iccuu.general_web_backend.module.role.cache.PermissionCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventListeners {

    private final AuditLogService auditLogService;
    private final PermissionCacheManager permissionCacheManager;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.debug("User registered: userId={}, identityType={}",
                event.getUserId(), event.getIdentityType());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserLoggedIn(UserLoggedInEvent event) {
        if (event.isSuccess()) {
            log.debug("User logged in: userId={}, identityType={}", event.getUserId(), event.getIdentityType());
        } else {
            log.debug("User login failed: userId={}, reason={}", event.getUserId(), event.getFailureReason());
        }

        auditLogService.record(new AuditLogService.AuditLogRecord(
                event.getUserId(),
                null,
                event.isSuccess() ? "login" : "login_failed",
                "auth",
                String.valueOf(event.getUserId()),
                event.isSuccess() ? "Login successful" : "Login failed: " + event.getFailureReason(),
                event.getIpAddress(),
                event.getUserAgent(),
                null,
                null,
                null
        ));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordChanged(PasswordChangedEvent event) {
        log.info("Password changed: userId={}", event.getUserId());
        permissionCacheManager.invalidate(event.getUserId());
    }
}
