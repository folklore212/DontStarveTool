package com.iccuu.general_web_backend.module.oauth.controller;

import com.iccuu.general_web_backend.common.annotation.RequirePermission;
import com.iccuu.general_web_backend.common.result.PageQuery;
import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.common.util.SecurityUtil;
import com.iccuu.general_web_backend.module.oauth.dto.*;
import com.iccuu.general_web_backend.module.oauth.service.OAuthAuthorizationService;
import com.iccuu.general_web_backend.module.oauth.service.OAuthClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/oauth")
@RequiredArgsConstructor
public class OAuthClientController {

    private final OAuthClientService oauthClientService;
    private final OAuthAuthorizationService oauthAuthorizationService;

    @GetMapping("/clients")
    @RequirePermission("client:read")
    public R<?> listClients(@Valid PageQuery query) {
        return R.ok(oauthClientService.list(query));
    }

    @GetMapping("/clients/{id}")
    @RequirePermission("client:read")
    public R<?> getClient(@PathVariable Long id) {
        return R.ok(oauthClientService.getById(id));
    }

    @PostMapping("/clients")
    @RequirePermission("client:create")
    public R<?> createClient(@Valid @RequestBody OAuthClientCreateRequest request) {
        return R.ok(oauthClientService.create(request));
    }

    @PutMapping("/clients/{id}")
    @RequirePermission("client:update")
    public R<?> updateClient(@PathVariable Long id, @Valid @RequestBody OAuthClientUpdateRequest request) {
        return R.ok(oauthClientService.update(id, request));
    }

    @DeleteMapping("/clients/{id}")
    @RequirePermission("client:delete")
    public R<?> deleteClient(@PathVariable Long id) {
        oauthClientService.delete(id);
        return R.ok();
    }

    @PostMapping("/clients/{id}/regenerate-secret")
    @RequirePermission("client:update")
    public R<?> regenerateSecret(@PathVariable Long id) {
        return R.ok(oauthClientService.regenerateSecret(id));
    }

    @GetMapping("/authorize")
    public R<?> authorize(@Valid AuthorizationRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        String code = oauthAuthorizationService.generateAuthorizationCode(request, userId);
        return R.ok(code);
    }

    @PostMapping("/token")
    public R<?> exchangeToken(@Valid @RequestBody TokenExchangeRequest request) {
        return R.ok(oauthAuthorizationService.exchangeCodeForToken(request));
    }

    @PostMapping("/revoke")
    public R<?> revokeToken(@RequestParam String token) {
        oauthAuthorizationService.revokeToken(token);
        return R.ok();
    }
}
