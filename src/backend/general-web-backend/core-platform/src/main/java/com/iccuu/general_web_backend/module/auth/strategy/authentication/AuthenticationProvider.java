package com.iccuu.general_web_backend.module.auth.strategy.authentication;

import com.iccuu.general_web_backend.common.enums.AuthMethod;
import com.iccuu.general_web_backend.module.auth.dto.LoginRequest;
import com.iccuu.general_web_backend.module.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthenticationProvider {

    AuthMethod getMethod();

    boolean supports(LoginRequest request);

    AuthenticationResult authenticate(LoginRequest request, HttpServletRequest httpRequest);

    void handleFailedLogin(User user);

    void handleSuccessfulLogin(User user);
}
