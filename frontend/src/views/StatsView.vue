<template>
  <section class="panel stats-panel">
    <div class="section-heading">
      <div>
        <h2>数据库运行视图</h2>
        <p>统计、审计和文件回收状态来自视图、存储过程与触发器。</p>
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
      <el-button :icon="RefreshRight" @click="runGc">执行文件回收</el-button>
    </div>

    <div class="db-object-grid">
      <article class="db-object-card">
        <span>视图</span>
        <strong>v_course_overview</strong>
        <small>课程总览表格</small>
      </article>
      <article class="db-object-card">
        <span>视图</span>
        <strong>v_assignment_submission_stats</strong>
        <small>作业提交统计</small>
      </article>
      <article class="db-object-card">
        <span>过程</span>
        <strong>sp_course_activity_stats</strong>
        <small>课程活动分布</small>
      </article>
      <article class="db-object-card">
        <span>视图</span>
        <strong>v_file_resource_status</strong>
        <small>文件引用状态</small>
      </article>
      <article class="db-object-card">
        <span>过程</span>
        <strong>sp_file_gc_stats</strong>
        <small>文件回收汇总</small>
      </article>
      <article class="db-object-card">
        <span>触发器</span>
        <strong>audit_history</strong>
        <small>业务操作历史</small>
      </article>
    </div>

    <el-tabs v-model="activeTab" class="stats-tabs">
      <el-tab-pane label="课程概览" name="courses">
        <el-table :data="stats" height="360">
          <el-table-column prop="course_name" label="课程" min-width="220" />
          <el-table-column prop="course_code" label="编号" min-width="150" />
          <el-table-column prop="teacher_name" label="教师" width="110" />
          <el-table-column prop="member_count" label="成员" width="90" />
          <el-table-column prop="resource_count" label="资源" width="90" />
          <el-table-column prop="assignment_count" label="作业" width="90" />
          <el-table-column prop="project_group_count" label="项目组" width="100" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="课程活动" name="activity">
        <div class="activity-grid">
          <article v-for="item in activityStats" :key="item.item_type" class="activity-card">
            <span>{{ activityLabel(item.item_type) }}</span>
            <strong>{{ item.item_count }}</strong>
          </article>
        </div>
        <el-table :data="assignmentStats" height="280">
          <el-table-column prop="title" label="作业" min-width="220" />
          <el-table-column prop="submitted_count" label="已提交" width="100" />
          <el-table-column prop="graded_count" label="已批改" width="100" />
          <el-table-column label="平均分" width="110">
            <template #default="{ row }">{{ formatScore(row.avg_score) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="文件资源" name="files">
        <div class="file-gc-summary">
          <article v-for="item in fileGcStats" :key="item.lifecycle_status" class="gc-chip">
            <span>{{ lifecycleLabel(item.lifecycle_status) }}</span>
            <strong>{{ item.file_count }}</strong>
            <small>{{ formatBytes(Number(item.total_size_bytes || 0)) }}</small>
          </article>
        </div>
        <el-table :data="fileStatuses" height="330">
          <el-table-column prop="original_name" label="文件" min-width="220" />
          <el-table-column prop="biz_type" label="类型" width="120" />
          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <el-tag size="small" effect="plain" :type="lifecycleTagType(row.lifecycle_status)">
                {{ lifecycleLabel(row.lifecycle_status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="active_reference_count" label="有效引用" width="100" />
          <el-table-column prop="pending_gc_count" label="待回收" width="100" />
          <el-table-column label="大小" width="110">
            <template #default="{ row }">{{ formatBytes(Number(row.size_bytes || 0)) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="审计历史" name="audit">
        <el-table :data="auditHistory" height="390">
          <el-table-column prop="create_time" label="时间" width="150">
            <template #default="{ row }">{{ formatDateTime(row.create_time) }}</template>
          </el-table-column>
          <el-table-column prop="table_name" label="表" width="170" />
          <el-table-column prop="action_type" label="动作" width="100" />
          <el-table-column prop="record_id" label="记录ID" width="90" />
          <el-table-column label="快照" min-width="260">
            <template #default="{ row }">
              <span class="table-ellipsis">{{ snapshotText(row.snapshot) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, RefreshRight } from '@element-plus/icons-vue'
import { courseService } from '../services/platform'
import { formatBytes, formatDateTime } from '../utils/display'
import type { ActivityStat, AssignmentSubmissionStat, AuditHistory, CourseStats, FileGcStat, FileResourceStatus } from '../types'

const activeTab = ref('courses')
const selectedCourseId = ref<number>()
const stats = ref<CourseStats[]>([])
const activityStats = ref<ActivityStat[]>([])
const assignmentStats = ref<AssignmentSubmissionStat[]>([])
const auditHistory = ref<AuditHistory[]>([])
const fileStatuses = ref<FileResourceStatus[]>([])
const fileGcStats = ref<FileGcStat[]>([])

async function loadAll() {
  const [courseRows, auditRows, fileRows, gcRows] = await Promise.all([
    courseService.getStats(),
    courseService.getAuditHistory(),
    courseService.getFileStatus(),
    courseService.getFileGcStats()
  ])
  stats.value = courseRows
  auditHistory.value = auditRows
  fileStatuses.value = fileRows
  fileGcStats.value = gcRows
  if (!selectedCourseId.value && courseRows.length > 0) {
    selectedCourseId.value = Number(courseRows[0].course_id)
  }
  await loadCourseStats()
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

async function runGc() {
  const count = await courseService.runFileGc()
  ElMessage.success(`已处理 ${count} 个文件回收任务`)
  await loadAll()
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

function lifecycleLabel(status: string) {
  const labels: Record<string, string> = {
    IN_USE: '使用中',
    PENDING_GC: '待回收',
    ORPHAN: '孤立文件',
    DELETED: '已释放',
    QUEUE_STATUS_0: '队列待处理',
    QUEUE_STATUS_1: '队列处理中',
    QUEUE_STATUS_2: '队列已释放',
    QUEUE_STATUS_3: '仍被引用',
    QUEUE_STATUS_4: '队列失败'
  }
  return labels[status] || status
}

function lifecycleTagType(status: string) {
  if (status === 'IN_USE') return 'success'
  if (status === 'PENDING_GC' || status === 'ORPHAN') return 'warning'
  if (status === 'DELETED') return 'info'
  return ''
}

function formatScore(value?: number | null) {
  return value == null ? '-' : Number(value).toFixed(1)
}

function snapshotText(snapshot: unknown) {
  if (!snapshot) return '-'
  return typeof snapshot === 'string' ? snapshot : JSON.stringify(snapshot)
}

watch(selectedCourseId, loadCourseStats)
onMounted(loadAll)
</script>
