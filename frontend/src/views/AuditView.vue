<template>
  <section class="panel audit-console" v-loading="loading">
    <div class="audit-console-head">
      <div>
        <span>ADMIN CONSOLE</span>
        <h2>审计日志</h2>
        <p>触发器记录业务变化，文件回收队列记录附件释放状态。</p>
      </div>
      <strong>{{ auditHistory.length }} 条记录</strong>
    </div>

    <div class="audit-toolbar">
      <el-select v-model="filters.tableName" placeholder="表" clearable>
        <el-option v-for="item in tableOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.actionType" placeholder="动作" clearable>
        <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-input v-model="recordIdText" placeholder="记录 ID" clearable />
      <el-select v-model="filters.limit" placeholder="数量">
        <el-option :value="30" label="最近 30 条" />
        <el-option :value="60" label="最近 60 条" />
        <el-option :value="100" label="最近 100 条" />
      </el-select>
      <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
      <el-button :icon="RefreshRight" @click="runGc">执行文件回收</el-button>
    </div>

    <el-tabs v-model="activeTab" class="audit-tabs">
      <el-tab-pane label="操作历史" name="history">
        <div class="audit-pane audit-pane-history">
          <el-table :data="auditHistory" height="100%" empty-text="暂无审计记录">
            <el-table-column prop="create_time" label="时间" width="174">
              <template #default="{ row }">
                <span class="audit-time">{{ formatDateTime(row.create_time) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="table_name" label="表" width="126">
              <template #default="{ row }">{{ tableLabel(row.table_name) }}</template>
            </el-table-column>
            <el-table-column prop="action_type" label="动作" width="88">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" :type="actionTagType(row.action_type)">
                  {{ actionLabel(row.action_type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="record_id" label="ID" width="76" />
            <el-table-column label="快照" :min-width="snapshotColumnWidth">
              <template #default="{ row }">
                <code class="snapshot-text">{{ snapshotText(row.snapshot) }}</code>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="文件生命周期" name="files">
        <div class="audit-pane audit-pane-files">
          <div class="file-gc-summary">
            <article v-for="item in fileGcStats" :key="item.lifecycle_status" class="gc-chip">
              <span>{{ lifecycleLabel(item.lifecycle_status) }}</span>
              <strong>{{ item.file_count }}</strong>
              <small>{{ formatBytes(Number(item.total_size_bytes || 0)) }}</small>
            </article>
          </div>
          <el-table :data="fileStatuses" height="100%" empty-text="暂无文件状态">
            <el-table-column prop="original_name" label="文件名(首次)" :min-width="fileNameColumnWidth" />
            <el-table-column label="SHA-256" width="188">
              <template #default="{ row }">
                <el-tooltip :content="displayHash(row) || '-'" placement="top" :disabled="!displayHash(row)">
                  <code class="hash-text">{{ shortHash(displayHash(row)) }}</code>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="biz_type" label="类型" :min-width="fileTypeColumnWidth">
              <template #default="{ row }">{{ bizTypeLabel(row.biz_type) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="106">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" :type="lifecycleTagType(row.lifecycle_status)">
                  {{ lifecycleLabel(row.lifecycle_status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="active_reference_count" label="引用" width="76" />
            <el-table-column prop="pending_gc_count" label="待收" width="76" />
            <el-table-column label="大小" width="92">
              <template #default="{ row }">{{ formatBytes(Number(row.size_bytes || 0)) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, RefreshRight } from '@element-plus/icons-vue'
import { courseService } from '../services/platform'
import { formatBytes, formatDateTime } from '../utils/display'
import type { AuditHistory, FileGcStat, FileResourceStatus } from '../types'

const activeTab = ref('history')
const loading = ref(false)
const recordIdText = ref('')
const filters = reactive({
  tableName: '',
  actionType: '',
  limit: 60
})
const auditHistory = ref<AuditHistory[]>([])
const fileStatuses = ref<FileResourceStatus[]>([])
const fileGcStats = ref<FileGcStat[]>([])

const snapshotColumnWidth = computed(() => {
  return contentColumnWidth(auditHistory.value.map((item) => snapshotText(item.snapshot)), 760, 3200)
})
const fileNameColumnWidth = computed(() => {
  return contentColumnWidth(fileStatuses.value.map((item) => item.original_name), 360, 1400)
})
const fileTypeColumnWidth = computed(() => {
  return contentColumnWidth(fileStatuses.value.map((item) => bizTypeLabel(item.biz_type)), 320, 820)
})

const tableOptions = [
  { label: '作业提交', value: 'assignment_submission' },
  { label: '文件引用', value: 'file_reference' },
  { label: '课程资源', value: 'course_resource' },
  { label: '项目成员', value: 'project_member' }
]

const actionOptions = [
  { label: '更新', value: 'UPDATE' },
  { label: '删除', value: 'DELETE' },
  { label: '批改', value: 'GRADE' },
  { label: '重交', value: 'RESUBMIT' },
  { label: '加入', value: 'JOIN' },
  { label: '释放', value: 'RELEASE' }
]

const recordId = computed(() => {
  const value = Number(recordIdText.value)
  return Number.isInteger(value) && value > 0 ? value : undefined
})

async function loadAll() {
  loading.value = true
  try {
    const [auditRows, fileRows, gcRows] = await Promise.all([
      courseService.getAuditHistory({
        tableName: filters.tableName || undefined,
        actionType: filters.actionType || undefined,
        recordId: recordId.value,
        limit: filters.limit
      }),
      courseService.getFileStatus(),
      courseService.getFileGcStats()
    ])
    auditHistory.value = auditRows
    fileStatuses.value = fileRows
    fileGcStats.value = gcRows
  } finally {
    loading.value = false
  }
}

async function runGc() {
  const count = await courseService.runFileGc()
  ElMessage.success(`已处理 ${count} 个文件回收任务`)
  await loadAll()
}

function tableLabel(tableName: string) {
  const labels: Record<string, string> = {
    assignment_submission: '作业提交',
    file_reference: '文件引用',
    course_resource: '课程资源',
    project_member: '项目成员'
  }
  return labels[tableName] || tableName
}

function actionLabel(action: string) {
  const labels: Record<string, string> = {
    UPDATE: '更新',
    DELETE: '删除',
    GRADE: '批改',
    RESUBMIT: '重交',
    JOIN: '加入',
    RELEASE: '释放'
  }
  return labels[action] || action
}

function actionTagType(action: string) {
  if (action === 'DELETE' || action === 'RELEASE') return 'warning'
  if (action === 'GRADE' || action === 'RESUBMIT') return 'success'
  return 'info'
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

function bizTypeLabel(type?: string) {
  const labels: Record<string, string> = {
    resource: '课程资源',
    assignment: '作业附件',
    submission: '提交附件',
    showcase: '成果附件',
    avatar: '头像',
    course_resource: '课程资源'
  }
  if (!type) return '-'
  return type
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => labels[item.toLowerCase()] || item)
    .filter((item, index, items) => items.indexOf(item) === index)
    .join('、') || '-'
}

function displayHash(row: FileResourceStatus) {
  return row.lifecycle_status === 'DELETED' ? null : row.content_hash
}

function shortHash(hash?: string | null) {
  return hash ? `${hash.slice(0, 16)}...` : '-'
}

function lifecycleTagType(status: string) {
  if (status === 'IN_USE') return 'success'
  if (status === 'PENDING_GC' || status === 'ORPHAN') return 'warning'
  if (status === 'DELETED') return 'info'
  return ''
}

function snapshotText(snapshot: unknown) {
  if (!snapshot) return '-'
  if (typeof snapshot === 'string') {
    try {
      return JSON.stringify(JSON.parse(snapshot))
    } catch {
      return snapshot
    }
  }
  return JSON.stringify(snapshot)
}

function contentColumnWidth(values: Array<string | undefined | null>, min: number, max: number) {
  const width = values.reduce((current, value) => Math.max(current, estimateTextWidth(value || '-')), min)
  return Math.min(width, max)
}

function estimateTextWidth(value: string) {
  let width = 34
  for (const char of value) {
    const code = char.codePointAt(0) || 0
    width += code > 255 ? 14 : 7.5
  }
  return Math.ceil(width)
}

onMounted(loadAll)
</script>

<style scoped>
.audit-console {
  min-width: 0;
  max-width: 100%;
  height: 100%;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  align-content: stretch;
  align-items: stretch;
  gap: 14px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgb(16 22 26 / 4%), transparent 160px),
    var(--app-surface);
}

.audit-console-head {
  min-width: 0;
  max-width: 100%;
  min-height: 78px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 13px 15px;
  border: 1px solid rgb(25 33 38 / 16%);
  border-radius: 8px;
  color: var(--app-on-dark);
  background:
    linear-gradient(135deg, #151c1f 0%, #263036 56%, #3b2d42 100%);
}

.audit-console-head > div {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.audit-console-head span {
  color: rgb(248 250 252 / 56%);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0;
}

.audit-console-head h2 {
  margin: 0;
  color: var(--app-on-dark-strong);
  font-size: 20px;
  line-height: 1.2;
}

.audit-console-head p {
  margin: 0;
  color: var(--app-on-dark-muted);
  font-size: 13px;
  line-height: 1.45;
}

.audit-console-head strong {
  flex: 0 0 auto;
  color: var(--app-on-dark-strong);
  font-size: 13px;
  white-space: nowrap;
}

.audit-toolbar {
  min-width: 0;
  max-width: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding: 6px 8px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-surface-soft);
}

.audit-toolbar :deep(.el-select),
.audit-toolbar :deep(.el-input) {
  flex: 1 1 122px;
  width: auto;
  min-width: 104px;
  max-width: 180px;
}

.audit-toolbar :deep(.el-select__wrapper),
.audit-toolbar :deep(.el-input__wrapper),
.audit-toolbar :deep(.el-button) {
  min-height: 32px;
  height: 32px;
}

.audit-toolbar :deep(.el-button) {
  flex: 0 0 auto;
}

.audit-tabs {
  height: 100%;
  min-height: 0;
  min-width: 0;
  max-width: 100%;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
}

.audit-tabs :deep(.el-tabs__header) {
  min-width: 0;
  margin-bottom: 12px;
}

.audit-tabs :deep(.el-tabs__nav-scroll) {
  padding-left: 22px;
}

.audit-tabs :deep(.el-tabs__content),
.audit-tabs :deep(.el-tab-pane) {
  height: 100%;
  min-width: 0;
  min-height: 0;
  max-width: 100%;
  overflow: hidden;
}

.audit-pane {
  height: 100%;
  min-height: 0;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
}

.audit-pane-files {
  display: grid;
  grid-template-rows: 86px minmax(0, 1fr);
  gap: 10px;
}

.audit-pane-files .file-gc-summary {
  min-height: 0;
  min-width: 0;
  max-width: 100%;
  display: flex;
  gap: 8px;
  margin: 0;
  overflow-x: auto;
  overflow-y: hidden;
}

.audit-pane-files .gc-chip {
  flex: 1 0 126px;
  min-width: 0;
  min-height: 0;
  padding: 10px 12px;
}

.audit-pane :deep(.el-table) {
  width: 100%;
  max-width: 100%;
}

.audit-pane :deep(.el-table .cell) {
  overflow: hidden;
  text-overflow: clip;
  white-space: nowrap;
}

.audit-pane :deep(.el-table__inner-wrapper),
.audit-pane :deep(.el-table__body-wrapper),
.audit-pane :deep(.el-scrollbar),
.audit-pane :deep(.el-scrollbar__wrap) {
  max-width: 100%;
}

.snapshot-text {
  display: block;
  max-width: 100%;
  overflow: hidden;
  color: var(--app-text-soft);
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  line-height: 1.6;
  text-overflow: clip;
  white-space: nowrap;
}

.audit-time {
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.hash-text {
  color: var(--app-text-soft);
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  white-space: nowrap;
}

@media (max-width: 1180px) {
  .audit-toolbar {
    align-items: stretch;
  }

  .audit-toolbar :deep(.el-select),
  .audit-toolbar :deep(.el-input) {
    flex: 1 1 calc(50% - 8px);
    max-width: none;
  }
}

@media (max-width: 760px) {
  .audit-console-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .audit-toolbar :deep(.el-button) {
    flex: 1 1 calc(50% - 8px);
  }
}

@media (max-width: 520px) {
  .audit-toolbar :deep(.el-select),
  .audit-toolbar :deep(.el-input),
  .audit-toolbar :deep(.el-button) {
    flex-basis: 100%;
  }
}
</style>
