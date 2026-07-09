package com.yan233.courseplatform.collaboration.dto;

import com.yan233.courseplatform.common.dto.FileBrief;

import java.time.LocalDateTime;

public record ShowcaseView(
        Long id,
        Long courseId,
        Long groupId,
        Long fileId,
        FileBrief file,
        String title,
        String summary,
        String linkUrl,
        Integer status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
