package com.linkchat.server.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linkchat.server.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserRepository extends BaseMapper<User> {

    @Select("SELECT * FROM user WHERE (email LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%') OR user_code LIKE CONCAT('%', #{keyword}, '%')) AND is_deleted = 0")
    List<User> searchByKeyword(String keyword);
}