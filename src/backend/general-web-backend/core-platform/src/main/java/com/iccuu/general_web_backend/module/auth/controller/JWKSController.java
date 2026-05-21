package com.iccuu.general_web_backend.module.auth.controller;

import com.iccuu.general_web_backend.core.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "JWKS")
@RestController
@RequiredArgsConstructor
public class JWKSController {

    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "JWKS端点 - 用于API网关验证JWT")
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return jwtTokenProvider.getJwks();
    }
}
