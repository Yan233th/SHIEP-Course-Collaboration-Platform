package com.yan233.courseplatform.collaboration.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan233.courseplatform.collaboration.dto.DiscussionRequest;
import com.yan233.courseplatform.collaboration.entity.DiscussionPost;
import com.yan233.courseplatform.collaboration.mapper.DiscussionPostMapper;
import com.yan233.courseplatform.collaboration.service.CollaborationAccessService;
import com.yan233.courseplatform.common.api.Result;
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
@RequestMapping("/discussions")
public class DiscussionController {
    private final DiscussionPostMapper mapper;
    private final CollaborationAccessService accessService;

    public DiscussionController(DiscussionPostMapper mapper, CollaborationAccessService accessService) {
        this.mapper = mapper;
        this.accessService = accessService;
    }

    @GetMapping
    public Result<List<DiscussionPost>> list(@RequestParam Long courseId,
                                             @RequestParam(required = false) Long groupId,
                                             HttpServletRequest servletRequest) {
        accessService.requireCanViewCourse(courseId, UserContext.from(servletRequest));
        return Result.ok(mapper.selectList(new LambdaQueryWrapper<DiscussionPost>()
                .eq(DiscussionPost::getCourseId, courseId)
                .eq(groupId != null, DiscussionPost::getGroupId, groupId)
                .orderByDesc(DiscussionPost::getCreateTime)));
    }

    @PostMapping
    public Result<DiscussionPost> create(@RequestBody @Valid DiscussionRequest request, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        accessService.requireCanViewCourse(request.getCourseId(), current);
        DiscussionPost post = new DiscussionPost();
        BeanUtils.copyProperties(request, post);
        post.setAuthorId(current.userId());
        post.setDeleted(0);
        mapper.insert(post);
        return Result.ok(post);
    }
}
