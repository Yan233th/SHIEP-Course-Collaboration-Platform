<template>
  <section class="panel">
    <div class="section-heading compact">
      <div>
        <h2>项目分组</h2>
        <p>{{ currentCourseLabel }}</p>
      </div>
      <div class="heading-actions">
        <strong>{{ groups.length }} 组</strong>
        <el-button v-if="can('CREATE_GROUP')" type="primary" :icon="Plus" @click="openGroupDrawer">
          创建项目组
        </el-button>
      </div>
    </div>

    <el-table :data="groups" height="calc(100vh - 270px)" empty-text="暂无项目组">
      <el-table-column prop="name" label="组名" />
      <el-table-column prop="topic" label="选题" />
      <el-table-column label="人数" width="90">
        <template #default="{ row }">{{ row.currentMembers }}/{{ row.maxMembers }}</template>
      </el-table-column>
      <el-table-column v-if="can('JOIN_GROUP')" label="操作" width="130">
        <template #default="{ row }">
          <el-button v-if="isJoined(row.id)" size="small" type="danger" text @click="leaveGroup(row.id)">退出</el-button>
          <el-button v-else size="small" :disabled="row.currentMembers >= row.maxMembers" @click="joinGroup(row.id)">加入</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer
      v-model="groupDrawer"
      title="创建项目组"
      append-to-body
      class="workspace-drawer"
      direction="rtl"
      size="420px"
      @closed="resetGroupForm"
    >
      <el-form :model="groupForm" label-position="top" class="drawer-form">
        <el-form-item label="组名"><el-input v-model="groupForm.name" /></el-form-item>
        <el-form-item label="选题"><el-input v-model="groupForm.topic" /></el-form-item>
        <el-form-item label="人数上限"><el-input-number v-model="groupForm.maxMembers" :min="1" :max="20" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="groupDrawer = false">取消</el-button>
          <el-button type="primary" :icon="Plus" :disabled="!canCreateGroup" @click="createGroup">创建</el-button>
        </div>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { collaborationService } from '../services/platform'
import { appState, can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import type { ProjectGroup } from '../types'

const groups = ref<ProjectGroup[]>([])
const joinedGroupIds = ref<Set<number>>(new Set())
const groupDrawer = ref(false)
const groupForm = reactive({ name: '', topic: '', maxMembers: 5, status: 1 })
const canCreateGroup = computed(() => Boolean(groupForm.name.trim() && groupForm.topic.trim()))

async function loadGroups() {
  groups.value = await collaborationService.getGroups(currentCourseId.value)
  await loadMemberships()
}

async function loadMemberships() {
  if (!can('JOIN_GROUP') || !groups.value.length) {
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

function openGroupDrawer() {
  resetGroupForm()
  groupDrawer.value = true
}

function resetGroupForm() {
  groupForm.name = ''
  groupForm.topic = ''
  groupForm.maxMembers = 5
  groupForm.status = 1
}

async function createGroup() {
  await collaborationService.createGroup({
    courseId: currentCourseId.value,
    ...groupForm,
    leaderId: appState.session.userId
  })
  groupDrawer.value = false
  resetGroupForm()
  await loadGroups()
}

async function joinGroup(groupId: number) {
  await collaborationService.joinGroup(groupId, { userId: appState.session.userId, roleName: '成员' })
  ElMessage.success('已加入项目组')
  await loadGroups()
}

async function leaveGroup(groupId: number) {
  await collaborationService.leaveGroup(groupId)
  ElMessage.success('已退出项目组')
  await loadGroups()
}

onMounted(loadGroups)
watch([currentCourseId, refreshSignal], () => {
  groupDrawer.value = false
  resetGroupForm()
  void loadGroups()
})
</script>
