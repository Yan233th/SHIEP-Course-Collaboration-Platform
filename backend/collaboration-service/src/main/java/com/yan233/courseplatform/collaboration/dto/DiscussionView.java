package com.yan233.courseplatform.collaboration.dto;

import java.time.LocalDateTime;

/**
 * 讨论展示视图：在帖子字段之外附带作者姓名，避免前端只拿到 authorId。
 */
public record DiscussionView(
        Long id,
        Long courseId,
        Long groupId,
        Long parentId,
        Long authorId,
        String authorName,
        String authorUsername,
        String title,
        String content,
        Integer status,
        LocalDateTime createTime
) {
}
