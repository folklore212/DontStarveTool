package com.iccuu.general_web_backend.core.geetest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.exception.BusinessException;
import com.iccuu.general_web_backend.infrastructure.metrics.MetricsService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class GeeTestVerifier {

    private static final Logger log = LoggerFactory.getLogger(GeeTestVerifier.class);
    private static final String VALIDATE_URL = "https://gcaptcha4.geetest.com/validate";

    private final boolean skipVerification;
    private final String loginCaptchaId;
    private final String loginCaptchaKey;
    private final String registerCaptchaId;
    private final String registerCaptchaKey;
    private final RestTemplate restTemplate;
    private final MetricsService metricsService;

    public GeeTestVerifier(
            @Value("${geetest.skip-verification:false}") boolean skipVerification,
            @Value("${geetest.login.captcha-id:dummy}") String loginCaptchaId,
            @Value("${geetest.login.captcha-key:dummy}") String loginCaptchaKey,
            @Value("${geetest.register.captcha-id:dummy}") String registerCaptchaId,
            @Value("${geetest.register.captcha-key:dummy}") String registerCaptchaKey,
            RestTemplate restTemplate,
            MetricsService metricsService) {
        this.skipVerification = skipVerification;
        this.loginCaptchaId = loginCaptchaId;
        this.loginCaptchaKey = loginCaptchaKey;
        this.registerCaptchaId = registerCaptchaId;
        this.registerCaptchaKey = registerCaptchaKey;
        this.restTemplate = restTemplate;
        this.metricsService = metricsService;
    }

    @CircuitBreaker(name = "geetest", fallbackMethod = "verifyFallback")
    public boolean verify(String captchaOutput, String lotNumber, String passToken, String genTime, boolean isLogin) {
        if (skipVerification) {
            log.info("GeeTest verification skipped (skip-verification=true)");
            metricsService.recordGeeTestResult("skipped");
            return true;
        }

        String captchaId = isLogin ? loginCaptchaId : registerCaptchaId;
        String captchaKey = isLogin ? loginCaptchaKey : registerCaptchaKey;
        String signToken = generateSignToken(captchaKey, lotNumber);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("lot_number", lotNumber);
        body.add("captcha_output", captchaOutput);
        body.add("pass_token", passToken);
        body.add("gen_time", genTime);
        body.add("captcha_id", captchaId);
        body.add("sign_token", signToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.postForEntity(VALIDATE_URL, request, Map.class);
        } catch (RestClientException e) {
            log.warn("GeeTest API unreachable for lot_number={}: {}", lotNumber, e.getMessage());
            metricsService.recordGeeTestResult("fail_open");
            throw new BusinessException(ErrorCode.GEE_TEST_FAILED, "GeeTest service unavailable");
        }

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object result = response.getBody().get("result");
            boolean success = "success".equals(result);
            if (success) {
                metricsService.recordGeeTestResult("success");
                return true;
            }
        }

        Map<String, Object> respBody = response.getBody();
        log.warn("GeeTest verification failed: lot_number={}, captcha_id={}", lotNumber, captchaId);
        metricsService.recordGeeTestResult("failed");
        throw new BusinessException(ErrorCode.GEE_TEST_FAILED,
                respBody != null ? respBody.toString() : "GeeTest verification failed");
    }

    @SuppressWarnings("unused")
    private boolean verifyFallback(String captchaOutput, String lotNumber, String passToken,
                                    String genTime, boolean isLogin, CallNotPermittedException e) {
        log.warn("GeeTest circuit breaker open — verification skipped for lot_number={}", lotNumber);
        metricsService.recordGeeTestResult("circuit_open");
        throw e;
    }

    @SuppressWarnings("unused")
    private boolean verifyFallback(String captchaOutput, String lotNumber, String passToken,
                                    String genTime, boolean isLogin, Exception e) {
        if (e instanceof BusinessException) {
            throw (BusinessException) e;
        }
        log.error("GeeTest verification error for lot_number={}", lotNumber, e);
        metricsService.recordGeeTestResult("error");
        throw new BusinessException(ErrorCode.GEE_TEST_FAILED, "GeeTest service unavailable");
    }

    private String generateSignToken(String captchaKey, String lotNumber) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(captchaKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(lotNumber.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate sign token", e);
        }
    }
}
