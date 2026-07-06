package com.yan233.courseplatform.course.dto;

import com.yan233.courseplatform.common.query.PageQuery;
import lombok.Data;

@Data
public class CourseQuery extends PageQuery {
    private String keyword;
    private Long teacherId;
    private Integer status;
}

