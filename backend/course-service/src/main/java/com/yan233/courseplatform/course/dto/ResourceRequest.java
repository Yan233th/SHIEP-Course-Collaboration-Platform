package com.yan233.courseplatform.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResourceRequest {
    @NotNull
    private Long courseId;
    private Long fileId;
    @NotBlank
    @Size(max = 120)
    private String title;
    @NotBlank
    @Size(max = 50)
    private String category;
    @Size(max = 200)
    private String tags;
    @Size(max = 500)
    private String description;
    private Long uploaderId;
    private Integer status = 1;
}

