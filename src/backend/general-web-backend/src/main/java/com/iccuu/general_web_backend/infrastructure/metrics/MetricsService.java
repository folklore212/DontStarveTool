package com.iccuu.general_web_backend.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public void recordLogin(String result, String identityType) {
        meterRegistry.counter("auth_login_attempts_total",
                "result", result, "identity_type", identityType).increment();
    }

    public void recordTokenValidation(boolean cacheHit, long durationMs) {
        meterRegistry.timer("auth_token_verification_seconds",
                        "cache_hit", String.valueOf(cacheHit))
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordRbacResolution(boolean cacheHit, long durationMs) {
        meterRegistry.timer("auth_rbac_resolution_seconds",
                        "cache_hit", String.valueOf(cacheHit))
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordRateLimitRejected(String endpoint, String dimension) {
        meterRegistry.counter("auth_ratelimit_rejected_total",
                "endpoint", endpoint, "dimension", dimension).increment();
    }

    public void recordGeeTestResult(String result) {
        meterRegistry.counter("auth_geetest_verification_total",
                "result", result).increment();
    }
}
