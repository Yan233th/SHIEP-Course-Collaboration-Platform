<template>
  <section class="panel">
    <div class="section-heading compact">
      <div>
        <h2>组内讨论</h2>
        <p>{{ currentCourseLabel }}</p>
      </div>
      <div class="heading-actions">
        <strong v-if="selectedGroupId">{{ topicList.length }} 个话题</strong>
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
    <div v-else-if="!selectedGroupId" class="empty-inline">请选择一个项目组查看讨论。</div>
    <div v-else-if="!topicList.length" class="empty-inline">暂无话题，发起第一个话题吧。</div>
    <div v-else class="dz-topics">
      <button v-for="topic in topicList" :key="topic.id" class="dz-card" @click="openTopic(topic, $event)">
        <div class="dz-card-title">{{ topic.title }}</div>
        <div class="dz-card-content">{{ topic.content }}</div>
        <div class="dz-card-meta">
          <span>作者 {{ topic.authorId }}</span>
          <span>{{ repliesOf(topic.id).length }} 回复</span>
        </div>
      </button>
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

    <!-- 帖子详情：卡片 morph 成近全屏弹窗，右侧拉开 -->
    <Teleport to="body">
      <Transition name="dz-overlay">
        <div v-if="overlayOpen" class="dz-overlay" @click.self="closeOverlay">
          <div class="dz-panel" :style="{ '--ox': origin.x + '%', '--oy': origin.y + '%' }">
            <aside class="dz-master">
              <div class="dz-master-head">话题 · {{ topicList.length }}</div>
              <div class="dz-master-list">
                <button
                  v-for="t in topicList"
                  :key="t.id"
                  class="dz-master-item"
                  :class="{ active: t.id === activeTopicId }"
                  @click="selectTopic(t.id)"
                >
                  <div class="dz-master-title">{{ t.title }}</div>
                  <div class="dz-master-meta">{{ repliesOf(t.id).length }} 回复</div>
                </button>
              </div>
            </aside>

            <section class="dz-detail" :class="{ ready: detailReady }">
              <header class="dz-detail-head">
                <div class="dz-detail-title">
                  <h3>{{ activeTopic?.title }}</h3>
                  <span>作者 {{ activeTopic?.authorId }}</span>
                </div>
                <el-button text @click="closeOverlay">关闭</el-button>
              </header>

              <div class="dz-detail-body">
                <article class="dz-post">
                  <p>{{ activeTopic?.content }}</p>
                </article>
                <div class="dz-replies-head">楼层回复 · {{ repliesOf(activeTopicId).length }}</div>
                <div class="dz-replies">
                  <div v-for="(reply, i) in repliesOf(activeTopicId)" :key="reply.id" class="dz-floor">
                    <div class="dz-floor-no">{{ i + 1 }}</div>
                    <div class="dz-floor-body">
                      <div class="dz-floor-meta">作者 {{ reply.authorId }}</div>
                      <p>{{ reply.content }}</p>
                    </div>
                  </div>
                  <div v-if="!repliesOf(activeTopicId).length" class="dz-empty">还没有回复，来说点什么</div>
                </div>
              </div>

              <footer v-if="can('CREATE_DISCUSSION')" class="dz-reply">
                <el-input v-model="replyContent" type="textarea" :rows="2" placeholder="回复该话题…" />
                <el-button type="primary" :disabled="!replyContent.trim()" @click="submitReply">回复</el-button>
              </footer>
            </section>
          </div>
        </div>
      </Transition>
    </Teleport>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { collaborationService } from '../services/platform'
import { can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import type { Discussion, ProjectGroup } from '../types'

const discussions = ref<Discussion[]>([])
const accessibleGroups = ref<ProjectGroup[]>([])
const selectedGroupId = ref<number | undefined>(undefined)
const topicDrawer = ref(false)
const topicForm = reactive({ title: '', content: '' })
const canCreateTopic = computed(() => Boolean(topicForm.title.trim() && topicForm.content.trim()))

// 话题 = 顶层帖；回复按根话题归拢（兼容历史"回复的回复"，统一两级展示）
const topicList = computed(() => discussions.value.filter((d) => !d.parentId))
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
}

function onGroupChange() {
  overlayOpen.value = false
  void loadDiscussions()
}

// —— 新话题 ——
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

// —— 帖子详情 morph 弹窗 ——
const overlayOpen = ref(false)
const activeTopicId = ref<number | undefined>(undefined)
const detailReady = ref(false)
const replyContent = ref('')
const origin = reactive({ x: 50, y: 50 })
const activeTopic = computed(() => topicList.value.find((t) => t.id === activeTopicId.value))

