package com.yan233.courseplatform.collaboration.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan233.courseplatform.collaboration.dto.ShowcaseRequest;
import com.yan233.courseplatform.collaboration.entity.Showcase;
import com.yan233.courseplatform.collaboration.mapper.ShowcaseMapper;
import com.yan233.courseplatform.collaboration.service.CollaborationAccessService;
import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.auth.AccessControl;
import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.web.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/showcases")
public class ShowcaseController {
    private final ShowcaseMapper mapper;
    private final CollaborationAccessService accessService;

    public ShowcaseController(ShowcaseMapper mapper, CollaborationAccessService accessService) {
        this.mapper = mapper;
        this.accessService = accessService;
    }

    @GetMapping
    public Result<List<Showcase>> list(@RequestParam(required = false) Long courseId, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        if (courseId == null) {
            AccessControl.requireRole(current, "ADMIN");
        } else {
            accessService.requireCanViewCourse(courseId, current);
        }
        return Result.ok(mapper.selectList(new LambdaQueryWrapper<Showcase>()
                .eq(courseId != null, Showcase::getCourseId, courseId)
                .eq(Showcase::getStatus, 1)
                .orderByDesc(Showcase::getCreateTime)));
    }

    @PostMapping
    public Result<Showcase> create(@RequestBody @Valid ShowcaseRequest request, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        accessService.requireCanViewCourse(request.getCourseId(), current);
        accessService.requireProjectWritable(request.getGroupId(), current);
        Showcase showcase = new Showcase();
        BeanUtils.copyProperties(request, showcase);
        showcase.setDeleted(0);
        mapper.insert(showcase);
        return Result.ok(showcase);
    }
}
