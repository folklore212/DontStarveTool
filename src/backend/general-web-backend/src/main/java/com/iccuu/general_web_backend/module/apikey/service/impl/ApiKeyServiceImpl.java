package com.iccuu.general_web_backend.module.apikey.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iccuu.general_web_backend.common.cache.ApiKeyCacheManager;
import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.enums.ApiKeyStatus;
import com.iccuu.general_web_backend.common.exception.ResourceNotFoundException;
import com.iccuu.general_web_backend.common.result.PageQuery;
import com.iccuu.general_web_backend.common.util.HashUtil;
import com.iccuu.general_web_backend.common.util.SecureRandomUtil;
import com.iccuu.general_web_backend.module.apikey.dto.*;
import com.iccuu.general_web_backend.module.apikey.entity.ApiKey;
import com.iccuu.general_web_backend.module.apikey.mapper.ApiKeyMapper;
import com.iccuu.general_web_backend.common.converter.ApiKeyConverter;
import com.iccuu.general_web_backend.module.apikey.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyMapper apiKeyMapper;
    private final ApiKeyCacheManager apiKeyCacheManager;
    private final ApiKeyConverter apiKeyConverter;
    private static final String KEY_PREFIX = "dsk-";

    @Override
    public IPage<ApiKeyVO> listByUser(Long userId, PageQuery query) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getUserId, userId)
                .orderByDesc(ApiKey::getCreatedAt);
        return apiKeyMapper.selectPage(query.toPage(), wrapper).convert(this::toVO);
    }

    @Override
    public ApiKeyCreateResponse create(Long userId, ApiKeyCreateRequest request) {
        String rawKey = generateRawKey();
        String keyPrefix = KEY_PREFIX + rawKey.substring(0, 8);
        String keyHash = HashUtil.sha256(rawKey);

        ApiKey apiKey = new ApiKey();
        apiKey.setUserId(userId);
        apiKey.setKeyName(request.getKeyName());
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setAllowedScopes(request.getAllowedScopes());
        apiKey.setExpiresAt(request.getExpiresAt());
        apiKey.setStatus(ApiKeyStatus.NORMAL.getValue());
        apiKey.setCreatedAt(LocalDateTime.now());

        apiKeyMapper.insert(apiKey);

        apiKeyCacheManager.cache(apiKey);

        return ApiKeyCreateResponse.builder()
                .keyPrefix(keyPrefix)
                .rawKey(rawKey)
                .expiresAt(request.getExpiresAt())
                .build();
    }

    @Override
    public void revoke(Long keyId, Long userId) {
        ApiKey apiKey = findOwned(keyId, userId);
        apiKey.setStatus(ApiKeyStatus.DISABLED.getValue());
        apiKeyMapper.updateById(apiKey);
        apiKeyMapper.deleteById(keyId);
        apiKeyCacheManager.invalidate(apiKey.getKeyHash());
    }

    @Override
    @Transactional
    public ApiKeyCreateResponse rotate(Long keyId, Long userId) {
        ApiKey oldKey = findOwned(keyId, userId);
        apiKeyCacheManager.invalidate(oldKey.getKeyHash());

        revoke(keyId, userId);

        ApiKeyCreateRequest request = new ApiKeyCreateRequest();
        request.setKeyName(oldKey.getKeyName());
        request.setAllowedScopes(oldKey.getAllowedScopes());
        request.setExpiresAt(oldKey.getExpiresAt());

        return create(userId, request);
    }

    private ApiKey findOwned(Long keyId, Long userId) {
        ApiKey apiKey = apiKeyMapper.selectOne(
                new LambdaQueryWrapper<ApiKey>()
                        .eq(ApiKey::getId, keyId)
                        .eq(ApiKey::getUserId, userId));
        if (apiKey == null) {
            throw new ResourceNotFoundException(ErrorCode.API_KEY_NOT_FOUND, "API key not found: " + keyId);
        }
        return apiKey;
    }

    private String generateRawKey() {
        return SecureRandomUtil.generateSecureToken(32);
    }

    private ApiKeyVO toVO(ApiKey apiKey) {
        return apiKeyConverter.toVO(apiKey);
    }
}
