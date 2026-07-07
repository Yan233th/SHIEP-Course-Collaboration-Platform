<template>
  <section class="dashboard">
    <section class="dashboard-header">
      <div class="dashboard-heading">
        <h1>{{ dashboardTitle }}</h1>
        <p>{{ dashboardLead }}</p>
        <div class="dashboard-meta">
          <el-tag effect="plain">{{ roleLabel(currentRole) }}</el-tag>
          <el-tag v-if="appState.courseAccess?.courseRole" effect="plain" :type="courseRoleTagType">
            课程身份：{{ roleLabel(appState.courseAccess.courseRole) }}
          </el-tag>
          <el-tag effect="plain" type="info">{{ permissionCount }} 项权限</el-tag>
        </div>
      </div>
      <div class="dashboard-course-summary">
        <span>当前课程</span>
        <strong>{{ currentCourseLabel }}</strong>
        <small>任课教师：{{ selectedCourse?.teacherName || '-' }}</small>
        <el-select v-model="currentCourseModel" filterable placeholder="选择课程">
          <el-option
            v-for="course in appState.courses.records"
            :key="course.id"
            :label="courseLabel(course)"
            :value="course.id"
          />
        </el-select>
      </div>
    </section>

    <div class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card" :class="metric.tone">
        <span class="metric-icon">
          <el-icon><component :is="metric.icon" /></el-icon>
        </span>
        <span class="metric-label">{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.caption }}</small>
      </article>
    </div>

    <div class="dashboard-grid">
      <section class="panel">
        <div class="section-heading">
          <div>
            <h2>快速进入</h2>
          </div>
        </div>
        <div class="quick-grid">
          <RouterLink v-for="link in quickLinks" :key="link.to" class="quick-link" :to="link.to">
            <span class="quick-link-icon">
              <el-icon><component :is="link.icon" /></el-icon>
            </span>
            <span>
              <strong>{{ link.title }}</strong>
              <small>{{ link.caption }}</small>
            </span>
          </RouterLink>
        </div>
      </section>

      <section class="panel status-panel">
        <div class="section-heading">
          <div>
            <h2>角色状态</h2>
            <p>{{ selectedCourse?.courseName || '当前平台' }}</p>
          </div>
          <strong>{{ roleLabel(appState.courseAccess?.courseRole || currentRole) }}</strong>
        </div>
        <div class="status-list">
          <div>
            <span>系统身份</span>
            <strong>{{ roleLabel(currentRole) }}</strong>
          </div>
          <div>
            <span>课程身份</span>
            <strong>{{ roleLabel(appState.courseAccess?.courseRole) }}</strong>
          </div>
          <div>
            <span>容量占用</span>
            <strong>{{ selectedCourse?.currentStudents ?? 0 }} / {{ selectedCourse?.maxStudents ?? '-' }}</strong>
          </div>
        </div>
        <el-progress class="capacity-progress" :percentage="capacityPercent" :show-text="false" />
        <div class="capability-tags">
          <el-tag v-for="action in visibleActions" :key="action" effect="plain">{{ action }}</el-tag>
          <span v-if="!visibleActions.length" class="muted">暂无课程操作权限</span>
        </div>
      </section>
    </div>

    <section v-if="hasSystemRole('ADMIN')" class="panel">
      <div class="section-heading">
        <div>
          <h2>课程概览</h2>
        </div>
        <el-button :icon="Refresh" @click="loadStats">刷新</el-button>
      </div>
      <el-table :data="stats" height="300px">
        <el-table-column prop="course_name" label="课程" />
        <el-table-column prop="member_count" label="成员" width="80" />
        <el-table-column prop="resource_count" label="资源" width="80" />
        <el-table-column prop="assignment_count" label="作业" width="80" />
        <el-table-column prop="project_group_count" label="项目组" width="90" />
      </el-table>
    </section>

    <section v-else class="panel">
      <div class="section-heading">
        <div>
          <h2>当前课程</h2>
        </div>
      </div>
      <dl class="course-summary">
        <div>
          <dt>课程编号</dt>
          <dd>{{ selectedCourse?.courseCode || '-' }}</dd>
        </div>
        <div>
          <dt>任课教师</dt>
          <dd>{{ selectedCourse?.teacherName || '-' }}</dd>
        </div>
        <div>
          <dt>课程说明</dt>
          <dd>{{ selectedCourse?.description || '暂无说明' }}</dd>
        </div>
      </dl>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Bell, ChatDotRound, Collection, Connection, DataAnalysis, Files, Notebook, Refresh, Trophy, User } from '@element-plus/icons-vue'
