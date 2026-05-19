package com.iccuu.general_web_backend.module.oauth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iccuu.general_web_backend.module.oauth.dto.*;
import com.iccuu.general_web_backend.common.result.PageQuery;

public interface OAuthClientService {

    IPage<OAuthClientVO> list(PageQuery query);

    OAuthClientVO getById(Long id);

    OAuthClientVO create(OAuthClientCreateRequest request);

    OAuthClientVO update(Long id, OAuthClientUpdateRequest request);

    void delete(Long id);

    String regenerateSecret(Long id);
}
