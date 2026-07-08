package com.yan233.courseplatform.course.dto;

import com.yan233.courseplatform.common.dto.FileBrief;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssignmentView(
        Long id,
        Long courseId,
        String title,
        String description,
        Long fileId,
        FileBrief file,
        LocalDateTime dueTime,
        BigDecimal totalScore,
        Integer status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
