import { computed, reactive, ref } from 'vue'
import { authService, courseService, userService } from '../services/platform'
import type { Course, CourseAccess, MenuNode, Page, RoleCode, Session } from '../types'

export const SESSION_KEY = 'course-platform-session'
export const TOKEN_KEY = 'course-platform-token'
export const LAST_ROUTE_KEY = 'course-platform-last-route'
export const COURSE_ID_KEY = 'course-platform-current-course-id'

const emptySession: Session = {
  token: '',
  userId: 0,
  username: '',
  realName: '',
  roles: []
}

function readSession(): Session {
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY) || JSON.stringify(emptySession)) as Session
  } catch {
    return { ...emptySession }
  }
}

function readCourseId() {
  const value = Number(localStorage.getItem(COURSE_ID_KEY))
  return Number.isFinite(value) && value > 0 ? value : 1
}

export const appState = reactive({
  session: readSession(),
  menus: [] as MenuNode[],
  courses: {
    total: 0,
    pageNum: 1,
    pageSize: 10,
    records: []
  } as Page<Course>,
  courseAccess: null as CourseAccess | null,
  courseAccessLoading: false,
  bootstrapped: false
})

export const currentCourseId = ref(readCourseId())
export const refreshSignal = ref(0)

export const isAuthenticated = computed(() => Boolean(appState.session.token))
export const currentRole = computed(() => (appState.session.roles[0] || '') as RoleCode | '')
export const selectedCourse = computed(() => appState.courses.records.find((course) => course.id === currentCourseId.value))
export const currentCourseLabel = computed(() => selectedCourse.value ? courseLabel(selectedCourse.value) : `课程 #${currentCourseId.value}`)

export function courseLabel(course: Course) {
  return `${course.courseName} · ${course.courseCode}`
}

export function roleLabel(role?: string | null) {
  const labels: Record<string, string> = {
    ADMIN: '管理员',
    TEACHER: '教师',
    TA: '助教',
    STUDENT: '学生'
  }
  return role ? labels[role] || role : '-'
}

export async function login(payload: { username: string; password: string }) {
  const session = await authService.login(payload)
  Object.assign(appState.session, session)
  localStorage.setItem(TOKEN_KEY, session.token)
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  appState.bootstrapped = false
  await bootstrapApp()
}

export function logout() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(SESSION_KEY)
  localStorage.removeItem(LAST_ROUTE_KEY)
  Object.assign(appState.session, emptySession)
  appState.menus = []
  appState.courses = { total: 0, pageNum: 1, pageSize: 10, records: [] }
  appState.courseAccess = null
  appState.courseAccessLoading = false
  appState.bootstrapped = false
}

export async function bootstrapApp() {
  if (!appState.session.token || appState.bootstrapped) return
  await Promise.all([loadMenus(), loadCourses()])
  await loadCourseAccess()
  appState.bootstrapped = true
}

export async function loadMenus() {
  appState.menus = await userService.getMenus()
}

export async function loadCourses() {
  appState.courses = await courseService.getCourses()
  normalizeCurrentCourse()
}

export async function loadCourseAccess() {
  if (!appState.session.token || !currentCourseId.value || !appState.courses.records.length) {
    appState.courseAccess = null
    return
  }
  appState.courseAccessLoading = true
  try {
    appState.courseAccess = await courseService.getCourseAccess(currentCourseId.value)
  } finally {
    appState.courseAccessLoading = false
  }
}

export async function setCurrentCourse(courseId: number) {
  currentCourseId.value = courseId
  localStorage.setItem(COURSE_ID_KEY, String(courseId))
  appState.courseAccess = null
  await loadCourseAccess()
}

export function triggerRefresh() {
  refreshSignal.value += 1
}

export function hasSystemRole(...roles: RoleCode[]) {
  return roles.includes(currentRole.value as RoleCode)
}

export function can(action: string) {
  return Boolean(appState.courseAccess?.actions.includes(action))
}

function normalizeCurrentCourse() {
  if (!appState.courses.records.length) return
  const exists = appState.courses.records.some((course) => course.id === currentCourseId.value)
  if (!exists) {
    currentCourseId.value = appState.courses.records[0].id
    localStorage.setItem(COURSE_ID_KEY, String(currentCourseId.value))
  }
}
