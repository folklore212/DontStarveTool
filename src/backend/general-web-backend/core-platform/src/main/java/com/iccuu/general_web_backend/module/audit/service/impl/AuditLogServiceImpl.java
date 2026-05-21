package com.iccuu.general_web_backend.module.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iccuu.general_web_backend.common.constant.Constants;
import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.exception.ResourceNotFoundException;
import com.iccuu.general_web_backend.common.util.DateUtil;
import com.iccuu.general_web_backend.module.audit.dto.*;
import com.iccuu.general_web_backend.module.audit.entity.AuditLog;
import com.iccuu.general_web_backend.module.audit.mapper.AuditLogMapper;
import com.iccuu.general_web_backend.core.converter.AuditLogConverter;
import com.iccuu.general_web_backend.module.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final AuditLogConverter auditLogConverter;

    @Override
    @Async("taskExecutor")
    public void record(AuditLogRecord record) {
        AuditLog entity = new AuditLog();
        entity.setUserId(record.userId());
        entity.setClientId(record.clientId());
        entity.setAction(record.action());
        entity.setResourceType(record.resourceType());
        entity.setResourceId(record.resourceId());
        entity.setDetail(record.detail());
        entity.setIpAddress(record.ipAddress());
        entity.setUserAgent(record.userAgent());
        entity.setSessionId(record.sessionId());
        entity.setRequestId(record.requestId());
        entity.setClientIpChain(record.clientIpChain());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedDate(LocalDate.now());

        auditLogMapper.insert(entity);
    }

    @Override
    public IPage<AuditLogVO> query(AuditLogQueryRequest request) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();

        if (request.getUserId() != null) {
            wrapper.eq(AuditLog::getUserId, request.getUserId());
        }
        if (StringUtils.isNotBlank(request.getClientId())) {
            wrapper.eq(AuditLog::getClientId, request.getClientId());
        }
        if (StringUtils.isNotBlank(request.getAction())) {
            wrapper.eq(AuditLog::getAction, request.getAction());
        }
        if (StringUtils.isNotBlank(request.getResourceType())) {
            wrapper.eq(AuditLog::getResourceType, request.getResourceType());
        }
        if (StringUtils.isNotBlank(request.getStartDate())) {
            wrapper.ge(AuditLog::getCreatedAt, DateUtil.parseDateTimeParam(request.getStartDate(), true));
        }
        if (StringUtils.isNotBlank(request.getEndDate())) {
            wrapper.le(AuditLog::getCreatedAt, DateUtil.parseDateTimeParam(request.getEndDate(), false));
        }

        long total = auditLogMapper.selectCount(wrapper);

        wrapper.orderByDesc(AuditLog::getCreatedAt);
        Page<AuditLog> page = new Page<>(request.getPage(), request.getSize(), false);
        IPage<AuditLogVO> result = auditLogMapper.selectPage(page, wrapper).convert(this::toVO);
        result.setTotal(total);
        return result;
    }

    @Override
    public AuditLogVO getById(Long id) {
        AuditLog entity = auditLogMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException(ErrorCode.AUDIT_LOG_NOT_FOUND, "Audit log not found: " + id);
        }
        return toVO(entity);
    }

    @Override
    public void anonymizeByUserId(Long userId) {
        auditLogMapper.update(null,
                new LambdaUpdateWrapper<AuditLog>()
                        .eq(AuditLog::getUserId, userId)
                        .set(AuditLog::getIpAddress, null)
                        .set(AuditLog::getClientIpChain, null));
    }

    @Override
    public List<AuditLogVO> exportByUserId(Long userId) {
        Page<AuditLog> exportPage = new Page<>(1, Constants.MAX_EXPORT_RECORDS, false);
        List<AuditLog> logs = auditLogMapper.selectPage(
                exportPage,
                new LambdaQueryWrapper<AuditLog>()
                        .eq(AuditLog::getUserId, userId)
                        .orderByDesc(AuditLog::getCreatedAt))
                .getRecords();
        return logs.stream().map(this::toVO).collect(Collectors.toList());
    }

    private AuditLogVO toVO(AuditLog entity) {
        return auditLogConverter.toVO(entity);
    }
}
