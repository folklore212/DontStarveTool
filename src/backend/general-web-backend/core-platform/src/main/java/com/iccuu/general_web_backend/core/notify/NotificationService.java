package com.iccuu.general_web_backend.core.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final List<NotificationChannel> channels;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public NotificationService(List<NotificationChannel> channels) {
        this.channels = channels;
    }

    public List<Map<String, String>> getAvailableChannels() {
        return channels.stream()
                .filter(NotificationChannel::isAvailable)
                .map(c -> Map.of("id", c.getChannelId(), "name", c.getDisplayName()))
                .toList();
    }

    public void notify(NotificationChannel.NotificationMessage msg) {
        for (NotificationChannel channel : channels) {
            if (!channel.isAvailable()) continue;
            CompletableFuture.runAsync(() -> {
                try {
                    channel.send(msg);
                } catch (Exception e) {
                    log.warn("Notify failed for channel={}: {}", channel.getChannelId(), e.getMessage());
                }
            }, executor).orTimeout(30, TimeUnit.SECONDS);
        }
    }

    public void notifyToChannel(String channelId, NotificationChannel.NotificationMessage msg) {
        channels.stream().filter(c -> c.getChannelId().equals(channelId)).findFirst()
                .ifPresent(c -> {
                    try { c.send(msg); } catch (Exception e) {
                        log.warn("Notify failed: {}", e.getMessage());
                    }
                });
    }
}
