package com.yan233.courseplatform.collaboration.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectGroupRequest {
    @NotNull
    private Long courseId;
    @NotBlank
    @Size(max = 100)
    private String name;
    @NotBlank
    @Size(max = 200)
    private String topic;
    @NotNull
    private Long leaderId;
    @Min(1)
    @Max(20)
    private Integer maxMembers = 5;
    private Integer status = 1;
}

