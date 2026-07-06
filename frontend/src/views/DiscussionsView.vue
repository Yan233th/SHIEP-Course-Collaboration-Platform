<template>
  <section class="panel split">
    <div class="content-column">
      <div class="section-heading compact">
        <div>
          <h2>讨论交流</h2>
          <p>{{ currentCourseLabel }}</p>
        </div>
        <strong>{{ discussions.length }} 条</strong>
      </div>
      <el-table :data="discussions" height="calc(100vh - 270px)" empty-text="暂无讨论">
        <el-table-column prop="title" label="主题" />
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="authorId" label="作者ID" width="90" />
      </el-table>
    </div>

    <el-form v-if="can('CREATE_DISCUSSION')" :model="discussionForm" label-position="top" class="side-form">
      <h3>发起讨论</h3>
      <el-form-item label="标题"><el-input v-model="discussionForm.title" /></el-form-item>
      <el-form-item label="内容"><el-input v-model="discussionForm.content" type="textarea" :rows="7" /></el-form-item>
      <el-button type="primary" :icon="Plus" :disabled="!canCreateDiscussion" @click="createDiscussion">发布讨论</el-button>
    </el-form>
    <aside v-else class="side-form muted-panel">当前角色只能查看讨论。</aside>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { collaborationService } from '../services/platform'
import { appState, can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import type { Discussion } from '../types'

const discussions = ref<Discussion[]>([])
const discussionForm = reactive({ groupId: 1, parentId: undefined as number | undefined, title: '', content: '', status: 1 })
const canCreateDiscussion = computed(() => Boolean(discussionForm.title.trim() && discussionForm.content.trim()))

async function loadDiscussions() {
  discussions.value = await collaborationService.getDiscussions(currentCourseId.value)
}

async function createDiscussion() {
  await collaborationService.createDiscussion({
    courseId: currentCourseId.value,
    groupId: discussionForm.groupId,
    parentId: discussionForm.parentId,
    authorId: appState.session.userId,
    title: discussionForm.title,
    content: discussionForm.content,
    status: discussionForm.status
  })
  discussionForm.title = ''
  discussionForm.content = ''
  await loadDiscussions()
}

onMounted(loadDiscussions)
watch([currentCourseId, refreshSignal], loadDiscussions)
</script>
