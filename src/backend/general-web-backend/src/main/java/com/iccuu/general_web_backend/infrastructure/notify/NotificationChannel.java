package com.iccuu.general_web_backend.infrastructure.notify;

import java.util.Map;

public interface NotificationChannel {
    String getChannelId();
    String getDisplayName();
    boolean isAvailable();
    void send(NotificationMessage msg) throws NotificationException;

    record NotificationMessage(String title, String body, String level, Map<String, String> metadata) {}
    class NotificationException extends Exception {
        public NotificationException(String msg, Throwable cause) { super(msg, cause); }
    }
}
