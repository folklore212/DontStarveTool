package com.iccuu.general_web_backend.module.role.service;

import com.iccuu.general_web_backend.module.role.dto.*;
import java.util.List;

public interface RoleService {
    List<RoleVO> listRoles();
    List<RoleTreeVO> getRoleTree();
    RoleVO getRole(Integer roleId);
    RoleVO createRole(RoleCreateRequest request);
    RoleVO updateRole(Integer roleId, RoleUpdateRequest request);
    void deleteRole(Integer roleId);
    List<PermissionVO> getRolePermissions(Integer roleId);
    void assignPermissions(Integer roleId, AssignPermissionRequest request);
    void removePermission(Integer roleId, Integer permissionId);
    void assignUserRoles(Long userId, AssignRoleRequest request);
    void removeUserRole(Long userId, Integer roleId, String scopeType, String scopeValue);
    List<UserRoleVO> getUserRoles(Long userId);
    List<String> getPermissionStrings(Long userId);
    void assignDefaultRole(Long userId);
}
