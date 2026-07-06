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
      <el-button :icon="Search" @click="loadUsers">查询</el-button>
      <el-button type="danger" :icon="Delete" :disabled="!selection.length" @click="batchDeleteUsers">批量删除</el-button>
    </div>

    <el-table :data="users.records" @selection-change="selection = $event" height="calc(100vh - 270px)" empty-text="暂无用户">
      <el-table-column type="selection" width="44" />
      <el-table-column prop="username" label="账号" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="roleCode" label="角色" width="110" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Search } from '@element-plus/icons-vue'
import { userService, type UserQuery } from '../services/platform'
import type { Page, UserRow } from '../types'

const query = reactive<UserQuery>({ keyword: '', roleCode: '', status: undefined })
const users = ref<Page<UserRow>>({ total: 0, pageNum: 1, pageSize: 10, records: [] })
const selection = ref<UserRow[]>([])

async function loadUsers() {
  users.value = await userService.getUsers(query)
}

async function batchDeleteUsers() {
  await userService.batchDelete(selection.value.map((user) => user.id))
  ElMessage.success('已删除选中用户')
  await loadUsers()
}

onMounted(loadUsers)
</script>
