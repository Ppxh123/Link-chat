package com.linkchat.server.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linkchat.server.entity.MessageAck;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageAckRepository extends BaseMapper<MessageAck> {
}