import { courseService } from '../services/platform'
import { appState, courseLabel, currentCourseId, currentCourseLabel, currentRole, hasSystemRole, roleLabel, selectedCourse, setCurrentCourse } from '../state/appState'
import type { CourseStats } from '../types'

const stats = ref<CourseStats[]>([])
const currentCourseModel = computed({
  get: () => currentCourseId.value,
  set: (value: number) => {
    void setCurrentCourse(value)
  }
})
const dashboardTitle = computed(() => {
  if (hasSystemRole('ADMIN')) return '平台管理总览'
  return currentCourseLabel.value
})
const dashboardLead = computed(() => {
  if (hasSystemRole('ADMIN')) return '查看课程、成员、资源和项目协作的整体状态。'
  const course = selectedCourse.value
  if (!course) return '选择课程后进入成员、资源、作业和项目协作。'
  return `${course.courseCode} · ${course.teacherName || '未设置教师'} · ${course.credit ?? '-'} 学分`
})
const permissionCount = computed(() => appState.courseAccess?.actions.length || 0)
const courseRoleTagType = computed(() => {
  const role = appState.courseAccess?.courseRole
  if (role === 'TEACHER') return 'success'
  if (role === 'TA') return 'warning'
  if (role === 'STUDENT') return 'info'
  return ''
})
const capacityPercent = computed(() => {
  const max = selectedCourse.value?.maxStudents || 0
  if (!max) return 0
  return Math.min(100, Math.round(((selectedCourse.value?.currentStudents || 0) / max) * 100))
})
const visibleActions = computed(() => (appState.courseAccess?.actions || []).slice(0, 10))
const metrics = computed(() => [
  {
    label: hasSystemRole('ADMIN') ? '平台课程' : '我的课程',
    value: appState.courses.total,
    caption: '可访问课程空间',
    icon: Files,
    tone: 'tone-brand'
  },
  {
    label: '当前课程人数',
    value: selectedCourse.value?.currentStudents ?? 0,
    caption: `容量 ${selectedCourse.value?.maxStudents ?? '-'}`,
    icon: Connection,
    tone: 'tone-blue'
  },
  {
    label: '学分',
    value: selectedCourse.value?.credit ?? '-',
    caption: selectedCourse.value?.courseCode || '课程编号',
    icon: Notebook,
    tone: 'tone-amber'
  },
  {
    label: '学时',
    value: selectedCourse.value?.hours ?? '-',
    caption: '教学计划学时',
    icon: Collection,
    tone: 'tone-rose'
  }
])
const quickLinks = computed(() => {
  const links = [
    { title: '成员与权限', caption: '课程角色与访问控制', to: '/courses/members', icon: Connection },
    { title: '课程通知', caption: '发布与查看课程公告', to: '/courses/notices', icon: Bell },
    { title: '课程资源', caption: '资料、分类与标签', to: '/courses/resources', icon: Files },
    { title: '作业与提交', caption: '作业发布、提交、评分', to: '/courses/assignments', icon: Notebook },
    { title: '项目分组', caption: '课程项目协作分组', to: '/projects/groups', icon: Collection },
    { title: '讨论交流', caption: '主题讨论与回复', to: '/projects/discussions', icon: ChatDotRound },
    { title: '成果展示', caption: '项目成果与展示页', to: '/projects/showcases', icon: Trophy }
  ]
  if (!hasSystemRole('ADMIN')) return links
  return [
    { title: '用户与角色', caption: '系统账号与角色维护', to: '/admin/users', icon: User },
    { title: '课程统计', caption: '课程资源与协作统计', to: '/admin/stats', icon: DataAnalysis },
    ...links
  ]
})

async function loadStats() {
  stats.value = await courseService.getStats()
}

onMounted(async () => {
  if (hasSystemRole('ADMIN')) {
    await loadStats()
  }
})
</script>
