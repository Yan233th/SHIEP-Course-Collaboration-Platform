package com.yan233.courseplatform.course.dto;

import com.yan233.courseplatform.common.dto.FileBrief;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubmissionView(
        Long id,
        Long assignmentId,
        Long studentId,
        Long fileId,
        FileBrief file,
        String content,
        BigDecimal score,
        String feedback,
        Integer status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
