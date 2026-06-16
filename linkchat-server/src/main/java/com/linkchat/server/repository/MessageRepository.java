package com.linkchat.server.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linkchat.server.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageRepository extends BaseMapper<Message> {
}