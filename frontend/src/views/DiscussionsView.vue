<template>
  <section class="panel">
    <div class="section-heading compact">
      <div>
        <h2>组内讨论</h2>
        <p>{{ currentCourseLabel }}</p>
      </div>
      <div class="heading-actions">
        <strong v-if="selectedGroupId">{{ discussions.length }} 条</strong>
        <el-select
          v-if="accessibleGroups.length"
          v-model="selectedGroupId"
          placeholder="选择项目组"
          style="width: 200px"
          @change="onGroupChange"
        >
          <el-option v-for="group in accessibleGroups" :key="group.id" :label="group.name" :value="group.id" />
        </el-select>
        <el-button
          v-if="can('CREATE_DISCUSSION') && selectedGroupId"
          type="primary"
          :icon="Plus"
          @click="openDiscussionDrawer()"
        >
          发起讨论
        </el-button>
      </div>
    </div>

    <div v-if="!accessibleGroups.length" class="empty-inline">
      你还没有加入任何项目组，加入后即可在组内讨论。
    </div>
    <div v-else-if="!selectedGroupId" class="empty-inline">请选择一个项目组查看讨论。</div>
    <el-table v-else :data="discussions" height="calc(100vh - 270px)" empty-text="暂无讨论">
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
import type { Discussion, ProjectGroup } from '../types'

const discussions = ref<Discussion[]>([])
const accessibleGroups = ref<ProjectGroup[]>([])
const selectedGroupId = ref<number | undefined>(undefined)
const discussionDrawer = ref(false)
const replyingDiscussion = ref<Discussion | null>(null)
const discussionForm = reactive({
  parentId: undefined as number | undefined,
  title: '',
  content: '',
  status: 1
})
const canCreateDiscussion = computed(() => Boolean((replyingDiscussion.value || discussionForm.title.trim()) && discussionForm.content.trim()))

async function loadAccessibleGroups() {
  accessibleGroups.value = await collaborationService.getAccessibleGroups(currentCourseId.value)
  // 默认选中第一个可访问的组；若当前已选的组不在列表里则重置
  if (!selectedGroupId.value || !accessibleGroups.value.some((group) => group.id === selectedGroupId.value)) {
    selectedGroupId.value = accessibleGroups.value[0]?.id
  }
  await loadDiscussions()
}

async function loadDiscussions() {
  if (!selectedGroupId.value) {
    discussions.value = []
    return
  }
  discussions.value = await collaborationService.getDiscussions(selectedGroupId.value)
}

function onGroupChange() {
  resetDiscussionForm()
  void loadDiscussions()
}

function openDiscussionDrawer(parent?: Discussion) {
  resetDiscussionForm()
  if (parent) {
    replyingDiscussion.value = parent
    discussionForm.parentId = parent.id
    // 回复标题沿用父主题标题，不加"回复："前缀，避免和"回复"标签字样重复
    discussionForm.title = parent.title
  }
  discussionDrawer.value = true
}

function resetDiscussionForm() {
  replyingDiscussion.value = null
  discussionForm.parentId = undefined
  discussionForm.title = ''
  discussionForm.content = ''
  discussionForm.status = 1
}

async function createDiscussion() {
  if (!selectedGroupId.value) return
  await collaborationService.createDiscussion({
    courseId: currentCourseId.value,
    groupId: selectedGroupId.value,
    parentId: discussionForm.parentId,
    title: discussionForm.title,
    content: discussionForm.content,
    status: discussionForm.status
  })
  discussionDrawer.value = false
  resetDiscussionForm()
  await loadDiscussions()
}

onMounted(loadAccessibleGroups)
watch([currentCourseId, refreshSignal], () => {
  discussionDrawer.value = false
  selectedGroupId.value = undefined
  resetDiscussionForm()
  void loadAccessibleGroups()
})
</script>
