package com.iccuu.general_web_backend.module.node.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iccuu.general_web_backend.common.util.HashUtil;
import com.iccuu.general_web_backend.common.util.SecureRandomUtil;
import com.iccuu.general_web_backend.module.node.entity.NodeToken;
import com.iccuu.general_web_backend.module.node.mapper.NodeTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NodeTokenService {

    private final NodeTokenMapper mapper;

    private static final String TOKEN_PREFIX = "dsn";

    /**
     * Generate a new bootstrap token for a server.
     * The full token is returned ONLY ONCE here; only the hash is stored.
     */
    public Map<String, Object> createToken(Long serverId) {
        String raw = TOKEN_PREFIX + "-" + SecureRandomUtil.generateSecureToken(40);
        String prefix = raw.substring(0, 12); // "dsn-xxxxxxxx"

        NodeToken entity = new NodeToken();
        entity.setServerId(serverId);
        entity.setTokenHash(HashUtil.sha256(raw));
        entity.setTokenPrefix(prefix);
        entity.setStatus(1);
        entity.setCreatedAt(LocalDateTime.now());
        mapper.insert(entity);

        return Map.of(
            "id", entity.getId(),
            "token", raw,
            "prefix", prefix,
            "serverId", serverId
        );
    }

    /**
     * Revoke a token (soft delete).
     */
    public void revokeToken(Long tokenId) {
        var token = mapper.selectById(tokenId);
        if (token != null) {
            token.setStatus(0);
            mapper.updateById(token);
        }
    }

    /**
     * Verify a bootstrap token. Called by node-gateway via internal API.
     */
    public Map<String, Object> verify(String rawToken) {
        String hash = HashUtil.sha256(rawToken);
        var token = mapper.selectOne(
            new LambdaQueryWrapper<NodeToken>()
                .eq(NodeToken::getTokenHash, hash)
                .eq(NodeToken::getStatus, 1));
        if (token == null) {
            return Map.of("valid", false);
        }
        // Update last used time
        token.setLastUsedAt(LocalDateTime.now());
        mapper.updateById(token);

        return Map.of(
            "valid", true,
            "nodeId", token.getId(),
            "serverId", token.getServerId()
        );
    }
}
