package com.yan233.courseplatform.file.controller;

import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.dto.FileBrief;
import com.yan233.courseplatform.common.dto.FileOwnerRequest;
import com.yan233.courseplatform.common.dto.FileReferenceReplaceRequest;
import com.yan233.courseplatform.common.dto.FileReferenceRequest;
import com.yan233.courseplatform.common.web.UserContext;
import com.yan233.courseplatform.file.entity.FileMetadata;
import com.yan233.courseplatform.file.service.FileLifecycleService;
import com.yan233.courseplatform.file.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileController {
    private final FileStorageService fileService;
    private final FileLifecycleService lifecycleService;

    public FileController(FileStorageService fileService, FileLifecycleService lifecycleService) {
        this.fileService = fileService;
        this.lifecycleService = lifecycleService;
    }

    @PostMapping("/upload")
    public Result<FileMetadata> upload(@RequestParam MultipartFile file,
                                       @RequestParam Long uploaderId,
                                       @RequestParam(defaultValue = "course") String bizType,
                                       HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        Long effectiveUploaderId = current.isAdmin() ? uploaderId : current.userId();
        return Result.ok(fileService.store(file, effectiveUploaderId, bizType));
    }

    @GetMapping("/{id}")
    public Result<FileMetadata> metadata(@PathVariable Long id) {
        return Result.ok(fileService.getById(id));
    }

    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> preview(@PathVariable Long id) {
        FileMetadata metadata = fileService.getById(id);
        Resource resource = fileService.loadAsResource(id);
        MediaType type = metadata.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(metadata.getContentType());
        if ("text".equalsIgnoreCase(type.getType()) && type.getCharset() == null) {
            type = new MediaType(type, StandardCharsets.UTF_8);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(metadata.getOriginalName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(type)
                .body(resource);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        FileMetadata metadata = fileService.getById(id);
        Resource resource = fileService.loadAsResource(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(metadata.getOriginalName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/internal/batch")
    public Result<List<FileBrief>> internalFiles(@RequestParam List<Long> ids) {
        return Result.ok(fileService.briefByIds(ids));
    }

    @PostMapping("/internal/references/bind")
    public Result<Void> bindReference(@RequestBody @Valid FileReferenceRequest request) {
        lifecycleService.bind(request);
        return Result.ok();
    }

    @PostMapping("/internal/references/replace")
    public Result<Void> replaceReference(@RequestBody @Valid FileReferenceReplaceRequest request) {
        lifecycleService.replace(request);
        return Result.ok();
    }

    @PostMapping("/internal/references/release")
    public Result<Void> releaseReference(@RequestBody @Valid FileReferenceRequest request) {
        lifecycleService.release(request);
        return Result.ok();
    }

    @PostMapping("/internal/references/release-owner")
    public Result<Void> releaseOwner(@RequestBody @Valid FileOwnerRequest request) {
        lifecycleService.releaseOwner(request);
        return Result.ok();
    }

    @PostMapping("/internal/gc/run")
    public Result<Integer> runGc() {
        return Result.ok(lifecycleService.processPendingGcNow());
    }

    @GetMapping("/internal/gc-stats")
    public Result<List<Map<String, Object>>> gcStats() {
        return Result.ok(lifecycleService.gcStats());
    }

    @GetMapping("/internal/status")
    public Result<List<Map<String, Object>>> fileStatuses() {
        return Result.ok(lifecycleService.fileStatuses());
    }
}
