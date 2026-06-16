package com.linkchat.server.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linkchat.server.entity.FileRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileRecordRepository extends BaseMapper<FileRecord> {
}