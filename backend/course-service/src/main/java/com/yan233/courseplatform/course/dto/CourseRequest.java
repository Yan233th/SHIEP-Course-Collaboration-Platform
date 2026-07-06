package com.yan233.courseplatform.course.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseRequest {
    @NotBlank
    @Size(max = 50)
    private String courseCode;
    @NotBlank
    @Size(max = 100)
    private String courseName;
    @NotNull
    private Long teacherId;
    @Size(max = 50)
    private String teacherName;
    @Size(max = 500)
    private String description;
    @NotNull
    @DecimalMin("0.5")
    @DecimalMax("10.0")
    private BigDecimal credit;
    @NotNull
    @Min(1)
    @Max(200)
    private Integer hours;
    @Min(1)
    @Max(500)
    private Integer maxStudents = 60;
    private Integer status = 1;
}

