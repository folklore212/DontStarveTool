package com.iccuu.general_web_backend.core.converter;

import com.iccuu.general_web_backend.module.auth.dto.LoginLogVO;
import com.iccuu.general_web_backend.module.auth.entity.LoginLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LoginLogConverter {
    LoginLogVO toVO(LoginLog entity);
}
