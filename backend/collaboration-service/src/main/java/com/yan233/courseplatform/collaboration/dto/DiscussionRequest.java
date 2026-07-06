package com.yan233.courseplatform.collaboration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DiscussionRequest {
    @NotNull
    private Long courseId;
    private Long groupId;
    private Long parentId;
    @NotNull
    private Long authorId;
    @NotBlank
    @Size(max = 120)
    private String title;
    @NotBlank
    @Size(max = 3000)
    private String content;
    private Integer status = 1;
}

