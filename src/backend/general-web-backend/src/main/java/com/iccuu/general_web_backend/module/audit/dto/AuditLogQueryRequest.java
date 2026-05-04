package com.iccuu.general_web_backend.module.audit.dto;

import com.iccuu.general_web_backend.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogQueryRequest extends PageQuery {

    private Long userId;

    private String clientId;

    private String action;

    private String resourceType;

    private String startDate;

    private String endDate;
}
