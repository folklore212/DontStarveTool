package com.iccuu.general_web_backend.common.converter;

import com.iccuu.general_web_backend.module.user.dto.UserAuthVO;
import com.iccuu.general_web_backend.module.user.dto.UserVO;
import com.iccuu.general_web_backend.module.user.entity.User;
import com.iccuu.general_web_backend.module.user.entity.UserAuth;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserConverter {
    UserVO toVO(User entity);
    UserAuthVO toVO(UserAuth entity);
}
