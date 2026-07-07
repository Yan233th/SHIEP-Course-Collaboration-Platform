import { http, unwrap } from './http'
import type {
  Assignment,
  Course,
  CourseAccess,
  CourseMember,
  CourseStats,
  Discussion,
  MenuNode,
  Notice,
  Page,
  ProjectGroup,
  ProjectMember,
  ResourceItem,
  Session,
  Showcase,
  Submission,
  UserRow
} from '../types'

export interface UserQuery {
  keyword?: string
  roleCode?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface CourseQuery {
  keyword?: string
  status?: number
}

export interface CourseMemberPayload {
  userId: number
  roleCode: 'TEACHER' | 'TA' | 'STUDENT'
  status: number
}

export interface UserPayload {
  username: string
  password?: string
  realName: string
  roleCode: 'ADMIN' | 'TEACHER' | 'TA' | 'STUDENT'
  email?: string
  phone?: string
  gender: 'M' | 'F' | 'U'
  status: number
}

export interface ProfilePayload {
  realName: string
  email?: string
  phone?: string
  gender: 'M' | 'F' | 'U'
}

export const authService = {
  login(payload: { username: string; password: string }) {
    return unwrap<Session>(http.post('/auth/login', payload))
  }
}

export const userService = {
  getMenus() {
    return unwrap<MenuNode[]>(http.get('/users/menus'))
  },
  getCurrentUser() {
    return unwrap<UserRow>(http.get('/users/me'))
  },
  getUsers(params: UserQuery) {
    return unwrap<Page<UserRow>>(http.get('/users', { params }))
  },
  getUserOptions(params: UserQuery) {
    return unwrap<Page<UserRow>>(http.get('/users/options', { params }))
  },
  createUser(payload: UserPayload) {
    return unwrap<UserRow>(http.post('/users', payload))
  },
  updateUser(id: number, payload: UserPayload) {
    return unwrap<UserRow>(http.put(`/users/${id}`, payload))
  },
  updateProfile(payload: ProfilePayload) {
    return unwrap<UserRow>(http.put('/users/me', payload))
  },
  batchDelete(ids: number[]) {
    return unwrap<void>(http.delete('/users/batch', { data: ids }))
  }
}

export const courseService = {
  getCourses(params: CourseQuery = {}) {
    return unwrap<Page<Course>>(http.get('/courses', { params }))
  },
  getCourseAccess(courseId: number) {
    return unwrap<CourseAccess>(http.get(`/courses/${courseId}/access`))
  },
  createCourse(payload: Partial<Course>) {
    return unwrap<Course>(http.post('/courses', payload))
  },
  updateCourse(id: number, payload: Partial<Course>) {
    return unwrap<Course>(http.put(`/courses/${id}`, payload))
  },
  batchDelete(ids: number[]) {
    return unwrap<void>(http.delete('/courses/batch', { data: ids }))
  },
  getMembers(courseId: number) {
    return unwrap<CourseMember[]>(http.get(`/courses/${courseId}/members`))
  },
  saveMember(courseId: number, payload: CourseMemberPayload) {
    return unwrap<CourseMember>(http.post(`/courses/${courseId}/members`, payload))
  },
  updateMember(courseId: number, memberId: number, payload: CourseMemberPayload) {
    return unwrap<CourseMember>(http.put(`/courses/${courseId}/members/${memberId}`, payload))
  },
  deleteMember(courseId: number, memberId: number) {
    return unwrap<void>(http.delete(`/courses/${courseId}/members/${memberId}`))
  },
  getNotices(courseId: number) {
    return unwrap<Notice[]>(http.get('/notices', { params: { courseId } }))
  },
  createNotice(payload: { courseId: number; title: string; content: string; publisherId: number; pinned: number; status: number }) {
    return unwrap<Notice>(http.post('/notices', payload))
  },
  getResources(params: { courseId: number; category?: string; tag?: string }) {
    return unwrap<ResourceItem[]>(http.get('/resources', { params }))
  },
  createResource(payload: Record<string, unknown>) {
    return unwrap<ResourceItem>(http.post('/resources', payload))
  },
  getAssignments(courseId: number) {
    return unwrap<Assignment[]>(http.get('/assignments', { params: { courseId } }))
  },
  createAssignment(payload: Record<string, unknown>) {
    return unwrap<Assignment>(http.post('/assignments', payload))
  },
  getSubmissions(assignmentId: number) {
    return unwrap<Submission[]>(http.get('/submissions', { params: { assignmentId } }))
  },
  createSubmission(payload: Record<string, unknown>) {
    return unwrap<Submission>(http.post('/submissions', payload))
  },
  gradeSubmission(id: number, payload: Record<string, unknown>) {
    return unwrap<Submission>(http.put(`/submissions/${id}/grade`, payload))
  },
  getStats() {
    return unwrap<CourseStats[]>(http.get('/stats/course-overview'))
  }
}

export const collaborationService = {
  getGroups(courseId: number) {
    return unwrap<ProjectGroup[]>(http.get('/projects/groups', { params: { courseId } }))
  },
  createGroup(payload: Record<string, unknown>) {
    return unwrap<ProjectGroup>(http.post('/projects/groups', payload))
  },
  joinGroup(groupId: number, payload: { userId: number; roleName: string }) {
    return unwrap<void>(http.post(`/projects/groups/${groupId}/join`, payload))
  },
  leaveGroup(groupId: number) {
    return unwrap<void>(http.delete(`/projects/groups/${groupId}/members/me`))
  },
  getGroupMembers(groupId: number) {
    return unwrap<ProjectMember[]>(http.get(`/projects/groups/${groupId}/members`))
  },
  getDiscussions(courseId: number) {
    return unwrap<Discussion[]>(http.get('/discussions', { params: { courseId } }))
  },
  createDiscussion(payload: Record<string, unknown>) {
    return unwrap<Discussion>(http.post('/discussions', payload))
  },
  getShowcases(courseId: number) {
    return unwrap<Showcase[]>(http.get('/showcases', { params: { courseId } }))
  },
  createShowcase(payload: Record<string, unknown>) {
    return unwrap<Showcase>(http.post('/showcases', payload))
  }
}

export const fileService = {
  upload(file: File, uploaderId: number, bizType: string) {
    const form = new FormData()
    form.append('file', file)
    form.append('uploaderId', String(uploaderId))
    form.append('bizType', bizType)
    return unwrap<{ id: number; originalName: string }>(http.post('/files/upload', form))
  }
}
