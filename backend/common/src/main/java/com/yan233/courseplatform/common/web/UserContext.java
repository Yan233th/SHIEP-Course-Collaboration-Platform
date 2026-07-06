package com.yan233.courseplatform.common.web;

import com.yan233.courseplatform.common.auth.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;

public final class UserContext {
    private UserContext() {
    }

    public static CurrentUser from(HttpServletRequest request) {
        String id = request.getHeader("X-User-Id");
        String username = request.getHeader("X-Username");
        String rolesHeader = request.getHeader("X-Roles");
        List<String> roles = rolesHeader == null || rolesHeader.isBlank()
                ? List.of()
                : Arrays.stream(rolesHeader.split(",")).filter(s -> !s.isBlank()).toList();
        return new CurrentUser(id == null || id.isBlank() ? 0L : Long.parseLong(id), username, roles);
    }
}

