package com.iccuu.general_web_backend.module.user.dto;

import com.iccuu.general_web_backend.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryRequest extends PageQuery {

    private Integer status;

    private String keyword;

    private String startDate;

    private String endDate;
}
