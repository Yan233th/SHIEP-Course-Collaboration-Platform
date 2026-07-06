package com.yan233.courseplatform.collaboration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yan233.courseplatform.collaboration.dto.JoinGroupRequest;
import com.yan233.courseplatform.collaboration.dto.ProjectGroupRequest;
import com.yan233.courseplatform.collaboration.entity.ProjectGroup;
import com.yan233.courseplatform.collaboration.entity.ProjectMember;

public interface ProjectGroupService extends IService<ProjectGroup> {
    ProjectGroup create(ProjectGroupRequest request);

    ProjectGroup updateGroup(Long id, ProjectGroupRequest request);

    ProjectMember join(Long groupId, JoinGroupRequest request);
}

