package com.yan233.courseplatform.collaboration.client;

import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.dto.UserBrief;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserFeignFallback implements UserFeignClient {
    @Override
    public Result<List<UserBrief>> usersByIds(List<Long> ids) {
        List<UserBrief> fallback = ids == null ? List.of() : ids.stream()
                .map(id -> new UserBrief(id, "unavailable", "用户服务暂不可用", "UNKNOWN"))
                .toList();
        return Result.ok(fallback);
    }
}

