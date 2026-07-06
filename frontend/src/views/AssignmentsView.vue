<template>
  <section class="panel split">
    <div class="content-column">
      <div class="section-heading compact">
        <div>
          <h2>作业与提交</h2>
          <p>{{ currentCourseLabel }}</p>
        </div>
        <strong>{{ assignments.length }} 项</strong>
      </div>
      <el-table :data="assignments" height="calc(100vh - 270px)" empty-text="暂无作业">
        <el-table-column prop="title" label="作业" />
        <el-table-column prop="dueTime" label="截止时间" width="190" />
        <el-table-column prop="totalScore" label="总分" width="90" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="selectAssignment(row.id)">查看</el-button>
            <el-button v-if="can('SUBMIT_ASSIGNMENT')" size="small" type="primary" @click="selectAssignment(row.id)">提交</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="side-form">
      <el-form v-if="can('CREATE_ASSIGNMENT')" :model="assignmentForm" label-position="top">
        <h3>发布作业</h3>
        <el-form-item label="标题"><el-input v-model="assignmentForm.title" /></el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="assignmentForm.dueTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="assignmentForm.description" type="textarea" :rows="4" /></el-form-item>
        <el-button type="primary" :icon="Plus" :disabled="!canCreateAssignment" @click="createAssignment">发布作业</el-button>
      </el-form>
      <el-divider v-if="can('CREATE_ASSIGNMENT') && can('SUBMIT_ASSIGNMENT')" />
      <el-form v-if="can('SUBMIT_ASSIGNMENT')" :model="submissionForm" label-position="top">
        <h3>提交作业</h3>
        <el-form-item label="当前作业">
          <el-select v-model="submissionForm.assignmentId" placeholder="选择作业">
            <el-option v-for="assignment in assignments" :key="assignment.id" :label="assignment.title" :value="assignment.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="提交内容"><el-input v-model="submissionForm.content" type="textarea" :rows="4" /></el-form-item>
        <el-button :icon="Upload" :disabled="!canCreateSubmission" @click="createSubmission">提交作业</el-button>
      </el-form>
      <el-divider v-if="submissions.length || can('GRADE_SUBMISSION')" />
      <div v-if="selectedAssignment" class="submission-panel">
        <h3>{{ can('GRADE_SUBMISSION') ? '提交与批改' : '我的提交' }}</h3>
        <el-table :data="submissions" max-height="260px" empty-text="暂无提交">
          <el-table-column prop="studentId" label="学生ID" width="82" />
          <el-table-column label="内容">
            <template #default="{ row }">
              <span class="table-ellipsis">{{ row.content || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="成绩" width="76">
            <template #default="{ row }">{{ row.score ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="82">
            <template #default="{ row }">
              <el-tag effect="plain" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已批改' : '已提交' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="can('GRADE_SUBMISSION')" label="操作" width="82">
            <template #default="{ row }">
              <el-button size="small" @click="selectSubmission(row)">批改</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-form v-if="can('GRADE_SUBMISSION')" :model="gradeForm" label-position="top" class="grade-form">
          <el-form-item label="分数">
            <el-input-number v-model="gradeForm.score" :min="0" :max="selectedAssignment.totalScore" :step="1" />
          </el-form-item>
          <el-form-item label="反馈"><el-input v-model="gradeForm.feedback" type="textarea" :rows="3" /></el-form-item>
          <el-button type="primary" :icon="Check" :disabled="!selectedSubmission" @click="gradeSubmission">保存批改</el-button>
        </el-form>
      </div>
      <div v-if="!can('CREATE_ASSIGNMENT') && !can('SUBMIT_ASSIGNMENT') && !selectedAssignment" class="muted-panel">当前角色只能查看作业。</div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Plus, Upload } from '@element-plus/icons-vue'
import { courseService } from '../services/platform'
import { appState, can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import type { Assignment, Submission } from '../types'

const assignments = ref<Assignment[]>([])
const submissions = ref<Submission[]>([])
const selectedSubmission = ref<Submission | null>(null)
const assignmentForm = reactive({ title: '', description: '', dueTime: '', totalScore: 100, status: 1 })
const submissionForm = reactive({ assignmentId: 1, content: '', status: 0 })
const gradeForm = reactive({ score: 0, feedback: '' })
const selectedAssignment = computed(() => assignments.value.find((assignment) => assignment.id === submissionForm.assignmentId))
const canCreateAssignment = computed(() => Boolean(assignmentForm.title.trim() && assignmentForm.dueTime))
const canCreateSubmission = computed(() => Boolean(submissionForm.assignmentId && submissionForm.content.trim()))

async function loadAssignments() {
  assignments.value = await courseService.getAssignments(currentCourseId.value)
  if (assignments.value.length && !assignments.value.some((assignment) => assignment.id === submissionForm.assignmentId)) {
    submissionForm.assignmentId = assignments.value[0].id
  }
  await loadSubmissions()
}

async function loadSubmissions() {
  if (!submissionForm.assignmentId) {
    submissions.value = []
    return
  }
  submissions.value = await courseService.getSubmissions(submissionForm.assignmentId)
}

function selectAssignment(assignmentId: number) {
  submissionForm.assignmentId = assignmentId
  void loadSubmissions()
}

function selectSubmission(submission: Submission) {
  selectedSubmission.value = submission
  gradeForm.score = submission.score ?? 0
  gradeForm.feedback = submission.feedback || ''
}

async function createAssignment() {
  await courseService.createAssignment({
    courseId: currentCourseId.value,
    ...assignmentForm
  })
  assignmentForm.title = ''
  assignmentForm.description = ''
  assignmentForm.dueTime = ''
  await loadAssignments()
}

async function createSubmission() {
  await courseService.createSubmission({
    assignmentId: submissionForm.assignmentId,
    studentId: appState.session.userId,
    content: submissionForm.content,
    status: submissionForm.status
  })
  submissionForm.content = ''
  ElMessage.success('已提交')
  await loadSubmissions()
}

async function gradeSubmission() {
  if (!selectedSubmission.value) return
  await courseService.gradeSubmission(selectedSubmission.value.id, {
    assignmentId: selectedSubmission.value.assignmentId,
    studentId: selectedSubmission.value.studentId,
    content: selectedSubmission.value.content,
    score: gradeForm.score,
    feedback: gradeForm.feedback,
    status: 1
  })
  ElMessage.success('批改已保存')
  selectedSubmission.value = null
  gradeForm.score = 0
  gradeForm.feedback = ''
  await loadSubmissions()
}

onMounted(loadAssignments)
watch([currentCourseId, refreshSignal], loadAssignments)
watch(() => submissionForm.assignmentId, () => {
  selectedSubmission.value = null
  void loadSubmissions()
})
</script>
