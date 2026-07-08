<template>
  <section class="panel" v-loading="loading">
    <div class="section-heading compact">
      <div>
        <h2>课程通知</h2>
        <p>{{ currentCourseLabel }}</p>
      </div>
      <div class="heading-actions">
        <strong>{{ notices.length }} 条</strong>
        <el-button v-if="canManageNotice" type="primary" :icon="Plus" @click="openNoticeDrawer">
          发布通知
        </el-button>
      </div>
    </div>

    <div v-if="!notices.length" class="empty-inline">暂无通知</div>
    <el-timeline v-else class="timeline-list">
      <el-timeline-item v-for="notice in displayNotices" :key="notice.id" :timestamp="formatNoticeTime(notice.createTime)">
        <article class="notice-item" :class="{ pinned: notice.pinned === 1 }">
          <div class="notice-title-row">
            <strong>{{ notice.title }}</strong>
            <el-tag v-if="notice.pinned === 1" size="small" effect="plain">置顶</el-tag>
          </div>
          <p>{{ notice.content }}</p>
          <div v-if="canManageNotice" class="notice-actions">
            <el-button size="small" text :icon="Edit" @click="openEditNotice(notice)">编辑</el-button>
            <el-button size="small" text type="danger" :icon="Delete" @click="deleteNotice(notice)">删除</el-button>
          </div>
        </article>
      </el-timeline-item>
    </el-timeline>

    <WorkspaceDrawer v-model="noticeDrawer" :title="editingNotice ? '编辑通知' : '发布通知'" @closed="resetNoticeForm">
      <el-form :model="noticeForm" label-position="top" class="drawer-form">
        <el-form-item label="标题"><el-input v-model="noticeForm.title" /></el-form-item>
        <el-form-item label="置顶"><el-checkbox v-model="noticePinned">置顶通知</el-checkbox></el-form-item>
        <el-form-item label="内容"><el-input v-model="noticeForm.content" type="textarea" :rows="8" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="noticeDrawer = false">取消</el-button>
          <el-button type="primary" :icon="editingNotice ? Check : Plus" :loading="saving" :disabled="!canCreateNotice" @click="saveNotice">
            {{ editingNotice ? '保存' : '发布' }}
          </el-button>
        </div>
      </template>
    </WorkspaceDrawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete, Edit, Plus } from '@element-plus/icons-vue'
import WorkspaceDrawer from '../components/WorkspaceDrawer.vue'
import { courseService } from '../services/platform'
import { appState, can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import { formatDateTime } from '../utils/display'
import type { Notice } from '../types'

const notices = ref<Notice[]>([])
const noticeDrawer = ref(false)
const noticePinned = ref(false)
const editingNotice = ref<Notice | null>(null)
const loading = ref(false)
const saving = ref(false)
const noticeForm = reactive({ title: '', content: '', pinned: 0, status: 1 })
const canManageNotice = computed(() => can('CREATE_NOTICE'))
const canCreateNotice = computed(() => Boolean(noticeForm.title.trim() && noticeForm.content.trim()))
const displayNotices = computed(() => [...notices.value].sort((a, b) => {
  if (a.pinned !== b.pinned) return b.pinned - a.pinned
  return new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
}))

async function loadNotices() {
  loading.value = true
  try {
    notices.value = await courseService.getNotices(currentCourseId.value)
  } finally {
    loading.value = false
  }
}

function formatNoticeTime(value: string) {
  return formatDateTime(value)
}

function openNoticeDrawer() {
  resetNoticeForm()
  noticeDrawer.value = true
}

function openEditNotice(notice: Notice) {
  editingNotice.value = notice
  noticeForm.title = notice.title
  noticeForm.content = notice.content
  noticeForm.pinned = notice.pinned
  noticeForm.status = 1
  noticePinned.value = notice.pinned === 1
  noticeDrawer.value = true
}

function resetNoticeForm() {
  editingNotice.value = null
  noticeForm.title = ''
  noticeForm.content = ''
  noticeForm.pinned = 0
  noticeForm.status = 1
  noticePinned.value = false
  saving.value = false
}

function noticePayload() {
  return {
    courseId: currentCourseId.value,
    title: noticeForm.title,
    content: noticeForm.content,
    publisherId: appState.session.userId,
    pinned: noticePinned.value ? 1 : 0,
    status: noticeForm.status
  }
}

async function saveNotice() {
  saving.value = true
  try {
    if (editingNotice.value) {
      await courseService.updateNotice(editingNotice.value.id, noticePayload())
      ElMessage.success('通知已更新')
    } else {
      await courseService.createNotice(noticePayload())
      ElMessage.success('通知已发布')
    }
    noticeDrawer.value = false
    await loadNotices()
  } finally {
    saving.value = false
  }
}

async function deleteNotice(notice: Notice) {
  await ElMessageBox.confirm(`确认删除通知「${notice.title}」？`, '删除通知', { type: 'warning' })
  await courseService.deleteNotice(notice.id)
  ElMessage.success('通知已删除')
  await loadNotices()
}

onMounted(loadNotices)
watch([currentCourseId, refreshSignal], () => {
  noticeDrawer.value = false
  resetNoticeForm()
  void loadNotices()
})
</script>
