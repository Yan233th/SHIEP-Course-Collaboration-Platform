package com.yan233.courseplatform.collaboration.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan233.courseplatform.collaboration.client.FileFeignClient;
import com.yan233.courseplatform.collaboration.dto.ShowcaseRequest;
import com.yan233.courseplatform.collaboration.dto.ShowcaseView;
import com.yan233.courseplatform.collaboration.entity.Showcase;
import com.yan233.courseplatform.collaboration.mapper.ShowcaseMapper;
import com.yan233.courseplatform.collaboration.service.CollaborationAccessService;
import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.auth.AccessControl;
import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.dto.FileOwnerRequest;
import com.yan233.courseplatform.common.dto.FileBrief;
import com.yan233.courseplatform.common.dto.FileReferenceReplaceRequest;
import com.yan233.courseplatform.common.dto.FileReferenceRequest;
import com.yan233.courseplatform.common.exception.BusinessException;
import com.yan233.courseplatform.common.web.UserContext;
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
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/showcases")
public class ShowcaseController {
    private static final String OWNER_TYPE = "SHOWCASE";

    private final ShowcaseMapper mapper;
    private final CollaborationAccessService accessService;
    private final FileFeignClient fileFeignClient;

    public ShowcaseController(ShowcaseMapper mapper, CollaborationAccessService accessService, FileFeignClient fileFeignClient) {
        this.mapper = mapper;
        this.accessService = accessService;
        this.fileFeignClient = fileFeignClient;
    }

    @GetMapping
    public Result<List<ShowcaseView>> list(@RequestParam(required = false) Long courseId, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        if (courseId == null) {
            AccessControl.requireRole(current, "ADMIN");
        } else {
            accessService.requireCanViewCourse(courseId, current);
        }
        List<Showcase> showcases = mapper.selectList(new LambdaQueryWrapper<Showcase>()
                .eq(courseId != null, Showcase::getCourseId, courseId)
                .eq(Showcase::getStatus, 1)
                .orderByDesc(Showcase::getCreateTime));
        return Result.ok(toViews(showcases));
    }

    @PostMapping
    @Transactional
    public Result<ShowcaseView> create(@RequestBody @Valid ShowcaseRequest request, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        accessService.requireCanViewCourse(request.getCourseId(), current);
        accessService.requireProjectWritable(request.getGroupId(), current);
        Showcase showcase = new Showcase();
        BeanUtils.copyProperties(request, showcase);
        showcase.setDeleted(0);
        mapper.insert(showcase);
        bindReference(showcase.getFileId(), showcase.getId());
        return Result.ok(toViews(List.of(showcase)).get(0));
    }

    @PutMapping("/{id}")
    @Transactional
    public Result<ShowcaseView> update(@PathVariable Long id,
                                       @RequestBody @Valid ShowcaseRequest request,
                                       HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        Showcase showcase = requireShowcase(id);
        accessService.requireCanViewCourse(request.getCourseId(), current);
        accessService.requireProjectWritable(showcase.getGroupId(), current);
        accessService.requireProjectWritable(request.getGroupId(), current);
        Long oldFileId = showcase.getFileId();
        BeanUtils.copyProperties(request, showcase);
        showcase.setId(id);
        mapper.updateById(showcase);
        replaceReference(oldFileId, showcase.getFileId(), showcase.getId());
        return Result.ok(toViews(List.of(showcase)).get(0));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        Showcase showcase = requireShowcase(id);
        accessService.requireProjectWritable(showcase.getGroupId(), current);
        mapper.deleteById(id);
        releaseOwner(showcase.getId());
        return Result.ok();
    }

    private Showcase requireShowcase(Long id) {
        Showcase showcase = mapper.selectById(id);
        if (showcase == null) {
            throw new BusinessException("成果不存在");
        }
        return showcase;
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

    private List<ShowcaseView> toViews(List<Showcase> showcases) {
        Map<Long, FileBrief> files = filesById(showcases.stream().map(Showcase::getFileId).toList());
        return showcases.stream()
                .map(showcase -> new ShowcaseView(
                        showcase.getId(),
                        showcase.getCourseId(),
                        showcase.getGroupId(),
                        showcase.getFileId(),
                        files.get(showcase.getFileId()),
                        showcase.getTitle(),
                        showcase.getSummary(),
                        showcase.getLinkUrl(),
                        showcase.getStatus(),
                        showcase.getCreateTime(),
                        showcase.getUpdateTime()))
                .toList();
    }

    private Map<Long, FileBrief> filesById(List<Long> fileIds) {
        List<Long> ids = fileIds.stream()
                .flatMap(id -> id == null ? Stream.empty() : Stream.of(id))
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Result<List<FileBrief>> result = fileFeignClient.filesByIds(ids);
            if (result.getCode() == 200 && result.getData() != null) {
                return result.getData().stream().collect(Collectors.toMap(FileBrief::id, Function.identity(), (a, b) -> a));
            }
        } catch (RuntimeException ignored) {
            return Collections.emptyMap();
        }
        return Collections.emptyMap();
    }
}
