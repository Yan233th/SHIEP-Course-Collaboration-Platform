<template>
  <section class="panel">
    <div class="section-heading compact">
      <div>
        <h2>课程通知</h2>
        <p>{{ currentCourseLabel }}</p>
      </div>
      <div class="heading-actions">
        <strong>{{ notices.length }} 条</strong>
        <el-button v-if="can('CREATE_NOTICE')" type="primary" :icon="Plus" @click="openNoticeDrawer">
          发布通知
        </el-button>
      </div>
    </div>

    <div v-if="!notices.length" class="empty-inline">暂无通知</div>
    <el-timeline v-else class="timeline-list">
      <el-timeline-item v-for="notice in notices" :key="notice.id" :timestamp="notice.createTime">
        <article class="notice-item">
          <strong>{{ notice.title }}</strong>
          <p>{{ notice.content }}</p>
        </article>
      </el-timeline-item>
    </el-timeline>

    <el-drawer
      v-model="noticeDrawer"
      title="发布通知"
      append-to-body
      class="workspace-drawer"
      direction="rtl"
      size="440px"
      @closed="resetNoticeForm"
    >
      <el-form :model="noticeForm" label-position="top" class="drawer-form">
        <el-form-item label="标题"><el-input v-model="noticeForm.title" /></el-form-item>
        <el-form-item label="置顶"><el-checkbox v-model="noticePinned">置顶通知</el-checkbox></el-form-item>
        <el-form-item label="内容"><el-input v-model="noticeForm.content" type="textarea" :rows="8" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="noticeDrawer = false">取消</el-button>
          <el-button type="primary" :icon="Plus" :disabled="!canCreateNotice" @click="createNotice">发布</el-button>
        </div>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { courseService } from '../services/platform'
import { appState, can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import type { Notice } from '../types'

const notices = ref<Notice[]>([])
const noticeDrawer = ref(false)
const noticePinned = ref(false)
const noticeForm = reactive({ title: '', content: '', pinned: 0, status: 1 })
const canCreateNotice = computed(() => Boolean(noticeForm.title.trim() && noticeForm.content.trim()))

async function loadNotices() {
  notices.value = await courseService.getNotices(currentCourseId.value)
}

function openNoticeDrawer() {
  resetNoticeForm()
  noticeDrawer.value = true
}

function resetNoticeForm() {
  noticeForm.title = ''
  noticeForm.content = ''
  noticePinned.value = false
}

async function createNotice() {
  await courseService.createNotice({
    courseId: currentCourseId.value,
    title: noticeForm.title,
    content: noticeForm.content,
    publisherId: appState.session.userId,
    pinned: noticePinned.value ? 1 : 0,
    status: noticeForm.status
  })
  noticeDrawer.value = false
  resetNoticeForm()
  await loadNotices()
}

onMounted(loadNotices)
watch([currentCourseId, refreshSignal], () => {
  noticeDrawer.value = false
  resetNoticeForm()
  void loadNotices()
})
</script>
