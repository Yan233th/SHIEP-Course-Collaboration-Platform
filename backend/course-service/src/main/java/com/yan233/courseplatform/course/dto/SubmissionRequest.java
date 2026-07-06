package com.yan233.courseplatform.course.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubmissionRequest {
    @NotNull
    private Long assignmentId;
    @NotNull
    private Long studentId;
    private Long fileId;
    @Size(max = 2000)
    private String content;
    private BigDecimal score;
    @Size(max = 1000)
    private String feedback;
    private Integer status = 0;
}

