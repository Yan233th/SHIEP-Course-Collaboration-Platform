<template>
  <main class="login-shell">
    <section class="login-layout">
      <div class="login-intro">
        <span class="login-kicker">课程空间入口</span>
        <h1>课程协同管理平台</h1>
        <p>统一处理课程、作业、项目、讨论和成果展示。</p>
        <dl class="login-facts">
          <div>
            <dt>角色</dt>
            <dd>教师 / 助教 / 学生</dd>
          </div>
          <div>
            <dt>入口</dt>
            <dd>工作台与课程空间</dd>
          </div>
        </dl>
      </div>

      <section class="login-panel">
        <div class="login-panel-heading">
          <h2>登录</h2>
          <p>使用课程平台账号进入系统。</p>
        </div>
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
          <span>演示账号</span>
          <el-tag v-for="account in demoAccounts" :key="account" effect="plain" @click="loginForm.username = account">
            {{ account }}
          </el-tag>
        </div>
      </section>
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
