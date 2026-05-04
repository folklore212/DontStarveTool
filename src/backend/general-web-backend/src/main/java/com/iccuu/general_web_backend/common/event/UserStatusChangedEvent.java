package com.iccuu.general_web_backend.common.event;

import org.springframework.context.ApplicationEvent;

public class UserStatusChangedEvent extends ApplicationEvent {

    private final Long userId;
    private final Integer oldStatus;
    private final Integer newStatus;

    public UserStatusChangedEvent(Object source, Long userId, Integer oldStatus, Integer newStatus) {
        super(source);
        this.userId = userId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public Long getUserId() { return userId; }
    public Integer getOldStatus() { return oldStatus; }
    public Integer getNewStatus() { return newStatus; }
}
