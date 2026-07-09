package com.yan233.courseplatform.file.service;

import com.yan233.courseplatform.common.dto.FileOwnerRequest;
import com.yan233.courseplatform.common.dto.FileReferenceReplaceRequest;
import com.yan233.courseplatform.common.dto.FileReferenceRequest;

import java.util.List;
import java.util.Map;

public interface FileLifecycleService {
    void bind(FileReferenceRequest request);

    void replace(FileReferenceReplaceRequest request);

    void release(FileReferenceRequest request);

    void releaseOwner(FileOwnerRequest request);

    int processPendingGc();

    int processPendingGcNow();

    List<Map<String, Object>> gcStats();

    List<Map<String, Object>> fileStatuses();
}
