package com.iccuu.general_web_backend.module.oauth.service;

import com.iccuu.general_web_backend.module.oauth.dto.*;

public interface OAuthAuthorizationService {

    String generateAuthorizationCode(AuthorizationRequest request, Long userId);

    TokenResponse exchangeCodeForToken(TokenExchangeRequest request);

    TokenResponse refreshToken(String refreshToken, String clientId);

    void revokeToken(String token);
}
