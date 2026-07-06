package com.yan233.courseplatform.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AssignmentRequest {
    @NotNull
    private Long courseId;
    @NotBlank
    @Size(max = 120)
    private String title;
    @Size(max = 2000)
    private String description;
    private LocalDateTime dueTime;
    private BigDecimal totalScore = BigDecimal.valueOf(100);
    private Integer status = 1;
}

