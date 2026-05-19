package com.iccuu.general_web_backend.module.user.service;

import com.iccuu.general_web_backend.module.user.entity.UserProfile;

public interface UserProfileService {

    UserProfile getByUserId(Long userId);

    void update(UserProfile profile);
}
