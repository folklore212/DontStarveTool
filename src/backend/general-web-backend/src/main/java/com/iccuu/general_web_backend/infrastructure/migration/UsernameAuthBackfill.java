package com.iccuu.general_web_backend.infrastructure.migration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iccuu.general_web_backend.module.user.entity.User;
import com.iccuu.general_web_backend.module.user.entity.UserAuth;
import com.iccuu.general_web_backend.module.user.mapper.UserAuthMapper;
import com.iccuu.general_web_backend.module.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One-time backfill: creates username identity UserAuth records for
 * existing users who registered with email but have no username-based login.
 * Runs on every startup but skips users who already have the record.
 */
@Component
public class UsernameAuthBackfill {

    private static final Logger log = LoggerFactory.getLogger(UsernameAuthBackfill.class);

    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;

    public UsernameAuthBackfill(UserMapper userMapper, UserAuthMapper userAuthMapper) {
        this.userMapper = userMapper;
        this.userAuthMapper = userAuthMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        try {
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .isNotNull(User::getEmail)
                    .ne(User::getEmail, ""));
            int created = 0;
            for (User u : users) {
                if (userAuthMapper.selectCount(new LambdaQueryWrapper<UserAuth>()
                        .eq(UserAuth::getUserId, u.getUserId())
                        .eq(UserAuth::getIdentityType, "username")) > 0) {
                    continue;
                }
                UserAuth primary = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>()
                        .eq(UserAuth::getUserId, u.getUserId())
                        .eq(UserAuth::getIsPrimary, 1));
                if (primary == null) continue;

                UserAuth ua = new UserAuth();
                ua.setUserId(u.getUserId());
                ua.setIdentityType("username");
                ua.setIdentifier(u.getUsername().toLowerCase());
                ua.setCredential(primary.getCredential());
                ua.setVerified(1);
                ua.setIsPrimary(0);
                ua.setCreatedAt(LocalDateTime.now());
                ua.setUpdatedAt(LocalDateTime.now());
                userAuthMapper.insert(ua);
                created++;
            }
            if (created > 0) {
                log.info("Backfilled username UserAuth for {} existing users", created);
            }
        } catch (Exception e) {
            log.error("UsernameAuthBackfill failed, will retry on next startup", e);
        }
    }
}
