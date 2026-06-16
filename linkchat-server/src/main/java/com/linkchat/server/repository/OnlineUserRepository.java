package com.linkchat.server.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linkchat.server.entity.OnlineUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OnlineUserRepository extends BaseMapper<OnlineUser> {
}