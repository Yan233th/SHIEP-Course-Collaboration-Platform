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

    <div class="assignment-workspace">
      <section class="assignment-list-panel">
        <el-table
          class="assignment-table"
          :data="assignments"
          :row-class-name="assignmentRowClassName"
          height="calc(100vh - 270px)"
          empty-text="暂无作业"
          @row-click="selectAssignmentByRow"
        >
          <el-table-column prop="title" label="作业" min-width="190" />
          <el-table-column label="截止时间" width="150">
            <template #default="{ row }">{{ formatDueTime(row.dueTime) }}</template>
          </el-table-column>
          <el-table-column label="附件" width="78">
            <template #default="{ row }">
              <el-tag v-if="row.fileId" size="small" effect="plain">有</el-tag>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="totalScore" label="总分" width="78" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text @click.stop="selectAssignment(row.id)">查看</el-button>
              <el-button v-if="canManageAssignment" size="small" text :icon="Edit" @click.stop="openEditAssignmentDrawer(row)">编辑</el-button>
              <el-button v-if="canSubmitAssignment" size="small" type="primary" @click.stop="openSubmissionDrawer(row.id)">提交</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <aside class="assignment-side-panel">
        <div v-if="!selectedAssignment" class="empty-inline">选择左侧作业查看详情</div>
        <template v-else>
          <section class="assignment-detail">
            <div class="assignment-detail-head">
              <div>
                <span>作业内容</span>
                <strong>{{ selectedAssignment.title }}</strong>
              </div>
              <div class="assignment-detail-actions">
                <el-button v-if="canManageAssignment" size="small" text :icon="Edit" @click="openEditAssignmentDrawer(selectedAssignment)">编辑</el-button>
                <el-button v-if="canSubmitAssignment" size="small" type="primary" @click="openSubmissionDrawer(selectedAssignment.id)">提交</el-button>
              </div>
            </div>
            <p class="assignment-description">{{ selectedAssignment.description || '暂无说明' }}</p>
            <div v-if="selectedAssignment.fileId" class="assignment-file-strip">
              <span>作业附件</span>
              <el-link :href="fileUrl(selectedAssignment.fileId, selectedAssignment.file)" target="_blank">
                {{ fileName(selectedAssignment.fileId, selectedAssignment.file) }}
              </el-link>
              <small>{{ formatFileSize(selectedAssignment.file?.sizeBytes) }}</small>
            </div>
            <dl>
              <div>
                <dt>截止时间</dt>
                <dd>{{ formatDueTime(selectedAssignment.dueTime) }}</dd>
              </div>
              <div>
                <dt>总分</dt>
                <dd>{{ selectedAssignment.totalScore }}</dd>
              </div>
            </dl>
          </section>

          <section class="submissions-section">
            <div class="section-heading compact subsection-heading">
              <div>
                <h2>{{ canGradeSubmission ? '提交与批改' : '我的提交' }}</h2>
                <p>{{ selectedAssignment.title }}</p>
              </div>
              <strong>{{ submissions.length }} 条</strong>
            </div>
            <el-table :data="submissions" max-height="300px" empty-text="暂无提交">
              <el-table-column prop="studentId" label="学生ID" width="90" />
              <el-table-column label="内容" min-width="180">
                <template #default="{ row }">
                  <span class="table-ellipsis">{{ row.content || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="附件" width="92">
                <template #default="{ row }">
                  <el-link v-if="row.fileId" :href="fileUrl(row.fileId, row.file)" target="_blank">打开</el-link>
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
        </template>
      </aside>
    </div>

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
              <el-link :href="fileUrl(assignmentForm.fileId, editingAssignment?.file)" target="_blank">
                {{ fileName(assignmentForm.fileId, editingAssignment?.file) }}
              </el-link>
              <el-button size="small" text type="danger" @click="removeAssignmentFile">移除</el-button>
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
        <el-link v-if="selectedSubmission.fileId" :href="fileUrl(selectedSubmission.fileId, selectedSubmission.file)" target="_blank">
          {{ fileName(selectedSubmission.fileId, selectedSubmission.file) }}
        </el-link>
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
import { ElMessage, type UploadFile, type UploadUserFile } from 'element-plus'
import { Check, Edit, Plus, Upload } from '@element-plus/icons-vue'
import WorkspaceDrawer from '../components/WorkspaceDrawer.vue'
import { courseService, fileService } from '../services/platform'
import { appState, can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import { formatDateTime } from '../utils/display'
import type { Assignment, FileBrief, Submission } from '../types'

const assignments = ref<Assignment[]>([])
const submissions = ref<Submission[]>([])
const selectedAssignmentId = ref<number | undefined>(undefined)
const selectedSubmission = ref<Submission | null>(null)
const editingAssignment = ref<Assignment | null>(null)
const assignmentDrawer = ref(false)
const submissionDrawer = ref(false)
const gradeDrawer = ref(false)
const loading = ref(false)
const assignmentSaving = ref(false)
const submissionSaving = ref(false)
const gradeSaving = ref(false)
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

function fileUrl(fileId?: number, file?: FileBrief | null) {
  if (file?.previewUrl) return file.previewUrl
  return fileId ? `/api/files/preview/${fileId}` : ''
}

function fileName(fileId?: number, file?: FileBrief | null) {
  return file?.originalName || (fileId ? `附件 #${fileId}` : '-')
}

function formatFileSize(size?: number) {
  if (!size && size !== 0) return ''
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

async function loadAssignments() {
  loading.value = true
  try {
    assignments.value = await courseService.getAssignments(currentCourseId.value)
    if (assignments.value.length && !assignments.value.some((assignment) => assignment.id === selectedAssignmentId.value)) {
      selectedAssignmentId.value = assignments.value[0].id
    }
    if (!assignments.value.length) {
      selectedAssignmentId.value = undefined
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

function selectAssignmentByRow(row: Assignment) {
  void selectAssignment(row.id)
}

async function selectAssignment(assignmentId: number) {
  selectedAssignmentId.value = assignmentId
  selectedSubmission.value = null
  await loadSubmissions()
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
.assignment-description {
  white-space: pre-wrap;
}

.assignment-file-strip,
.drawer-current-file,
.grading-preview {
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-surface);
}

.assignment-file-strip {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
}

.assignment-file-strip small {
  color: var(--app-muted);
}

.drawer-file-field {
  display: grid;
  gap: 10px;
}

.drawer-current-file {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
</style>
