package com.yan233.courseplatform.course.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String courseCode;
    private String courseName;
    private Long teacherId;
    private String teacherName;
    private String description;
    private BigDecimal credit;
    private Integer hours;
    private Integer maxStudents;
    private Integer currentStudents;
    private Integer status;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

