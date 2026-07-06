package com.yan233.courseplatform.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CourseMemberRequest {
    @NotNull
    private Long userId;

    @NotNull
    @Pattern(regexp = "TEACHER|TA|STUDENT")
    private String roleCode;

    @Min(0)
    @Max(1)
    private Integer status = 1;
}
