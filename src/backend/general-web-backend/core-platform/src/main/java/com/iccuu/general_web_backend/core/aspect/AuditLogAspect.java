package com.iccuu.general_web_backend.core.aspect;

import com.iccuu.general_web_backend.common.annotation.AuditLog;
import com.iccuu.general_web_backend.common.util.IpUtil;
import com.iccuu.general_web_backend.common.util.SecurityUtil;
import com.iccuu.general_web_backend.module.audit.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        String outcome = "success";
        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            outcome = "failure";
            throw e;
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("AUDIT action={} resource={} outcome={} elapsed={}ms",
                    auditLog.action(), auditLog.resourceType(), outcome, elapsed);

            try {
                persistAuditLog(auditLog);
            } catch (Exception e) {
                log.error("Failed to persist audit log action={} resource={}",
                        auditLog.action(), auditLog.resourceType(), e);
            }
        }
    }

    private void persistAuditLog(AuditLog auditLog) {
        Long userId = SecurityUtil.getCurrentUserId();
        HttpServletRequest req = SecurityUtil.getCurrentRequest();

        auditLogService.record(new AuditLogService.AuditLogRecord(
                userId,
                null,
                auditLog.action(),
                auditLog.resourceType(),
                null,
                null,
                IpUtil.getClientIp(req),
                req.getHeader("User-Agent"),
                req.getRequestedSessionId(),
                req.getHeader("X-Request-Id"),
                req.getHeader("X-Forwarded-For")
        ));
    }
}
