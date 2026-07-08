<template>
  <section class="panel dz-board">
    <div class="section-heading compact">
      <div>
        <h2>组内讨论</h2>
        <p>{{ currentCourseLabel }}</p>
      </div>
      <div class="heading-actions">
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
          @click="openTopicDrawer"
        >
          发起话题
        </el-button>
      </div>
    </div>

    <div v-if="!accessibleGroups.length" class="empty-inline">
      你还没有加入任何项目组，加入后即可在组内讨论。
    </div>
    <div v-else-if="!selectedGroupId" class="empty-inline">请选择一个项目组。</div>
    <div v-else class="dz-split">
      <!-- 左：话题总列表 -->
      <aside class="dz-list">
        <div class="dz-list-head">话题 · {{ topicList.length }}</div>
        <div class="dz-list-body">
          <button
            v-for="topic in topicList"
            :key="topic.id"
            class="dz-list-item"
            :class="{ active: topic.id === selectedTopicId }"
            :data-topic-id="topic.id"
            @click="selectTopic(topic.id)"
          >
            <div class="dz-list-title">{{ topic.title }}</div>
            <div class="dz-list-meta">{{ authorName(topic) }} · {{ repliesOf(topic.id).length }} 回复</div>
          </button>
          <div v-if="!topicList.length" class="empty-inline">暂无话题，发起第一个吧。</div>
        </div>
      </aside>

      <!-- 右：选中帖子的内容 -->
      <section class="dz-read">
        <div v-if="!activeTopic" class="dz-read-empty">选择左侧话题查看内容</div>
        <div v-else class="dz-read-inner">
          <header class="dz-read-head">
            <h3>{{ activeTopic.title }}</h3>
            <span>{{ authorName(activeTopic) }}</span>
          </header>
          <div class="dz-read-body">
            <article class="dz-post">
              <p>{{ activeTopic.content }}</p>
            </article>
            <div class="dz-replies-head">楼层回复 · {{ repliesOf(activeTopic.id).length }}</div>
            <div class="dz-replies">
              <div v-for="(reply, i) in repliesOf(activeTopic.id)" :key="reply.id" class="dz-floor">
                <div class="dz-floor-no">{{ i + 1 }}</div>
                <div class="dz-floor-body">
                  <div class="dz-floor-meta">{{ authorName(reply) }}</div>
                  <p>{{ reply.content }}</p>
                </div>
              </div>
              <div v-if="!repliesOf(activeTopic.id).length" class="dz-empty">还没有回复，来说点什么</div>
            </div>
          </div>
          <footer v-if="can('CREATE_DISCUSSION')" class="dz-reply">
            <el-input v-model="replyContent" type="textarea" :rows="2" placeholder="回复该话题…" />
            <el-button type="primary" :disabled="!replyContent.trim()" @click="submitReply">回复</el-button>
          </footer>
        </div>
      </section>
    </div>

    <!-- 新话题抽屉 -->
    <el-drawer
      v-model="topicDrawer"
      title="发起话题"
      append-to-body
      class="workspace-drawer"
      direction="rtl"
      size="440px"
    >
      <el-form :model="topicForm" label-position="top" class="drawer-form">
        <el-form-item label="标题"><el-input v-model="topicForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="topicForm.content" type="textarea" :rows="8" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="topicDrawer = false">取消</el-button>
          <el-button type="primary" :icon="Plus" :disabled="!canCreateTopic" @click="submitTopic">发布</el-button>
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
const selectedTopicId = ref<number | undefined>(undefined)
const topicDrawer = ref(false)
const topicForm = reactive({ title: '', content: '' })
const replyContent = ref('')
const canCreateTopic = computed(() => Boolean(topicForm.title.trim() && topicForm.content.trim()))

// 话题 = 顶层帖；回复按根话题归拢（兼容历史"回复的回复"，统一两级展示）
const topicList = computed(() => discussions.value.filter((d) => !d.parentId))
const activeTopic = computed(() => topicList.value.find((t) => t.id === selectedTopicId.value))
const repliesByRoot = computed(() => {
  const byId = new Map(discussions.value.map((d) => [d.id, d]))
  const map = new Map<number, Discussion[]>()
  for (const d of discussions.value) {
    if (!d.parentId) continue
    let cur = d
    let guard = 0
    while (cur.parentId && guard++ < 50) {
      const parent = byId.get(cur.parentId)
      if (!parent) break
      cur = parent
    }
    const rootId = cur.id
    if (!map.has(rootId)) map.set(rootId, [])
    map.get(rootId)!.push(d)
  }
  return map
})
function repliesOf(topicId?: number): Discussion[] {
  if (!topicId) return []
  return (repliesByRoot.value.get(topicId) || [])
    .slice()
    .sort((a, b) => String(a.createTime || '').localeCompare(String(b.createTime || '')))
}
function authorName(d: Discussion) {
  return d.authorName || d.authorUsername || `用户${d.authorId}`
}

