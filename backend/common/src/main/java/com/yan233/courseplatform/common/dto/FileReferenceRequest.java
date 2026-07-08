package com.yan233.courseplatform.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FileReferenceRequest {
    @NotNull
    private Long fileId;
    @NotBlank
    private String ownerType;
    @NotNull
    private Long ownerId;
}
