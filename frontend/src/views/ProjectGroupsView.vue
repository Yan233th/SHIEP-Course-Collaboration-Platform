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

    <el-table :data="groups" row-key="id" height="calc(100vh - 270px)" empty-text="暂无项目组">
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
      <el-table-column label="操作" width="310" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openMembersDrawer(row)">成员</el-button>
          <template v-if="canManageGroups">
            <el-button size="small" text :icon="Edit" @click="openEditGroupDrawer(row)">编辑</el-button>
            <el-button size="small" text type="danger" :icon="Delete" @click="deleteGroup(row)">删除</el-button>
          </template>
          <template v-if="canJoinGroups">
            <el-button
              v-if="isJoined(row.id)"
              size="small"
              type="danger"
              text
              :loading="actionGroupId === row.id"
              @click="leaveGroup(row.id)"
            >
              退出
            </el-button>
            <el-button
              v-else
              size="small"
              :disabled="row.currentMembers >= row.maxMembers"
              :loading="actionGroupId === row.id"
              @click="joinGroup(row.id)"
            >
              加入
            </el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

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

    <WorkspaceDrawer
      v-model="membersDrawer"
      :title="membersGroupName ? `${membersGroupName} · 项目组成员` : '项目组成员'"
      size="520px"
    >
      <div v-loading="membersLoading">
        <div v-if="!members.length" class="empty-inline">暂无成员</div>
        <el-table v-else :data="members" size="small">
          <el-table-column label="姓名">
            <template #default="{ row }">{{ memberLabel(row.user) }}</template>
          </el-table-column>
          <el-table-column prop="roleName" label="组内角色" width="120" />
          <el-table-column label="账号" width="150">
            <template #default="{ row }">{{ row.user?.username || '-' }}</template>
          </el-table-column>
        </el-table>
      </div>
    </WorkspaceDrawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete, Edit, Plus } from '@element-plus/icons-vue'
import WorkspaceDrawer from '../components/WorkspaceDrawer.vue'
import { collaborationService } from '../services/platform'
import { appState, can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import type { ProjectGroup, ProjectMemberDetail, UserBrief } from '../types'

const groups = ref<ProjectGroup[]>([])
const joinedGroupIds = ref<Set<number>>(new Set())
const groupDrawer = ref(false)
const membersDrawer = ref(false)
const membersGroupName = ref('')
const members = ref<ProjectMemberDetail[]>([])
const editingGroup = ref<ProjectGroup | null>(null)
const loading = ref(false)
const groupSaving = ref(false)
const membersLoading = ref(false)
const actionGroupId = ref<number | null>(null)
const groupForm = reactive({ name: '', topic: '', maxMembers: 5, status: 1 })

const canCreateGroups = computed(() => can('CREATE_GROUP'))
const canManageGroups = computed(() => can('MANAGE_PROJECT'))
const canJoinGroups = computed(() => can('JOIN_GROUP'))
const groupDrawerTitle = computed(() => editingGroup.value ? '编辑项目组' : '创建项目组')
const groupMemberMin = computed(() => editingGroup.value?.currentMembers || 1)
const canSaveGroup = computed(() => {
  return Boolean(groupForm.name.trim() && groupForm.topic.trim() && groupForm.maxMembers >= groupMemberMin.value)
})

async function loadGroups() {
  loading.value = true
  try {
    groups.value = await collaborationService.getGroups(currentCourseId.value)
    await loadMemberships()
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

function isJoined(groupId: number) {
  return joinedGroupIds.value.has(groupId)
}

async function openMembersDrawer(group: ProjectGroup) {
  membersGroupName.value = group.name
  members.value = []
  membersDrawer.value = true
  membersLoading.value = true
  try {
    members.value = await collaborationService.getGroupMemberDetails(group.id)
  } finally {
    membersLoading.value = false
  }
}

function memberLabel(user?: UserBrief | null) {
  if (!user) return '-'
  return user.realName || user.username
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
    if (editingGroup.value) {
      await collaborationService.updateGroup(editingGroup.value.id, groupPayload())
      ElMessage.success('项目组已更新')
    } else {
      await collaborationService.createGroup(groupPayload())
      ElMessage.success('项目组已创建')
    }
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
  await loadGroups()
}

async function joinGroup(groupId: number) {
  actionGroupId.value = groupId
  try {
    await collaborationService.joinGroup(groupId, { userId: appState.session.userId, roleName: '成员' })
    ElMessage.success('已加入项目组')
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
    await loadGroups()
  } finally {
    actionGroupId.value = null
  }
}

onMounted(loadGroups)
watch([currentCourseId, refreshSignal], () => {
  groupDrawer.value = false
  membersDrawer.value = false
  resetGroupForm()
  void loadGroups()
})
</script>

<style scoped>
.member-count {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}
</style>
