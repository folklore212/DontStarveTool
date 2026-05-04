package com.iccuu.general_web_backend.module.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iccuu.general_web_backend.module.user.dto.*;

import java.util.List;

public interface UserService {

    IPage<UserVO> listUsers(UserQueryRequest request);

    UserVO getUserById(Long userId);

    UserVO createUser(UserCreateRequest request);

    UserVO updateUser(Long userId, UserUpdateRequest request);

    void deleteUser(Long userId);

    void updateStatus(Long userId, UserStatusRequest request);

    List<UserAuthVO> getUserAuths(Long userId);

    void bindIdentity(Long userId, BindAuthRequest request);

    void unbindIdentity(Long userId, Long authId);

    UserVO getCurrentUser();

    UserVO updateProfile(Long userId, UserProfileUpdateRequest request);

    UserVO updateNickname(Long userId, NicknameUpdateRequest request);

    List<UserVO> getRecentlyActiveUsers(int limit);
}
