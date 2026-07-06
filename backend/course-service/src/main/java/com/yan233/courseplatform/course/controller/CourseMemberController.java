package com.yan233.courseplatform.course.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.dto.UserBrief;
import com.yan233.courseplatform.common.exception.BusinessException;
import com.yan233.courseplatform.common.web.UserContext;
import com.yan233.courseplatform.course.client.UserFeignClient;
import com.yan233.courseplatform.course.dto.CourseMemberDetail;
import com.yan233.courseplatform.course.dto.CourseMemberRequest;
import com.yan233.courseplatform.course.entity.Course;
import com.yan233.courseplatform.course.entity.CourseMember;
import com.yan233.courseplatform.course.mapper.CourseMemberMapper;
import com.yan233.courseplatform.course.service.CourseBizService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/courses/{courseId}/members")
public class CourseMemberController {
    private final CourseBizService courseService;
    private final CourseMemberMapper memberMapper;
    private final UserFeignClient userFeignClient;

    public CourseMemberController(CourseBizService courseService, CourseMemberMapper memberMapper, UserFeignClient userFeignClient) {
        this.courseService = courseService;
        this.memberMapper = memberMapper;
        this.userFeignClient = userFeignClient;
    }

    @GetMapping
    public Result<List<CourseMemberDetail>> list(@PathVariable Long courseId, HttpServletRequest servletRequest) {
        courseService.requireCanView(courseId, UserContext.from(servletRequest));
        List<CourseMember> members = memberMapper.selectList(new LambdaQueryWrapper<CourseMember>()
                .eq(CourseMember::getCourseId, courseId)
                .orderByAsc(CourseMember::getRoleCode)
                .orderByAsc(CourseMember::getId));
        return Result.ok(toDetails(members));
    }

    @PostMapping
    @CacheEvict(cacheNames = {"course", "coursePage"}, allEntries = true)
    public Result<CourseMemberDetail> save(@PathVariable Long courseId, @RequestBody @Valid CourseMemberRequest request,
                                           HttpServletRequest servletRequest) {
        courseService.requireCanManageMembers(courseId, UserContext.from(servletRequest));
        CourseMember member = upsertMember(courseId, request);
        syncStudentCount(courseId);
        return Result.ok(toDetails(List.of(member)).get(0));
    }

    @PutMapping("/{memberId}")
    @CacheEvict(cacheNames = {"course", "coursePage"}, allEntries = true)
    public Result<CourseMemberDetail> update(@PathVariable Long courseId, @PathVariable Long memberId,
                                             @RequestBody @Valid CourseMemberRequest request,
                                             HttpServletRequest servletRequest) {
        courseService.requireCanManageMembers(courseId, UserContext.from(servletRequest));
        CourseMember member = requiredMember(courseId, memberId);
        ensureMemberUpdateAllowed(courseId, member, request);
        member.setUserId(request.getUserId());
        member.setRoleCode(request.getRoleCode());
        member.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        try {
            memberMapper.updateById(member);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("课程成员用户不存在或数据不合法");
        }
        syncStudentCount(courseId);
        return Result.ok(toDetails(List.of(member)).get(0));
    }

    @DeleteMapping("/{memberId}")
    @CacheEvict(cacheNames = {"course", "coursePage"}, allEntries = true)
    public Result<Void> delete(@PathVariable Long courseId, @PathVariable Long memberId, HttpServletRequest servletRequest) {
        courseService.requireCanManageMembers(courseId, UserContext.from(servletRequest));
        CourseMember member = requiredMember(courseId, memberId);
        Course course = requiredCourse(courseId);
        if (course.getTeacherId().equals(member.getUserId())) {
            throw new BusinessException("不能移除课程负责人");
        }
        memberMapper.deleteById(memberId);
        syncStudentCount(courseId);
        return Result.ok();
    }

    private CourseMember upsertMember(Long courseId, CourseMemberRequest request) {
        requiredCourse(courseId);
        ensurePrimaryTeacherKeepsRole(courseId, request.getUserId(), request.getRoleCode());
        CourseMember member = memberMapper.selectOne(new LambdaQueryWrapper<CourseMember>()
                .eq(CourseMember::getCourseId, courseId)
                .eq(CourseMember::getUserId, request.getUserId())
                .last("LIMIT 1"));
        boolean creating = member == null;
        if (creating) {
            member = new CourseMember();
            member.setCourseId(courseId);
            member.setUserId(request.getUserId());
            member.setDeleted(0);
        }
        member.setRoleCode(request.getRoleCode());
        member.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        try {
            if (creating) {
                memberMapper.insert(member);
            } else {
                memberMapper.updateById(member);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("课程成员用户不存在或数据不合法");
        }
        return member;
    }

    private CourseMember requiredMember(Long courseId, Long memberId) {
        CourseMember member = memberMapper.selectById(memberId);
        if (member == null || !courseId.equals(member.getCourseId())) {
            throw new BusinessException("课程成员不存在");
        }
        return member;
    }

    private Course requiredCourse(Long courseId) {
        Course course = courseService.getById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        return course;
    }

    private void ensurePrimaryTeacherKeepsRole(Long courseId, Long userId, String roleCode) {
        Course course = requiredCourse(courseId);
        if (course.getTeacherId().equals(userId) && !"TEACHER".equals(roleCode)) {
            throw new BusinessException("课程负责人必须保留教师身份");
        }
    }

    private void ensureMemberUpdateAllowed(Long courseId, CourseMember member, CourseMemberRequest request) {
        Course course = requiredCourse(courseId);
        if (course.getTeacherId().equals(member.getUserId())
                && (!course.getTeacherId().equals(request.getUserId()) || !"TEACHER".equals(request.getRoleCode()))) {
            throw new BusinessException("课程负责人必须保留教师身份");
        }
        ensurePrimaryTeacherKeepsRole(courseId, request.getUserId(), request.getRoleCode());
    }

    private void syncStudentCount(Long courseId) {
        Long count = memberMapper.selectCount(new LambdaQueryWrapper<CourseMember>()
                .eq(CourseMember::getCourseId, courseId)
                .eq(CourseMember::getRoleCode, "STUDENT")
                .eq(CourseMember::getStatus, 1));
        Course course = new Course();
        course.setId(courseId);
        course.setCurrentStudents(count.intValue());
        courseService.updateById(course);
    }

    private List<CourseMemberDetail> toDetails(List<CourseMember> members) {
        Map<Long, UserBrief> users = usersById(members.stream().map(CourseMember::getUserId).distinct().toList());
        return members.stream()
                .map(member -> {
                    UserBrief user = users.get(member.getUserId());
                    return new CourseMemberDetail(
                            member.getId(),
                            member.getCourseId(),
                            member.getUserId(),
                            user == null ? "user-" + member.getUserId() : user.username(),
                            user == null ? "用户#" + member.getUserId() : user.realName(),
                            user == null ? "" : user.roleCode(),
                            member.getRoleCode(),
                            member.getStatus()
                    );
                })
                .toList();
    }

    private Map<Long, UserBrief> usersById(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Result<List<UserBrief>> result = userFeignClient.usersByIds(userIds);
            if (result.getCode() == 200 && result.getData() != null) {
                return result.getData().stream().collect(Collectors.toMap(UserBrief::id, Function.identity(), (a, b) -> a));
            }
        } catch (RuntimeException ignored) {
            return Collections.emptyMap();
        }
        return Collections.emptyMap();
    }
}
