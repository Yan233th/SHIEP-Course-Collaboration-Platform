package com.yan233.courseplatform.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yan233.courseplatform.common.api.PageResult;
import com.yan233.courseplatform.common.auth.AccessControl;
import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.dto.CourseBrief;
import com.yan233.courseplatform.common.exception.BusinessException;
import com.yan233.courseplatform.common.util.PageUtils;
import com.yan233.courseplatform.course.dto.CourseAccess;
import com.yan233.courseplatform.course.dto.CourseQuery;
import com.yan233.courseplatform.course.dto.CourseRequest;
import com.yan233.courseplatform.course.entity.Course;
import com.yan233.courseplatform.course.entity.CourseMember;
import com.yan233.courseplatform.course.mapper.CourseMapper;
import com.yan233.courseplatform.course.mapper.CourseMemberMapper;
import com.yan233.courseplatform.course.service.CourseBizService;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CourseBizServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseBizService {
    private final CourseMemberMapper courseMemberMapper;

    public CourseBizServiceImpl(CourseMemberMapper courseMemberMapper) {
        this.courseMemberMapper = courseMemberMapper;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"course", "coursePage"}, allEntries = true)
    public Course create(CourseRequest request, CurrentUser currentUser) {
        AccessControl.requireRole(currentUser, "ADMIN");
        boolean exists = count(new LambdaQueryWrapper<Course>().eq(Course::getCourseCode, request.getCourseCode())) > 0;
        if (exists) {
            throw new BusinessException("课程编号已存在");
        }
        Course course = new Course();
        BeanUtils.copyProperties(request, course);
        course.setCurrentStudents(0);
        course.setDeleted(0);
        save(course);
        CourseMember teacherMember = new CourseMember();
        teacherMember.setCourseId(course.getId());
        teacherMember.setUserId(course.getTeacherId());
        teacherMember.setRoleCode("TEACHER");
        teacherMember.setStatus(1);
        teacherMember.setDeleted(0);
        courseMemberMapper.insert(teacherMember);
        return course;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"course", "coursePage"}, allEntries = true)
    public Course updateCourse(Long id, CourseRequest request, CurrentUser currentUser) {
        Course course = getById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        boolean canUpdate = currentUser != null
                && (currentUser.isAdmin() || (currentUser.isTeacher() && currentUser.userId().equals(course.getTeacherId())));
        if (!canUpdate) {
            throw new BusinessException(403, "无权限维护该课程");
        }
        boolean duplicate = count(new LambdaQueryWrapper<Course>().eq(Course::getCourseCode, request.getCourseCode()).ne(Course::getId, id)) > 0;
        if (duplicate) {
            throw new BusinessException("课程编号已存在");
        }
        BeanUtils.copyProperties(request, course);
        updateById(course);
        return course;
    }

    @Override
    public PageResult<Course> pageCourses(CourseQuery query, CurrentUser currentUser) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        applyCourseFilters(wrapper, query);
        if (currentUser == null || !currentUser.isAdmin()) {
            Set<Long> courseIds = visibleCourseIds(currentUser);
            boolean teacherOwner = currentUser != null && currentUser.isTeacher();
            if (courseIds.isEmpty() && !teacherOwner) {
                return new PageResult<>(0, query.getPageNum(), query.getPageSize(), List.of());
            }
            wrapper.and(w -> {
                boolean hasIn = !courseIds.isEmpty();
                if (hasIn) {
                    w.in(Course::getId, courseIds);
                }
                if (teacherOwner) {
                    if (hasIn) {
                        w.or();
                    }
                    w.eq(Course::getTeacherId, currentUser.userId());
                }
            });
        }
        wrapper.orderByDesc(Course::getId);
        return PageUtils.of(page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper));
    }

    @Override
    @Cacheable(cacheNames = "course", key = "#id")
    public Course cachedDetail(Long id) {
        return getById(id);
    }

    @Override
    public CourseAccess access(Long courseId, CurrentUser currentUser) {
        Course course = getById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        String courseRole = courseRole(course, currentUser);
        if (!canView(course, currentUser, courseRole)) {
            throw new BusinessException(403, "无权限访问该课程");
        }
        String systemRole = currentUser.roles() == null || currentUser.roles().isEmpty() ? "" : currentUser.roles().get(0);
        return new CourseAccess(courseId, systemRole, courseRole, actionsFor(currentUser, courseRole));
    }

    @Override
    public void requireCanView(Long courseId, CurrentUser currentUser) {
        access(courseId, currentUser);
    }

    @Override
    public void requireCourseStaff(Long courseId, CurrentUser currentUser) {
        CourseAccess access = access(courseId, currentUser);
        if (!access.actions().contains("MANAGE_CONTENT")) {
            throw new BusinessException(403, "无权限维护该课程内容");
        }
    }

    @Override
    public void requireCanManageMembers(Long courseId, CurrentUser currentUser) {
        CourseAccess access = access(courseId, currentUser);
        if (!access.actions().contains("MANAGE_MEMBERS")) {
            throw new BusinessException(403, "无权限维护课程成员");
        }
    }

    @Override
    public List<CourseBrief> briefByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return listByIds(ids).stream()
                .map(c -> new CourseBrief(c.getId(), c.getCourseCode(), c.getCourseName(), c.getTeacherId(), c.getTeacherName()))
                .toList();
    }

    @Override
    public List<Map<String, Object>> overview() {
        return baseMapper.overview();
    }

    @Override
    public List<Map<String, Object>> activityStats(Long courseId) {
        return baseMapper.activityStats(courseId);
    }

    private void applyCourseFilters(LambdaQueryWrapper<Course> wrapper, CourseQuery query) {
        wrapper.and(query.getKeyword() != null && !query.getKeyword().isBlank(), w -> w
                .like(Course::getCourseName, query.getKeyword())
                .or()
                .like(Course::getCourseCode, query.getKeyword())
                .or()
                .like(Course::getTeacherName, query.getKeyword()));
        wrapper.eq(query.getTeacherId() != null, Course::getTeacherId, query.getTeacherId());
        wrapper.eq(query.getStatus() != null, Course::getStatus, query.getStatus());
    }

    private Set<Long> visibleCourseIds(CurrentUser currentUser) {
        if (currentUser == null || currentUser.userId() == null || currentUser.userId() <= 0) {
            return Set.of();
        }
        List<CourseMember> members = courseMemberMapper.selectList(new LambdaQueryWrapper<CourseMember>()
                .eq(CourseMember::getUserId, currentUser.userId())
                .eq(CourseMember::getStatus, 1));
        Set<Long> ids = new LinkedHashSet<>();
        members.forEach(member -> ids.add(member.getCourseId()));
        return ids;
    }

    private String courseRole(Course course, CurrentUser currentUser) {
        if (currentUser == null || currentUser.userId() == null) {
            return null;
        }
        if (currentUser.isTeacher() && currentUser.userId().equals(course.getTeacherId())) {
            return "TEACHER";
        }
        CourseMember member = courseMemberMapper.selectOne(new LambdaQueryWrapper<CourseMember>()
                .eq(CourseMember::getCourseId, course.getId())
                .eq(CourseMember::getUserId, currentUser.userId())
                .eq(CourseMember::getStatus, 1)
                .last("LIMIT 1"));
        return member == null ? null : member.getRoleCode();
    }

    private boolean canView(Course course, CurrentUser currentUser, String courseRole) {
        return currentUser != null
                && (currentUser.isAdmin()
                || courseRole != null
                || (currentUser.isTeacher() && currentUser.userId().equals(course.getTeacherId())));
    }

    private List<String> actionsFor(CurrentUser currentUser, String courseRole) {
        List<String> actions = new ArrayList<>();
        actions.add("VIEW_COURSE");
        if (currentUser.isAdmin()) {
            actions.addAll(List.of(
                    "MANAGE_USERS",
                    "CREATE_COURSE",
                    "UPDATE_COURSE",
                    "DELETE_COURSE",
                    "MANAGE_MEMBERS",
                    "MANAGE_CONTENT",
                    "CREATE_NOTICE",
                    "UPLOAD_RESOURCE",
                    "CREATE_ASSIGNMENT",
                    "GRADE_SUBMISSION",
                    "MANAGE_PROJECT",
                    "CREATE_DISCUSSION",
                    "CREATE_GROUP",
                    "JOIN_GROUP",
                    "SUBMIT_ASSIGNMENT",
                    "PUBLISH_SHOWCASE"
            ));
            return actions;
        }
        if ("TEACHER".equals(courseRole)) {
            actions.add("UPDATE_COURSE");
            actions.add("MANAGE_MEMBERS");
        }
        if ("TEACHER".equals(courseRole) || "TA".equals(courseRole)) {
            actions.addAll(List.of(
                    "MANAGE_CONTENT",
                    "CREATE_NOTICE",
                    "UPLOAD_RESOURCE",
                    "CREATE_ASSIGNMENT",
                    "GRADE_SUBMISSION",
                    "MANAGE_PROJECT",
                    "CREATE_DISCUSSION",
                    "PUBLISH_SHOWCASE"
            ));
        }
        if ("STUDENT".equals(courseRole)) {
            actions.addAll(List.of(
                    "CREATE_DISCUSSION",
                    "CREATE_GROUP",
                    "JOIN_GROUP",
                    "SUBMIT_ASSIGNMENT",
                    "PUBLISH_SHOWCASE"
            ));
        }
        return actions;
    }
}
