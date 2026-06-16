package com.linkchat.server.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linkchat.server.entity.Friend;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FriendRepository extends BaseMapper<Friend> {
}