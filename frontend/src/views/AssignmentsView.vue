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
        <el-table-column v-if="can('SUBMIT_ASSIGNMENT')" label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" @click="selectAssignment(row.id)">提交</el-button>
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
        <el-form-item label="作业ID"><el-input-number v-model="submissionForm.assignmentId" :min="1" /></el-form-item>
        <el-form-item label="提交内容"><el-input v-model="submissionForm.content" type="textarea" :rows="4" /></el-form-item>
        <el-button :icon="Upload" :disabled="!canCreateSubmission" @click="createSubmission">提交作业</el-button>
      </el-form>
      <div v-if="!can('CREATE_ASSIGNMENT') && !can('SUBMIT_ASSIGNMENT')" class="muted-panel">当前角色只能查看作业。</div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'
import { courseService } from '../services/platform'
import { appState, can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import type { Assignment } from '../types'

const assignments = ref<Assignment[]>([])
const assignmentForm = reactive({ title: '', description: '', dueTime: '', totalScore: 100, status: 1 })
const submissionForm = reactive({ assignmentId: 1, content: '', status: 0 })
const canCreateAssignment = computed(() => Boolean(assignmentForm.title.trim() && assignmentForm.dueTime))
const canCreateSubmission = computed(() => Boolean(submissionForm.assignmentId && submissionForm.content.trim()))

async function loadAssignments() {
  assignments.value = await courseService.getAssignments(currentCourseId.value)
}

function selectAssignment(assignmentId: number) {
  submissionForm.assignmentId = assignmentId
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
}

onMounted(loadAssignments)
watch([currentCourseId, refreshSignal], loadAssignments)
</script>
