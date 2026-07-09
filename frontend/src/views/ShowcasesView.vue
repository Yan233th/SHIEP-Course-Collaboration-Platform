<template>
  <section class="panel showcases-page" v-loading="loading">
    <div class="section-heading compact">
      <div>
        <h2>成果展示</h2>
        <p>{{ currentCourseLabel }}</p>
      </div>
      <div class="heading-actions">
        <strong>{{ showcases.length }} 项</strong>
        <el-button
          v-if="can('PUBLISH_SHOWCASE')"
          type="primary"
          :icon="Plus"
          :disabled="!showcaseGroups.length"
          @click="openShowcaseDrawer"
        >
          发布成果
        </el-button>
      </div>
    </div>

    <div v-if="!showcases.length" class="empty-inline">暂无成果</div>
    <div v-else class="showcase-grid">
      <article v-for="showcase in showcases" :key="showcase.id" class="showcase-card">
        <div class="showcase-card-top">
          <el-tag effect="plain">{{ groupName(showcase.groupId) }}</el-tag>
          <span>{{ formatShowcaseTime(showcase.createTime) }}</span>
        </div>
        <h3>{{ showcase.title }}</h3>
        <p>{{ showcase.summary }}</p>
        <FileActions v-if="showcase.fileId" :file-id="showcase.fileId" :file="showcase.file" />
        <div class="showcase-card-foot">
          <el-link v-if="showcase.linkUrl" :href="showcase.linkUrl" target="_blank">打开链接</el-link>
          <span v-else>暂无外部链接</span>
          <div v-if="can('PUBLISH_SHOWCASE')" class="showcase-card-actions">
            <el-button size="small" text :icon="Edit" @click="openEditShowcaseDrawer(showcase)">编辑</el-button>
            <el-button
              size="small"
              text
              type="danger"
              :icon="Delete"
              :loading="deletingShowcaseId === showcase.id"
              @click="deleteShowcase(showcase)"
            >
              删除
            </el-button>
          </div>
        </div>
      </article>
    </div>

    <WorkspaceDrawer v-model="showcaseDrawer" :title="drawerTitle" @closed="resetShowcaseForm">
      <el-form :model="showcaseForm" label-position="top" class="drawer-form">
        <el-form-item label="项目组">
          <el-select v-model="showcaseForm.groupId" filterable placeholder="选择项目组">
            <el-option v-for="group in showcaseGroups" :key="group.id" :label="group.name" :value="group.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="showcaseForm.title" /></el-form-item>
        <el-form-item label="链接"><el-input v-model="showcaseForm.linkUrl" placeholder="https://" /></el-form-item>
        <el-form-item label="摘要"><el-input v-model="showcaseForm.summary" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="成果附件">
          <div class="drawer-file-field">
            <div v-if="showcaseForm.fileId" class="drawer-current-file">
              <FileActions :file-id="showcaseForm.fileId" :file="editingShowcase?.file" />
              <el-button size="small" text type="danger" @click="removeShowcaseFile">移除</el-button>
            </div>
            <el-upload
              action="#"
              :auto-upload="false"
              :limit="1"
              :file-list="showcaseFileList"
              :on-change="handleShowcaseFileChange"
              :on-remove="handleShowcaseFileRemove"
            >
              <el-button :icon="Upload">{{ showcaseForm.fileId ? '替换附件' : '选择附件' }}</el-button>
            </el-upload>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="showcaseDrawer = false">取消</el-button>
          <el-button type="primary" :icon="editingShowcase ? Check : Plus" :loading="saving" :disabled="!canSaveShowcase" @click="saveShowcase">
            {{ editingShowcase ? '保存' : '发布' }}
          </el-button>
        </div>
      </template>
    </WorkspaceDrawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile, type UploadUserFile } from 'element-plus'
