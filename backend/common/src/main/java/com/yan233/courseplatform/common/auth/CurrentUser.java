package com.yan233.courseplatform.common.auth;

import java.util.List;

public record CurrentUser(Long userId, String username, List<String> roles) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasAnyRole(String... candidates) {
        if (roles == null || candidates == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (roles.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isTeacher() {
        return hasRole("TEACHER");
    }

    public boolean isTa() {
        return hasRole("TA");
    }

    public boolean isStudent() {
        return hasRole("STUDENT");
    }
}
