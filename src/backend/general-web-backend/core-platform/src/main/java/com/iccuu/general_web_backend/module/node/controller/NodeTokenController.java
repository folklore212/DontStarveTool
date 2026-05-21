package com.iccuu.general_web_backend.module.node.controller;

import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.module.node.service.NodeTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-facing API for managing Node bootstrap tokens.
 * Internal verify endpoint is on InternalController.
 */
@RestController
@RequestMapping("/api/v1/servers/{serverId}/tokens")
@RequiredArgsConstructor
public class NodeTokenController {

    private final NodeTokenService service;

    @PostMapping
    public R<Map<String, Object>> create(@PathVariable Long serverId) {
        return R.ok(service.createToken(serverId));
    }

    @DeleteMapping("/{tokenId}")
    public R<Void> revoke(@PathVariable Long serverId, @PathVariable Long tokenId) {
        service.revokeToken(tokenId);
        return R.ok();
    }
}
