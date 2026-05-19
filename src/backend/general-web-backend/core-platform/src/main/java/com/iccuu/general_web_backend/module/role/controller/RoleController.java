package com.iccuu.general_web_backend.module.role.controller;

import com.iccuu.general_web_backend.common.annotation.RequirePermission;
import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.module.role.dto.*;
import com.iccuu.general_web_backend.module.role.service.PermissionService;
import com.iccuu.general_web_backend.module.role.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;

    @GetMapping("/roles")
    @RequirePermission("role:read")
    public R<List<RoleVO>> listRoles() {
        return R.ok(roleService.listRoles());
    }

    @GetMapping("/roles/tree")
    @RequirePermission("role:read")
    public R<List<RoleTreeVO>> getRoleTree() {
        return R.ok(roleService.getRoleTree());
    }

    @GetMapping("/roles/{id}")
    @RequirePermission("role:read")
    public R<RoleVO> getRole(@PathVariable("id") Integer roleId) {
        return R.ok(roleService.getRole(roleId));
    }

    @PostMapping("/roles")
    @RequirePermission("role:create")
    public R<RoleVO> createRole(@Valid @RequestBody RoleCreateRequest request) {
        return R.ok(roleService.createRole(request));
    }

    @PutMapping("/roles/{id}")
    @RequirePermission("role:update")
    public R<RoleVO> updateRole(@PathVariable("id") Integer roleId,
                                @RequestBody RoleUpdateRequest request) {
        return R.ok(roleService.updateRole(roleId, request));
    }

    @DeleteMapping("/roles/{id}")
    @RequirePermission("role:delete")
    public R<Void> deleteRole(@PathVariable("id") Integer roleId) {
        roleService.deleteRole(roleId);
        return R.ok();
    }

    @GetMapping("/roles/{id}/permissions")
    @RequirePermission("perm:read")
    public R<List<PermissionVO>> getRolePermissions(@PathVariable("id") Integer roleId) {
        return R.ok(roleService.getRolePermissions(roleId));
    }

    @PostMapping("/roles/{id}/permissions")
    @RequirePermission("perm:assign")
    public R<Void> assignPermissions(@PathVariable("id") Integer roleId,
                                     @RequestBody AssignPermissionRequest request) {
        roleService.assignPermissions(roleId, request);
        return R.ok();
    }

    @DeleteMapping("/roles/{id}/permissions/{permId}")
    @RequirePermission("perm:assign")
    public R<Void> removePermission(@PathVariable("id") Integer roleId,
                                    @PathVariable("permId") Integer permId) {
        roleService.removePermission(roleId, permId);
        return R.ok();
    }

    @GetMapping("/permissions")
    @RequirePermission("perm:read")
    public R<List<PermissionVO>> listAllPermissions() {
        return R.ok(permissionService.listAllPermissions());
    }

    @GetMapping("/scopes")
    @RequirePermission("perm:read")
    public R<List<ScopeVO>> listAllScopes() {
        return R.ok(permissionService.listAllScopes());
    }
}
