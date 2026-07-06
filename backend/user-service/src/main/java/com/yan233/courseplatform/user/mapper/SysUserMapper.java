package com.yan233.courseplatform.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan233.courseplatform.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("""
            SELECT role_code AS roleCode, COUNT(*) AS count
            FROM sys_user
            WHERE deleted = 0
            GROUP BY role_code
            ORDER BY count DESC
            """)
    List<Map<String, Object>> countByRole();
}

