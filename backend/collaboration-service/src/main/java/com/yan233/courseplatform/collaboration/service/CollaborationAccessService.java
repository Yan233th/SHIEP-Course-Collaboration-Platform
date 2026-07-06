package com.yan233.courseplatform.collaboration.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan233.courseplatform.collaboration.entity.CourseMember;
import com.yan233.courseplatform.collaboration.entity.ProjectGroup;
import com.yan233.courseplatform.collaboration.entity.ProjectMember;
import com.yan233.courseplatform.collaboration.mapper.CourseMemberMapper;
import com.yan233.courseplatform.collaboration.mapper.ProjectGroupMapper;
import com.yan233.courseplatform.collaboration.mapper.ProjectMemberMapper;
import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class CollaborationAccessService {
    private final CourseMemberMapper courseMemberMapper;
    private final ProjectGroupMapper groupMapper;
    private final ProjectMemberMapper memberMapper;

    public CollaborationAccessService(CourseMemberMapper courseMemberMapper, ProjectGroupMapper groupMapper, ProjectMemberMapper memberMapper) {
        this.courseMemberMapper = courseMemberMapper;
        this.groupMapper = groupMapper;
        this.memberMapper = memberMapper;
    }

    public void requireCanViewCourse(Long courseId, CurrentUser currentUser) {
        if (!canViewCourse(courseId, currentUser)) {
            throw new BusinessException(403, "无权限访问该课程");
        }
    }

    public void requireCourseStaff(Long courseId, CurrentUser currentUser) {
        String role = courseRole(courseId, currentUser);
        if (currentUser == null || !(currentUser.isAdmin() || "TEACHER".equals(role) || "TA".equals(role))) {
            throw new BusinessException(403, "无权限维护该课程协作内容");
        }
    }

    public void requireCanViewGroup(Long groupId, CurrentUser currentUser) {
        ProjectGroup group = requireGroup(groupId);
        requireCanViewCourse(group.getCourseId(), currentUser);
    }

    public void requireProjectWritable(Long groupId, CurrentUser currentUser) {
        ProjectGroup group = requireGroup(groupId);
        if (currentUser != null && currentUser.isAdmin()) {
            return;
        }
        String role = courseRole(group.getCourseId(), currentUser);
        if ("TEACHER".equals(role) || "TA".equals(role) || isProjectMember(groupId, currentUser)) {
            return;
        }
        throw new BusinessException(403, "无权限维护该项目组内容");
    }

    public ProjectGroup requireGroup(Long groupId) {
        ProjectGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("项目组不存在");
        }
        return group;
    }

    public String courseRole(Long courseId, CurrentUser currentUser) {
        if (currentUser == null || currentUser.userId() == null) {
            return null;
        }
        CourseMember member = courseMemberMapper.selectOne(new LambdaQueryWrapper<CourseMember>()
                .eq(CourseMember::getCourseId, courseId)
                .eq(CourseMember::getUserId, currentUser.userId())
                .eq(CourseMember::getStatus, 1)
                .last("LIMIT 1"));
        return member == null ? null : member.getRoleCode();
    }

    public boolean canViewCourse(Long courseId, CurrentUser currentUser) {
        return currentUser != null && (currentUser.isAdmin() || courseRole(courseId, currentUser) != null);
    }

    public boolean isProjectMember(Long groupId, CurrentUser currentUser) {
        if (currentUser == null || currentUser.userId() == null) {
            return false;
        }
        return memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getGroupId, groupId)
                .eq(ProjectMember::getUserId, currentUser.userId())
                .eq(ProjectMember::getStatus, 1)) > 0;
    }
}
