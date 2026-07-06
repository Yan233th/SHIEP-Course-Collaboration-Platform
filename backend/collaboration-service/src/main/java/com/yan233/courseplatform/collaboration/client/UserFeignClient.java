package com.yan233.courseplatform.collaboration.client;

import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.dto.UserBrief;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service", contextId = "collaborationUserClient", fallback = UserFeignFallback.class)
public interface UserFeignClient {
    @GetMapping("/users/internal/batch")
    Result<List<UserBrief>> usersByIds(@RequestParam("ids") List<Long> ids);
}

