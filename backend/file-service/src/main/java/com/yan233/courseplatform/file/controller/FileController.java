package com.yan233.courseplatform.file.controller;

import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.dto.FileBrief;
import com.yan233.courseplatform.common.web.UserContext;
import com.yan233.courseplatform.file.entity.FileMetadata;
import com.yan233.courseplatform.file.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {
    private final FileStorageService fileService;

    public FileController(FileStorageService fileService) {
        this.fileService = fileService;
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
        return ResponseEntity.ok()
                .contentType(type)
                .body(resource);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        FileMetadata metadata = fileService.getById(id);
        Resource resource = fileService.loadAsResource(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getOriginalName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/internal/batch")
    public Result<List<FileBrief>> internalFiles(@RequestParam List<Long> ids) {
        return Result.ok(fileService.briefByIds(ids));
    }
}
