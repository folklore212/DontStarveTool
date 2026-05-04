package com.iccuu.general_web_backend.module.server.controller;

import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.infrastructure.notify.NotificationChannel;
import com.iccuu.general_web_backend.infrastructure.notify.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final NotificationService notificationService;

    @GetMapping("/channels")
    public R<List<Map<String, String>>> getChannels() {
        return R.ok(notificationService.getAvailableChannels());
    }

    @PostMapping("/test")
    public R<Void> testNotify(@RequestBody Map<String, String> body) {
        var msg = new NotificationChannel.NotificationMessage(
                body.getOrDefault("title", "Test Notification"),
                body.getOrDefault("body", "This is a test"),
                body.getOrDefault("level", "INFO"),
                Map.of()
        );
        notificationService.notify(msg);
        return R.ok();
    }
}
