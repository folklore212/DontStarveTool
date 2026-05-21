package com.iccuu.general_web_backend.core.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import cn.hutool.crypto.digest.DigestUtil;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class HibpService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public CompletableFuture<Boolean> isPasswordPwned(String password) {
        try {
            String sha1 = DigestUtil.sha1Hex(password).toUpperCase();
            String prefix = sha1.substring(0, 5);
            String suffix = sha1.substring(5);

            ResponseEntity<String> resp = restTemplate.getForEntity(
                    "https://api.pwnedpasswords.com/range/" + prefix, String.class);

            if (resp.getBody() != null) {
                for (String line : resp.getBody().split("\n")) {
                    String[] parts = line.trim().split(":");
                    if (parts[0].equals(suffix) && Integer.parseInt(parts[1]) >= 100) {
                        return CompletableFuture.completedFuture(true);
                    }
                }
            }
            return CompletableFuture.completedFuture(false);
        } catch (Exception e) {
            log.warn("HIBP check failed (fail-open)", e);
            return CompletableFuture.completedFuture(false);
        }
    }
}
