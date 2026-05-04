package com.iccuu.general_web_backend.module.user.service.impl;

import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.exception.ResourceNotFoundException;
import com.iccuu.general_web_backend.module.user.entity.UserProfile;
import com.iccuu.general_web_backend.module.user.mapper.UserProfileMapper;
import com.iccuu.general_web_backend.module.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileMapper userProfileMapper;

    @Override
    public UserProfile getByUserId(Long userId) {
        UserProfile profile = userProfileMapper.selectById(userId);
        if (profile == null) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
        }
        return profile;
    }

    @Override
    public void update(UserProfile profile) {
        profile.setUpdatedAt(LocalDateTime.now());
        userProfileMapper.insertOrUpdate(profile);
    }
}
