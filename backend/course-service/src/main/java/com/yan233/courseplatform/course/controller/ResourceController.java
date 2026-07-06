package com.yan233.courseplatform.course.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.web.UserContext;
import com.yan233.courseplatform.course.dto.ResourceRequest;
import com.yan233.courseplatform.course.entity.CourseResource;
import com.yan233.courseplatform.course.mapper.CourseResourceMapper;
import com.yan233.courseplatform.course.service.CourseBizService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
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
@RequestMapping("/resources")
public class ResourceController {
    private final CourseResourceMapper resourceMapper;
    private final CourseBizService courseService;

    public ResourceController(CourseResourceMapper resourceMapper, CourseBizService courseService) {
        this.resourceMapper = resourceMapper;
        this.courseService = courseService;
    }

    @GetMapping
    public Result<List<CourseResource>> list(@RequestParam Long courseId,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(required = false) String tag,
                                             HttpServletRequest servletRequest) {
        courseService.requireCanView(courseId, UserContext.from(servletRequest));
        LambdaQueryWrapper<CourseResource> wrapper = new LambdaQueryWrapper<CourseResource>()
                .eq(CourseResource::getCourseId, courseId)
                .eq(CourseResource::getStatus, 1)
                .eq(category != null && !category.isBlank(), CourseResource::getCategory, category)
                .like(tag != null && !tag.isBlank(), CourseResource::getTags, tag)
                .orderByDesc(CourseResource::getCreateTime);
        return Result.ok(resourceMapper.selectList(wrapper));
    }

    @PostMapping
    public Result<CourseResource> create(@RequestBody @Valid ResourceRequest request, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        courseService.requireCourseStaff(request.getCourseId(), current);
        CourseResource resource = new CourseResource();
        BeanUtils.copyProperties(request, resource);
        resource.setUploaderId(current.userId());
        resource.setDeleted(0);
        resourceMapper.insert(resource);
        return Result.ok(resource);
    }

    @PutMapping("/{id}")
    public Result<CourseResource> update(@PathVariable Long id, @RequestBody @Valid ResourceRequest request, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        courseService.requireCourseStaff(request.getCourseId(), current);
        CourseResource resource = resourceMapper.selectById(id);
        BeanUtils.copyProperties(request, resource);
        resource.setId(id);
        resource.setUploaderId(current.userId());
        resourceMapper.updateById(resource);
        return Result.ok(resource);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest servletRequest) {
        CourseResource resource = resourceMapper.selectById(id);
        if (resource != null) {
            courseService.requireCourseStaff(resource.getCourseId(), UserContext.from(servletRequest));
        }
        resourceMapper.deleteById(id);
        return Result.ok();
    }
}
