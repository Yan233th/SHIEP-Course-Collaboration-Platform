<template>
  <section class="panel" v-loading="loading">
    <div class="section-heading">
      <div>
        <h2>课程列表</h2>
      </div>
      <strong>{{ courses.total }} 门</strong>
    </div>

    <div class="panel-toolbar">
      <el-input v-model="query.keyword" placeholder="课程名称/编号/教师" clearable />
      <el-radio-group v-model="query.status">
        <el-radio-button :label="undefined">全部</el-radio-button>
        <el-radio-button :label="1">正常</el-radio-button>
        <el-radio-button :label="0">停开</el-radio-button>
      </el-radio-group>
      <el-button :icon="Search" @click="loadCourses">查询</el-button>
      <el-button v-if="hasSystemRole('ADMIN')" type="primary" :icon="Plus" @click="openCreateDialog">新增课程</el-button>
      <el-button v-if="hasSystemRole('ADMIN')" type="danger" :icon="Delete" :disabled="!selection.length" @click="batchDeleteCourses">批量删除</el-button>
    </div>

    <el-table
      class="course-table"
      :data="courses.records"
      :row-class-name="courseRowClassName"
      @row-click="selectCourse"
      @selection-change="selection = $event"
      height="calc(100vh - 270px)"
      empty-text="暂无课程"
    >
      <el-table-column v-if="hasSystemRole('ADMIN')" type="selection" width="44" />
      <el-table-column prop="courseCode" label="编号" width="150" />
      <el-table-column prop="courseName" label="课程名称" />
      <el-table-column prop="teacherName" label="教师" width="110" />
      <el-table-column prop="credit" label="学分" width="80" />
      <el-table-column prop="hours" label="学时" width="80" />
      <el-table-column prop="currentStudents" label="人数" width="90" />
      <el-table-column label="当前课程" width="92">
        <template #default="{ row }">
          <el-tag v-if="row.id === currentCourseId" type="success">当前</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="canMaintainCourses" label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button v-if="canEditCourse(row)" size="small" @click.stop="openEditDialog(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <WorkspaceDrawer v-model="courseDialog" :title="courseDialogTitle" size="480px" @closed="resetCourseDrawer">
      <el-form :model="courseForm" label-position="top" class="drawer-form">
        <el-form-item label="课程编号"><el-input v-model="courseForm.courseCode" /></el-form-item>
        <el-form-item label="课程名称"><el-input v-model="courseForm.courseName" /></el-form-item>
        <el-form-item label="教师ID"><el-input-number v-model="courseForm.teacherId" :min="1" :disabled="Boolean(editingCourse) && !hasSystemRole('ADMIN')" /></el-form-item>
        <el-form-item label="教师姓名"><el-input v-model="courseForm.teacherName" :disabled="Boolean(editingCourse) && !hasSystemRole('ADMIN')" /></el-form-item>
        <el-form-item label="学分"><el-input-number v-model="courseForm.credit" :min="0.5" :max="10" :step="0.5" /></el-form-item>
        <el-form-item label="学时"><el-input-number v-model="courseForm.hours" :min="1" :max="200" /></el-form-item>
        <el-form-item label="容量"><el-input-number v-model="courseForm.maxStudents" :min="1" :max="500" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="courseForm.status">
            <el-radio-button :label="1">正常</el-radio-button>
            <el-radio-button :label="0">停开</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="简介"><el-input v-model="courseForm.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="courseDialog = false">取消</el-button>
          <el-button type="primary" :loading="savingCourse" :disabled="!canSaveCourse" @click="saveCourse">保存</el-button>
        </div>
      </template>
    </WorkspaceDrawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Plus, Search } from '@element-plus/icons-vue'
import WorkspaceDrawer from '../components/WorkspaceDrawer.vue'
import { courseService, type CourseQuery } from '../services/platform'
import { appState, currentCourseId, hasSystemRole, loadCourses as loadGlobalCourses, setCurrentCourse } from '../state/appState'
import type { Course, Page } from '../types'

