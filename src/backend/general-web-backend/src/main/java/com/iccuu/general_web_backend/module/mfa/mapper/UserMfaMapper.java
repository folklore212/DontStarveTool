package com.iccuu.general_web_backend.module.mfa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iccuu.general_web_backend.module.mfa.entity.UserMfa;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMfaMapper extends BaseMapper<UserMfa> {
}
