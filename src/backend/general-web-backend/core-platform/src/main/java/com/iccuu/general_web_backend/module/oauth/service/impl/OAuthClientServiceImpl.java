package com.iccuu.general_web_backend.module.oauth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iccuu.general_web_backend.core.converter.OAuthClientConverter;
import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.exception.DuplicateResourceException;
import com.iccuu.general_web_backend.common.exception.ResourceNotFoundException;
import com.iccuu.general_web_backend.common.result.PageQuery;
import com.iccuu.general_web_backend.common.util.SecureRandomUtil;
import com.iccuu.general_web_backend.module.oauth.dto.*;
import com.iccuu.general_web_backend.module.oauth.entity.OAuthClient;
import com.iccuu.general_web_backend.module.oauth.mapper.OAuthClientMapper;
import com.iccuu.general_web_backend.module.oauth.service.OAuthClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Primary
@Service
@RequiredArgsConstructor
public class OAuthClientServiceImpl implements OAuthClientService {

    private final OAuthClientMapper oauthClientMapper;
    private final PasswordEncoder passwordEncoder;
    private final OAuthClientConverter oauthClientConverter;
    private static final int STATUS_ENABLED = 1;

    @Override
    public IPage<OAuthClientVO> list(PageQuery query) {
        return oauthClientMapper.selectPage(query.toPage(), new LambdaQueryWrapper<>())
                .convert(this::toVO);
    }

    @Override
    public OAuthClientVO getById(Long id) {
        OAuthClient client = oauthClientMapper.selectById(id);
        if (client == null) {
            throw new ResourceNotFoundException(ErrorCode.CLIENT_NOT_FOUND, "OAuth client not found: " + id);
        }
        return toVO(client);
    }

    @Override
    public OAuthClientVO create(OAuthClientCreateRequest request) {
        Long count = oauthClientMapper.selectCount(
                new LambdaQueryWrapper<OAuthClient>().eq(OAuthClient::getClientId, request.getClientId()));
        if (count != null && count > 0) {
            throw new DuplicateResourceException(ErrorCode.CLIENT_ID_EXISTS, "Client ID already exists: " + request.getClientId());
        }

        OAuthClient client = new OAuthClient();
        client.setClientId(request.getClientId());
        client.setClientName(request.getClientName());
        client.setClientType(request.getClientType());
        client.setGrantTypes(request.getGrantTypes());
        client.setRedirectUris(request.getRedirectUris());
        client.setAllowedScopes(request.getAllowedScopes());
        client.setIsTrusted(request.getIsTrusted() != null ? request.getIsTrusted() : 0);
        client.setStatus(STATUS_ENABLED);

        String rawSecret = generateRawSecret();
        client.setClientSecret(passwordEncoder.encode(rawSecret));
        client.setCreatedAt(LocalDateTime.now());
        client.setUpdatedAt(LocalDateTime.now());

        oauthClientMapper.insert(client);
        return toVO(client);
    }

    @Override
    public OAuthClientVO update(Long id, OAuthClientUpdateRequest request) {
        OAuthClient client = oauthClientMapper.selectById(id);
        if (client == null) {
            throw new ResourceNotFoundException(ErrorCode.CLIENT_NOT_FOUND, "OAuth client not found: " + id);
        }

        if (request.getClientName() != null) {
            client.setClientName(request.getClientName());
        }
        if (request.getGrantTypes() != null) {
            client.setGrantTypes(request.getGrantTypes());
        }
        if (request.getRedirectUris() != null) {
            client.setRedirectUris(request.getRedirectUris());
        }
        if (request.getAllowedScopes() != null) {
            client.setAllowedScopes(request.getAllowedScopes());
        }
        if (request.getIsTrusted() != null) {
            client.setIsTrusted(request.getIsTrusted());
        }
        if (request.getStatus() != null) {
            client.setStatus(request.getStatus());
        }
        client.setUpdatedAt(LocalDateTime.now());

        oauthClientMapper.updateById(client);
        return toVO(client);
    }

    @Override
    public void delete(Long id) {
        OAuthClient client = oauthClientMapper.selectById(id);
        if (client == null) {
            throw new ResourceNotFoundException(ErrorCode.CLIENT_NOT_FOUND, "OAuth client not found: " + id);
        }
        oauthClientMapper.deleteById(id);
    }

    @Override
    public String regenerateSecret(Long id) {
        OAuthClient client = oauthClientMapper.selectById(id);
        if (client == null) {
            throw new ResourceNotFoundException(ErrorCode.CLIENT_NOT_FOUND, "OAuth client not found: " + id);
        }

        String rawSecret = generateRawSecret();
        client.setClientSecret(passwordEncoder.encode(rawSecret));
        client.setUpdatedAt(LocalDateTime.now());
        oauthClientMapper.updateById(client);

        return rawSecret;
    }

    private String generateRawSecret() {
        return SecureRandomUtil.generateSecureToken(32);
    }

    private OAuthClientVO toVO(OAuthClient client) {
        return oauthClientConverter.toVO(client);
    }
}