interface CourseForm {
  courseCode: string
  courseName: string
  teacherId?: number
  teacherName: string
  description: string
  credit: number
  hours: number
  maxStudents: number
  status: number
}

const query = reactive<CourseQuery>({ keyword: '', status: undefined })
const courses = ref<Page<Course>>({ total: 0, pageNum: 1, pageSize: 10, records: [] })
const selection = ref<Course[]>([])
const courseDialog = ref(false)
const editingCourse = ref<Course | null>(null)
const loading = ref(false)
const savingCourse = ref(false)
const courseForm = reactive<CourseForm>(defaultCourseForm())
const courseDialogTitle = computed(() => editingCourse.value ? '编辑课程' : '新增课程')
const canMaintainCourses = computed(() => hasSystemRole('ADMIN') || courses.value.records.some((course) => canEditCourse(course)))
const canSaveCourse = computed(() => Boolean(courseForm.courseCode.trim() && courseForm.courseName.trim() && courseForm.teacherId))

function defaultCourseForm(): CourseForm {
  return {
    courseCode: '',
    courseName: '',
    teacherId: undefined,
    teacherName: '',
    description: '',
    credit: 3,
    hours: 48,
    maxStudents: 60,
    status: 1
  }
}

async function loadCourses() {
  loading.value = true
  try {
    courses.value = await courseService.getCourses(query)
  } finally {
    loading.value = false
  }
}

function selectCourse(course: Course) {
  if (course.id === currentCourseId.value) return
  void setCurrentCourse(course.id)
  ElMessage.success(`当前课程：${course.courseName}`)
}

function courseRowClassName({ row }: { row: Course }) {
  return row.id === currentCourseId.value ? 'course-row-current' : ''
}

function canEditCourse(course: Course) {
  return hasSystemRole('ADMIN') || course.teacherId === appState.session.userId
}

function openCreateDialog() {
  editingCourse.value = null
  Object.assign(courseForm, defaultCourseForm())
  courseDialog.value = true
}

function openEditDialog(course: Course) {
  editingCourse.value = course
  Object.assign(courseForm, {
    courseCode: course.courseCode,
    courseName: course.courseName,
    teacherId: course.teacherId,
    teacherName: course.teacherName,
    description: course.description || '',
    credit: course.credit,
    hours: course.hours,
    maxStudents: course.maxStudents,
    status: course.status
  })
  courseDialog.value = true
}

function resetCourseDrawer() {
  editingCourse.value = null
  Object.assign(courseForm, defaultCourseForm())
  savingCourse.value = false
}

function coursePayload() {
  if (!courseForm.courseCode || !courseForm.courseName || !courseForm.teacherId) {
    ElMessage.warning('请补全课程编号、名称和教师ID')
    return null
  }
  return {
    courseCode: courseForm.courseCode,
    courseName: courseForm.courseName,
    teacherId: courseForm.teacherId,
    teacherName: courseForm.teacherName,
    description: courseForm.description,
    credit: courseForm.credit,
    hours: courseForm.hours,
    maxStudents: courseForm.maxStudents,
    status: courseForm.status
  }
}

async function saveCourse() {
  const payload = coursePayload()
  if (!payload) return
  savingCourse.value = true
  try {
    if (editingCourse.value) {
      await courseService.updateCourse(editingCourse.value.id, payload)
      ElMessage.success('课程信息已更新')
    } else {
      await courseService.createCourse(payload)
      ElMessage.success('课程已创建')
    }
    courseDialog.value = false
    await Promise.all([loadCourses(), loadGlobalCourses()])
  } finally {
    savingCourse.value = false
  }
}

async function batchDeleteCourses() {
  await courseService.batchDelete(selection.value.map((course) => course.id))
  ElMessage.success('已删除选中课程')
  await Promise.all([loadCourses(), loadGlobalCourses()])
}

onMounted(async () => {
  if (appState.courses.records.length) {
    courses.value = appState.courses
    return
  }
  await loadCourses()
})
</script>
