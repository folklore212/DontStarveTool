package com.iccuu.general_web_backend.module.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iccuu.general_web_backend.module.server.entity.Server;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ServerMapper extends BaseMapper<Server> {
}
