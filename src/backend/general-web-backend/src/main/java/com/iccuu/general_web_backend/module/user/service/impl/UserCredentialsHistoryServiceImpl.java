package com.iccuu.general_web_backend.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iccuu.general_web_backend.module.user.entity.UserCredentialsHistory;
import com.iccuu.general_web_backend.module.user.mapper.UserCredentialsHistoryMapper;
import com.iccuu.general_web_backend.module.user.service.UserCredentialsHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCredentialsHistoryServiceImpl implements UserCredentialsHistoryService {

    private final UserCredentialsHistoryMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean isPasswordUsedBefore(Long userId, String newPasswordRaw) {
        List<UserCredentialsHistory> historyList = mapper.selectList(
                new LambdaQueryWrapper<UserCredentialsHistory>()
                        .eq(UserCredentialsHistory::getUserId, userId)
                        .orderByDesc(UserCredentialsHistory::getCreatedAt)
                        .last("LIMIT 10"));
        if (historyList == null || historyList.isEmpty()) {
            return false;
        }
        for (UserCredentialsHistory entry : historyList) {
            if (passwordEncoder.matches(newPasswordRaw, entry.getCredential())) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public void recordPasswordChange(Long userId, String passwordHash) {
        UserCredentialsHistory entry = new UserCredentialsHistory();
        entry.setUserId(userId);
        entry.setCredential(passwordHash);
        entry.setCreatedAt(LocalDateTime.now());
        mapper.insert(entry);

        // Trim to last 10 entries
        List<UserCredentialsHistory> all = mapper.selectList(
                new LambdaQueryWrapper<UserCredentialsHistory>()
                        .eq(UserCredentialsHistory::getUserId, userId)
                        .orderByDesc(UserCredentialsHistory::getCreatedAt));
        if (all != null && all.size() > 10) {
            List<Long> idsToDelete = all.stream()
                    .skip(10)
                    .map(UserCredentialsHistory::getId)
                    .toList();
            mapper.deleteByIds(idsToDelete);
        }
    }
}
