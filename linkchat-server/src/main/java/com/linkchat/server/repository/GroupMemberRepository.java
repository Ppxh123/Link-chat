package com.linkchat.server.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linkchat.server.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupMemberRepository extends BaseMapper<GroupMember> {
}