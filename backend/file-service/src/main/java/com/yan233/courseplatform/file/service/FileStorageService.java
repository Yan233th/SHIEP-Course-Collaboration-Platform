package com.yan233.courseplatform.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yan233.courseplatform.common.dto.FileBrief;
import com.yan233.courseplatform.file.entity.FileMetadata;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

public interface FileStorageService extends IService<FileMetadata> {
    FileMetadata store(MultipartFile file, Long uploaderId, String bizType);

    Resource loadAsResource(Long id);

    List<FileBrief> briefByIds(Collection<Long> ids);
}

