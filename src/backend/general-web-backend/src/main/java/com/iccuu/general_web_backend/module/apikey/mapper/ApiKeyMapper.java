package com.iccuu.general_web_backend.module.apikey.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iccuu.general_web_backend.module.apikey.entity.ApiKey;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKey> {
}
