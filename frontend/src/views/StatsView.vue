<template>
  <section class="panel stats-panel" v-loading="loading">
    <div class="section-heading">
      <div>
        <h2>课程统计</h2>
        <p>课程概览来自视图，课程活动来自存储过程。</p>
      </div>
      <strong>{{ stats.length }} 门课程</strong>
    </div>

    <div class="panel-toolbar stats-toolbar">
      <el-select v-model="selectedCourseId" placeholder="选择课程" filterable>
        <el-option
          v-for="course in stats"
          :key="course.course_id"
          :label="course.course_name"
          :value="Number(course.course_id)"
        />
      </el-select>
      <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" class="stats-tabs">
      <el-tab-pane label="课程概览" name="courses">
        <el-table :data="stats" height="380">
          <el-table-column prop="course_name" label="课程" min-width="220" />
          <el-table-column prop="course_code" label="编号" min-width="150" />
          <el-table-column prop="teacher_name" label="教师" width="110" />
          <el-table-column prop="member_count" label="成员" width="90" />
          <el-table-column prop="resource_count" label="资源" width="90" />
          <el-table-column prop="assignment_count" label="作业" width="90" />
          <el-table-column prop="project_group_count" label="项目组" width="100" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="活动与作业" name="activity">
        <div class="activity-grid">
          <article v-for="item in activityStats" :key="item.item_type" class="activity-card">
            <span>{{ activityLabel(item.item_type) }}</span>
            <strong>{{ item.item_count }}</strong>
          </article>
        </div>
        <el-table :data="assignmentStats" height="300" empty-text="暂无作业统计">
          <el-table-column prop="title" label="作业" min-width="220" />
          <el-table-column prop="submitted_count" label="已提交" width="100" />
          <el-table-column prop="graded_count" label="已批改" width="100" />
          <el-table-column label="平均分" width="110">
            <template #default="{ row }">{{ formatScore(row.avg_score) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { courseService } from '../services/platform'
import type { ActivityStat, AssignmentSubmissionStat, CourseStats } from '../types'

const activeTab = ref('courses')
const selectedCourseId = ref<number>()
const loading = ref(false)
const stats = ref<CourseStats[]>([])
const activityStats = ref<ActivityStat[]>([])
const assignmentStats = ref<AssignmentSubmissionStat[]>([])

async function loadAll() {
  loading.value = true
  try {
    const courseRows = await courseService.getStats()
    stats.value = courseRows
    const selectedExists = courseRows.some((course) => Number(course.course_id) === selectedCourseId.value)
    if (!selectedExists) {
      selectedCourseId.value = courseRows.length > 0 ? Number(courseRows[0].course_id) : undefined
    }
    await loadCourseStats()
  } finally {
    loading.value = false
  }
}

async function loadCourseStats() {
  if (!selectedCourseId.value) {
    activityStats.value = []
    assignmentStats.value = []
    return
  }
  const [activityRows, assignmentRows] = await Promise.all([
    courseService.getActivityStats(selectedCourseId.value),
    courseService.getAssignmentSubmissionStats(selectedCourseId.value)
  ])
  activityStats.value = activityRows
  assignmentStats.value = assignmentRows
}

function activityLabel(type: string) {
  const labels: Record<string, string> = {
    notice: '通知',
    resource: '资源',
    assignment: '作业',
    discussion: '讨论',
    showcase: '成果'
  }
  return labels[type] || type
}

function formatScore(value?: number | null) {
  return value == null ? '-' : Number(value).toFixed(1)
}

watch(selectedCourseId, () => {
  void loadCourseStats()
})
onMounted(loadAll)
</script>
