export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface Page<T> {
  total: number
  pageNum: number
  pageSize: number
  records: T[]
}

export interface MenuNode {
  title: string
  path: string
  children: MenuNode[]
}

export interface Session {
  token: string
  userId: number
  username: string
  realName: string
  roles: string[]
}

export type RoleCode = 'ADMIN' | 'TEACHER' | 'TA' | 'STUDENT'
export type CourseRole = 'TEACHER' | 'TA' | 'STUDENT' | null

export interface CourseAccess {
  courseId: number
  systemRole: RoleCode | ''
  courseRole: CourseRole
  actions: string[]
}

export interface CourseMember {
  id: number
  courseId: number
  userId: number
  username: string
  realName: string
  systemRole: RoleCode | ''
  courseRole: Exclude<CourseRole, null>
  status: number
}

export interface Course {
  id: number
  courseCode: string
  courseName: string
  teacherId: number
  teacherName: string
  description?: string
  credit: number
  hours: number
  maxStudents: number
  currentStudents: number
  status: number
}

export interface UserRow {
  id: number
  username: string
  realName: string
  roleCode: RoleCode
  email?: string
  phone?: string
  gender?: 'M' | 'F' | 'U'
  status: number
}

export interface Notice {
  id: number
  courseId: number
  title: string
  content: string
  pinned: number
  createTime: string
}

export interface ResourceItem {
  id: number
  courseId: number
  fileId?: number
  title: string
  category: string
  tags?: string
}

export interface Assignment {
  id: number
  courseId: number
  title: string
  description?: string
  dueTime: string
  totalScore: number
}

export interface Submission {
  id: number
  assignmentId: number
  studentId: number
  fileId?: number
  content?: string
  score?: number
  feedback?: string
  status: number
  createTime?: string
  updateTime?: string
}

export interface ProjectGroup {
  id: number
  courseId: number
  name: string
  topic: string
  leaderId?: number
  currentMembers: number
  maxMembers: number
  status?: number
  createTime?: string
  updateTime?: string
}

export interface ProjectMember {
  id: number
  groupId: number
  userId: number
  roleName: string
  status: number
}

export interface UserBrief {
  id: number
  username: string
  realName: string
  roleCode: string
}

export interface ProjectMemberDetail {
  id: number
  groupId: number
  userId: number
  roleName: string
  status: number
  user?: UserBrief | null
}

export interface Discussion {
  id: number
  courseId: number
  groupId: number
  parentId?: number
  title: string
  content: string
  authorId: number
  authorName?: string
  authorUsername?: string
  createTime?: string
}

export interface Showcase {
  id: number
  courseId: number
  groupId: number
  fileId?: number
  title: string
  summary: string
  linkUrl?: string
  createTime?: string
  updateTime?: string
}

export interface CourseStats {
  course_name: string
  member_count: number
  resource_count: number
  assignment_count: number
  project_group_count: number
}
