package com.yan233.courseplatform.collaboration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JoinGroupRequest {
    @NotNull
    private Long userId;
    private String roleName = "成员";
}

