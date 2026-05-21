package com.iccuu.general_web_backend.core.security;

import java.util.Set;

public interface PermissionResolver {

    Set<EffectivePermission> resolvePermissions(Long userId);

    record EffectivePermission(String permissionCode, String scopeType, Set<String> scopeValues) {}
}