import { Check, Delete, Edit, Plus, Upload } from '@element-plus/icons-vue'
import FileActions from '../components/FileActions.vue'
import WorkspaceDrawer from '../components/WorkspaceDrawer.vue'
import { collaborationService, fileService } from '../services/platform'
import { appState, can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import { formatDateTime } from '../utils/display'
import type { ProjectGroup, Showcase } from '../types'

const showcases = ref<Showcase[]>([])
const showcaseGroups = ref<ProjectGroup[]>([])
const showcaseDrawer = ref(false)
const editingShowcase = ref<Showcase | null>(null)
const loading = ref(false)
const saving = ref(false)
const deletingShowcaseId = ref<number | undefined>(undefined)
const showcaseFile = ref<File | null>(null)
const showcaseFileList = ref<UploadUserFile[]>([])
const showcaseForm = reactive({
  groupId: undefined as number | undefined,
  fileId: undefined as number | undefined,
  title: '',
  summary: '',
  linkUrl: '',
  status: 1
})

const drawerTitle = computed(() => editingShowcase.value ? '编辑成果' : '发布成果')
const canSaveShowcase = computed(() => {
  return Boolean(showcaseForm.groupId && showcaseForm.title.trim() && showcaseForm.summary.trim())
})
const groupNameMap = computed(() => new Map(showcaseGroups.value.map((group) => [group.id, group.name])))

async function loadShowcases() {
  loading.value = true
  try {
    const [items, groups] = await Promise.all([
      collaborationService.getShowcases(currentCourseId.value),
      collaborationService.getAccessibleGroups(currentCourseId.value)
    ])
    showcases.value = items
    showcaseGroups.value = groups
    if (showcaseDrawer.value && !showcaseGroups.value.some((group) => group.id === showcaseForm.groupId)) {
      showcaseForm.groupId = defaultGroupId()
    }
  } finally {
    loading.value = false
  }
}

function defaultGroupId() {
  return showcaseGroups.value[0]?.id
}

function groupName(groupId: number) {
  return groupNameMap.value.get(groupId) || `项目组 #${groupId}`
}

function formatShowcaseTime(value?: string) {
  return formatDateTime(value)
}

function openShowcaseDrawer() {
  resetShowcaseForm()
  showcaseDrawer.value = true
}

function openEditShowcaseDrawer(showcase: Showcase) {
  editingShowcase.value = showcase
  showcaseForm.groupId = showcase.groupId
  showcaseForm.fileId = showcase.fileId
  showcaseForm.title = showcase.title
  showcaseForm.summary = showcase.summary || ''
  showcaseForm.linkUrl = showcase.linkUrl || ''
  showcaseForm.status = 1
  showcaseFile.value = null
  showcaseFileList.value = []
  showcaseDrawer.value = true
}

function resetShowcaseForm() {
  editingShowcase.value = null
  showcaseForm.groupId = defaultGroupId()
  showcaseForm.fileId = undefined
  showcaseForm.title = ''
  showcaseForm.summary = ''
  showcaseForm.linkUrl = ''
  showcaseForm.status = 1
  showcaseFile.value = null
  showcaseFileList.value = []
  saving.value = false
}

function handleShowcaseFileChange(file: UploadFile) {
  showcaseFileList.value = [file]
  showcaseFile.value = file.raw || null
}

function handleShowcaseFileRemove() {
  showcaseFile.value = null
  showcaseFileList.value = []
}

function removeShowcaseFile() {
  showcaseForm.fileId = undefined
  showcaseFile.value = null
  showcaseFileList.value = []
}

function showcasePayload(fileId?: number) {
  return {
    courseId: currentCourseId.value,
    groupId: showcaseForm.groupId,
    fileId,
    title: showcaseForm.title.trim(),
    summary: showcaseForm.summary,
    linkUrl: showcaseForm.linkUrl,
    status: showcaseForm.status
  }
}

async function saveShowcase() {
  if (!canSaveShowcase.value) return
  saving.value = true
  try {
    let fileId = showcaseForm.fileId
    if (showcaseFile.value) {
      const uploaded = await fileService.upload(showcaseFile.value, appState.session.userId, 'showcase')
      fileId = uploaded.id
    }
    if (editingShowcase.value) {
      await collaborationService.updateShowcase(editingShowcase.value.id, showcasePayload(fileId))
      ElMessage.success('成果已更新')
    } else {
      await collaborationService.createShowcase(showcasePayload(fileId))
      ElMessage.success('成果已发布')
    }
    showcaseDrawer.value = false
    await loadShowcases()
  } finally {
    saving.value = false
  }
}

async function deleteShowcase(showcase: Showcase) {
  await ElMessageBox.confirm(`确认删除成果「${showcase.title}」？关联附件会进入文件回收流程。`, '删除成果', { type: 'warning' })
  deletingShowcaseId.value = showcase.id
  try {
    await collaborationService.deleteShowcase(showcase.id)
    ElMessage.success('成果已删除')
    await loadShowcases()
  } finally {
    deletingShowcaseId.value = undefined
  }
}

onMounted(loadShowcases)
watch([currentCourseId, refreshSignal], () => {
  showcaseDrawer.value = false
  resetShowcaseForm()
  void loadShowcases()
})
</script>

<style scoped>
.showcase-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
  max-height: calc(100vh - 270px);
  overflow: auto;
  padding-right: 4px;
}

.showcase-card {
  display: flex;
  min-height: 190px;
  flex-direction: column;
  padding: 16px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-surface);
  box-shadow: 0 10px 28px rgb(25 33 38 / 5%);
}

.showcase-card-top,
.showcase-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--app-muted);
  font-size: 12px;
}

.showcase-card h3 {
  margin: 14px 0 8px;
  color: var(--app-ink-strong);
  font-size: 17px;
  line-height: 1.35;
}

.showcase-card p {
  flex: 1;
  margin: 0 0 14px;
  color: var(--app-ink);
  line-height: 1.65;
  white-space: pre-wrap;
}

.showcase-card-foot {
  padding-top: 12px;
  border-top: 1px solid var(--app-divider);
}

.showcase-card .file-actions {
  margin: 0 0 12px;
}

.showcase-card-actions {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.drawer-file-field {
  display: grid;
  gap: 10px;
}

.drawer-current-file {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-surface);
}
</style>
