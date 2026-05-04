package com.iccuu.general_web_backend.common.aspect;

import com.iccuu.general_web_backend.common.annotation.RateLimit;
import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.exception.RateLimitException;
import com.iccuu.general_web_backend.common.util.IpUtil;
import com.iccuu.general_web_backend.common.util.SecurityUtil;
import com.iccuu.general_web_backend.infrastructure.metrics.MetricsService;
import com.iccuu.general_web_backend.infrastructure.security.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimiterService rateLimiterService;
    private final MetricsService metricsService;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = rateLimit.key();
        Long userId = SecurityUtil.getCurrentUserId();
        String clientId = userId != null ? "user:" + userId
                : IpUtil.getClientIp(SecurityUtil.getCurrentRequest());
        String compositeKey = key + ":" + clientId;
        int permits = rateLimit.permits();
        int windowSeconds = rateLimit.windowSeconds();

        if (!rateLimiterService.isAllowed(compositeKey, permits, windowSeconds)) {
            log.warn("Rate limit exceeded: key={} permits={} window={}s", key, permits, windowSeconds);
            String endpoint = joinPoint.getSignature().toShortString();
            metricsService.recordRateLimitRejected(endpoint, key);
            throw new RateLimitException(ErrorCode.RATE_LIMITED, "请求过于频繁，请稍后再试");
        }

        return joinPoint.proceed();
    }
}
