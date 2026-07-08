package com.yan233.courseplatform.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan233.courseplatform.file.entity.FileReference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FileReferenceMapper extends BaseMapper<FileReference> {
    @Select("""
            SELECT COUNT(*)
            FROM file_reference
            WHERE file_id = #{fileId}
              AND deleted = 0
              AND status = 1
            """)
    int countActiveByFileId(@Param("fileId") Long fileId);
}
