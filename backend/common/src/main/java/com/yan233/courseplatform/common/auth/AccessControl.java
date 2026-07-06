package com.yan233.courseplatform.common.auth;

import com.yan233.courseplatform.common.exception.BusinessException;

public final class AccessControl {
    private AccessControl() {
    }

    public static void requireRole(CurrentUser user, String... roles) {
        if (user == null || !user.hasAnyRole(roles)) {
            throw new BusinessException(403, "无权限执行该操作");
        }
    }
}
