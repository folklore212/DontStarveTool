package com.iccuu.general_web_backend.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iccuu.general_web_backend.common.constant.Constants;
import com.iccuu.general_web_backend.common.converter.LoginLogConverter;
import com.iccuu.general_web_backend.common.util.DateUtil;
import com.iccuu.general_web_backend.module.auth.dto.LoginLogQueryRequest;
import com.iccuu.general_web_backend.module.auth.dto.LoginLogVO;
import com.iccuu.general_web_backend.module.auth.entity.LoginLog;
import com.iccuu.general_web_backend.module.auth.mapper.LoginLogMapper;
import com.iccuu.general_web_backend.module.auth.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Primary
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogMapper loginLogMapper;
    private final LoginLogConverter loginLogConverter;

    @Override
    @Async("taskExecutor")
    public void record(LoginLog log) {
        loginLogMapper.insert(log);
    }

    @Override
    public IPage<LoginLogVO> queryPage(LoginLogQueryRequest request) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();

        if (request.getUserId() != null) {
            wrapper.eq(LoginLog::getUserId, request.getUserId());
        }
        if (StringUtils.isNotBlank(request.getResult())) {
            wrapper.eq(LoginLog::getResult, request.getResult());
        }
        if (StringUtils.isNotBlank(request.getIdentityType())) {
            wrapper.eq(LoginLog::getIdentityType, request.getIdentityType().toLowerCase());
        }
        if (StringUtils.isNotBlank(request.getStartDate())) {
            wrapper.ge(LoginLog::getCreatedAt, DateUtil.parseDateTimeParam(request.getStartDate(), true));
        }
        if (StringUtils.isNotBlank(request.getEndDate())) {
            wrapper.le(LoginLog::getCreatedAt, DateUtil.parseDateTimeParam(request.getEndDate(), false));
        }

        long total = loginLogMapper.selectCount(wrapper);

        wrapper.orderByDesc(LoginLog::getCreatedAt);
        Page<LoginLog> page = new Page<>(request.getPage(), request.getSize(), false);
        IPage<LoginLogVO> result = loginLogMapper.selectPage(page, wrapper).convert(this::toVO);
        result.setTotal(total);
        return result;
    }

    @Override
    public List<LoginLogVO> exportByUserId(Long userId) {
        Page<LoginLog> exportPage = new Page<>(1, Constants.MAX_EXPORT_RECORDS, false);
        List<LoginLog> logs = loginLogMapper.selectPage(
                exportPage,
                new LambdaQueryWrapper<LoginLog>()
                        .eq(LoginLog::getUserId, userId)
                        .orderByDesc(LoginLog::getCreatedAt))
                .getRecords();
        return logs.stream().map(this::toVO).collect(Collectors.toList());
    }

    private LoginLogVO toVO(LoginLog entity) {
        return loginLogConverter.toVO(entity);
    }
}
