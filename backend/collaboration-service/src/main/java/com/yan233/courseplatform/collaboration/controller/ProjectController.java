package com.yan233.courseplatform.collaboration.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan233.courseplatform.collaboration.client.UserFeignClient;
import com.yan233.courseplatform.collaboration.dto.JoinGroupRequest;
import com.yan233.courseplatform.collaboration.dto.ProjectMemberDetail;
import com.yan233.courseplatform.collaboration.dto.ProjectGroupRequest;
import com.yan233.courseplatform.collaboration.entity.ProjectGroup;
import com.yan233.courseplatform.collaboration.entity.ProjectMember;
import com.yan233.courseplatform.collaboration.mapper.ProjectMemberMapper;
import com.yan233.courseplatform.collaboration.service.CollaborationAccessService;
import com.yan233.courseplatform.collaboration.service.ProjectGroupService;
import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.auth.AccessControl;
import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.dto.UserBrief;
import com.yan233.courseplatform.common.web.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectGroupService groupService;
    private final ProjectMemberMapper memberMapper;
    private final UserFeignClient userFeignClient;
    private final CollaborationAccessService accessService;

    public ProjectController(ProjectGroupService groupService, ProjectMemberMapper memberMapper, UserFeignClient userFeignClient, CollaborationAccessService accessService) {
        this.groupService = groupService;
        this.memberMapper = memberMapper;
        this.userFeignClient = userFeignClient;
        this.accessService = accessService;
    }

    @GetMapping("/groups")
    public Result<List<ProjectGroup>> groups(@RequestParam Long courseId, HttpServletRequest servletRequest) {
        accessService.requireCanViewCourse(courseId, UserContext.from(servletRequest));
        return Result.ok(groupService.list(new LambdaQueryWrapper<ProjectGroup>()
                .eq(ProjectGroup::getCourseId, courseId)
                .orderByDesc(ProjectGroup::getCreateTime)));
    }

    @PostMapping("/groups")
    public Result<ProjectGroup> create(@RequestBody @Valid ProjectGroupRequest request, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        accessService.requireCanViewCourse(request.getCourseId(), current);
        if (!current.isAdmin()) {
            request.setLeaderId(current.userId());
        }
        return Result.ok(groupService.create(request));
    }

    @PutMapping("/groups/{id}")
    public Result<ProjectGroup> update(@PathVariable Long id, @RequestBody @Valid ProjectGroupRequest request, HttpServletRequest servletRequest) {
        accessService.requireCourseStaff(request.getCourseId(), UserContext.from(servletRequest));
        return Result.ok(groupService.updateGroup(id, request));
    }

    @DeleteMapping("/groups/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest servletRequest) {
        ProjectGroup group = accessService.requireGroup(id);
        accessService.requireCourseStaff(group.getCourseId(), UserContext.from(servletRequest));
        groupService.removeById(id);
        return Result.ok();
    }

    @PostMapping("/groups/{id}/join")
    public Result<ProjectMember> join(@PathVariable Long id, @RequestBody @Valid JoinGroupRequest request, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        ProjectGroup group = accessService.requireGroup(id);
        accessService.requireCanViewCourse(group.getCourseId(), current);
        AccessControl.requireRole(current, "ADMIN", "STUDENT");
        if (!current.isAdmin()) {
            request.setUserId(current.userId());
        }
        return Result.ok(groupService.join(id, request));
    }

    @DeleteMapping("/groups/{id}/members/me")
    public Result<Void> leave(@PathVariable Long id, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        ProjectGroup group = accessService.requireGroup(id);
        accessService.requireCanViewCourse(group.getCourseId(), current);
        AccessControl.requireRole(current, "ADMIN", "STUDENT");
        groupService.leave(id, current.userId());
        return Result.ok();
    }

    @GetMapping("/groups/{id}/members")
    public Result<List<ProjectMember>> members(@PathVariable Long id, HttpServletRequest servletRequest) {
        accessService.requireCanViewGroup(id, UserContext.from(servletRequest));
        return Result.ok(memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getGroupId, id)
                .eq(ProjectMember::getStatus, 1)));
    }

    @GetMapping("/groups/{id}/member-details")
    public Result<List<ProjectMemberDetail>> memberDetails(@PathVariable Long id, HttpServletRequest servletRequest) {
        accessService.requireCanViewGroup(id, UserContext.from(servletRequest));
        List<ProjectMember> members = memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getGroupId, id)
                .eq(ProjectMember::getStatus, 1));
        List<Long> userIds = members.stream().map(ProjectMember::getUserId).toList();
        List<UserBrief> users = userFeignClient.usersByIds(userIds).getData();
        return Result.ok(members.stream()
                .map(member -> new ProjectMemberDetail(
                        member.getId(),
                        member.getGroupId(),
                        member.getUserId(),
                        member.getRoleName(),
                        member.getStatus(),
                        users.stream().filter(user -> user.id().equals(member.getUserId())).findFirst().orElse(null)))
                .toList());
    }
}
