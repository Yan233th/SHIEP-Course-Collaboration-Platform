package com.yan233.courseplatform.course.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.web.UserContext;
import com.yan233.courseplatform.course.dto.NoticeRequest;
import com.yan233.courseplatform.course.entity.CourseNotice;
import com.yan233.courseplatform.course.mapper.CourseNoticeMapper;
import com.yan233.courseplatform.course.service.CourseBizService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
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
@RequestMapping("/notices")
public class NoticeController {
    private final CourseNoticeMapper noticeMapper;
    private final CourseBizService courseService;

    public NoticeController(CourseNoticeMapper noticeMapper, CourseBizService courseService) {
        this.noticeMapper = noticeMapper;
        this.courseService = courseService;
    }

    @GetMapping
    public Result<List<CourseNotice>> list(@RequestParam Long courseId, HttpServletRequest servletRequest) {
        courseService.requireCanView(courseId, UserContext.from(servletRequest));
        LambdaQueryWrapper<CourseNotice> wrapper = new LambdaQueryWrapper<CourseNotice>()
                .eq(CourseNotice::getCourseId, courseId)
                .eq(CourseNotice::getStatus, 1)
                .orderByDesc(CourseNotice::getPinned)
                .orderByDesc(CourseNotice::getCreateTime);
        return Result.ok(noticeMapper.selectList(wrapper));
    }

    @PostMapping
    @CacheEvict(cacheNames = "noticeList", allEntries = true)
    public Result<CourseNotice> create(@RequestBody @Valid NoticeRequest request, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        courseService.requireCourseStaff(request.getCourseId(), current);
        CourseNotice notice = new CourseNotice();
        BeanUtils.copyProperties(request, notice);
        notice.setPublisherId(current.userId());
        notice.setDeleted(0);
        noticeMapper.insert(notice);
        return Result.ok(notice);
    }

    @PutMapping("/{id}")
    @CacheEvict(cacheNames = "noticeList", allEntries = true)
    public Result<CourseNotice> update(@PathVariable Long id, @RequestBody @Valid NoticeRequest request, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        courseService.requireCourseStaff(request.getCourseId(), current);
        CourseNotice notice = noticeMapper.selectById(id);
        BeanUtils.copyProperties(request, notice);
        notice.setId(id);
        notice.setPublisherId(current.userId());
        noticeMapper.updateById(notice);
        return Result.ok(notice);
    }

    @DeleteMapping("/{id}")
    @CacheEvict(cacheNames = "noticeList", allEntries = true)
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest servletRequest) {
        CourseNotice notice = noticeMapper.selectById(id);
        if (notice != null) {
            courseService.requireCourseStaff(notice.getCourseId(), UserContext.from(servletRequest));
        }
        noticeMapper.deleteById(id);
        return Result.ok();
    }
}
