package com.iccuu.general_web_backend.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iccuu.general_web_backend.module.user.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
