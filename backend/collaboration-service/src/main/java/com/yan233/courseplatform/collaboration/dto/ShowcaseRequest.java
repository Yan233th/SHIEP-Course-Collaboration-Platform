package com.yan233.courseplatform.collaboration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShowcaseRequest {
    @NotNull
    private Long courseId;
    @NotNull
    private Long groupId;
    private Long fileId;
    @NotBlank
    @Size(max = 120)
    private String title;
    @Size(max = 1000)
    private String summary;
    @Size(max = 255)
    private String linkUrl;
    private Integer status = 1;
}

