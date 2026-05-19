package com.iccuu.general_web_backend.common.converter;

import com.iccuu.general_web_backend.module.oauth.dto.OAuthClientVO;
import com.iccuu.general_web_backend.module.oauth.entity.OAuthClient;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OAuthClientConverter {
    OAuthClientVO toVO(OAuthClient entity);
}
