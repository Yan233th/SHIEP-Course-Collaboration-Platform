<template>
  <section class="panel">
    <div class="section-heading">
      <div>
        <h2>用户与角色</h2>
        <p>按账号、姓名、邮箱和角色筛选平台成员。</p>
      </div>
      <strong>{{ users.total }} 人</strong>
    </div>

    <div class="panel-toolbar">
      <el-input v-model="query.keyword" placeholder="账号/姓名/邮箱" clearable />
      <el-select v-model="query.roleCode" placeholder="角色" clearable>
        <el-option label="管理员" value="ADMIN" />
        <el-option label="教师" value="TEACHER" />
        <el-option label="助教" value="TA" />
        <el-option label="学生" value="STUDENT" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable>
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button :icon="Search" @click="loadUsers">查询</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增用户</el-button>
      <el-button type="danger" :icon="Delete" :disabled="!selection.length" @click="batchDeleteUsers">批量删除</el-button>
    </div>

    <el-table :data="users.records" @selection-change="selection = $event" height="calc(100vh - 270px)" empty-text="暂无用户">
      <el-table-column type="selection" width="44" />
      <el-table-column prop="username" label="账号" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column label="角色" width="110">
        <template #default="{ row }">
          <el-tag effect="plain">{{ roleLabel(row.roleCode) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="userDialog" :title="editingUser ? '编辑用户' : '新增用户'" width="560px" append-to-body>
      <el-form :model="userForm" label-width="88px">
        <el-form-item label="账号"><el-input v-model="userForm.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="userForm.realName" /></el-form-item>
        <el-form-item :label="editingUser ? '新密码' : '密码'">
          <el-input v-model="userForm.password" type="password" show-password :placeholder="editingUser ? '留空则不修改' : '默认 123456'" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.roleCode">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="助教" value="TA" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱"><el-input v-model="userForm.email" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="userForm.phone" /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="userForm.gender">
            <el-radio-button label="U">未指定</el-radio-button>
            <el-radio-button label="M">男</el-radio-button>
            <el-radio-button label="F">女</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="userForm.status">
            <el-radio-button :label="1">启用</el-radio-button>
            <el-radio-button :label="0">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!canSaveUser" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Plus, Search } from '@element-plus/icons-vue'
import { userService, type UserPayload, type UserQuery } from '../services/platform'
import { roleLabel } from '../state/appState'
import type { Page, RoleCode, UserRow } from '../types'

const query = reactive<UserQuery>({ keyword: '', roleCode: '', status: undefined })
const users = ref<Page<UserRow>>({ total: 0, pageNum: 1, pageSize: 10, records: [] })
const selection = ref<UserRow[]>([])
const userDialog = ref(false)
const editingUser = ref<UserRow | null>(null)
const userForm = reactive({
  username: '',
  password: '',
  realName: '',
  roleCode: 'STUDENT' as RoleCode,
  email: '',
  phone: '',
  gender: 'U' as 'M' | 'F' | 'U',
  status: 1
})
const canSaveUser = computed(() => Boolean(userForm.username.trim() && userForm.realName.trim() && userForm.roleCode))

async function loadUsers() {
  users.value = await userService.getUsers(query)
}

function resetUserForm() {
  editingUser.value = null
  Object.assign(userForm, {
    username: '',
    password: '',
    realName: '',
    roleCode: 'STUDENT',
    email: '',
    phone: '',
    gender: 'U',
    status: 1
  })
}

function openCreateDialog() {
  resetUserForm()
  userDialog.value = true
}

function openEditDialog(user: UserRow) {
  editingUser.value = user
  Object.assign(userForm, {
    username: user.username,
    password: '',
    realName: user.realName,
    roleCode: user.roleCode,
    email: user.email || '',
    phone: user.phone || '',
    gender: user.gender || 'U',
    status: user.status
  })
  userDialog.value = true
}

function userPayload(): UserPayload {
  const payload: UserPayload = {
    username: userForm.username.trim(),
    realName: userForm.realName.trim(),
    roleCode: userForm.roleCode,
    email: userForm.email.trim() || undefined,
    phone: userForm.phone.trim() || undefined,
    gender: userForm.gender,
    status: userForm.status
  }
  if (userForm.password.trim()) {
    payload.password = userForm.password.trim()
  }
  return payload
}

async function saveUser() {
  if (editingUser.value) {
    await userService.updateUser(editingUser.value.id, userPayload())
    ElMessage.success('用户信息已更新')
  } else {
    await userService.createUser(userPayload())
    ElMessage.success('用户已创建')
  }
  userDialog.value = false
  resetUserForm()
  await loadUsers()
}

async function batchDeleteUsers() {
  await userService.batchDelete(selection.value.map((user) => user.id))
  ElMessage.success('已删除选中用户')
  await loadUsers()
}

onMounted(loadUsers)
</script>
