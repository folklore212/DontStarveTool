package com.iccuu.general_web_backend.module.auth.strategy.authentication;

import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.enums.IdentityType;
import com.iccuu.general_web_backend.common.event.UserLoggedInEvent;
import com.iccuu.general_web_backend.module.user.entity.User;
import com.iccuu.general_web_backend.module.user.entity.UserAuth;

import java.util.List;

public record AuthenticationResult(
        boolean success,
        User user,
        UserAuth userAuth,
        boolean mfaRequired,
        MfaContext mfaContext,
        ErrorCode errorCode,
        String failureReason,
        IdentityType identityType,
        String ipAddress,
        String userAgent,
        boolean newDevice) {

    public record MfaContext(long userId, List<String> mfaTypes) {}

    public static AuthenticationResult success(User user, UserAuth userAuth,
            IdentityType identityType, String ipAddress, String userAgent, boolean newDevice) {
        return new AuthenticationResult(true, user, userAuth, false, null,
                null, null, identityType, ipAddress, userAgent, newDevice);
    }

    public static AuthenticationResult mfaRequired(User user, UserAuth userAuth,
            long userId, List<String> mfaTypes, IdentityType identityType,
            String ipAddress, String userAgent) {
        return new AuthenticationResult(true, user, userAuth, true,
                new MfaContext(userId, mfaTypes),
                null, null, identityType, ipAddress, userAgent, false);
    }

    public static AuthenticationResult failure(User user, UserAuth userAuth,
            ErrorCode errorCode, String failureReason, IdentityType identityType) {
        return new AuthenticationResult(false, user, userAuth, false, null,
                errorCode, failureReason, identityType, null, null, false);
    }

    public UserLoggedInEvent toSuccessEvent() {
        return new UserLoggedInEvent(this, user.getUserId(), identityType,
                ipAddress, userAgent, true, null);
    }

    public UserLoggedInEvent toFailedEvent() {
        return new UserLoggedInEvent(this, user != null ? user.getUserId() : null,
                identityType, ipAddress, userAgent, false,
                errorCode != null ? String.valueOf(errorCode.getCode()) : failureReason);
    }
}
