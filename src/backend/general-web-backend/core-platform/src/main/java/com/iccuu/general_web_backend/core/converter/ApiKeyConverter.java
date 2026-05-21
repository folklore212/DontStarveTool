package com.iccuu.general_web_backend.core.converter;

import com.iccuu.general_web_backend.module.apikey.dto.ApiKeyVO;
import com.iccuu.general_web_backend.module.apikey.entity.ApiKey;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApiKeyConverter {
    ApiKeyVO toVO(ApiKey entity);
}
