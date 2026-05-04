package com.iccuu.general_web_backend.module.auth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iccuu.general_web_backend.module.auth.dto.LoginLogQueryRequest;
import com.iccuu.general_web_backend.module.auth.dto.LoginLogVO;
import com.iccuu.general_web_backend.module.auth.entity.LoginLog;

public interface LoginLogService {
    void record(LoginLog log);

    IPage<LoginLogVO> queryPage(LoginLogQueryRequest request);

    java.util.List<LoginLogVO> exportByUserId(Long userId);
}
