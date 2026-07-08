import dayjs from 'dayjs'

const permissionLabels: Record<string, string> = {
  VIEW_COURSE: '查看课程',
  MANAGE_USERS: '管理用户',
  CREATE_COURSE: '创建课程',
  UPDATE_COURSE: '维护课程',
  DELETE_COURSE: '删除课程',
  MANAGE_MEMBERS: '管理成员',
  MANAGE_CONTENT: '维护内容',
  CREATE_NOTICE: '发布通知',
  UPLOAD_RESOURCE: '上传资源',
  CREATE_ASSIGNMENT: '发布作业',
  GRADE_SUBMISSION: '批改提交',
  MANAGE_PROJECT: '管理项目',
  CREATE_DISCUSSION: '参与讨论',
  CREATE_GROUP: '创建项目组',
  JOIN_GROUP: '加入项目组',
  SUBMIT_ASSIGNMENT: '提交作业',
  PUBLISH_SHOWCASE: '发布成果'
}

export function permissionLabel(action: string) {
  return permissionLabels[action] || action
}

export function formatDateTime(value?: string | null) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

export function splitTags(tags?: string | null) {
  return (tags || '')
    .split(/[,，\s]+/)
    .map((tag) => tag.trim())
    .filter(Boolean)
}

export function joinTags(tags: string[]) {
  return [...new Set(tags.map((tag) => tag.trim()).filter(Boolean))].join(',')
}
