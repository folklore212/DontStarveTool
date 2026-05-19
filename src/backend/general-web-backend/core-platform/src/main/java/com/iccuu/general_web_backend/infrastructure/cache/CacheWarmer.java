package com.iccuu.general_web_backend.infrastructure.cache;

import com.iccuu.general_web_backend.module.role.cache.PermissionCacheManager;
import com.iccuu.general_web_backend.module.user.dto.UserVO;
import com.iccuu.general_web_backend.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class CacheWarmer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmer.class);

    private final PermissionCacheManager cacheManager;
    private final UserService userService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        CompletableFuture.runAsync(() -> {
            try {
                List<UserVO> recentUsers = userService.getRecentlyActiveUsers(100);
                int count = 0;
                for (UserVO u : recentUsers) {
                    try {
                        cacheManager.getEffectivePermissions(u.getUserId());
                        count++;
                    } catch (Exception e) {
                        log.warn("Cache warmup failed for userId={}", u.getUserId(), e);
                    }
                }
                log.info("Cache warmup completed: {} users preloaded", count);
            } catch (Exception e) {
                log.error("Cache warmup failed", e);
            }
        });
    }
}
