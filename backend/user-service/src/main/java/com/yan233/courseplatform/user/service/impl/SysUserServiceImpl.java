package com.yan233.courseplatform.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yan233.courseplatform.common.api.PageResult;
import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.auth.JwtSupport;
import com.yan233.courseplatform.common.dto.UserBrief;
import com.yan233.courseplatform.common.exception.BusinessException;
import com.yan233.courseplatform.common.util.PageUtils;
import com.yan233.courseplatform.user.dto.LoginResponse;
import com.yan233.courseplatform.user.dto.MenuItem;
import com.yan233.courseplatform.user.dto.UserQuery;
import com.yan233.courseplatform.user.dto.UserRequest;
import com.yan233.courseplatform.user.entity.SysUser;
import com.yan233.courseplatform.user.mapper.SysUserMapper;
import com.yan233.courseplatform.user.service.SysUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expire-hours:12}")
    private long expireHours;

    public SysUserServiceImpl(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResponse login(String username, String password) {
        SysUser user = getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username).eq(SysUser::getStatus, 1));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        return tokenFor(user);
    }

    @Override
    public LoginResponse refresh(String token) {
        CurrentUser current = JwtSupport.parse(jwtSecret, JwtSupport.stripBearer(token));
        SysUser user = getById(current.userId());
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(401, "用户不可用");
        }
        return tokenFor(user);
    }

    @Override
    @Transactional
    public SysUser create(UserRequest request) {
        boolean exists = count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername())) > 0;
        if (exists) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        BeanUtils.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword() == null ? "123456" : request.getPassword()));
        user.setDeleted(0);
        save(user);
        return user;
    }

    @Override
    @Transactional
    public SysUser updateUser(Long id, UserRequest request) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        boolean duplicate = count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .ne(SysUser::getId, id)) > 0;
        if (duplicate) {
            throw new BusinessException("用户名已存在");
        }
        String oldPassword = user.getPassword();
        BeanUtils.copyProperties(request, user);
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            user.setPassword(oldPassword);
        } else {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        updateById(user);
        return user;
    }

    @Override
    public PageResult<SysUser> pageUsers(UserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(query.getKeyword() != null && !query.getKeyword().isBlank(), w -> w
                .like(SysUser::getUsername, query.getKeyword())
                .or()
                .like(SysUser::getRealName, query.getKeyword())
                .or()
                .like(SysUser::getEmail, query.getKeyword()));
        wrapper.eq(query.getRoleCode() != null && !query.getRoleCode().isBlank(), SysUser::getRoleCode, query.getRoleCode());
        wrapper.eq(query.getStatus() != null, SysUser::getStatus, query.getStatus());
        applySort(wrapper, query.getSortField(), query.getSortOrder());
        Page<SysUser> page = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        page.getRecords().forEach(u -> u.setPassword(null));
        return PageUtils.of(page);
    }

    @Override
    public List<MenuItem> menus(String roleCode) {
        List<MenuItem> common = List.of(
                new MenuItem("课程空间", "/courses", List.of(
                        new MenuItem("课程列表", "/courses/list", List.of()),
                        new MenuItem("成员与权限", "/courses/members", List.of()),
                        new MenuItem("课程通知", "/courses/notices", List.of()),
                        new MenuItem("课程资源", "/courses/resources", List.of()),
                        new MenuItem("课程作业", "/courses/assignments", List.of())
                )),
                new MenuItem("协作中心", "/projects", List.of(
                        new MenuItem("项目分组", "/projects/groups", List.of()),
                        new MenuItem("讨论交流", "/projects/discussions", List.of()),
                        new MenuItem("成果展示", "/projects/showcases", List.of())
                ))
        );
        if ("ADMIN".equals(roleCode)) {
            return List.of(
                    new MenuItem("系统管理", "/admin", List.of(
                            new MenuItem("用户管理", "/admin/users", List.of()),
                            new MenuItem("统计分析", "/admin/stats", List.of())
                    )),
                    common.get(0),
                    common.get(1)
            );
        }
        return common;
    }

    @Override
    public List<UserBrief> briefByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return listByIds(ids).stream()
                .sorted(Comparator.comparing(SysUser::getId))
                .map(u -> new UserBrief(u.getId(), u.getUsername(), u.getRealName(), u.getRoleCode()))
                .toList();
    }

    @Override
    public List<Map<String, Object>> countByRole() {
        return baseMapper.countByRole();
    }

    private LoginResponse tokenFor(SysUser user) {
        List<String> roles = List.of(user.getRoleCode());
        String token = JwtSupport.createToken(jwtSecret, user.getId(), user.getUsername(), roles, Duration.ofHours(expireHours));
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRealName(), roles);
    }

    private void applySort(LambdaQueryWrapper<SysUser> wrapper, String sortField, String sortOrder) {
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if ("username".equals(sortField)) {
            wrapper.orderBy(true, asc, SysUser::getUsername);
        } else if ("createTime".equals(sortField)) {
            wrapper.orderBy(true, asc, SysUser::getCreateTime);
        } else {
            wrapper.orderByDesc(SysUser::getId);
        }
    }
}
