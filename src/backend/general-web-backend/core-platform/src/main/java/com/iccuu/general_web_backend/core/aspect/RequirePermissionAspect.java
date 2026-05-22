package com.iccuu.general_web_backend.core.aspect;

import com.iccuu.general_web_backend.common.annotation.RequirePermission;
import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.exception.AuthorizationException;
import com.iccuu.general_web_backend.common.util.SecurityUtil;
import com.iccuu.general_web_backend.infrastructure.security.PermissionResolver.EffectivePermission;
import com.iccuu.general_web_backend.module.role.cache.PermissionCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequirePermissionAspect {

    private final PermissionCacheManager cacheManager;

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint jp, RequirePermission requirePermission) throws Throwable {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new AuthorizationException(ErrorCode.UNAUTHORIZED);
        }

        Set<EffectivePermission> perms = cacheManager.getEffectivePermissions(userId);
        boolean hasPerm = perms.stream().anyMatch(p ->
                p.permissionCode().equals(requirePermission.value()));
        if (!hasPerm) {
            throw new AccessDeniedException(
                    "Permission denied: " + requirePermission.value());
        }

        return jp.proceed();
    }
}
