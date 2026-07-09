package com.yan233.courseplatform.course.dto;

import com.yan233.courseplatform.common.dto.FileBrief;

import java.time.LocalDateTime;

public record ResourceView(
        Long id,
        Long courseId,
        Long fileId,
        FileBrief file,
        String title,
        String category,
        String tags,
        String description,
        Long uploaderId,
        Integer status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
