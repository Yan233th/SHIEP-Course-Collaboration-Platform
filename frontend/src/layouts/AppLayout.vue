<template>
  <el-container class="app-shell">
    <el-aside width="248px" class="sidebar">
      <RouterLink class="brand" to="/dashboard">
        <span class="brand-copy">
          <strong>课程协同</strong>
          <small>Course Workspace</small>
        </span>
      </RouterLink>

      <div class="nav-scroll">
        <el-menu :default-active="route.path" router :collapse-transition="false">
          <el-menu-item index="/dashboard">
            <span class="menu-entry">
              <el-icon><Platform /></el-icon>
              <span>工作台</span>
            </span>
          </el-menu-item>
          <el-sub-menu v-for="menu in appState.menus" :key="menu.path" :index="menu.path">
            <template #title>
              <span class="menu-entry">
                <el-icon><component :is="menuIcon(menu.path)" /></el-icon>
                <span>{{ menu.title }}</span>
              </span>
            </template>
            <el-menu-item v-for="child in menu.children" :key="child.path" :index="child.path">
              <span class="menu-entry submenu-entry">{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>

      <RouterLink class="sidebar-profile" to="/profile">
        <span>当前用户</span>
        <strong>{{ appState.session.realName || appState.session.username }}</strong>
        <small>{{ roleLabel(currentRole) }}</small>
      </RouterLink>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div class="page-title">
          <span class="page-kicker">{{ pageScope }}</span>
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
          <div v-if="showCourseContext" class="course-context" @click.capture="handleCourseContextClick">
            <span class="course-context-label">当前课程</span>
            <el-select
              ref="courseSelectRef"
              v-model="currentCourseModel"
              filterable
              placeholder="选择课程"
              :loading="appState.courseAccessLoading"
              @visible-change="courseSelectOpen = $event"
            >
              <el-option
                v-for="course in appState.courses.records"
                :key="course.id"
                :label="courseLabel(course)"
                :value="course.id"
              />
            </el-select>
            <el-tooltip content="刷新当前页">
              <el-button class="icon-button" :icon="Refresh" aria-label="刷新当前页" @click="triggerRefresh" />
            </el-tooltip>
          </div>
          <el-button class="logout-button" :icon="SwitchButton" @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-main class="workspace">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Collection, Files, FolderOpened, Platform, Refresh, SwitchButton, User } from '@element-plus/icons-vue'
import { appState, bootstrapApp, courseLabel, currentCourseId, currentRole, logout, roleLabel, setCurrentCourse, triggerRefresh } from '../state/appState'

const route = useRoute()
const router = useRouter()
const courseSelectRef = ref<{ blur: () => void; toggleMenu: (event?: Event) => void } | null>(null)
const courseSelectOpen = ref(false)

const pageTitle = computed(() => typeof route.meta.title === 'string' ? route.meta.title : '工作台')
const showCourseContext = computed(() => Boolean(route.meta.courseScoped))
const pageScope = computed(() => {
  if (route.path.startsWith('/admin')) return '平台管理'
  if (showCourseContext.value) return '课程空间'
  return '总览'
})
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

function handleCourseContextClick(event: MouseEvent) {
  const target = event.target as HTMLElement | null
  if (!target || target.closest('.icon-button')) return
  const isCourseTrigger = Boolean(target.closest('.el-select') || target.closest('.course-context-label'))
  if (!isCourseTrigger) return
  event.preventDefault()
  event.stopPropagation()
  event.stopImmediatePropagation()
  if (courseSelectOpen.value) {
    courseSelectRef.value?.blur()
    courseSelectOpen.value = false
    return
  }
  courseSelectRef.value?.toggleMenu(event)
}

onMounted(async () => {
  await bootstrapApp()
})
</script>
