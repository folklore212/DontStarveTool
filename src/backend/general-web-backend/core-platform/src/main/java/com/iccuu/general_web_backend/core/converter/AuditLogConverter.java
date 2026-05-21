package com.iccuu.general_web_backend.core.converter;

import com.iccuu.general_web_backend.module.audit.dto.AuditLogVO;
import com.iccuu.general_web_backend.module.audit.entity.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogConverter {
    AuditLogVO toVO(AuditLog entity);
}
