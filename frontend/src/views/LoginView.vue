<template>
  <main class="login-shell">
    <section class="login-panel">
      <p class="login-kicker">Course Collaboration</p>
      <h1>课程协同管理平台</h1>
      <p class="login-subtitle">统一管理课程资料、作业、项目分组、讨论和成果展示。</p>
      <el-form :model="loginForm" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="账号">
          <el-input v-model="loginForm.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" type="password" autocomplete="current-password" show-password />
        </el-form-item>
        <el-button type="primary" :icon="Key" :loading="loading" @click="handleLogin">登录</el-button>
      </el-form>
      <div class="account-chips">
        <el-tag v-for="account in demoAccounts" :key="account" effect="plain" @click="loginForm.username = account">
          {{ account }}
        </el-tag>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Key } from '@element-plus/icons-vue'
import { LAST_ROUTE_KEY, login } from '../state/appState'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const loginForm = reactive({ username: 'admin', password: '123456' })
const demoAccounts = ['admin', 'teacher1', 'ta1', 'student1']

async function handleLogin() {
  loading.value = true
  try {
    await login(loginForm)
    const redirect = typeof route.query.redirect === 'string'
      ? route.query.redirect
      : localStorage.getItem(LAST_ROUTE_KEY) || '/dashboard'
    await router.replace(redirect)
    ElMessage.success('已登录')
  } finally {
    loading.value = false
  }
}
</script>
