<template>
  <section class="panel assignments-page" v-loading="loading">
    <div class="section-heading compact">
      <div>
        <h2>作业与提交</h2>
        <p>{{ currentCourseLabel }}</p>
      </div>
      <div class="heading-actions">
        <strong>{{ assignments.length }} 项</strong>
        <el-button v-if="canManageAssignment" type="primary" :icon="Plus" @click="openAssignmentDrawer">
          发布作业
        </el-button>
      </div>
    </div>

    <section class="assignment-list-panel">
      <el-table
        class="assignment-table"
        :data="assignments"
        :row-class-name="assignmentRowClassName"
        height="calc(100vh - 270px)"
        empty-text="暂无作业"
        @row-click="openAssignmentWorkspaceByRow"
      >
        <el-table-column prop="title" label="作业" min-width="240" />
        <el-table-column label="截止时间" width="170">
          <template #default="{ row }">{{ formatDueTime(row.dueTime) }}</template>
        </el-table-column>
        <el-table-column label="附件" width="78">
          <template #default="{ row }">
            <el-tag v-if="row.fileId" size="small" effect="plain">有</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="78" />
        <el-table-column label="操作" width="238" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text @click.stop="openAssignmentWorkspace(row)">详情</el-button>
            <el-button v-if="canManageAssignment" size="small" text :icon="Edit" @click.stop="openEditAssignmentDrawer(row)">编辑</el-button>
            <el-button
              v-if="canManageAssignment"
              size="small"
              text
              type="danger"
              :icon="Delete"
              :loading="assignmentDeletingId === row.id"
              @click.stop="deleteAssignment(row)"
            >
              删除
            </el-button>
            <el-button v-if="canSubmitAssignment" size="small" type="primary" @click.stop="openSubmissionDrawer(row.id)">提交</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <WorkspaceDrawer v-model="assignmentWorkspaceDrawer" title="作业详情" size="min(820px, calc(100vw - 28px))">
      <article v-if="selectedAssignment" class="assignment-content-drawer assignment-workspace-drawer">
        <header>
          <span>作业说明</span>
          <h3>{{ selectedAssignment.title }}</h3>
          <div class="assignment-detail-actions">
            <el-button v-if="canManageAssignment" size="small" text :icon="Edit" @click="openEditAssignmentDrawer(selectedAssignment)">编辑</el-button>
            <el-button
              v-if="canManageAssignment"
              size="small"
              text
              type="danger"
              :icon="Delete"
              :loading="assignmentDeletingId === selectedAssignment.id"
              @click="deleteAssignment(selectedAssignment)"
            >
              删除
            </el-button>
            <el-button v-if="canSubmitAssignment" size="small" type="primary" @click="openSubmissionDrawer(selectedAssignment.id)">提交</el-button>
          </div>
        </header>
        <dl class="assignment-content-meta">
          <div>
            <dt>截止时间</dt>
            <dd>{{ formatDueTime(selectedAssignment.dueTime) }}</dd>
          </div>
          <div>
            <dt>总分</dt>
            <dd>{{ selectedAssignment.totalScore }}</dd>
          </div>
          <div>
            <dt>附件</dt>
            <dd>{{ selectedAssignment.fileId ? '有附件' : '无附件' }}</dd>
          </div>
        </dl>
        <section class="assignment-content-section">
          <h4>说明</h4>
          <p>{{ selectedAssignment.description || '暂无说明' }}</p>
        </section>
        <section class="assignment-content-section">
          <h4>附件</h4>
          <FileActions v-if="selectedAssignment.fileId" :file-id="selectedAssignment.fileId" :file="selectedAssignment.file" />
          <span v-else class="muted">无附件</span>
        </section>

        <section class="assignment-content-section submissions-section">
          <div class="submissions-head">
            <div>
              <h4>{{ canGradeSubmission ? '提交与批改' : '我的提交' }}</h4>
              <p>{{ submissions.length }} 条记录</p>
            </div>
          </div>
          <el-table :data="submissions" max-height="320px" empty-text="暂无提交">
            <el-table-column prop="studentId" label="学生ID" width="90" />
            <el-table-column label="内容" min-width="180">
              <template #default="{ row }">
                <span class="table-ellipsis">{{ row.content || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="附件" min-width="220">
              <template #default="{ row }">
                <FileActions v-if="row.fileId" :file-id="row.fileId" :file="row.file" />
                <span v-else class="muted">-</span>
              </template>
            </el-table-column>
            <el-table-column label="成绩" width="82">
              <template #default="{ row }">{{ row.score ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag effect="plain" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已批改' : '已提交' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="canGradeSubmission" label="操作" width="90">
              <template #default="{ row }">
                <el-button size="small" @click="selectSubmission(row)">批改</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </article>
    </WorkspaceDrawer>

    <WorkspaceDrawer v-model="assignmentDrawer" :title="assignmentDrawerTitle" @closed="resetAssignmentForm">
      <el-form :model="assignmentForm" label-position="top" class="drawer-form">
        <el-form-item label="标题"><el-input v-model="assignmentForm.title" /></el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="assignmentForm.dueTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="总分"><el-input-number v-model="assignmentForm.totalScore" :min="1" :max="1000" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="assignmentForm.description" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="作业附件">
          <div class="drawer-file-field">
            <div v-if="assignmentForm.fileId" class="drawer-current-file">
              <FileActions variant="inline" :file-id="assignmentForm.fileId" :file="editingAssignment?.file" />
              <el-button class="drawer-file-remove" size="small" text type="danger" @click="removeAssignmentFile">移除</el-button>
            </div>
            <el-upload
              action="#"
              :auto-upload="false"
              :limit="1"
              :file-list="assignmentFileList"
              :on-change="handleAssignmentFileChange"
              :on-remove="handleAssignmentFileRemove"
            >
              <el-button :icon="Upload">{{ assignmentForm.fileId ? '替换附件' : '选择附件' }}</el-button>
            </el-upload>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="assignmentDrawer = false">取消</el-button>
          <el-button type="primary" :icon="editingAssignment ? Check : Plus" :loading="assignmentSaving" :disabled="!canSaveAssignment" @click="saveAssignment">
            {{ editingAssignment ? '保存' : '发布' }}
          </el-button>
        </div>
      </template>
    </WorkspaceDrawer>

    <WorkspaceDrawer v-model="submissionDrawer" title="提交作业" @closed="resetSubmissionForm">
      <el-form :model="submissionForm" label-position="top" class="drawer-form">
        <el-form-item label="当前作业">
          <el-input :model-value="submissionAssignmentTitle" disabled />
        </el-form-item>
        <el-form-item label="提交内容"><el-input v-model="submissionForm.content" type="textarea" :rows="8" /></el-form-item>
        <el-form-item label="提交附件">
          <el-upload
            action="#"
            :auto-upload="false"
            :limit="1"
            :file-list="submissionFileList"
            :on-change="handleSubmissionFileChange"
            :on-remove="handleSubmissionFileRemove"
          >
            <el-button :icon="Upload">选择附件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="submissionDrawer = false">取消</el-button>
          <el-button type="primary" :icon="Upload" :loading="submissionSaving" :disabled="!canCreateSubmission" @click="createSubmission">提交</el-button>
        </div>
      </template>
    </WorkspaceDrawer>

    <WorkspaceDrawer v-model="gradeDrawer" title="批改提交" size="460px" @closed="resetGradeForm">
      <div v-if="selectedSubmission" class="grading-preview">
        <span>学生提交</span>
        <p>{{ selectedSubmission.content || '仅提交附件' }}</p>
        <FileActions v-if="selectedSubmission.fileId" :file-id="selectedSubmission.fileId" :file="selectedSubmission.file" />
      </div>
      <el-form :model="gradeForm" label-position="top" class="drawer-form">
        <el-form-item label="分数">
          <el-input-number v-model="gradeForm.score" :min="0" :max="selectedAssignment?.totalScore || 100" :step="1" />
        </el-form-item>
        <el-form-item label="反馈"><el-input v-model="gradeForm.feedback" type="textarea" :rows="6" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="gradeDrawer = false">取消</el-button>
          <el-button type="primary" :icon="Check" :loading="gradeSaving" :disabled="!selectedSubmission" @click="gradeSubmission">保存</el-button>
        </div>
      </template>
    </WorkspaceDrawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile, type UploadUserFile } from 'element-plus'
import { Check, Delete, Edit, Plus, Upload } from '@element-plus/icons-vue'
import FileActions from '../components/FileActions.vue'
import WorkspaceDrawer from '../components/WorkspaceDrawer.vue'
import { courseService, fileService } from '../services/platform'
import { appState, can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import { formatDateTime } from '../utils/display'
import type { Assignment, Submission } from '../types'

const assignments = ref<Assignment[]>([])
const submissions = ref<Submission[]>([])
const selectedAssignmentId = ref<number | undefined>(undefined)
const selectedSubmission = ref<Submission | null>(null)
const editingAssignment = ref<Assignment | null>(null)
const assignmentWorkspaceDrawer = ref(false)
const assignmentDrawer = ref(false)
const submissionDrawer = ref(false)
const gradeDrawer = ref(false)
const loading = ref(false)
const assignmentSaving = ref(false)
const submissionSaving = ref(false)
const gradeSaving = ref(false)
const assignmentDeletingId = ref<number | undefined>(undefined)
const assignmentFile = ref<File | null>(null)
const assignmentFileList = ref<UploadUserFile[]>([])
const submissionFile = ref<File | null>(null)
const submissionFileList = ref<UploadUserFile[]>([])
const assignmentForm = reactive({
  title: '',
  description: '',
  fileId: undefined as number | undefined,
  dueTime: '',
  totalScore: 100,
  status: 1
})
const submissionForm = reactive({
  assignmentId: undefined as number | undefined,
  fileId: undefined as number | undefined,
  content: '',
  status: 0
})
const gradeForm = reactive({ score: 0, feedback: '' })

const canManageAssignment = computed(() => can('CREATE_ASSIGNMENT'))
const canSubmitAssignment = computed(() => can('SUBMIT_ASSIGNMENT'))
const canGradeSubmission = computed(() => can('GRADE_SUBMISSION'))
const selectedAssignment = computed(() => assignments.value.find((assignment) => assignment.id === selectedAssignmentId.value))
const assignmentDrawerTitle = computed(() => editingAssignment.value ? '编辑作业' : '发布作业')
const submissionAssignmentTitle = computed(() => assignments.value.find((assignment) => assignment.id === submissionForm.assignmentId)?.title || '-')
const canSaveAssignment = computed(() => Boolean(assignmentForm.title.trim() && assignmentForm.dueTime && assignmentForm.totalScore > 0))
const canCreateSubmission = computed(() => {
  return Boolean(submissionForm.assignmentId && (submissionForm.content.trim() || submissionFile.value))
})

function formatDueTime(value?: string) {
  return formatDateTime(value)
}

async function loadAssignments() {
  loading.value = true
  try {
    assignments.value = await courseService.getAssignments(currentCourseId.value)
    if (!assignments.value.some((assignment) => assignment.id === selectedAssignmentId.value)) {
      selectedAssignmentId.value = undefined
      assignmentWorkspaceDrawer.value = false
    }
    await loadSubmissions()
  } finally {
    loading.value = false
  }
}

async function loadSubmissions() {
  if (!selectedAssignmentId.value) {
    submissions.value = []
    return
  }
  submissions.value = await courseService.getSubmissions(selectedAssignmentId.value)
}

function openAssignmentWorkspaceByRow(row: Assignment) {
  void openAssignmentWorkspace(row)
}

async function selectAssignment(assignmentId: number) {
  selectedAssignmentId.value = assignmentId
  selectedSubmission.value = null
  await loadSubmissions()
}

async function openAssignmentWorkspace(assignment: Assignment) {
  if (selectedAssignmentId.value !== assignment.id) {
    await selectAssignment(assignment.id)
  }
  assignmentWorkspaceDrawer.value = true
}

function assignmentRowClassName({ row }: { row: Assignment }) {
  return row.id === selectedAssignmentId.value ? 'course-row-current' : ''
}

function openAssignmentDrawer() {
  resetAssignmentForm()
  assignmentDrawer.value = true
}

function openEditAssignmentDrawer(assignment: Assignment) {
  editingAssignment.value = assignment
  assignmentForm.title = assignment.title
  assignmentForm.description = assignment.description || ''
  assignmentForm.fileId = assignment.fileId
  assignmentForm.dueTime = assignment.dueTime
  assignmentForm.totalScore = assignment.totalScore
  assignmentForm.status = 1
  assignmentFile.value = null
  assignmentFileList.value = []
  assignmentDrawer.value = true
}

function resetAssignmentForm() {
  editingAssignment.value = null
  assignmentForm.title = ''
  assignmentForm.description = ''
  assignmentForm.fileId = undefined
  assignmentForm.dueTime = ''
  assignmentForm.totalScore = 100
  assignmentForm.status = 1
  assignmentFile.value = null
  assignmentFileList.value = []
  assignmentSaving.value = false
}

function handleAssignmentFileChange(file: UploadFile) {
  assignmentFileList.value = [file]
  assignmentFile.value = file.raw || null
}

function handleAssignmentFileRemove() {
  assignmentFile.value = null
  assignmentFileList.value = []
}

function removeAssignmentFile() {
  assignmentForm.fileId = undefined
  assignmentFile.value = null
  assignmentFileList.value = []
}

function openSubmissionDrawer(assignmentId: number) {
  selectedAssignmentId.value = assignmentId
  submissionForm.assignmentId = assignmentId
  submissionForm.fileId = undefined
  submissionForm.content = ''
  submissionFile.value = null
  submissionFileList.value = []
  submissionDrawer.value = true
}

function resetSubmissionForm() {
  submissionForm.assignmentId = undefined
  submissionForm.fileId = undefined
  submissionForm.content = ''
  submissionFile.value = null
  submissionFileList.value = []
  submissionSaving.value = false
}

function handleSubmissionFileChange(file: UploadFile) {
  submissionFileList.value = [file]
  submissionFile.value = file.raw || null
}

function handleSubmissionFileRemove() {
  submissionFile.value = null
  submissionFileList.value = []
}

function selectSubmission(submission: Submission) {
  selectedSubmission.value = submission
  gradeForm.score = submission.score ?? 0
  gradeForm.feedback = submission.feedback || ''
  gradeDrawer.value = true
}

function resetGradeForm() {
  selectedSubmission.value = null
  gradeForm.score = 0
  gradeForm.feedback = ''
  gradeSaving.value = false
}

function assignmentPayload(fileId?: number) {
  return {
    courseId: currentCourseId.value,
    title: assignmentForm.title.trim(),
    description: assignmentForm.description,
    fileId,
    dueTime: assignmentForm.dueTime,
    totalScore: assignmentForm.totalScore,
    status: assignmentForm.status
  }
}

async function saveAssignment() {
  assignmentSaving.value = true
  try {
    let fileId = assignmentForm.fileId
    if (assignmentFile.value) {
      const uploaded = await fileService.upload(assignmentFile.value, appState.session.userId, 'assignment')
      fileId = uploaded.id
    }
    const saved = editingAssignment.value
      ? await courseService.updateAssignment(editingAssignment.value.id, assignmentPayload(fileId))
      : await courseService.createAssignment(assignmentPayload(fileId))
    ElMessage.success(editingAssignment.value ? '作业已更新' : '作业已发布')
    assignmentDrawer.value = false
    await loadAssignments()
    await selectAssignment(saved.id)
  } finally {
    assignmentSaving.value = false
  }
}

async function deleteAssignment(assignment: Assignment) {
  await ElMessageBox.confirm(`确认删除作业「${assignment.title}」？该作业下的提交记录也会一并隐藏。`, '删除作业', { type: 'warning' })
  assignmentDeletingId.value = assignment.id
  try {
    await courseService.deleteAssignment(assignment.id)
    ElMessage.success('作业已删除')
    if (selectedAssignmentId.value === assignment.id) {
      selectedAssignmentId.value = undefined
      selectedSubmission.value = null
      assignmentWorkspaceDrawer.value = false
      submissions.value = []
    }
    await loadAssignments()
  } finally {
    assignmentDeletingId.value = undefined
  }
}

async function createSubmission() {
  if (!submissionForm.assignmentId) return
  submissionSaving.value = true
  try {
    let fileId = submissionForm.fileId
    if (submissionFile.value) {
      const uploaded = await fileService.upload(submissionFile.value, appState.session.userId, 'submission')
      fileId = uploaded.id
    }
    await courseService.createSubmission({
      assignmentId: submissionForm.assignmentId,
      studentId: appState.session.userId,
      fileId,
      content: submissionForm.content,
      status: submissionForm.status
    })
    submissionDrawer.value = false
    ElMessage.success('已提交')
    if (selectedAssignmentId.value === submissionForm.assignmentId) {
      await loadSubmissions()
    }
  } finally {
    submissionSaving.value = false
  }
}

async function gradeSubmission() {
  if (!selectedSubmission.value) return
  gradeSaving.value = true
  try {
    await courseService.gradeSubmission(selectedSubmission.value.id, {
      assignmentId: selectedSubmission.value.assignmentId,
      studentId: selectedSubmission.value.studentId,
      fileId: selectedSubmission.value.fileId,
      content: selectedSubmission.value.content,
      score: gradeForm.score,
      feedback: gradeForm.feedback,
      status: 1
    })
    ElMessage.success('批改已保存')
    gradeDrawer.value = false
    await loadSubmissions()
  } finally {
    gradeSaving.value = false
  }
}

onMounted(loadAssignments)
watch([currentCourseId, refreshSignal], () => {
  assignmentWorkspaceDrawer.value = false
  assignmentDrawer.value = false
  submissionDrawer.value = false
  gradeDrawer.value = false
  selectedAssignmentId.value = undefined
  resetAssignmentForm()
  resetSubmissionForm()
  resetGradeForm()
  void loadAssignments()
})
</script>

<style scoped>
.drawer-current-file,
.grading-preview {
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-surface);
}

.drawer-file-field {
  display: grid;
  gap: 10px;
}

.drawer-current-file {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
}

.grading-preview {
  display: grid;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px;
}

.grading-preview span {
  color: var(--app-muted);
  font-size: 12px;
  font-weight: 700;
}

.grading-preview p {
  margin: 0;
  color: var(--app-ink);
  line-height: 1.65;
  white-space: pre-wrap;
}

.assignment-content-meta dt {
  margin-bottom: 4px;
  color: var(--app-muted);
  font-size: 12px;
  font-weight: 600;
}

.assignment-content-meta dd {
  margin: 0;
  color: var(--app-ink-strong);
  font-size: 13px;
  line-height: 1.45;
}

.assignment-content-drawer {
  display: grid;
  gap: 16px;
}

.assignment-content-drawer header {
  display: grid;
  gap: 6px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--app-divider);
}

