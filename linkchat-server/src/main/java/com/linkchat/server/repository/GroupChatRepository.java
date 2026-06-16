package com.linkchat.server.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linkchat.server.entity.GroupChat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupChatRepository extends BaseMapper<GroupChat> {
}