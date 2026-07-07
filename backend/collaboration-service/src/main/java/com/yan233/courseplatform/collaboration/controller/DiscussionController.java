package com.yan233.courseplatform.collaboration.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan233.courseplatform.collaboration.dto.DiscussionRequest;
import com.yan233.courseplatform.collaboration.entity.DiscussionPost;
import com.yan233.courseplatform.collaboration.entity.ProjectGroup;
import com.yan233.courseplatform.collaboration.mapper.DiscussionPostMapper;
import com.yan233.courseplatform.collaboration.service.CollaborationAccessService;
import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.exception.BusinessException;
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
    public Result<List<DiscussionPost>> list(@RequestParam Long groupId, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        accessService.requireCanAccessGroup(groupId, current);
        return Result.ok(mapper.selectList(new LambdaQueryWrapper<DiscussionPost>()
                .eq(DiscussionPost::getGroupId, groupId)
                .orderByDesc(DiscussionPost::getCreateTime)));
    }

    @PostMapping
    public Result<DiscussionPost> create(@RequestBody @Valid DiscussionRequest request, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        // 组内讨论：必须是该组成员、课程教师/助教或管理员
        ProjectGroup group = accessService.requireCanAccessGroup(request.getGroupId(), current);
        if (group.getStatus() == 0) {
            throw new BusinessException(400, "该项目组不可用");
        }
        if (!group.getCourseId().equals(request.getCourseId())) {
            throw new BusinessException(400, "项目组不属于该课程");
        }
        // 回复的主题必须属于同一项目组
        if (request.getParentId() != null) {
            DiscussionPost parent = mapper.selectById(request.getParentId());
            if (parent == null || !request.getGroupId().equals(parent.getGroupId())) {
                throw new BusinessException(400, "回复的主题不存在或不属于该项目组");
            }
            // 两级结构：只能回复话题，不能回复回复
            if (parent.getParentId() != null) {
                throw new BusinessException(400, "请直接在话题下回复，不支持回复回复");
            }
        }
        DiscussionPost post = new DiscussionPost();
        BeanUtils.copyProperties(request, post);
        post.setCourseId(group.getCourseId());
        post.setGroupId(request.getGroupId());
        post.setAuthorId(current.userId());
        post.setDeleted(0);
        mapper.insert(post);
        return Result.ok(post);
    }
}
