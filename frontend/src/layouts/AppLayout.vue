<template>
  <el-container class="app-shell">
    <el-aside width="236px" class="sidebar">
      <RouterLink class="brand" to="/dashboard">
        <span class="brand-mark">协</span>
        <span>课程协同管理平台</span>
      </RouterLink>

      <el-menu :default-active="route.path" router>
        <el-menu-item index="/dashboard">
          <el-icon><Platform /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-sub-menu v-for="menu in appState.menus" :key="menu.path" :index="menu.path">
          <template #title>
            <el-icon><component :is="menuIcon(menu.path)" /></el-icon>
            <span>{{ menu.title }}</span>
          </template>
          <el-menu-item v-for="child in menu.children" :key="child.path" :index="child.path">
            {{ child.title }}
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div class="page-title">
          <strong>{{ pageTitle }}</strong>
          <div class="identity-strip">
            <span class="muted">{{ appState.session.realName }}</span>
            <el-tag size="small" effect="plain">{{ roleLabel(currentRole) }}</el-tag>
            <el-tag v-if="showCourseContext" size="small" effect="plain" :type="courseRoleTagType">
              {{ courseRoleText }}
            </el-tag>
          </div>
        </div>
        <div class="topbar-actions">
          <div v-if="showCourseContext" class="course-context">
            <span>当前课程</span>
            <el-select v-model="currentCourseModel" filterable placeholder="选择课程" :loading="appState.courseAccessLoading">
              <el-option
                v-for="course in appState.courses.records"
                :key="course.id"
                :label="courseLabel(course)"
                :value="course.id"
              />
            </el-select>
            <el-tooltip content="刷新当前页">
              <el-button :icon="Refresh" @click="triggerRefresh">刷新</el-button>
            </el-tooltip>
          </div>
          <el-button :icon="SwitchButton" @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-main class="workspace">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Collection, Files, FolderOpened, Platform, Refresh, SwitchButton, User } from '@element-plus/icons-vue'
import { appState, bootstrapApp, courseLabel, currentCourseId, currentRole, logout, roleLabel, setCurrentCourse, triggerRefresh } from '../state/appState'

const route = useRoute()
const router = useRouter()

const pageTitle = computed(() => typeof route.meta.title === 'string' ? route.meta.title : '工作台')
const showCourseContext = computed(() => Boolean(route.meta.courseScoped))
const currentCourseModel = computed({
  get: () => currentCourseId.value,
  set: (value: number) => {
    void setCurrentCourse(value).then(triggerRefresh)
  }
})
const courseRoleText = computed(() => {
  if (appState.courseAccessLoading) return '权限同步中'
  return `课程身份：${roleLabel(appState.courseAccess?.courseRole)}`
})
const courseRoleTagType = computed(() => {
  const role = appState.courseAccess?.courseRole
  if (role === 'TEACHER') return 'success'
  if (role === 'TA') return 'warning'
  if (role === 'STUDENT') return 'info'
  return ''
})

function menuIcon(path: string) {
  if (path.includes('admin')) return User
  if (path.includes('project')) return Collection
  if (path.includes('course')) return FolderOpened
  return Files
}

function handleLogout() {
  logout()
  router.replace('/login')
}

onMounted(async () => {
  await bootstrapApp()
})
</script>
