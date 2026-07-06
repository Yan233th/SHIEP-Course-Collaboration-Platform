package com.yan233.courseplatform.user.controller;

import com.yan233.courseplatform.common.api.PageResult;
import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.auth.AccessControl;
import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.dto.UserBrief;
import com.yan233.courseplatform.common.web.UserContext;
import com.yan233.courseplatform.user.dto.MenuItem;
import com.yan233.courseplatform.user.dto.UserQuery;
import com.yan233.courseplatform.user.dto.UserRequest;
import com.yan233.courseplatform.user.entity.SysUser;
import com.yan233.courseplatform.user.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    private final SysUserService userService;

    public UserController(SysUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<PageResult<SysUser>> page(UserQuery query, HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        return Result.ok(userService.pageUsers(query));
    }

    @GetMapping("/options")
    public Result<PageResult<SysUser>> options(UserQuery query, HttpServletRequest servletRequest) {
        AccessControl.requireRole(UserContext.from(servletRequest), "ADMIN", "TEACHER");
        query.setStatus(1);
        return Result.ok(userService.pageUsers(query));
    }

    @PostMapping
    public Result<SysUser> create(@RequestBody @Valid UserRequest request, HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        SysUser user = userService.create(request);
        user.setPassword(null);
        return Result.ok(user);
    }

    @PutMapping("/{id}")
    public Result<SysUser> update(@PathVariable Long id, @RequestBody @Valid UserRequest request, HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        SysUser user = userService.updateUser(id, request);
        user.setPassword(null);
        return Result.ok(user);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        userService.removeById(id);
        return Result.ok();
    }

    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids, HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        userService.removeBatchByIds(ids);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<SysUser> get(@PathVariable Long id, HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        SysUser user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.ok(user);
    }

    @GetMapping("/menus")
    public Result<List<MenuItem>> menus(HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        String roleCode = current.roles() == null || current.roles().isEmpty() ? "STUDENT" : current.roles().get(0);
        return Result.ok(userService.menus(roleCode));
    }

    @GetMapping("/stats/roles")
    public Result<List<Map<String, Object>>> countByRole(HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        return Result.ok(userService.countByRole());
    }

    @GetMapping("/internal/batch")
    public Result<List<UserBrief>> internalUsers(@RequestParam List<Long> ids) {
        return Result.ok(userService.briefByIds(ids));
    }

    private void requireAdmin(HttpServletRequest servletRequest) {
        AccessControl.requireRole(UserContext.from(servletRequest), "ADMIN");
    }
}
