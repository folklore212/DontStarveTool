package com.iccuu.general_web_backend.module.role.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iccuu.general_web_backend.core.security.PermissionResolver;
import com.iccuu.general_web_backend.module.role.entity.*;
import com.iccuu.general_web_backend.module.role.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PermissionEvaluatorImpl implements PermissionResolver {

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final ScopeMapper scopeMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public Set<EffectivePermission> resolvePermissions(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Integer> roleIdSet = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toSet());

        Set<Integer> allRoleIds = resolveAllParentRoleIds(roleIdSet);

        List<RolePermission> allRolePermissions = new ArrayList<>();
        for (Integer roleId : allRoleIds) {
            List<RolePermission> rps = rolePermissionMapper.selectList(
                    new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
            allRolePermissions.addAll(rps);
        }

        if (allRolePermissions.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Integer> permIds = allRolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toSet());
        List<Permission> permissions = permissionMapper.selectBatchIds(
                new ArrayList<>(permIds));
        Map<Integer, Permission> permMap = permissions.stream()
                .collect(Collectors.toMap(Permission::getId, p -> p));

        Set<Integer> scopeIds = allRolePermissions.stream()
                .map(RolePermission::getScopeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Scope> scopeMap;
        if (!scopeIds.isEmpty()) {
            List<Scope> scopes = scopeMapper.selectBatchIds(new ArrayList<>(scopeIds));
            scopeMap = scopes.stream().collect(Collectors.toMap(Scope::getId, s -> s));
        } else {
            scopeMap = Collections.emptyMap();
        }

        Set<EffectivePermission> result = new HashSet<>();
        for (RolePermission rp : allRolePermissions) {
            Permission perm = permMap.get(rp.getPermissionId());
            if (perm == null) {
                continue;
            }

            String scopeType = "all";
            Set<String> scopeValues = new HashSet<>();
            if (rp.getScopeId() != null) {
                Scope scope = scopeMap.get(rp.getScopeId());
                if (scope != null) {
                    scopeType = scope.getScopeKey();
                }
            }

            for (UserRole ur : userRoles) {
                if (allRoleIds.contains(ur.getRoleId())) {
                    if (ur.getScopeType() != null) {
                        scopeType = ur.getScopeType();
                    }
                    if (ur.getScopeValue() != null) {
                        scopeValues.add(ur.getScopeValue());
                    }
                }
            }

            result.add(new EffectivePermission(perm.getCode(), scopeType, scopeValues));
        }

        return result;
    }

    private Set<Integer> resolveAllParentRoleIds(Set<Integer> roleIds) {
        Set<Integer> allRoleIds = new HashSet<>(roleIds);
        Set<Integer> toProcess = new HashSet<>(roleIds);

        while (!toProcess.isEmpty()) {
            List<Role> roles = roleMapper.selectBatchIds(new ArrayList<>(toProcess));
            toProcess.clear();
            for (Role role : roles) {
                if (role.getParentRoleId() != null && role.getParentRoleId() > 0
                        && !allRoleIds.contains(role.getParentRoleId())) {
                    allRoleIds.add(role.getParentRoleId());
                    toProcess.add(role.getParentRoleId());
                }
            }
        }

        return allRoleIds;
    }
}
