package com.iccuu.general_web_backend.module.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iccuu.general_web_backend.module.role.dto.PermissionVO;
import com.iccuu.general_web_backend.module.role.dto.ScopeVO;
import com.iccuu.general_web_backend.module.role.entity.Permission;
import com.iccuu.general_web_backend.module.role.entity.Scope;
import com.iccuu.general_web_backend.module.role.mapper.PermissionMapper;
import com.iccuu.general_web_backend.module.role.mapper.ScopeMapper;
import com.iccuu.general_web_backend.module.role.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;
    private final ScopeMapper scopeMapper;

    @Override
    public List<PermissionVO> listAllPermissions() {
        List<Permission> permissions = permissionMapper.selectList(null);
        return permissions.stream().map(p -> {
            PermissionVO vo = new PermissionVO();
            vo.setId(p.getId());
            vo.setCode(p.getCode());
            vo.setName(p.getName());
            vo.setResourceType(p.getResourceType());
            vo.setAction(p.getAction());
            vo.setDescription(p.getDescription());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ScopeVO> listAllScopes() {
        List<Scope> scopes = scopeMapper.selectList(null);
        return scopes.stream().map(s -> {
            ScopeVO vo = new ScopeVO();
            vo.setId(s.getId());
            vo.setScopeKey(s.getScopeKey());
            vo.setDescription(s.getDescription());
            return vo;
        }).collect(Collectors.toList());
    }
}
