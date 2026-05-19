package com.iccuu.general_web_backend.module.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.exception.BusinessException;
import com.iccuu.general_web_backend.common.exception.DuplicateResourceException;
import com.iccuu.general_web_backend.module.role.dto.*;
import com.iccuu.general_web_backend.module.role.entity.*;
import com.iccuu.general_web_backend.module.role.mapper.*;
import com.iccuu.general_web_backend.module.role.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final ScopeMapper scopeMapper;

    @Override
    public List<RoleVO> listRoles() {
        List<Role> roles = roleMapper.selectList(null);
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, List<Role>> childrenMap = new HashMap<>();
        List<Role> roots = new ArrayList<>();
        for (Role role : roles) {
            if (role.getParentRoleId() == null || role.getParentRoleId() == 0) {
                roots.add(role);
            } else {
                childrenMap.computeIfAbsent(role.getParentRoleId(), k -> new ArrayList<>()).add(role);
            }
        }

        return roots.stream()
                .map(root -> buildRoleVO(root, childrenMap))
                .collect(Collectors.toList());
    }

    private RoleVO buildRoleVO(Role role, Map<Integer, List<Role>> childrenMap) {
        RoleVO vo = toRoleVO(role);
        List<Role> children = childrenMap.get(role.getId());
        if (children != null && !children.isEmpty()) {
            vo.setChildren(children.stream()
                    .map(child -> buildRoleVO(child, childrenMap))
                    .collect(Collectors.toList()));
        }
        return vo;
    }

    @Override
    public List<RoleTreeVO> getRoleTree() {
        List<Role> roles = roleMapper.selectList(null);
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, List<Role>> childrenMap = new HashMap<>();
        List<Role> roots = new ArrayList<>();
        for (Role role : roles) {
            if (role.getParentRoleId() == null || role.getParentRoleId() == 0) {
                roots.add(role);
            } else {
                childrenMap.computeIfAbsent(role.getParentRoleId(), k -> new ArrayList<>()).add(role);
            }
        }

        return roots.stream()
                .map(root -> buildRoleTreeVO(root, childrenMap))
                .collect(Collectors.toList());
    }

    private RoleTreeVO buildRoleTreeVO(Role role, Map<Integer, List<Role>> childrenMap) {
        RoleTreeVO vo = new RoleTreeVO();
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setDescription(role.getDescription());
        vo.setParentRoleId(role.getParentRoleId());
        List<Role> children = childrenMap.get(role.getId());
        if (children != null && !children.isEmpty()) {
            vo.setChildren(children.stream()
                    .map(child -> buildRoleTreeVO(child, childrenMap))
                    .collect(Collectors.toList()));
        }
        return vo;
    }

    @Override
    public RoleVO getRole(Integer roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        RoleVO vo = toRoleVO(role);

        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        if (!rolePermissions.isEmpty()) {
            List<Integer> permIds = rolePermissions.stream()
                    .map(RolePermission::getPermissionId)
                    .collect(Collectors.toList());
            List<Permission> permissions = permissionMapper.selectBatchIds(permIds);
            vo.setPermissions(permissions.stream().map(this::toPermissionVO).collect(Collectors.toList()));
        }
        return vo;
    }

    @Override
    @Transactional
    public RoleVO createRole(RoleCreateRequest request) {
        Long count = roleMapper.selectCount(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleName, request.getRoleName()));
        if (count > 0) {
            throw new DuplicateResourceException(ErrorCode.ROLE_NAME_EXISTS);
        }

        Role role = new Role();
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setParentRoleId(request.getParentRoleId());
        role.setIsSystem(request.getIsSystem() != null ? request.getIsSystem() : 0);
        roleMapper.insert(role);

        return toRoleVO(role);
    }

    @Override
    @Transactional
    public RoleVO updateRole(Integer roleId, RoleUpdateRequest request) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getParentRoleId() != null) {
            role.setParentRoleId(request.getParentRoleId());
        }
        roleMapper.updateById(role);

        return toRoleVO(role);
    }

    @Override
    @Transactional
    public void deleteRole(Integer roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        if (role.getIsSystem() != null && role.getIsSystem() == 1) {
            throw new BusinessException(ErrorCode.ROLE_SYSTEM_PROTECTED);
        }

        Long userCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId));
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.ROLE_IN_USE.getCode(), "角色已分配给用户，无法删除");
        }

        roleMapper.deleteById(roleId);
    }

    @Override
    public List<PermissionVO> getRolePermissions(Integer roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }

        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        if (rolePermissions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> permIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
        List<Permission> permissions = permissionMapper.selectBatchIds(permIds);
        return permissions.stream().map(this::toPermissionVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignPermissions(Integer roleId, AssignPermissionRequest request) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }

        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            for (Integer permId : request.getPermissionIds()) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permId);
                rp.setScopeId(request.getScopeId());
                rolePermissionMapper.insert(rp);
            }
        }
    }

    @Override
    @Transactional
    public void removePermission(Integer roleId, Integer permissionId) {
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>()
                        .eq(RolePermission::getRoleId, roleId)
                        .eq(RolePermission::getPermissionId, permissionId));
    }

    @Override
    @Transactional
    public void assignUserRoles(Long userId, AssignRoleRequest request) {
        if (request.getRoleIds() == null || request.getRoleIds().isEmpty()) {
            return;
        }

        for (Integer roleId : request.getRoleIds()) {
            UserRole existing = userRoleMapper.selectOne(
                    new LambdaQueryWrapper<UserRole>()
                            .eq(UserRole::getUserId, userId)
                            .eq(UserRole::getRoleId, roleId)
                            .eq(request.getScopeType() != null, UserRole::getScopeType, request.getScopeType())
                            .eq(request.getScopeValue() != null, UserRole::getScopeValue, request.getScopeValue()));

            if (existing != null) {
                if (request.getExpiresAt() != null) {
                    existing.setExpiresAt(request.getExpiresAt());
                }
                userRoleMapper.updateById(existing);
            } else {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRole.setScopeType(request.getScopeType());
                userRole.setScopeValue(request.getScopeValue());
                userRole.setExpiresAt(request.getExpiresAt());
                userRoleMapper.insert(userRole);
            }
        }
    }

    @Override
    @Transactional
    public void removeUserRole(Long userId, Integer roleId, String scopeType, String scopeValue) {
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, roleId);
        if (scopeType != null) {
            wrapper.eq(UserRole::getScopeType, scopeType);
        }
        if (scopeValue != null) {
            wrapper.eq(UserRole::getScopeValue, scopeValue);
        }
        userRoleMapper.delete(wrapper);
    }

    @Override
    public List<String> getPermissionStrings(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        return userRoles.stream()
                .map(ur -> "ROLE_" + ur.getRoleId())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignDefaultRole(Long userId) {
        Role defaultRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleName, "user"));
        if (defaultRole != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(defaultRole.getId());
            userRole.setCreatedAt(java.time.LocalDateTime.now());
            userRoleMapper.insert(userRole);
        }
    }

    @Override
    public List<UserRoleVO> getUserRoles(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        Map<Integer, String> roleNameMap = roles.stream()
                .collect(Collectors.toMap(Role::getId, Role::getRoleName));

        return userRoles.stream().map(ur -> {
            UserRoleVO vo = new UserRoleVO();
            vo.setRoleId(ur.getRoleId());
            vo.setRoleName(roleNameMap.getOrDefault(ur.getRoleId(), ""));
            vo.setScopeType(ur.getScopeType());
            vo.setScopeValue(ur.getScopeValue());
            vo.setGrantedBy(ur.getGrantedBy());
            vo.setExpiresAt(ur.getExpiresAt());
            vo.setCreatedAt(ur.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    private RoleVO toRoleVO(Role role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setDescription(role.getDescription());
        vo.setParentRoleId(role.getParentRoleId());
        vo.setIsSystem(role.getIsSystem());
        vo.setCreatedAt(role.getCreatedAt());
        return vo;
    }

    private PermissionVO toPermissionVO(Permission p) {
        PermissionVO vo = new PermissionVO();
        vo.setId(p.getId());
        vo.setCode(p.getCode());
        vo.setName(p.getName());
        vo.setResourceType(p.getResourceType());
        vo.setAction(p.getAction());
        vo.setDescription(p.getDescription());
        return vo;
    }
}
