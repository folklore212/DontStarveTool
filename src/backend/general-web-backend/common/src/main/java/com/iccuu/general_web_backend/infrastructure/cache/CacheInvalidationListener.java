package com.iccuu.general_web_backend.core.warmer;

import com.iccuu.general_web_backend.common.constant.RedisKeyPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class CacheInvalidationListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationListener.class);

    private final ConcurrentHashMap<String, Consumer<String>> handlers = new ConcurrentHashMap<>();

    public void registerHandler(String channel, Consumer<String> handler) {
        handlers.put(channel, handler);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());
        log.debug("Received cache invalidation: channel={}, body={}", channel, body);

        Consumer<String> handler = handlers.get(channel);
        if (handler != null) {
            try {
                handler.accept(body);
            } catch (Exception e) {
                log.warn("Cache invalidation handler failed for channel={}: {}", channel, e.getMessage());
            }
        }
    }
}
