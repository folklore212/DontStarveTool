package com.iccuu.general_web_backend.module.auth.dto;

import com.iccuu.general_web_backend.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoginLogQueryRequest extends PageQuery {

    private Long userId;

    private String result;

    private String identityType;

    private String startDate;

    private String endDate;
}
