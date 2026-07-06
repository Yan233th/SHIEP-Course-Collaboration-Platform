<template>
  <section class="panel split">
    <div class="content-column">
      <div class="section-heading compact">
        <div>
          <h2>课程通知</h2>
          <p>{{ currentCourseLabel }}</p>
        </div>
        <strong>{{ notices.length }} 条</strong>
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
    </div>

    <el-form v-if="can('CREATE_NOTICE')" :model="noticeForm" label-position="top" class="side-form">
      <h3>发布通知</h3>
      <el-form-item label="标题"><el-input v-model="noticeForm.title" /></el-form-item>
      <el-form-item label="置顶"><el-checkbox v-model="noticePinned">置顶通知</el-checkbox></el-form-item>
      <el-form-item label="内容"><el-input v-model="noticeForm.content" type="textarea" :rows="6" /></el-form-item>
      <el-button type="primary" :icon="Plus" :disabled="!canCreateNotice" @click="createNotice">发布</el-button>
    </el-form>
    <aside v-else class="side-form muted-panel">当前角色只能查看课程通知。</aside>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { courseService } from '../services/platform'
import { appState, can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import type { Notice } from '../types'

const notices = ref<Notice[]>([])
const noticePinned = ref(false)
const noticeForm = reactive({ title: '', content: '', pinned: 0, status: 1 })
const canCreateNotice = computed(() => Boolean(noticeForm.title.trim() && noticeForm.content.trim()))

async function loadNotices() {
  notices.value = await courseService.getNotices(currentCourseId.value)
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
  noticeForm.title = ''
  noticeForm.content = ''
  await loadNotices()
}

onMounted(loadNotices)
watch([currentCourseId, refreshSignal], loadNotices)
</script>
