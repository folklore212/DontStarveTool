package com.iccuu.general_web_backend.module.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iccuu.general_web_backend.common.annotation.RequirePermission;
import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.module.role.dto.AssignRoleRequest;
import com.iccuu.general_web_backend.module.role.dto.UserRoleVO;
import com.iccuu.general_web_backend.module.role.service.RoleService;
import com.iccuu.general_web_backend.module.user.dto.*;
import com.iccuu.general_web_backend.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleService roleService;

    @GetMapping
    @RequirePermission("user:read")
    public R<IPage<UserVO>> listUsers(@Valid UserQueryRequest request) {
        return R.ok(userService.listUsers(request));
    }

    @GetMapping("/{userId}")
    @RequirePermission("user:read")
    public R<UserVO> getUserById(@PathVariable Long userId) {
        return R.ok(userService.getUserById(userId));
    }

    @PostMapping("/")
    @RequirePermission("user:create")
    public R<UserVO> createUser(@Valid @RequestBody UserCreateRequest request) {
        return R.ok(userService.createUser(request));
    }

    @PutMapping("/{userId}")
    @RequirePermission("user:update")
    public R<UserVO> updateUser(@PathVariable Long userId, @Valid @RequestBody UserUpdateRequest request) {
        return R.ok(userService.updateUser(userId, request));
    }

    @DeleteMapping("/{userId}")
    @RequirePermission("user:delete")
    public R<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return R.ok();
    }

    @PatchMapping("/{userId}/status")
    @RequirePermission("user:lock")
    public R<Void> updateStatus(@PathVariable Long userId, @Valid @RequestBody UserStatusRequest request) {
        userService.updateStatus(userId, request);
        return R.ok();
    }

    @GetMapping("/{userId}/roles")
    @RequirePermission("role:read")
    public R<List<UserRoleVO>> getUserRoles(@PathVariable Long userId) {
        return R.ok(roleService.getUserRoles(userId));
    }

    @PostMapping("/{userId}/roles")
    @RequirePermission("role:assign")
    public R<Void> assignRole(@PathVariable Long userId, @Valid @RequestBody AssignRoleRequest request) {
        roleService.assignUserRoles(userId, request);
        return R.ok();
    }

    @DeleteMapping("/{userId}/roles/{roleId}/{scopeType}/{scopeValue}")
    @RequirePermission("role:assign")
    public R<Void> removeRole(@PathVariable Long userId,
                              @PathVariable Integer roleId,
                              @PathVariable String scopeType,
                              @PathVariable String scopeValue) {
        roleService.removeUserRole(userId, roleId, scopeType, scopeValue);
        return R.ok();
    }

    @GetMapping("/{userId}/auths")
    @RequirePermission("user:read")
    public R<List<UserAuthVO>> getUserAuths(@PathVariable Long userId) {
        return R.ok(userService.getUserAuths(userId));
    }

    @PostMapping("/{userId}/auths")
    @RequirePermission("user:update")
    public R<Void> bindIdentity(@PathVariable Long userId, @Valid @RequestBody BindAuthRequest request) {
        userService.bindIdentity(userId, request);
        return R.ok();
    }

    @DeleteMapping("/{userId}/auths/{authId}")
    @RequirePermission("user:update")
    public R<Void> unbindIdentity(@PathVariable Long userId, @PathVariable Long authId) {
        userService.unbindIdentity(userId, authId);
        return R.ok();
    }

    @GetMapping("/me")
    public R<UserVO> getCurrentUser() {
        return R.ok(userService.getCurrentUser());
    }

    @PutMapping("/me/profile")
    public R<UserVO> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        Long userId = com.iccuu.general_web_backend.common.util.SecurityUtil.getCurrentUserId();
        return R.ok(userService.updateProfile(userId, request));
    }

    @PutMapping("/me/nickname")
    public R<UserVO> updateNickname(@Valid @RequestBody NicknameUpdateRequest request) {
        Long userId = com.iccuu.general_web_backend.common.util.SecurityUtil.getCurrentUserId();
        return R.ok(userService.updateNickname(userId, request));
    }

    @PutMapping("/me/avatar")
    public R<UserVO> updateAvatar(@RequestBody UserUpdateRequest request) {
        Long userId = com.iccuu.general_web_backend.common.util.SecurityUtil.getCurrentUserId();
        UserUpdateRequest avatarRequest = new UserUpdateRequest();
        avatarRequest.setAvatar(request.getAvatar());
        return R.ok(userService.updateUser(userId, avatarRequest));
    }
}
