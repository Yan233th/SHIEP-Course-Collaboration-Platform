package com.yan233.courseplatform.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan233.courseplatform.course.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
    @Select("SELECT * FROM v_course_overview ORDER BY resource_count DESC, assignment_count DESC")
    List<Map<String, Object>> overview();

    @Select("CALL sp_course_activity_stats(#{courseId})")
    List<Map<String, Object>> activityStats(Long courseId);
}

