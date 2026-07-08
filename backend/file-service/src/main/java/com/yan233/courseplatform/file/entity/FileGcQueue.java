package com.yan233.courseplatform.file.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_gc_queue")
public class FileGcQueue {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fileId;
    private Long sourceReferenceId;
    private String reason;
    private Integer status;
    private Integer attempts;
    private String lastError;
    private LocalDateTime nextRetryTime;
    private LocalDateTime processedTime;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
