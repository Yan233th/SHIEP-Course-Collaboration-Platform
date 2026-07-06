package com.yan233.courseplatform.course.dto;

import java.util.List;

public record CourseAccess(
        Long courseId,
        String systemRole,
        String courseRole,
        List<String> actions
) {
}
