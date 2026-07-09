<template>
  <section class="panel project-groups-page" v-loading="loading">
    <div class="section-heading compact">
      <div>
        <h2>项目分组</h2>
        <p>{{ currentCourseLabel }}</p>
      </div>
      <div class="heading-actions">
        <strong>{{ groups.length }} 组</strong>
        <el-button v-if="canCreateGroups" type="primary" :icon="Plus" @click="openCreateGroupDrawer">
          创建项目组
        </el-button>
      </div>
    </div>

    <div class="project-groups-workspace">
      <section class="project-group-list">
        <el-table
          class="project-group-table"
          :data="groups"
          :row-class-name="groupRowClassName"
          row-key="id"
          height="calc(100vh - 270px)"
          empty-text="暂无项目组"
          @row-click="selectGroupByRow"
        >
          <el-table-column label="组名" min-width="190">
            <template #default="{ row }">
              <div class="group-name-cell">
                <span>{{ row.name }}</span>
                <el-tag v-if="isJoined(row.id)" size="small" type="success" effect="plain">已加入</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="topic" label="选题" min-width="220" show-overflow-tooltip />
          <el-table-column label="人数" width="120">
            <template #default="{ row }">
              <span class="member-count">{{ row.currentMembers }}/{{ row.maxMembers }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" effect="plain" :type="row.status === 0 ? 'info' : 'success'">
                {{ row.status === 0 ? '停用' : '开放' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <template v-if="canManageGroups">
                <el-button size="small" text :icon="Edit" @click.stop="openEditGroupDrawer(row)">编辑</el-button>
                <el-button size="small" text type="danger" :icon="Delete" @click.stop="deleteGroup(row)">删除</el-button>
              </template>
              <template v-if="canJoinGroups">
                <el-button
                  v-if="isJoined(row.id)"
                  size="small"
                  type="danger"
                  text
                  :loading="actionGroupId === row.id"
                  @click.stop="leaveGroup(row.id)"
                >
                  退出
                </el-button>
                <el-button
                  v-else
                  size="small"
                  :disabled="row.currentMembers >= row.maxMembers"
                  :loading="actionGroupId === row.id"
                  @click.stop="joinGroup(row.id)"
                >
                  加入
                </el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <aside class="project-group-detail" v-loading="detailLoading">
        <div v-if="!selectedGroup" class="empty-inline">选择左侧项目组查看协作状态</div>
        <template v-else>
          <div class="group-detail-head">
            <div>
              <span>当前项目组</span>
              <h3>{{ selectedGroup.name }}</h3>
            </div>
            <el-tag v-if="isJoined(selectedGroup.id)" size="small" type="success" effect="plain">已加入</el-tag>
          </div>
          <p class="group-topic">{{ selectedGroup.topic }}</p>

          <div class="group-detail-metrics">
            <div>
              <span>成员</span>
              <strong>{{ selectedGroup.currentMembers }}/{{ selectedGroup.maxMembers }}</strong>
            </div>
            <div>
              <span>讨论</span>
              <strong>{{ groupDiscussions.length }}</strong>
            </div>
            <div>
              <span>成果</span>
              <strong>{{ groupShowcases.length }}</strong>
            </div>
          </div>

          <div class="group-quick-actions">
            <el-button size="small" @click="goDiscussions">讨论交流</el-button>
            <el-button size="small" @click="goShowcases">成果展示</el-button>
          </div>

          <section class="group-detail-section">
            <div class="detail-section-head">
              <strong>成员</strong>
              <small>{{ members.length }} 人</small>
            </div>
            <ul v-if="members.length" class="member-list">
              <li v-for="member in members" :key="member.id">
                <span>{{ memberLabel(member.user) }}</span>
                <small>{{ memberRoleText(member) }}</small>
              </li>
            </ul>
            <div v-else class="empty-small">暂无成员</div>
          </section>

          <section class="group-detail-section">
            <div class="detail-section-head">
              <strong>最近成果</strong>
              <small>{{ groupShowcases.length }} 项</small>
            </div>
            <ul v-if="recentShowcases.length" class="showcase-mini-list">
              <li v-for="showcase in recentShowcases" :key="showcase.id">
                <span>{{ showcase.title }}</span>
                <small>{{ showcase.summary || '暂无摘要' }}</small>
              </li>
            </ul>
            <div v-else class="empty-small">暂无成果</div>
          </section>
        </template>
      </aside>
    </div>

    <WorkspaceDrawer v-model="groupDrawer" :title="groupDrawerTitle" @closed="resetGroupForm">
      <el-form :model="groupForm" label-position="top" class="drawer-form">
        <el-form-item label="组名"><el-input v-model="groupForm.name" /></el-form-item>
        <el-form-item label="选题"><el-input v-model="groupForm.topic" /></el-form-item>
        <el-form-item label="人数上限">
          <el-input-number v-model="groupForm.maxMembers" :min="groupMemberMin" :max="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="groupDrawer = false">取消</el-button>
          <el-button
            type="primary"
            :icon="editingGroup ? Check : Plus"
            :loading="groupSaving"
            :disabled="!canSaveGroup"
            @click="saveGroup"
          >
            {{ editingGroup ? '保存' : '创建' }}
          </el-button>
        </div>
      </template>
    </WorkspaceDrawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete, Edit, Plus } from '@element-plus/icons-vue'
import WorkspaceDrawer from '../components/WorkspaceDrawer.vue'
import { collaborationService } from '../services/platform'
import { appState, can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import type { Discussion, ProjectGroup, ProjectMemberDetail, Showcase, UserBrief } from '../types'

const router = useRouter()
const groups = ref<ProjectGroup[]>([])
const selectedGroupId = ref<number | undefined>(undefined)
const joinedGroupIds = ref<Set<number>>(new Set())
const groupDrawer = ref(false)
const members = ref<ProjectMemberDetail[]>([])
const groupDiscussions = ref<Discussion[]>([])
const groupShowcases = ref<Showcase[]>([])
const editingGroup = ref<ProjectGroup | null>(null)
const loading = ref(false)
const detailLoading = ref(false)
const groupSaving = ref(false)
const actionGroupId = ref<number | null>(null)
const groupForm = reactive({ name: '', topic: '', maxMembers: 5, status: 1 })

const canCreateGroups = computed(() => can('CREATE_GROUP'))
const canManageGroups = computed(() => can('MANAGE_PROJECT'))
const canJoinGroups = computed(() => can('JOIN_GROUP'))
const groupDrawerTitle = computed(() => editingGroup.value ? '编辑项目组' : '创建项目组')
const groupMemberMin = computed(() => editingGroup.value?.currentMembers || 1)
const selectedGroup = computed(() => groups.value.find((group) => group.id === selectedGroupId.value))
const recentShowcases = computed(() => {
  return [...groupShowcases.value]
    .sort((a, b) => String(b.createTime || '').localeCompare(String(a.createTime || '')))
    .slice(0, 3)
})
const canSaveGroup = computed(() => {
  return Boolean(groupForm.name.trim() && groupForm.topic.trim() && groupForm.maxMembers >= groupMemberMin.value)
})

async function loadGroups() {
  loading.value = true
  try {
    groups.value = await collaborationService.getGroups(currentCourseId.value)
    await loadMemberships()
    normalizeSelectedGroup()
    await loadGroupDetail()
  } finally {
    loading.value = false
  }
}

async function loadMemberships() {
  if (!canJoinGroups.value || !groups.value.length) {
    joinedGroupIds.value = new Set()
    return
  }
  const entries = await Promise.all(groups.value.map(async (group) => {
    const members = await collaborationService.getGroupMembers(group.id)
    return [group.id, members.some((member) => member.userId === appState.session.userId)] as const
  }))
  joinedGroupIds.value = new Set(entries.filter(([, joined]) => joined).map(([groupId]) => groupId))
}

function normalizeSelectedGroup() {
  if (!groups.value.length) {
    selectedGroupId.value = undefined
    return
  }
  const exists = groups.value.some((group) => group.id === selectedGroupId.value)
  if (!exists) {
    selectedGroupId.value = groups.value[0].id
  }
}

async function loadGroupDetail() {
  members.value = []
  groupDiscussions.value = []
  groupShowcases.value = []
  if (!selectedGroupId.value) return
  detailLoading.value = true
  try {
    const [memberRows, discussionRows, showcaseRows] = await Promise.all([
      collaborationService.getGroupMemberDetails(selectedGroupId.value),
      collaborationService.getDiscussions(selectedGroupId.value),
      collaborationService.getShowcases(currentCourseId.value)
    ])
    members.value = memberRows
    groupDiscussions.value = discussionRows
    groupShowcases.value = showcaseRows.filter((showcase) => showcase.groupId === selectedGroupId.value)
  } finally {
    detailLoading.value = false
  }
}

function selectGroupByRow(row: ProjectGroup) {
  void selectGroup(row.id)
}

async function selectGroup(groupId: number) {
  if (selectedGroupId.value === groupId) return
  selectedGroupId.value = groupId
  await loadGroupDetail()
}

function groupRowClassName({ row }: { row: ProjectGroup }) {
  return row.id === selectedGroupId.value ? 'course-row-current' : ''
}

function isJoined(groupId: number) {
  return joinedGroupIds.value.has(groupId)
}

function memberLabel(user?: UserBrief | null) {
  if (!user) return '-'
  return user.realName || user.username
}

function memberRoleText(member: ProjectMemberDetail) {
  const role = member.roleName || '成员'
  return member.userId === selectedGroup.value?.leaderId ? `${role} · 负责人` : role
}

function goDiscussions() {
  void router.push('/projects/discussions')
}

function goShowcases() {
  void router.push('/projects/showcases')
}

function openCreateGroupDrawer() {
  resetGroupForm()
  groupDrawer.value = true
}

function openEditGroupDrawer(group: ProjectGroup) {
  editingGroup.value = group
  groupForm.name = group.name
  groupForm.topic = group.topic
  groupForm.maxMembers = group.maxMembers
  groupForm.status = group.status ?? 1
  groupDrawer.value = true
}

function resetGroupForm() {
  editingGroup.value = null
  groupForm.name = ''
  groupForm.topic = ''
  groupForm.maxMembers = 5
  groupForm.status = 1
  groupSaving.value = false
}

function groupPayload() {
  return {
    courseId: currentCourseId.value,
    name: groupForm.name,
    topic: groupForm.topic,
    leaderId: editingGroup.value?.leaderId ?? appState.session.userId,
    maxMembers: groupForm.maxMembers,
    status: groupForm.status
  }
}

async function saveGroup() {
  if (!canSaveGroup.value) return
  groupSaving.value = true
  try {
    const saved = editingGroup.value
      ? await collaborationService.updateGroup(editingGroup.value.id, groupPayload())
      : await collaborationService.createGroup(groupPayload())
    selectedGroupId.value = saved.id
    ElMessage.success(editingGroup.value ? '项目组已更新' : '项目组已创建')
    groupDrawer.value = false
    await loadGroups()
  } finally {
    groupSaving.value = false
  }
}

async function deleteGroup(group: ProjectGroup) {
  await ElMessageBox.confirm(`确认删除项目组「${group.name}」？`, '删除项目组', { type: 'warning' })
  await collaborationService.deleteGroup(group.id)
  ElMessage.success('项目组已删除')
  if (selectedGroupId.value === group.id) {
    selectedGroupId.value = undefined
  }
  await loadGroups()
}

async function joinGroup(groupId: number) {
  actionGroupId.value = groupId
  try {
    await collaborationService.joinGroup(groupId, { userId: appState.session.userId, roleName: '成员' })
    ElMessage.success('已加入项目组')
    selectedGroupId.value = groupId
    await loadGroups()
  } finally {
    actionGroupId.value = null
  }
}

async function leaveGroup(groupId: number) {
  actionGroupId.value = groupId
  try {
    await collaborationService.leaveGroup(groupId)
    ElMessage.success('已退出项目组')
    selectedGroupId.value = groupId
    await loadGroups()
  } finally {
    actionGroupId.value = null
  }
}

onMounted(loadGroups)
watch([currentCourseId, refreshSignal], () => {
  groupDrawer.value = false
  selectedGroupId.value = undefined
  resetGroupForm()
  void loadGroups()
})
</script>

<style scoped>
.project-groups-page {
  display: grid;
  gap: 16px;
}

.project-groups-workspace {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(520px, 1fr) minmax(340px, 0.48fr);
  gap: 14px;
}

.project-group-list,
.project-group-detail {
  min-width: 0;
}

.project-group-table .el-table__row {
  cursor: pointer;
}

.project-group-table :deep(.course-row-current > .el-table__cell) {
  background: rgb(var(--app-brand-rgb) / 5%);
}

.project-group-detail {
  min-height: calc(100vh - 270px);
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-surface-soft);
}

.group-detail-head {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.group-detail-head div {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.group-detail-head span,
.group-detail-metrics span,
.detail-section-head small {
  color: var(--app-muted);
  font-size: 12px;
  font-weight: 700;
}

.group-detail-head h3 {
  overflow: hidden;
  margin: 0;
  color: var(--app-ink-strong);
  font-size: 17px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-topic {
  margin: 0;
  color: var(--app-text-soft);
  line-height: 1.65;
}

.group-detail-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.group-detail-metrics div {
  display: grid;
  gap: 4px;
  padding: 10px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-surface);
}

.group-detail-metrics strong {
  color: var(--app-ink-strong);
  font-size: 20px;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.group-quick-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.group-detail-section {
  display: grid;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--app-divider);
}

.detail-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.detail-section-head strong {
  color: var(--app-ink-strong);
  font-size: 14px;
}

.member-list,
.showcase-mini-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.member-list li,
.showcase-mini-list li {
  min-width: 0;
  display: grid;
  gap: 3px;
  padding: 9px 10px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-surface);
}

.member-list span,
.showcase-mini-list span {
  overflow: hidden;
  color: var(--app-ink-strong);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-list small,
.showcase-mini-list small {
  overflow: hidden;
  color: var(--app-muted);
  font-size: 12px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-small {
  padding: 14px;
  border: 1px dashed var(--app-border-strong);
  border-radius: 8px;
  color: var(--app-muted);
  background: var(--app-surface);
  text-align: center;
}

.member-count {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}

@media (max-width: 1180px) {
  .project-groups-workspace {
    grid-template-columns: 1fr;
  }

  .project-group-detail {
    min-height: 0;
  }
}
</style>
