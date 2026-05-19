package com.iccuu.general_web_backend.module.user.service;

public interface UserCredentialsHistoryService {

    boolean isPasswordUsedBefore(Long userId, String newPasswordRaw);

    void recordPasswordChange(Long userId, String passwordHash);
}