async function loadAccessibleGroups() {
  accessibleGroups.value = await collaborationService.getAccessibleGroups(currentCourseId.value)
  if (!selectedGroupId.value || !accessibleGroups.value.some((g) => g.id === selectedGroupId.value)) {
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
  // 默认不预选话题；仅当前选中失效时置空，由用户点选
  if (selectedTopicId.value && !topicList.value.some((t) => t.id === selectedTopicId.value)) {
    selectedTopicId.value = undefined
  }
}

function onGroupChange() {
  selectedTopicId.value = undefined
  void loadDiscussions()
}

function selectTopic(id: number) {
  selectedTopicId.value = id
  replyContent.value = ''
}

function openTopicDrawer() {
  topicForm.title = ''
  topicForm.content = ''
  topicDrawer.value = true
}

async function submitTopic() {
  if (!selectedGroupId.value || !canCreateTopic.value) return
  await collaborationService.createDiscussion({
    courseId: currentCourseId.value,
    groupId: selectedGroupId.value,
    parentId: undefined,
    title: topicForm.title,
    content: topicForm.content,
    status: 1
  })
  topicDrawer.value = false
  await loadDiscussions()
}

async function submitReply() {
  if (!selectedTopicId.value || !selectedGroupId.value || !replyContent.value.trim()) return
  await collaborationService.createDiscussion({
    courseId: currentCourseId.value,
    groupId: selectedGroupId.value,
    parentId: selectedTopicId.value,
    title: activeTopic.value?.title || '',
    content: replyContent.value,
    status: 1
  })
  replyContent.value = ''
  await loadDiscussions()
}

onMounted(() => {
  void loadAccessibleGroups()
})
watch([currentCourseId, refreshSignal], () => {
  selectedTopicId.value = undefined
  selectedGroupId.value = undefined
  topicDrawer.value = false
  void loadAccessibleGroups()
})
</script>

<style scoped>
.dz-split {
  display: flex;
  gap: 14px;
  height: calc(100vh - 230px);
}

.dz-list {
  width: 320px;
  flex: 0 0 320px;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color, #e5e7eb);
  border-radius: 12px;
  background: var(--el-bg-color, #fff);
  overflow: hidden;
}
.dz-list-head {
  padding: 12px 14px;
  font-weight: 600;
  font-size: 13px;
  color: #6b7280;
  border-bottom: 1px solid #eef0f4;
}
.dz-list-body {
  position: relative;
  overflow: auto;
  flex: 1;
  padding: 6px;
}
.dz-list-item {
  position: relative;
  z-index: 1;
  display: block;
  width: 100%;
  text-align: left;
  background: transparent;
  border: 0;
  border-radius: 10px;
  padding: 10px 30px 10px 12px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: color 0.18s ease;
}
.dz-list-item::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  border: 1px solid rgb(var(--app-brand-rgb) / 12%);
  border-radius: 10px;
  background: transparent;
  opacity: 0;
  pointer-events: none;
  transition:
    opacity 0.2s ease,
    border-color 0.2s ease,
    background-color 0.2s ease,
    transform 0.24s cubic-bezier(0.33, 1, 0.68, 1);
}
.dz-list-item::after {
  content: "";
  position: absolute;
  top: 50%;
  right: 13px;
  z-index: 1;
  width: 6px;
  height: 6px;
  border-right: 1px solid var(--app-brand-deep);
  border-bottom: 1px solid var(--app-brand-deep);
  opacity: 0;
  transform: translate(-2px, -50%) rotate(-45deg);
  transition:
    opacity 0.2s ease,
    transform 0.24s cubic-bezier(0.33, 1, 0.68, 1);
}
.dz-list-title,
.dz-list-meta {
  position: relative;
  z-index: 1;
  transition:
    color 0.18s ease,
    transform 0.24s cubic-bezier(0.33, 1, 0.68, 1);
}
.dz-list-item:not(.active):hover {
  background: transparent;
}
.dz-list-item:not(.active):hover::before {
  opacity: 1;
  border-color: var(--app-brand-soft-strong);
  background: rgb(255 255 255 / 42%);
}
.dz-list-item.active {
  background: transparent;
}
.dz-list-item.active::before {
  opacity: 1;
  border-color: rgb(var(--app-brand-rgb) / 18%);
  background: rgb(var(--app-brand-rgb) / 12%);
  transform: translateX(3px);
}
.dz-list-item.active::after {
  opacity: 0.45;
  transform: translate(3px, -50%) rotate(-45deg);
}
.dz-list-item.active .dz-list-title {
  color: var(--app-brand-deep);
  font-weight: 650;
  transform: translateX(4px);
}
.dz-list-item.active .dz-list-meta {
  color: var(--app-muted);
  transform: translateX(4px);
}
.dz-list-title {
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dz-list-meta {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 3px;
}

.dz-read {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color, #e5e7eb);
  border-radius: 12px;
  background: var(--el-bg-color, #fff);
  overflow: hidden;
}
.dz-read-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
}
.dz-read-inner {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.dz-read-head {
  padding: 14px 18px;
  border-bottom: 1px solid #eef0f4;
}
.dz-read-head h3 {
  margin: 0 0 2px;
  font-size: 16px;
}
.dz-read-head span {
  font-size: 12px;
  color: #9ca3af;
}
.dz-read-body {
  flex: 1;
  overflow: auto;
  padding: 14px 18px;
}
.dz-post {
  padding: 4px 0 14px;
  border-bottom: 1px dashed #eef0f4;
}
.dz-post p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.7;
}
.dz-replies-head {
  font-size: 12px;
  color: #6b7280;
  margin: 14px 0 10px;
}
.dz-replies {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.dz-floor {
  display: flex;
  gap: 10px;
}
.dz-floor-no {
  width: 24px;
  height: 24px;
  flex: 0 0 24px;
  border-radius: 50%;
  background: #eef1f5;
  color: #6b7280;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.dz-floor-meta {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 2px;
}
.dz-floor-body p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.6;
}
.dz-empty {
  color: #9ca3af;
  font-size: 13px;
  padding: 8px 0;
}
.dz-reply {
  border-top: 1px solid #eef0f4;
  padding: 10px 16px;
  display: flex;
  gap: 10px;
  align-items: flex-end;
}
.dz-reply :deep(.el-textarea) {
  flex: 1;
}

</style>
