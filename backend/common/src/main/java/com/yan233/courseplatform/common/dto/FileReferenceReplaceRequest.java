package com.yan233.courseplatform.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FileReferenceReplaceRequest {
    private Long oldFileId;
    private Long newFileId;
    @NotBlank
    private String ownerType;
    @NotNull
    private Long ownerId;
}
