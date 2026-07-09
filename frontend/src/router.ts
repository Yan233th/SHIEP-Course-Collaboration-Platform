import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import AppLayout from './layouts/AppLayout.vue'
import { appState, bootstrapApp, isAuthenticated, LAST_ROUTE_KEY } from './state/appState'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('./views/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'dashboard', component: () => import('./views/DashboardView.vue'), meta: { title: '工作台' } },
      { path: 'profile', name: 'profile', component: () => import('./views/ProfileView.vue'), meta: { title: '个人信息' } },
      { path: 'admin/users', name: 'admin-users', component: () => import('./views/UserManagementView.vue'), meta: { title: '用户与角色', roles: ['ADMIN'] } },
      { path: 'admin/stats', name: 'admin-stats', component: () => import('./views/StatsView.vue'), meta: { title: '课程统计', roles: ['ADMIN'] } },
      { path: 'admin/audit', name: 'admin-audit', component: () => import('./views/AuditView.vue'), meta: { title: '审计日志', roles: ['ADMIN'] } },
      { path: 'courses/list', name: 'courses-list', component: () => import('./views/CourseListView.vue'), meta: { title: '课程空间' } },
      { path: 'courses/members', name: 'courses-members', component: () => import('./views/CourseMembersView.vue'), meta: { title: '成员与权限', courseScoped: true } },
      { path: 'courses/notices', name: 'courses-notices', component: () => import('./views/NoticesView.vue'), meta: { title: '课程通知', courseScoped: true } },
      { path: 'courses/resources', name: 'courses-resources', component: () => import('./views/ResourcesView.vue'), meta: { title: '课程资源', courseScoped: true } },
      { path: 'courses/assignments', name: 'courses-assignments', component: () => import('./views/AssignmentsView.vue'), meta: { title: '作业与提交', courseScoped: true } },
      { path: 'projects/groups', name: 'projects-groups', component: () => import('./views/ProjectGroupsView.vue'), meta: { title: '项目分组', courseScoped: true } },
      { path: 'projects/discussions', name: 'projects-discussions', component: () => import('./views/DiscussionsView.vue'), meta: { title: '讨论交流', courseScoped: true } },
      { path: 'projects/showcases', name: 'projects-showcases', component: () => import('./views/ShowcasesView.vue'), meta: { title: '成果展示', courseScoped: true } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

export const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  if (requiresAuth && !isAuthenticated.value) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (requiresAuth) {
    await bootstrapApp()
    const allowedRoles = to.meta.roles
    if (Array.isArray(allowedRoles) && !allowedRoles.includes(appState.session.roles[0])) {
      return '/dashboard'
    }
  }
  if (to.path === '/login' && isAuthenticated.value) {
    return localStorage.getItem(LAST_ROUTE_KEY) || '/dashboard'
  }
})

router.afterEach((to) => {
  if (to.path !== '/login') {
    localStorage.setItem(LAST_ROUTE_KEY, to.fullPath)
  }
  if (typeof to.meta.title === 'string') {
    document.title = `${to.meta.title} - 课程协同管理平台`
  }
})
