package com.iccuu.general_web_backend.module.oauth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iccuu.general_web_backend.module.oauth.entity.OAuthClient;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OAuthClientMapper extends BaseMapper<OAuthClient> {
}
