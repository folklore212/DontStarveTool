package com.iccuu.general_web_backend.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iccuu.general_web_backend.core.converter.UserConverter;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.enums.IdentityType;
import com.iccuu.general_web_backend.common.enums.UserStatus;
import com.iccuu.general_web_backend.common.exception.BusinessException;
import com.iccuu.general_web_backend.common.exception.DuplicateResourceException;
import com.iccuu.general_web_backend.common.exception.ResourceNotFoundException;
import com.iccuu.general_web_backend.common.util.DateUtil;
import com.iccuu.general_web_backend.common.util.SecurityUtil;
import com.iccuu.general_web_backend.module.user.dto.*;
import com.iccuu.general_web_backend.module.user.entity.User;
import com.iccuu.general_web_backend.module.user.entity.UserAuth;
import com.iccuu.general_web_backend.module.user.entity.UserProfile;
import com.iccuu.general_web_backend.module.user.mapper.UserAuthMapper;
import com.iccuu.general_web_backend.module.user.mapper.UserMapper;
import com.iccuu.general_web_backend.module.user.mapper.UserProfileMapper;
import com.iccuu.general_web_backend.module.user.service.UserCredentialsHistoryService;
import com.iccuu.general_web_backend.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Primary
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;
    private final UserProfileMapper userProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserCredentialsHistoryService credentialsHistoryService;
    private final UserConverter userConverter;

    @Override
    public IPage<UserVO> listUsers(UserQueryRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (request.getStatus() != null) {
            wrapper.eq(User::getStatus, request.getStatus());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w
                    .like(User::getUsername, request.getKeyword())
                    .or()
                    .like(User::getEmail, request.getKeyword()));
        }
        if (StringUtils.hasText(request.getStartDate())) {
            wrapper.ge(User::getCreatedAt, DateUtil.parseDateTimeParam(request.getStartDate(), true));
        }
        if (StringUtils.hasText(request.getEndDate())) {
            wrapper.le(User::getCreatedAt, DateUtil.parseDateTimeParam(request.getEndDate(), false));
        }
        wrapper.orderByDesc(User::getCreatedAt);

        IPage<User> page = userMapper.selectPage(request.toPage(), wrapper);
        return page.convert(this::toUserVO);
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
        }
        return toUserVO(user);
    }

    @Override
    @Transactional
    public UserVO createUser(UserCreateRequest request) {
        // Check username uniqueness
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())) > 0) {
            throw new DuplicateResourceException(ErrorCode.USERNAME_EXISTS);
        }
        // Check email uniqueness
        if (StringUtils.hasText(request.getEmail())) {
            if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())) > 0) {
                throw new DuplicateResourceException(ErrorCode.EMAIL_EXISTS);
            }
        }
        // Check phone uniqueness
        if (StringUtils.hasText(request.getPhone())) {
            if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())) > 0) {
                throw new DuplicateResourceException(ErrorCode.PHONE_EXISTS);
            }
        }

        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setStatus(UserStatus.NORMAL.getValue());
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);

        // Create auth record (username + password)
        UserAuth auth = new UserAuth();
        auth.setUserId(user.getUserId());
        auth.setIdentityType(IdentityType.USERNAME.getValue());
        auth.setIdentifier(request.getUsername());
        auth.setCredential(passwordEncoder.encode(request.getPassword()));
        auth.setVerified(1);
        auth.setIsPrimary(1);
        auth.setCreatedAt(now);
        auth.setUpdatedAt(now);
        userAuthMapper.insert(auth);

        // Create profile
        UserProfile profile = new UserProfile();
        profile.setUserId(user.getUserId());
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        userProfileMapper.insert(profile);

        // Record password change
        credentialsHistoryService.recordPasswordChange(user.getUserId(), auth.getCredential());

        return toUserVO(user);
    }

    @Override
    public UserVO updateUser(Long userId, UserUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
        }

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, request.getEmail())
                    .ne(User::getUserId, userId)) > 0) {
                throw new DuplicateResourceException(ErrorCode.EMAIL_EXISTS);
            }
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().equals(user.getPhone())) {
            if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, request.getPhone())
                    .ne(User::getUserId, userId)) > 0) {
                throw new DuplicateResourceException(ErrorCode.PHONE_EXISTS);
            }
            user.setPhone(request.getPhone());
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        return toUserVO(userMapper.selectById(userId));
    }

    @Override
    public void deleteUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
        }
        // MyBatis-Plus @TableLogic handles soft delete automatically
        userMapper.deleteById(userId);
    }

    @Override
    public void updateStatus(Long userId, UserStatusRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
        }
        user.setStatus(request.getStatus());
        user.setLockedUntil(request.getLockedUntil());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public List<UserAuthVO> getUserAuths(Long userId) {
        if (userMapper.selectById(userId) == null) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
        }
        List<UserAuth> auths = userAuthMapper.selectList(
                new LambdaQueryWrapper<UserAuth>().eq(UserAuth::getUserId, userId));
        if (auths == null || auths.isEmpty()) {
            return Collections.emptyList();
        }
        return auths.stream()
                .map(userConverter::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void bindIdentity(Long userId, BindAuthRequest request) {
        if (userMapper.selectById(userId) == null) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
        }

        // Check identity not taken
        String normalizedType = request.getIdentityType().toLowerCase();
        Long count = userAuthMapper.selectCount(new LambdaQueryWrapper<UserAuth>()
                .eq(UserAuth::getIdentityType, normalizedType)
                .eq(UserAuth::getIdentifier, request.getIdentifier()));
        if (count > 0) {
            throw new DuplicateResourceException(ErrorCode.IDENTITY_TAKEN);
        }

        UserAuth auth = new UserAuth();
        auth.setUserId(userId);
        auth.setIdentityType(request.getIdentityType().toLowerCase());
        auth.setIdentifier(request.getIdentifier());
        auth.setCredential(request.getCredential() != null ? passwordEncoder.encode(request.getCredential()) : null);
        auth.setVerified(0);
        auth.setIsPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : 0);
        LocalDateTime now = LocalDateTime.now();
        auth.setCreatedAt(now);
        auth.setUpdatedAt(now);
        userAuthMapper.insert(auth);

        // If set as primary, unset others
        if (auth.getIsPrimary() == 1) {
            UserAuth updateWrapper = new UserAuth();
            updateWrapper.setIsPrimary(0);
            userAuthMapper.update(updateWrapper, new LambdaQueryWrapper<UserAuth>()
                    .eq(UserAuth::getUserId, userId)
                    .ne(UserAuth::getId, auth.getId()));
        }
    }

    @Override
    @Transactional
    public void unbindIdentity(Long userId, Long authId) {
        if (userMapper.selectById(userId) == null) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
        }

        // Check not last identity
        Long count = userAuthMapper.selectCount(new LambdaQueryWrapper<UserAuth>()
                .eq(UserAuth::getUserId, userId));
        if (count <= 1) {
            throw new BusinessException(ErrorCode.LAST_IDENTITY);
        }

        // MyBatis-Plus @TableLogic handles soft delete
        userAuthMapper.deleteById(authId);
    }

    @Override
    public UserVO getCurrentUser() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
        }
        return toUserVO(user);
    }

    @Override
    public UserVO updateProfile(Long userId, UserProfileUpdateRequest request) {
        UserProfile profile = userProfileMapper.selectById(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setCreatedAt(LocalDateTime.now());
        }

        if (request.getRealName() != null) {
            profile.setRealName(request.getRealName());
        }
        if (request.getLocale() != null) {
            profile.setLocale(request.getLocale());
        }
        if (request.getTimezone() != null) {
            profile.setTimezone(request.getTimezone());
        }
        if (request.getMetadata() != null) {
            profile.setMetadata(request.getMetadata());
        }
        profile.setUpdatedAt(LocalDateTime.now());

        userProfileMapper.insertOrUpdate(profile);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
        }
        return toUserVO(user);
    }

    @Override
    public UserVO updateNickname(Long userId, NicknameUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
        }
        user.setNickname(request.getNickname());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toUserVO(user);
    }

    @Override
    public List<UserVO> getRecentlyActiveUsers(int limit) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .orderByDesc(User::getLastLoginAt)
                .last("LIMIT " + limit));
        return users.stream()
                .map(this::toUserVO)
                .collect(Collectors.toList());
    }

    private UserVO toUserVO(User user) {
        return userConverter.toVO(user);
    }
}
