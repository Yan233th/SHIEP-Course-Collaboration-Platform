package com.yan233.courseplatform.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoticeRequest {
    @NotNull
    private Long courseId;
    @NotBlank
    @Size(max = 100)
    private String title;
    @NotBlank
    @Size(max = 2000)
    private String content;
    private Long publisherId;
    private Integer pinned = 0;
    private Integer status = 1;
}

