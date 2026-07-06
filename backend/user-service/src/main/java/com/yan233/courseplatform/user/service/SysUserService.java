package com.yan233.courseplatform.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yan233.courseplatform.common.api.PageResult;
import com.yan233.courseplatform.common.dto.UserBrief;
import com.yan233.courseplatform.user.dto.LoginResponse;
import com.yan233.courseplatform.user.dto.MenuItem;
import com.yan233.courseplatform.user.dto.ProfileUpdateRequest;
import com.yan233.courseplatform.user.dto.UserQuery;
import com.yan233.courseplatform.user.dto.UserRequest;
import com.yan233.courseplatform.user.entity.SysUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SysUserService extends IService<SysUser> {
    LoginResponse login(String username, String password);

    LoginResponse refresh(String token);

    SysUser create(UserRequest request);

    SysUser updateUser(Long id, UserRequest request);

    SysUser getProfile(Long id);

    SysUser updateProfile(Long id, ProfileUpdateRequest request);

    PageResult<SysUser> pageUsers(UserQuery query);

    List<MenuItem> menus(String roleCode);

    List<UserBrief> briefByIds(Collection<Long> ids);

    List<Map<String, Object>> countByRole();
}
