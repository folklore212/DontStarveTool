package com.iccuu.general_web_backend.module.node.service;

import com.iccuu.general_web_backend.module.node.mapper.NodeTokenMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NodeTokenServiceTest {

    private NodeTokenMapper mapper;
    private NodeTokenService service;

    @BeforeEach
    void setup() {
        mapper = mock(NodeTokenMapper.class);
        // MyBatis-Plus @TableId ASSIGN_ID doesn't work without context.
        // Mock insert to set the ID like MyBatis would.
        doAnswer(inv -> {
            com.iccuu.general_web_backend.module.node.entity.NodeToken entity = inv.getArgument(0);
            entity.setId(1L);
            return 1;
        }).when(mapper).insert(any(com.iccuu.general_web_backend.module.node.entity.NodeToken.class));
        service = new NodeTokenService(mapper);
    }

    @Test
    void createTokenShouldReturnTokenWithPrefix() {
        Map<String, Object> result = service.createToken(1L);
        assertNotNull(result.get("token"));
        assertTrue(result.get("token").toString().startsWith("dsn-"), "token should start with dsn-");
        assertEquals(1L, result.get("serverId"));
    }

    @Test
    void createTokenShouldHavePrefixField() {
        Map<String, Object> result = service.createToken(1L);
        String prefix = result.get("prefix").toString();
        assertTrue(prefix.startsWith("dsn-"), "prefix should start with dsn-");
        assertEquals(12, prefix.length(), "prefix should be 12 chars");
    }

    @Test
    void verifyInvalidTokenShouldReturnNotValid() {
        when(mapper.selectOne(any())).thenReturn(null);
        Map<String, Object> result = service.verify("invalid-token");
        assertFalse(Boolean.TRUE.equals(result.get("valid")), "invalid token should return valid=false");
    }

    @Test
    void revokeTokenShouldSetStatusZero() {
        var entity = new com.iccuu.general_web_backend.module.node.entity.NodeToken();
        entity.setId(1L);
        entity.setStatus(1);
        when(mapper.selectById(1L)).thenReturn(entity);

        service.revokeToken(1L);
        assertEquals(0, entity.getStatus());
        verify(mapper).updateById(entity);
    }
}
