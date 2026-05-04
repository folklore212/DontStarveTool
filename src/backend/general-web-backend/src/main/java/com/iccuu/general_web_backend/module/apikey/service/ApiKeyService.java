package com.iccuu.general_web_backend.module.apikey.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iccuu.general_web_backend.module.apikey.dto.*;
import com.iccuu.general_web_backend.common.result.PageQuery;

public interface ApiKeyService {

    IPage<ApiKeyVO> listByUser(Long userId, PageQuery query);

    ApiKeyCreateResponse create(Long userId, ApiKeyCreateRequest request);

    void revoke(Long keyId, Long userId);

    ApiKeyCreateResponse rotate(Long keyId, Long userId);
}
