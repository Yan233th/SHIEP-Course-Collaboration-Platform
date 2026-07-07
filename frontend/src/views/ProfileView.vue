<template>
  <section class="profile-page">
    <section class="profile-header">
      <div class="profile-heading">
        <span class="section-label">账户资料</span>
        <h1>个人信息</h1>
        <p>维护当前登录账号的基础资料和联系方式，账号身份由平台权限体系统一管理。</p>
      </div>
      <div class="profile-header-meta">
        <el-tag effect="plain">{{ roleLabel(profile?.roleCode || currentRole) }}</el-tag>
        <el-tag :type="profile?.status === 1 ? 'success' : 'info'" effect="plain">{{ statusText }}</el-tag>
        <span>{{ profile?.username || appState.session.username }}</span>
      </div>
    </section>

    <div class="profile-grid">
      <section v-loading="loading" class="panel profile-form-panel">
        <div class="section-heading">
          <div>
            <h2>基础资料</h2>
            <p>这些信息用于课程成员、通知发布和协作记录中的姓名与联系方式展示。</p>
          </div>
          <el-tag :type="profile?.status === 1 ? 'success' : 'info'" effect="plain">{{ statusText }}</el-tag>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="profile-form-grid">
            <el-form-item label="账号">
              <el-input v-model="form.username" disabled />
            </el-form-item>
            <el-form-item label="系统身份">
              <el-input :model-value="roleLabel(profile?.roleCode || currentRole)" disabled />
            </el-form-item>
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="form.realName" maxlength="50" show-word-limit />
            </el-form-item>
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio-button label="U">未指定</el-radio-button>
                <el-radio-button label="M">男</el-radio-button>
                <el-radio-button label="F">女</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="name@example.com" maxlength="100" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="用于课程联系" maxlength="30" />
            </el-form-item>
          </div>
        </el-form>

        <div class="form-actions profile-actions">
          <el-button :icon="Refresh" @click="loadProfile">刷新</el-button>
          <el-button type="primary" :icon="Check" :loading="saving" :disabled="!dirty" @click="saveProfile">
            保存修改
          </el-button>
        </div>
      </section>

      <section class="panel profile-detail-panel">
        <div class="section-heading compact">
          <div>
            <h2>账号概览</h2>
            <p>当前会话中的账号与权限摘要。</p>
          </div>
        </div>
        <dl class="profile-detail-list">
          <div>
            <dt>用户编号</dt>
            <dd>{{ profile?.id || appState.session.userId || '-' }}</dd>
          </div>
          <div>
            <dt>登录账号</dt>
            <dd>{{ profile?.username || appState.session.username || '-' }}</dd>
          </div>
          <div>
            <dt>系统身份</dt>
            <dd>{{ roleLabel(profile?.roleCode || currentRole) }}</dd>
          </div>
          <div>
            <dt>邮箱</dt>
            <dd>{{ profile?.email || '未填写' }}</dd>
          </div>
          <div>
            <dt>手机号</dt>
            <dd>{{ profile?.phone || '未填写' }}</dd>
          </div>
        </dl>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Check, Refresh } from '@element-plus/icons-vue'
import { userService, type ProfilePayload } from '../services/platform'
import { appState, currentRole, roleLabel, syncSessionProfile } from '../state/appState'
import type { UserRow } from '../types'

const loading = ref(false)
const saving = ref(false)
const profile = ref<UserRow | null>(null)
const formRef = ref<FormInstance>()
const originalPayload = ref('')
const form = reactive({
  username: '',
  realName: '',
  email: '',
  phone: '',
  gender: 'U' as 'M' | 'F' | 'U'
})
const rules: FormRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}
const dirty = computed(() => originalPayload.value !== JSON.stringify(profilePayload()))
const statusText = computed(() => profile.value?.status === 0 ? '禁用' : '启用')

function profilePayload(): ProfilePayload {
  return {
    realName: form.realName.trim(),
    email: form.email.trim() || undefined,
    phone: form.phone.trim() || undefined,
    gender: form.gender
  }
}

function fillForm(user: UserRow) {
  profile.value = user
  Object.assign(form, {
    username: user.username,
    realName: user.realName || '',
    email: user.email || '',
    phone: user.phone || '',
    gender: user.gender || 'U'
  })
  originalPayload.value = JSON.stringify(profilePayload())
}

async function loadProfile() {
  loading.value = true
  try {
    const user = await userService.getCurrentUser()
    fillForm(user)
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const user = await userService.updateProfile(profilePayload())
    fillForm(user)
    syncSessionProfile(user)
    ElMessage.success('个人信息已保存')
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>
