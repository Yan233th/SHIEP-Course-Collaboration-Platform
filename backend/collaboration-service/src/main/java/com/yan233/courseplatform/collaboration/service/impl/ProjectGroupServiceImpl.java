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
            Long exists = memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                    .eq(ProjectMember::getGroupId, groupId)
                    .eq(ProjectMember::getUserId, request.getUserId()));
            if (exists > 0) {
                throw new BusinessException("用户已加入该项目组");
            }
            if (group.getCurrentMembers() >= group.getMaxMembers()) {
                throw new BusinessException("项目组人数已满");
            }
            ProjectMember member = new ProjectMember();
            member.setGroupId(groupId);
            member.setUserId(request.getUserId());
            member.setRoleName(request.getRoleName());
            member.setStatus(1);
            member.setDeleted(0);
            memberMapper.insert(member);
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
}

