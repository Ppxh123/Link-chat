package com.linkchat.server.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linkchat.server.entity.OfflineMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OfflineMessageRepository extends BaseMapper<OfflineMessage> {
}