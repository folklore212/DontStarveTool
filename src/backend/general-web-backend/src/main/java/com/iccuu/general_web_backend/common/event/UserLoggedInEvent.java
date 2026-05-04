package com.iccuu.general_web_backend.common.event;

import com.iccuu.general_web_backend.common.enums.IdentityType;
import org.springframework.context.ApplicationEvent;

public class UserLoggedInEvent extends ApplicationEvent {

    private final Long userId;
    private final IdentityType identityType;
    private final String ipAddress;
    private final String userAgent;
    private final boolean success;
    private final String failureReason;

    public UserLoggedInEvent(Object source, Long userId, IdentityType identityType,
                              String ipAddress, String userAgent, boolean success,
                              String failureReason) {
        super(source);
        this.userId = userId;
        this.identityType = identityType;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.success = success;
        this.failureReason = failureReason;
    }

    public Long getUserId() { return userId; }
    public IdentityType getIdentityType() { return identityType; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public boolean isSuccess() { return success; }
    public String getFailureReason() { return failureReason; }
}
