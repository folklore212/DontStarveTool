package com.iccuu.general_web_backend.common.event;

import org.springframework.context.ApplicationEvent;

public class UserRegisteredEvent extends ApplicationEvent {

    private final Long userId;
    private final String username;
    private final String email;
    private final String phone;
    private final String identityType;
    private final String identifier;
    private final String encodedPassword;

    public UserRegisteredEvent(Object source, Long userId, String username,
                                String email, String phone, String identityType,
                                String identifier, String encodedPassword) {
        super(source);
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.identityType = identityType;
        this.identifier = identifier;
        this.encodedPassword = encodedPassword;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getIdentityType() { return identityType; }
    public String getIdentifier() { return identifier; }
    public String getEncodedPassword() { return encodedPassword; }
}
