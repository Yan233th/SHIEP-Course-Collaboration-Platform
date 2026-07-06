<template>
  <section class="panel split">
    <div class="content-column">
      <div class="section-heading compact">
        <div>
          <h2>成员与权限</h2>
          <p>{{ currentCourseLabel }}</p>
        </div>
        <strong>{{ members.length }} 人</strong>
      </div>

      <div class="panel-toolbar">
        <el-input v-model="keyword" placeholder="姓名/账号/用户ID" clearable />
        <el-radio-group v-model="roleFilter">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="TEACHER">教师</el-radio-button>
          <el-radio-button label="TA">助教</el-radio-button>
          <el-radio-button label="STUDENT">学生</el-radio-button>
        </el-radio-group>
        <el-button :icon="Refresh" @click="loadMembers">同步</el-button>
      </div>

      <el-table :data="filteredMembers" height="calc(100vh - 292px)" empty-text="暂无课程成员">
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="realName" label="姓名" width="130" />
        <el-table-column prop="username" label="账号" width="150" />
        <el-table-column label="系统角色" width="110">
          <template #default="{ row }">
            <el-tag effect="plain">{{ roleLabel(row.systemRole) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="课程身份" width="110">
          <template #default="{ row }">
            <el-tag effect="plain" :type="roleTagType(row.courseRole)">{{ roleLabel(row.courseRole) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="canManageMembers" label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="editMember(row)">编辑</el-button>
            <el-button size="small" type="danger" text @click="deleteMember(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-form v-if="canManageMembers" :model="memberForm" label-position="top" class="side-form">
      <h3>{{ editingMember ? '编辑成员' : '添加成员' }}</h3>
      <el-form-item label="用户ID">
        <el-input-number v-model="memberForm.userId" :min="1" :disabled="Boolean(editingMember)" />
      </el-form-item>
      <el-form-item label="课程身份">
        <el-select v-model="memberForm.roleCode">
          <el-option label="教师" value="TEACHER" />
          <el-option label="助教" value="TA" />
          <el-option label="学生" value="STUDENT" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="memberForm.status">
          <el-radio-button :label="1">启用</el-radio-button>
          <el-radio-button :label="0">停用</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <div class="form-actions">
        <el-button type="primary" :icon="Check" @click="saveMember">保存</el-button>
        <el-button v-if="editingMember" @click="resetForm">取消编辑</el-button>
      </div>
    </el-form>
    <aside v-else class="side-form muted-panel">当前角色可查看成员名单，不能调整课程成员身份。</aside>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Refresh } from '@element-plus/icons-vue'
import { courseService } from '../services/platform'
import { can, currentCourseId, currentCourseLabel, refreshSignal, roleLabel } from '../state/appState'
import type { CourseMember } from '../types'

type EditableCourseRole = 'TEACHER' | 'TA' | 'STUDENT'

const members = ref<CourseMember[]>([])
const keyword = ref('')
const roleFilter = ref('')
const editingMember = ref<CourseMember | null>(null)
const memberForm = reactive({
  userId: undefined as number | undefined,
  roleCode: 'STUDENT' as EditableCourseRole,
  status: 1
})

const canManageMembers = computed(() => can('MANAGE_MEMBERS'))
const filteredMembers = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return members.value.filter((member) => {
    const matchesRole = !roleFilter.value || member.courseRole === roleFilter.value
    const matchesKeyword = !key
      || String(member.userId).includes(key)
      || member.username.toLowerCase().includes(key)
      || member.realName.toLowerCase().includes(key)
    return matchesRole && matchesKeyword
  })
})

function roleTagType(role: string) {
  if (role === 'TEACHER') return 'success'
  if (role === 'TA') return 'warning'
  if (role === 'STUDENT') return 'info'
  return ''
}

async function loadMembers() {
  members.value = await courseService.getMembers(currentCourseId.value)
}

function editMember(member: CourseMember) {
  editingMember.value = member
  memberForm.userId = member.userId
  memberForm.roleCode = member.courseRole
  memberForm.status = member.status
}

function resetForm() {
  editingMember.value = null
  memberForm.userId = undefined
  memberForm.roleCode = 'STUDENT'
  memberForm.status = 1
}

async function saveMember() {
  if (!memberForm.userId) {
    ElMessage.warning('请填写用户ID')
    return
  }
  const payload = {
    userId: memberForm.userId,
    roleCode: memberForm.roleCode,
    status: memberForm.status
  }
  if (editingMember.value) {
    await courseService.updateMember(currentCourseId.value, editingMember.value.id, payload)
    ElMessage.success('成员信息已更新')
  } else {
    await courseService.saveMember(currentCourseId.value, payload)
    ElMessage.success('成员已加入课程')
  }
  resetForm()
  await loadMembers()
}

async function deleteMember(member: CourseMember) {
  await ElMessageBox.confirm(`确认移除 ${member.realName} 的课程身份？`, '移除成员', { type: 'warning' })
  await courseService.deleteMember(currentCourseId.value, member.id)
  ElMessage.success('成员已移除')
  await loadMembers()
}

onMounted(loadMembers)
watch([currentCourseId, refreshSignal], async () => {
  resetForm()
  await loadMembers()
})
</script>
