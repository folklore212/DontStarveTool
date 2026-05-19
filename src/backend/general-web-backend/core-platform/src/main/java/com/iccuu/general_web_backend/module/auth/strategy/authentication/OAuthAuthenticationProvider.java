package com.iccuu.general_web_backend.module.auth.strategy.authentication;

import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.enums.AuthMethod;
import com.iccuu.general_web_backend.common.exception.AuthenticationException;
import com.iccuu.general_web_backend.module.auth.dto.LoginRequest;
import com.iccuu.general_web_backend.module.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class OAuthAuthenticationProvider implements AuthenticationProvider {

    @Override
    public AuthMethod getMethod() {
        return AuthMethod.OAUTH;
    }

    @Override
    public boolean supports(LoginRequest request) {
        return false;
    }

    @Override
    public AuthenticationResult authenticate(LoginRequest request, HttpServletRequest httpRequest) {
        throw new UnsupportedOperationException("OAuth social login not yet implemented");
    }

    @Override
    public void handleFailedLogin(User user) {
    }

    @Override
    public void handleSuccessfulLogin(User user) {
    }
}
