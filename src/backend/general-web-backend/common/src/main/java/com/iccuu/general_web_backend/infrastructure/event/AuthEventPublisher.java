package com.iccuu.general_web_backend.core.event;

import com.iccuu.general_web_backend.common.event.PasswordChangedEvent;
import com.iccuu.general_web_backend.common.event.UserLoggedInEvent;
import com.iccuu.general_web_backend.common.event.UserRegisteredEvent;
import com.iccuu.general_web_backend.common.event.UserStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publishUserRegistered(UserRegisteredEvent event) {
        publisher.publishEvent(event);
    }

    public void publishPasswordChanged(PasswordChangedEvent event) {
        publisher.publishEvent(event);
    }

    public void publishUserLoggedIn(UserLoggedInEvent event) {
        publisher.publishEvent(event);
    }

    public void publishUserStatusChanged(UserStatusChangedEvent event) {
        publisher.publishEvent(event);
    }
}