function openTopic(topic: Discussion, event: Event) {
  const el = event.currentTarget as HTMLElement
  const rect = el.getBoundingClientRect()
  origin.x = ((rect.left + rect.width / 2) / window.innerWidth) * 100
  origin.y = ((rect.top + rect.height / 2) / window.innerHeight) * 100
  activeTopicId.value = topic.id
  detailReady.value = false
  replyContent.value = ''
  overlayOpen.value = true
  // 等弹窗 morph 起来后，再触发右侧"拉开"
  nextTick(() => setTimeout(() => { detailReady.value = true }, 70))
}

function closeOverlay() {
  detailReady.value = false
  overlayOpen.value = false
}

function selectTopic(id: number) {
  activeTopicId.value = id
  replyContent.value = ''
}

async function submitReply() {
  if (!activeTopicId.value || !selectedGroupId.value || !replyContent.value.trim()) return
  await collaborationService.createDiscussion({
    courseId: currentCourseId.value,
    groupId: selectedGroupId.value,
    parentId: activeTopicId.value,
    title: activeTopic.value?.title || '',
    content: replyContent.value,
    status: 1
  })
  replyContent.value = ''
  await loadDiscussions()
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && overlayOpen.value) closeOverlay()
}

onMounted(() => {
  void loadAccessibleGroups()
  window.addEventListener('keydown', onKeydown)
})
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
watch([currentCourseId, refreshSignal], () => {
  overlayOpen.value = false
  topicDrawer.value = false
  selectedGroupId.value = undefined
  void loadAccessibleGroups()
})
</script>

<style scoped>
.dz-topics {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.dz-card {
  width: 100%;
  text-align: left;
  background: var(--el-bg-color, #fff);
  border: 1px solid var(--el-border-color, #e5e7eb);
  border-radius: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.2s ease, border-color 0.2s ease;
}
.dz-card:hover {
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.09);
  transform: translateY(-1px);
  border-color: #c7c9d1;
}
.dz-card-title {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 4px;
}
.dz-card-content {
  color: #6b7280;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dz-card-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #9ca3af;
  display: flex;
  gap: 14px;
}

/* overlay + morph */
.dz-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}
.dz-overlay::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(15, 18, 25, 0.5);
}
.dz-panel {
  position: relative;
  width: min(1120px, 94vw);
  height: min(760px, 88vh);
  background: var(--el-bg-color, #fff);
  border-radius: 18px;
  display: flex;
  overflow: hidden;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.35);
  transform-origin: var(--ox, 50%) var(--oy, 50%);
}
.dz-overlay-enter-from .dz-panel,
.dz-overlay-leave-to .dz-panel {
  transform: scale(0.82);
  opacity: 0;
}
.dz-overlay-enter-from::before,
.dz-overlay-leave-to::before {
  opacity: 0;
}
.dz-overlay-enter-active .dz-panel,
.dz-overlay-leave-active .dz-panel {
  transition: transform 0.34s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.34s cubic-bezier(0.22, 1, 0.36, 1);
}
.dz-overlay-enter-active::before,
.dz-overlay-leave-active::before {
  transition: opacity 0.3s ease;
}

/* 左：话题列表 */
.dz-master {
  width: 300px;
  border-right: 1px solid #eef0f4;
  display: flex;
  flex-direction: column;
  background: #fafbfc;
}
.dz-master-head {
  padding: 16px 18px;
  font-weight: 600;
  font-size: 13px;
  color: #6b7280;
  border-bottom: 1px solid #eef0f4;
}
.dz-master-list {
  overflow: auto;
  flex: 1;
  padding: 8px;
}
.dz-master-item {
  display: block;
  width: 100%;
  text-align: left;
  background: transparent;
  border: 0;
  border-radius: 10px;
  padding: 10px 12px;
  cursor: pointer;
  margin-bottom: 4px;
}
.dz-master-item:hover {
  background: #eef1f5;
}
.dz-master-item.active {
  background: #e8eef9;
}
.dz-master-title {
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dz-master-meta {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 2px;
}

/* 右：帖子详情，拉开 */
.dz-detail {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  transform: translateX(48px);
  opacity: 0;
  transition: transform 0.38s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.38s ease;
}
.dz-detail.ready {
  transform: none;
  opacity: 1;
}
.dz-detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #eef0f4;
}
.dz-detail-title h3 {
  margin: 0;
  font-size: 16px;
}
.dz-detail-title span {
  font-size: 12px;
  color: #9ca3af;
}
.dz-detail-body {
  flex: 1;
  overflow: auto;
  padding: 16px 20px;
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
  padding: 12px 20px;
  display: flex;
  gap: 10px;
  align-items: flex-end;
}
.dz-reply :deep(.el-textarea) {
  flex: 1;
}
</style>