.assignment-content-drawer header span {
  color: var(--app-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0;
}

.assignment-content-drawer h3,
.assignment-content-drawer h4 {
  margin: 0;
  color: var(--app-ink-strong);
}

.assignment-content-drawer h3 {
  font-size: 20px;
  line-height: 1.35;
}

.assignment-content-drawer h4 {
  font-size: 13px;
}

.assignment-workspace-drawer header {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
}

.assignment-workspace-drawer header span,
.assignment-workspace-drawer header h3 {
  grid-column: 1;
}

.assignment-workspace-drawer .assignment-detail-actions {
  grid-column: 2;
  grid-row: 1 / span 2;
}

.assignment-content-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.assignment-content-meta div,
.assignment-content-section {
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-surface-soft);
}

.assignment-content-meta div {
  padding: 10px 12px;
}

.assignment-content-section {
  display: grid;
  gap: 10px;
  padding: 14px;
}

.assignment-content-section p {
  margin: 0;
  color: var(--app-ink);
  line-height: 1.7;
  white-space: pre-wrap;
}

.submissions-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.submissions-head p {
  margin: 4px 0 0;
  color: var(--app-muted);
  font-size: 12px;
  line-height: 1.4;
}

@media (max-width: 760px) {
  .assignment-content-meta {
    grid-template-columns: 1fr;
  }

  .assignment-workspace-drawer header {
    grid-template-columns: 1fr;
  }

  .assignment-workspace-drawer .assignment-detail-actions {
    grid-column: 1;
    grid-row: auto;
  }
}
</style>
