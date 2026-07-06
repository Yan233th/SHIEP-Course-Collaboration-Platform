package com.yan233.courseplatform.course.controller;

import com.yan233.courseplatform.common.api.PageResult;
import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.auth.AccessControl;
import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.dto.CourseBrief;
import com.yan233.courseplatform.common.web.UserContext;
import com.yan233.courseplatform.course.dto.CourseAccess;
import com.yan233.courseplatform.course.dto.CourseQuery;
import com.yan233.courseplatform.course.dto.CourseRequest;
import com.yan233.courseplatform.course.entity.Course;
import com.yan233.courseplatform.course.service.CourseBizService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.CacheEvict;
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
@RequestMapping("/courses")
public class CourseController {
    private final CourseBizService courseService;

    public CourseController(CourseBizService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public Result<PageResult<Course>> page(CourseQuery query, HttpServletRequest servletRequest) {
        return Result.ok(courseService.pageCourses(query, UserContext.from(servletRequest)));
    }

    @GetMapping("/{id}")
    public Result<Course> detail(@PathVariable Long id, HttpServletRequest servletRequest) {
        courseService.requireCanView(id, UserContext.from(servletRequest));
        return Result.ok(courseService.cachedDetail(id));
    }

    @GetMapping("/{id}/access")
    public Result<CourseAccess> access(@PathVariable Long id, HttpServletRequest servletRequest) {
        return Result.ok(courseService.access(id, UserContext.from(servletRequest)));
    }

    @PostMapping
    public Result<Course> create(@RequestBody @Valid CourseRequest request, HttpServletRequest servletRequest) {
        return Result.ok(courseService.create(request, UserContext.from(servletRequest)));
    }

    @PutMapping("/{id}")
    public Result<Course> update(@PathVariable Long id, @RequestBody @Valid CourseRequest request, HttpServletRequest servletRequest) {
        return Result.ok(courseService.updateCourse(id, request, UserContext.from(servletRequest)));
    }

    @DeleteMapping("/{id}")
    @CacheEvict(cacheNames = {"course", "coursePage"}, allEntries = true)
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest servletRequest) {
        AccessControl.requireRole(UserContext.from(servletRequest), "ADMIN");
        courseService.removeById(id);
        return Result.ok();
    }

    @DeleteMapping("/batch")
    @CacheEvict(cacheNames = {"course", "coursePage"}, allEntries = true)
    public Result<Void> batchDelete(@RequestBody List<Long> ids, HttpServletRequest servletRequest) {
        AccessControl.requireRole(UserContext.from(servletRequest), "ADMIN");
        courseService.removeBatchByIds(ids);
        return Result.ok();
    }

    @GetMapping("/internal/batch")
    public Result<List<CourseBrief>> internalCourses(@RequestParam List<Long> ids) {
        return Result.ok(courseService.briefByIds(ids));
    }
}
