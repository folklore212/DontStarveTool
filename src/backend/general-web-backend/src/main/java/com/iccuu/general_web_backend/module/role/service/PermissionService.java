package com.iccuu.general_web_backend.module.role.service;

import com.iccuu.general_web_backend.module.role.dto.*;
import java.util.List;

public interface PermissionService {
    List<PermissionVO> listAllPermissions();
    List<ScopeVO> listAllScopes();
}
