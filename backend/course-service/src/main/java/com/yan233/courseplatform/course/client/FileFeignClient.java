package com.yan233.courseplatform.course.client;

import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.dto.FileBrief;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "file-service", contextId = "courseFileClient")
public interface FileFeignClient {
    @GetMapping("/files/internal/batch")
    Result<List<FileBrief>> filesByIds(@RequestParam("ids") List<Long> ids);
}
