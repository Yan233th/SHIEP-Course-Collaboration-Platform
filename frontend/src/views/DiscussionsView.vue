<template>
  <section class="panel">
    <div class="section-heading compact">
      <div>
        <h2>讨论交流</h2>
        <p>{{ currentCourseLabel }}</p>
      </div>
      <div class="heading-actions">
        <strong>{{ discussions.length }} 条</strong>
        <el-button v-if="can('CREATE_DISCUSSION')" type="primary" :icon="Plus" @click="openDiscussionDrawer()">
          发起讨论
        </el-button>
      </div>
    </div>

    <el-table :data="discussions" height="calc(100vh - 270px)" empty-text="暂无讨论">
      <el-table-column label="主题" min-width="220">
        <template #default="{ row }">
          <div class="discussion-title-cell">
            <el-tag v-if="row.parentId" size="small" effect="plain">回复</el-tag>
            <span>{{ row.title }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="内容" min-width="320" />
      <el-table-column prop="authorId" label="作者ID" width="90" />
      <el-table-column v-if="can('CREATE_DISCUSSION')" label="操作" width="90">
        <template #default="{ row }">
          <el-button size="small" @click="openDiscussionDrawer(row)">回复</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer
      v-model="discussionDrawer"
      :title="replyingDiscussion ? '回复讨论' : '发起讨论'"
      append-to-body
      class="workspace-drawer"
      direction="rtl"
      size="440px"
      @closed="resetDiscussionForm"
    >
      <el-form :model="discussionForm" label-position="top" class="drawer-form">
        <div v-if="replyingDiscussion" class="reply-context">
          <span>回复</span>
          <strong>{{ replyingDiscussion.title }}</strong>
        </div>
        <el-form-item v-if="!replyingDiscussion" label="标题"><el-input v-model="discussionForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="discussionForm.content" type="textarea" :rows="8" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="discussionDrawer = false">取消</el-button>
          <el-button type="primary" :icon="Plus" :disabled="!canCreateDiscussion" @click="createDiscussion">发布</el-button>
        </div>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { collaborationService } from '../services/platform'
import { can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import type { Discussion } from '../types'

const discussions = ref<Discussion[]>([])
const discussionDrawer = ref(false)
const replyingDiscussion = ref<Discussion | null>(null)
const discussionForm = reactive({
  groupId: undefined as number | undefined,
  parentId: undefined as number | undefined,
  title: '',
  content: '',
  status: 1
})
const canCreateDiscussion = computed(() => Boolean((replyingDiscussion.value || discussionForm.title.trim()) && discussionForm.content.trim()))

async function loadDiscussions() {
  discussions.value = await collaborationService.getDiscussions(currentCourseId.value)
}

function openDiscussionDrawer(parent?: Discussion) {
  resetDiscussionForm()
  if (parent) {
    replyingDiscussion.value = parent
    discussionForm.groupId = parent.groupId
    discussionForm.parentId = parent.id
    // 不加"回复："前缀，避免和"回复"标签里的字重复；标签已标识为回复
    discussionForm.title = parent.title
  }
  discussionDrawer.value = true
}

function resetDiscussionForm() {
  replyingDiscussion.value = null
  discussionForm.groupId = undefined
  discussionForm.parentId = undefined
  discussionForm.title = ''
  discussionForm.content = ''
  discussionForm.status = 1
}

async function createDiscussion() {
  await collaborationService.createDiscussion({
    courseId: currentCourseId.value,
    groupId: discussionForm.groupId,
    parentId: discussionForm.parentId,
    title: discussionForm.title,
    content: discussionForm.content,
    status: discussionForm.status
  })
  discussionDrawer.value = false
  resetDiscussionForm()
  await loadDiscussions()
}

onMounted(loadDiscussions)
watch([currentCourseId, refreshSignal], () => {
  discussionDrawer.value = false
  resetDiscussionForm()
  void loadDiscussions()
})
</script>
