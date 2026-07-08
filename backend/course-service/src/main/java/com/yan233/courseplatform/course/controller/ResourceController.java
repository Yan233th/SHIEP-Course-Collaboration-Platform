package com.yan233.courseplatform.course.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.dto.FileOwnerRequest;
import com.yan233.courseplatform.common.dto.FileReferenceReplaceRequest;
import com.yan233.courseplatform.common.dto.FileReferenceRequest;
import com.yan233.courseplatform.common.exception.BusinessException;
import com.yan233.courseplatform.common.web.UserContext;
import com.yan233.courseplatform.course.client.FileFeignClient;
import com.yan233.courseplatform.course.dto.ResourceRequest;
import com.yan233.courseplatform.course.entity.CourseResource;
import com.yan233.courseplatform.course.mapper.CourseResourceMapper;
import com.yan233.courseplatform.course.service.CourseBizService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
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
    private static final String OWNER_TYPE = "COURSE_RESOURCE";

    private final CourseResourceMapper resourceMapper;
    private final CourseBizService courseService;
    private final FileFeignClient fileFeignClient;

    public ResourceController(CourseResourceMapper resourceMapper, CourseBizService courseService, FileFeignClient fileFeignClient) {
        this.resourceMapper = resourceMapper;
        this.courseService = courseService;
        this.fileFeignClient = fileFeignClient;
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
    @Transactional
    public Result<CourseResource> create(@RequestBody @Valid ResourceRequest request, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        courseService.requireCourseStaff(request.getCourseId(), current);
        CourseResource resource = new CourseResource();
        BeanUtils.copyProperties(request, resource);
        resource.setUploaderId(current.userId());
        resource.setDeleted(0);
        resourceMapper.insert(resource);
        bindReference(resource.getFileId(), resource.getId());
        return Result.ok(resource);
    }

    @PutMapping("/{id}")
    @Transactional
    public Result<CourseResource> update(@PathVariable Long id, @RequestBody @Valid ResourceRequest request, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        courseService.requireCourseStaff(request.getCourseId(), current);
        CourseResource resource = resourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException("资源不存在");
        }
        Long oldFileId = resource.getFileId();
        BeanUtils.copyProperties(request, resource);
        resource.setId(id);
        resource.setUploaderId(current.userId());
        resourceMapper.updateById(resource);
        replaceReference(oldFileId, resource.getFileId(), resource.getId());
        return Result.ok(resource);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest servletRequest) {
        CourseResource resource = resourceMapper.selectById(id);
        if (resource != null) {
            courseService.requireCourseStaff(resource.getCourseId(), UserContext.from(servletRequest));
        }
        resourceMapper.deleteById(id);
        if (resource != null) {
            releaseOwner(resource.getId());
        }
        return Result.ok();
    }

    private void bindReference(Long fileId, Long ownerId) {
        if (fileId == null) {
            return;
        }
        FileReferenceRequest request = new FileReferenceRequest();
        request.setFileId(fileId);
        request.setOwnerType(OWNER_TYPE);
        request.setOwnerId(ownerId);
        fileFeignClient.bindReference(request);
    }

    private void replaceReference(Long oldFileId, Long newFileId, Long ownerId) {
        FileReferenceReplaceRequest request = new FileReferenceReplaceRequest();
        request.setOldFileId(oldFileId);
        request.setNewFileId(newFileId);
        request.setOwnerType(OWNER_TYPE);
        request.setOwnerId(ownerId);
        fileFeignClient.replaceReference(request);
    }

    private void releaseOwner(Long ownerId) {
        FileOwnerRequest request = new FileOwnerRequest();
        request.setOwnerType(OWNER_TYPE);
        request.setOwnerId(ownerId);
        fileFeignClient.releaseOwner(request);
    }
}
