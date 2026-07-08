package com.yan233.courseplatform.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan233.courseplatform.common.dto.FileOwnerRequest;
import com.yan233.courseplatform.common.dto.FileReferenceReplaceRequest;
import com.yan233.courseplatform.common.dto.FileReferenceRequest;
import com.yan233.courseplatform.common.exception.BusinessException;
import com.yan233.courseplatform.file.entity.FileGcQueue;
import com.yan233.courseplatform.file.entity.FileMetadata;
import com.yan233.courseplatform.file.entity.FileReference;
import com.yan233.courseplatform.file.mapper.FileGcQueueMapper;
import com.yan233.courseplatform.file.mapper.FileMetadataMapper;
import com.yan233.courseplatform.file.mapper.FileReferenceMapper;
import com.yan233.courseplatform.file.service.FileLifecycleService;
import com.yan233.courseplatform.file.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FileLifecycleServiceImpl implements FileLifecycleService {
    private static final Set<String> OWNER_TYPES = Set.of("COURSE_RESOURCE", "ASSIGNMENT", "SUBMISSION", "SHOWCASE");

    private final FileReferenceMapper referenceMapper;
    private final FileGcQueueMapper gcQueueMapper;
    private final FileMetadataMapper metadataMapper;
    private final FileStorageService storageService;

    @Value("${app.file.gc.batch-size:20}")
    private int batchSize;
    @Value("${app.file.gc.max-attempts:3}")
    private int maxAttempts;
    @Value("${app.file.gc.retry-delay-seconds:60}")
    private long retryDelaySeconds;
    @Value("${app.file.gc.orphan-age-minutes:30}")
    private int orphanAgeMinutes;
    @Value("${app.file.gc.enabled:true}")
    private boolean gcEnabled;

    public FileLifecycleServiceImpl(FileReferenceMapper referenceMapper,
                                    FileGcQueueMapper gcQueueMapper,
                                    FileMetadataMapper metadataMapper,
                                    FileStorageService storageService) {
        this.referenceMapper = referenceMapper;
        this.gcQueueMapper = gcQueueMapper;
        this.metadataMapper = metadataMapper;
        this.storageService = storageService;
    }

    @Override
    @Transactional
    public void bind(FileReferenceRequest request) {
        validateOwner(request.getOwnerType(), request.getOwnerId());
        if (request.getFileId() == null) {
            return;
        }
        FileMetadata metadata = metadataMapper.selectById(request.getFileId());
        if (metadata == null) {
            throw new BusinessException("文件不存在或已释放");
        }
        releaseActiveReferences(request.getOwnerType(), request.getOwnerId(), request.getFileId());
        boolean exists = referenceMapper.selectCount(new LambdaQueryWrapper<FileReference>()
                .eq(FileReference::getOwnerType, request.getOwnerType())
                .eq(FileReference::getOwnerId, request.getOwnerId())
                .eq(FileReference::getFileId, request.getFileId())
                .eq(FileReference::getStatus, 1)) > 0;
        if (exists) {
            return;
        }
        FileReference reference = new FileReference();
        reference.setFileId(request.getFileId());
        reference.setOwnerType(request.getOwnerType());
        reference.setOwnerId(request.getOwnerId());
        reference.setStatus(1);
        reference.setDeleted(0);
        referenceMapper.insert(reference);
    }

    @Override
    @Transactional
    public void replace(FileReferenceReplaceRequest request) {
        validateOwner(request.getOwnerType(), request.getOwnerId());
        if (request.getOldFileId() != null && !request.getOldFileId().equals(request.getNewFileId())) {
            release(referenceRequest(request.getOldFileId(), request.getOwnerType(), request.getOwnerId()));
        }
        if (request.getNewFileId() == null) {
            releaseOwner(ownerRequest(request.getOwnerType(), request.getOwnerId()));
            return;
        }
        bind(referenceRequest(request.getNewFileId(), request.getOwnerType(), request.getOwnerId()));
    }

    @Override
    @Transactional
    public void release(FileReferenceRequest request) {
        validateOwner(request.getOwnerType(), request.getOwnerId());
        if (request.getFileId() == null) {
            return;
        }
        referenceMapper.delete(new LambdaQueryWrapper<FileReference>()
                .eq(FileReference::getOwnerType, request.getOwnerType())
                .eq(FileReference::getOwnerId, request.getOwnerId())
                .eq(FileReference::getFileId, request.getFileId())
                .eq(FileReference::getStatus, 1));
    }

    @Override
    @Transactional
    public void releaseOwner(FileOwnerRequest request) {
        validateOwner(request.getOwnerType(), request.getOwnerId());
        referenceMapper.delete(new LambdaQueryWrapper<FileReference>()
                .eq(FileReference::getOwnerType, request.getOwnerType())
                .eq(FileReference::getOwnerId, request.getOwnerId())
                .eq(FileReference::getStatus, 1));
    }

    @Override
    @Transactional
    public int processPendingGc() {
        if (!gcEnabled) {
            return 0;
        }
        gcQueueMapper.enqueueOrphans(orphanAgeMinutes);
        List<FileGcQueue> queues = gcQueueMapper.selectRunnable(batchSize, maxAttempts);
        int processed = 0;
        for (FileGcQueue queue : queues) {
            if (gcQueueMapper.markProcessing(queue.getId()) == 0) {
                continue;
            }
            processed++;
            processOne(queue);
        }
        return processed;
    }

    @Override
    public List<Map<String, Object>> gcStats() {
        return gcQueueMapper.gcStats();
    }

    @Override
    public List<Map<String, Object>> fileStatuses() {
        return gcQueueMapper.fileStatuses();
    }

    @Scheduled(fixedDelayString = "${app.file.gc.fixed-delay-ms:30000}")
    public void scheduledGc() {
        processPendingGc();
    }

    private void processOne(FileGcQueue queue) {
        FileGcQueue update = new FileGcQueue();
        update.setId(queue.getId());
        try {
            int activeReferences = referenceMapper.countActiveByFileId(queue.getFileId());
            if (activeReferences > 0) {
                update.setStatus(3);
                update.setProcessedTime(LocalDateTime.now());
                gcQueueMapper.updateById(update);
                return;
            }

            FileMetadata metadata = metadataMapper.selectById(queue.getFileId());
            if (metadata != null) {
                storageService.deleteContent(metadata);
                metadataMapper.deleteById(queue.getFileId());
            }
            update.setStatus(2);
            update.setProcessedTime(LocalDateTime.now());
            gcQueueMapper.updateById(update);
        } catch (RuntimeException ex) {
            update.setStatus(4);
            update.setLastError(shorten(ex.getMessage()));
            update.setNextRetryTime(LocalDateTime.now().plusSeconds(retryDelaySeconds));
            gcQueueMapper.updateById(update);
        }
    }

    private void releaseActiveReferences(String ownerType, Long ownerId, Long keepFileId) {
        referenceMapper.delete(new LambdaQueryWrapper<FileReference>()
                .eq(FileReference::getOwnerType, ownerType)
                .eq(FileReference::getOwnerId, ownerId)
                .eq(FileReference::getStatus, 1)
                .ne(keepFileId != null, FileReference::getFileId, keepFileId));
    }

    private void validateOwner(String ownerType, Long ownerId) {
        if (!OWNER_TYPES.contains(ownerType)) {
            throw new BusinessException(400, "不支持的文件引用类型");
        }
        if (ownerId == null || ownerId <= 0) {
            throw new BusinessException(400, "文件引用业务ID无效");
        }
    }

    private FileReferenceRequest referenceRequest(Long fileId, String ownerType, Long ownerId) {
        FileReferenceRequest request = new FileReferenceRequest();
        request.setFileId(fileId);
        request.setOwnerType(ownerType);
        request.setOwnerId(ownerId);
        return request;
    }

    private FileOwnerRequest ownerRequest(String ownerType, Long ownerId) {
        FileOwnerRequest request = new FileOwnerRequest();
        request.setOwnerType(ownerType);
        request.setOwnerId(ownerId);
        return request;
    }

    private String shorten(String message) {
        if (message == null) {
            return "文件回收失败";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
