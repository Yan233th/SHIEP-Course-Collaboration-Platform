package com.yan233.courseplatform.course.dto;

public record CourseMemberDetail(
        Long id,
        Long courseId,
        Long userId,
        String username,
        String realName,
        String systemRole,
        String courseRole,
        Integer status
) {
}
