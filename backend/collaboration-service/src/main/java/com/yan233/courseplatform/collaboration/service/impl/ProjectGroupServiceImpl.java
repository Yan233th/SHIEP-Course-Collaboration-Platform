package com.yan233.courseplatform.collaboration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yan233.courseplatform.collaboration.dto.JoinGroupRequest;
import com.yan233.courseplatform.collaboration.dto.ProjectGroupRequest;
import com.yan233.courseplatform.collaboration.entity.ProjectGroup;
import com.yan233.courseplatform.collaboration.entity.ProjectMember;
import com.yan233.courseplatform.collaboration.mapper.ProjectGroupMapper;
import com.yan233.courseplatform.collaboration.mapper.ProjectMemberMapper;
import com.yan233.courseplatform.collaboration.service.ProjectGroupService;
import com.yan233.courseplatform.common.exception.BusinessException;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
public class ProjectGroupServiceImpl extends ServiceImpl<ProjectGroupMapper, ProjectGroup> implements ProjectGroupService {
    private final ProjectMemberMapper memberMapper;
    private final StringRedisTemplate redisTemplate;

    public ProjectGroupServiceImpl(ProjectMemberMapper memberMapper, StringRedisTemplate redisTemplate) {
        this.memberMapper = memberMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public ProjectGroup create(ProjectGroupRequest request) {
        ProjectGroup group = new ProjectGroup();
        BeanUtils.copyProperties(request, group);
        group.setCurrentMembers(0);
        group.setDeleted(0);
        save(group);
        join(group.getId(), new JoinGroupRequest() {{
            setUserId(request.getLeaderId());
            setRoleName("组长");
        }});
        return getById(group.getId());
    }

    @Override
    @Transactional
    public ProjectGroup updateGroup(Long id, ProjectGroupRequest request) {
        ProjectGroup group = getById(id);
        if (group == null) {
            throw new BusinessException("项目组不存在");
        }
        BeanUtils.copyProperties(request, group);
        group.setId(id);
        updateById(group);
        return group;
    }

    @Override
    @Transactional
    public ProjectMember join(Long groupId, JoinGroupRequest request) {
        String lockKey = "lock:project-group:" + groupId;
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(10));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(429, "项目组正在被其他同学加入，请稍后重试");
        }
        try {
            ProjectGroup group = getById(groupId);
            if (group == null || group.getStatus() == 0) {
                throw new BusinessException("项目组不可用");
            }
            // 用 status 标记是否在组，deleted 始终保持 0，避免 (group_id, user_id, deleted)
            // 唯一索引在反复加入/退出时撞键。退出过的成员(status=0)可重新加入。
            ProjectMember existing = memberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
                    .eq(ProjectMember::getGroupId, groupId)
                    .eq(ProjectMember::getUserId, request.getUserId()));
            if (existing != null && existing.getStatus() != null && existing.getStatus() == 1) {
                throw new BusinessException("用户已加入该项目组");
            }
            if (group.getCurrentMembers() >= group.getMaxMembers()) {
                throw new BusinessException("项目组人数已满");
            }
            ProjectMember member;
            if (existing != null) {
                existing.setStatus(1);
                existing.setRoleName(request.getRoleName());
                memberMapper.updateById(existing);
                member = existing;
            } else {
                member = new ProjectMember();
                member.setGroupId(groupId);
                member.setUserId(request.getUserId());
                member.setRoleName(request.getRoleName());
                member.setStatus(1);
                member.setDeleted(0);
                memberMapper.insert(member);
            }
            group.setCurrentMembers(group.getCurrentMembers() + 1);
            updateById(group);
            return member;
        } finally {
            String current = redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(current)) {
                redisTemplate.delete(lockKey);
            }
        }
    }

    @Override
    @Transactional
    public void leave(Long groupId, Long userId) {
        String lockKey = "lock:project-group:" + groupId;
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(10));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(429, "项目组正在更新，请稍后重试");
        }
        try {
            ProjectGroup group = getById(groupId);
            if (group == null || group.getStatus() == 0) {
                throw new BusinessException("项目组不可用");
            }
            ProjectMember member = memberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
                    .eq(ProjectMember::getGroupId, groupId)
                    .eq(ProjectMember::getUserId, userId));
            if (member == null || member.getStatus() == null || member.getStatus() == 0) {
                throw new BusinessException("用户不在该项目组中");
            }
            // 软删除：置 status=0，保留行，保持 deleted=0 不变，避免唯一索引冲突
            member.setStatus(0);
            memberMapper.updateById(member);
            group.setCurrentMembers(Math.max(0, group.getCurrentMembers() - 1));
            updateById(group);
        } finally {
            String current = redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(current)) {
                redisTemplate.delete(lockKey);
            }
        }
    }
}
