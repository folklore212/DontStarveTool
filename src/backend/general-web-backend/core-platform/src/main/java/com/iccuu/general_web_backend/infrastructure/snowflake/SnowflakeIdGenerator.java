package com.iccuu.general_web_backend.infrastructure.snowflake;

import cn.hutool.core.util.IdUtil;
import com.iccuu.general_web_backend.common.constant.RedisKeyPrefix;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class SnowflakeIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    private static final int HEARTBEAT_TTL_SECONDS = 30;
    private static final int HEARTBEAT_INTERVAL_SECONDS = 10;

    private final long workerId;
    private final long datacenterId;
    private final String instanceId;
    private volatile cn.hutool.core.lang.Snowflake snowflake;

    private final StringRedisTemplate redisTemplate;
    private ScheduledExecutorService heartbeatScheduler;

    public SnowflakeIdGenerator(@Value("${snowflake.worker-id:1}") long workerId,
                                @Value("${snowflake.datacenter-id:1}") long datacenterId,
                                StringRedisTemplate redisTemplate) {
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.redisTemplate = redisTemplate;
        this.instanceId = resolveInstanceId();
        this.snowflake = IdUtil.getSnowflake(workerId, datacenterId);
    }

    @PostConstruct
    public void init() {
        String workerKey = RedisKeyPrefix.fmt(RedisKeyPrefix.SNOWFLAKE_WORKER, String.valueOf(workerId));

        Boolean claimed = redisTemplate.opsForValue()
                .setIfAbsent(workerKey, instanceId, HEARTBEAT_TTL_SECONDS, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(claimed)) {
            log.info("Snowflake worker-id={} claimed by instance={}", workerId, instanceId);
        } else {
            String currentHolder = redisTemplate.opsForValue().get(workerKey);
            log.warn("Snowflake worker-id={} already claimed by {}. If this is a stale registration, "
                    + "it will expire after {}s. Proceeding with configured worker-id.",
                    workerId, currentHolder, HEARTBEAT_TTL_SECONDS);
        }

        startHeartbeat(workerKey);
    }

    @PreDestroy
    public void destroy() {
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdown();
        }
        String workerKey = RedisKeyPrefix.fmt(RedisKeyPrefix.SNOWFLAKE_WORKER, String.valueOf(workerId));
        try {
            String currentHolder = redisTemplate.opsForValue().get(workerKey);
            if (instanceId.equals(currentHolder)) {
                redisTemplate.delete(workerKey);
                log.info("Snowflake worker-id={} released by instance={}", workerId, instanceId);
            }
        } catch (Exception e) {
            log.warn("Failed to release Snowflake worker-id={}: {}", workerId, e.getMessage());
        }
    }

    public synchronized long nextId() {
        return snowflake.nextId();
    }

    private void startHeartbeat(String workerKey) {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "snowflake-heartbeat");
            t.setDaemon(true);
            return t;
        });

        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                String currentHolder = redisTemplate.opsForValue().get(workerKey);
                if (instanceId.equals(currentHolder)) {
                    redisTemplate.expire(workerKey, HEARTBEAT_TTL_SECONDS, TimeUnit.SECONDS);
                } else {
                    log.warn("Snowflake worker-id={} heartbeat conflict: current holder is {}, instance is {}",
                            workerId, currentHolder, instanceId);
                    Boolean reclaimed = redisTemplate.opsForValue()
                            .setIfAbsent(workerKey, instanceId, HEARTBEAT_TTL_SECONDS, TimeUnit.SECONDS);
                    if (Boolean.TRUE.equals(reclaimed)) {
                        log.info("Snowflake worker-id={} reclaimed by instance={}", workerId, instanceId);
                    }
                }
            } catch (Exception e) {
                log.warn("Snowflake heartbeat failed for worker-id={}: {}", workerId, e.getMessage());
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private static String resolveInstanceId() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown-" + System.currentTimeMillis();
        }
    }
}
