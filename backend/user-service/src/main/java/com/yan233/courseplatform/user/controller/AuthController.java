package com.yan233.courseplatform.user.controller;

import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.user.dto.LoginRequest;
import com.yan233.courseplatform.user.dto.LoginResponse;
import com.yan233.courseplatform.user.service.SysUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final SysUserService userService;

    public AuthController(SysUserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return Result.ok(userService.login(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestHeader("Authorization") String authorization) {
        return Result.ok(userService.refresh(authorization));
    }
}

