package com.yan233.courseplatform.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yan233.courseplatform.common.dto.FileBrief;
import com.yan233.courseplatform.common.exception.BusinessException;
import com.yan233.courseplatform.file.entity.FileMetadata;
import com.yan233.courseplatform.file.mapper.FileMetadataMapper;
import com.yan233.courseplatform.file.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageServiceImpl extends ServiceImpl<FileMetadataMapper, FileMetadata> implements FileStorageService {
    @Value("${app.file.storage-dir:uploads}")
    private String storageDir;

    @Override
    @Transactional
    public FileMetadata store(MultipartFile file, Long uploaderId, String bizType) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        String original = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot);
        }
        String storageName = UUID.randomUUID() + ext;
        Path directory = storageRoot().resolve(LocalDate.now().toString());
        Path target = directory.resolve(storageName);
        try {
            Files.createDirectories(directory);
            file.transferTo(target);
        } catch (IOException ex) {
            throw new BusinessException(500, "文件保存失败: " + ex.getMessage());
        }
        FileMetadata metadata = new FileMetadata();
        metadata.setOriginalName(original);
        metadata.setStorageName(storageName);
        metadata.setStoragePath(target.normalize().toString());
        metadata.setContentType(file.getContentType());
        metadata.setSizeBytes(file.getSize());
        metadata.setUploaderId(uploaderId);
        metadata.setBizType(bizType);
        metadata.setStatus(1);
        metadata.setDeleted(0);
        save(metadata);
        return metadata;
    }

    @Override
    public Resource loadAsResource(Long id) {
        FileMetadata metadata = getById(id);
        if (metadata == null || metadata.getStatus() == null || metadata.getStatus() != 1) {
            throw new BusinessException("文件不存在");
        }
        try {
            Resource resource = new UrlResource(resolveStoragePath(metadata.getStoragePath()).toUri());
            if (!resource.exists()) {
                throw new BusinessException("文件内容不存在");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new BusinessException(500, "文件路径无效");
        }
    }

    @Override
    public List<FileBrief> briefByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(FileMetadata::getId, ids)
                .eq(FileMetadata::getStatus, 1)
                .list()
                .stream()
                .map(f -> new FileBrief(f.getId(), f.getOriginalName(), f.getContentType(), f.getSizeBytes(), "/api/files/preview/" + f.getId()))
                .toList();
    }

    @Override
    public void deleteContent(FileMetadata metadata) {
        if (metadata == null || metadata.getStoragePath() == null || metadata.getStoragePath().isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveStoragePath(metadata.getStoragePath()));
        } catch (IOException ex) {
            throw new BusinessException(500, "文件内容删除失败: " + ex.getMessage());
        }
    }

    private Path storageRoot() {
        return Path.of(storageDir).toAbsolutePath().normalize();
    }

    private Path resolveStoragePath(String storagePath) {
        Path stored = Path.of(storagePath);
        if (stored.isAbsolute()) {
            return stored.normalize();
        }

        List<Path> candidates = new ArrayList<>();
        candidates.add(stored.toAbsolutePath().normalize());
        candidates.add(storageRoot().resolve(stored).normalize());

        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            candidates.add(current.resolve(stored).normalize());
            current = current.getParent();
        }

        return candidates.stream()
                .filter(Files::exists)
                .findFirst()
                .orElse(candidates.get(0));
    }
}
