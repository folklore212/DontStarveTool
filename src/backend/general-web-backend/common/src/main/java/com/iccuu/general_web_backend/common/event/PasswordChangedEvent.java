package com.iccuu.general_web_backend.common.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

public class PasswordChangedEvent extends ApplicationEvent {

    private final Long userId;
    private final LocalDateTime changedAt;

    public PasswordChangedEvent(Object source, Long userId, LocalDateTime changedAt) {
        super(source);
        this.userId = userId;
        this.changedAt = changedAt;
    }

    public Long getUserId() { return userId; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
