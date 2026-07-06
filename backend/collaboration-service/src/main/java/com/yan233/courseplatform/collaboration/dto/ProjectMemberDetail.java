package com.yan233.courseplatform.collaboration.dto;

import com.yan233.courseplatform.common.dto.UserBrief;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectMemberDetail {
    private Long id;
    private Long groupId;
    private Long userId;
    private String roleName;
    private Integer status;
    private UserBrief user;
}

