package com.iccuu.general_web_backend.infrastructure.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class DiscordNotifier implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(DiscordNotifier.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${notify.discord.webhook-url:}")
    private String webhookUrl;

    @Override
    public String getChannelId() { return "discord"; }

    @Override
    public String getDisplayName() { return "Discord"; }

    @Override
    public boolean isAvailable() { return webhookUrl != null && !webhookUrl.isBlank(); }

    @Override
    public void send(NotificationMessage msg) throws NotificationException {
        if (!isAvailable()) return;
        try {
            int color = "CRITICAL".equals(msg.level()) ? 0xff0000 :
                        "WARN".equals(msg.level()) ? 0xffa500 : 0x3498db;
            Map<String, Object> embed = Map.of(
                "title", msg.title(),
                "description", msg.body(),
                "color", color,
                "timestamp", java.time.Instant.now().toString()
            );
            Map<String, Object> body = Map.of("embeds", java.util.List.of(embed));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(webhookUrl, new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            throw new NotificationException("Discord send failed", e);
        }
    }
}
