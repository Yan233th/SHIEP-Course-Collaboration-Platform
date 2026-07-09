package com.yan233.courseplatform.collaboration.client;

import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.dto.FileBrief;
import com.yan233.courseplatform.common.dto.FileOwnerRequest;
import com.yan233.courseplatform.common.dto.FileReferenceReplaceRequest;
import com.yan233.courseplatform.common.dto.FileReferenceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "file-service", contextId = "collaborationFileClient")
public interface FileFeignClient {
    @GetMapping("/files/internal/batch")
    Result<List<FileBrief>> filesByIds(@RequestParam("ids") List<Long> ids);

    @PostMapping("/files/internal/references/bind")
    Result<Void> bindReference(@RequestBody FileReferenceRequest request);

    @PostMapping("/files/internal/references/replace")
    Result<Void> replaceReference(@RequestBody FileReferenceReplaceRequest request);

    @PostMapping("/files/internal/references/release-owner")
    Result<Void> releaseOwner(@RequestBody FileOwnerRequest request);
}
