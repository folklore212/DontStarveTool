package com.iccuu.general_web_backend.core.controller;

import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Internal API for cross-service communication.
 * Only accessible from other platform services (Docker internal network).
 */
@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalController {

    private final UserService userService;

    @GetMapping("/users/{userId}/profile")
    public R<Map<String, Object>> getUserProfile(@PathVariable Long userId) {
        var user = userService.getUserById(userId);
        if (user == null) return R.fail(404, "User not found");
        return R.ok(Map.of(
            "userId", user.getUserId(),
            "username", user.getUsername(),
            "nickname", user.getNickname() != null ? user.getNickname() : user.getUsername(),
            "avatar", user.getAvatar() != null ? user.getAvatar() : ""
        ));
    }
}
