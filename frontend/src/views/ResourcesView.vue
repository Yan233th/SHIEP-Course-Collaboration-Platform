<template>
  <section class="panel" v-loading="loading">
    <div class="section-heading">
      <div>
        <h2>课程资源</h2>
        <p>{{ currentCourseLabel }}</p>
      </div>
      <strong>{{ resources.length }} 份</strong>
    </div>

    <div class="panel-toolbar">
      <el-select v-model="resourceCategory" placeholder="分类" clearable>
        <el-option label="课件" value="课件" />
        <el-option label="实验" value="实验" />
        <el-option label="参考资料" value="参考资料" />
      </el-select>
      <el-input v-model="resourceTag" placeholder="标签" clearable />
      <el-button :icon="Search" @click="loadResources">查询</el-button>
      <el-button v-if="hasActiveFilters" :icon="Close" @click="clearFilters">清除筛选</el-button>
      <el-button v-if="canManageResource" type="primary" :icon="Upload" @click="openCreateResource">上传资源</el-button>
    </div>

    <div v-if="tagOptions.length" class="resource-tag-bar">
      <span>资源标签</span>
      <el-tag
        v-for="tag in tagOptions"
        :key="tag"
        effect="plain"
        class="tag-chip clickable-tag"
        :class="{ active: resourceTag === tag }"
        @click="selectTag(tag)"
      >
        {{ tag }}
      </el-tag>
    </div>

    <el-table :data="resources" height="calc(100vh - 270px)" empty-text="暂无课程资源">
      <el-table-column prop="title" label="资源" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column label="标签">
        <template #default="{ row }">
          <el-tag
            v-for="tag in splitTags(row.tags)"
            :key="tag"
            effect="plain"
            class="tag-chip clickable-tag"
            :class="{ active: resourceTag === tag }"
            @click.stop="selectTag(tag)"
          >
            {{ tag }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="预览" width="90">
        <template #default="{ row }">
          <el-link v-if="row.fileId" :href="`/api/files/preview/${row.fileId}`" target="_blank">打开</el-link>
        </template>
      </el-table-column>
      <el-table-column v-if="canManageResource" label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEditResource(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="deleteResource(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <WorkspaceDrawer v-model="resourceDrawer" :title="editingResource ? '编辑资源' : '上传资源'" @closed="resetResourceForm">
      <el-form :model="resourceForm" label-position="top" class="drawer-form">
        <el-form-item label="资源标题"><el-input v-model="resourceForm.title" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="resourceForm.category" filterable allow-create default-first-option placeholder="选择或输入分类">
            <el-option v-for="category in categoryOptions" :key="category" :label="category" :value="category" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="resourceForm.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入标签"
          >
            <el-option v-for="tag in tagOptions" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!editingResource" label="文件">
          <el-upload
            action="#"
            :auto-upload="false"
            :limit="1"
            :file-list="resourceFileList"
            :on-change="handleResourceFileChange"
            :on-remove="handleResourceFileRemove"
          >
            <el-button :icon="Upload">选择文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="resourceDrawer = false">取消</el-button>
          <el-button type="primary" :icon="editingResource ? Check : Upload" :loading="saving" :disabled="!canSaveResource" @click="saveResource">
            {{ editingResource ? '保存' : '上传' }}
          </el-button>
        </div>
      </template>
    </WorkspaceDrawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile, type UploadUserFile } from 'element-plus'
import { Check, Close, Search, Upload } from '@element-plus/icons-vue'
import WorkspaceDrawer from '../components/WorkspaceDrawer.vue'
import { courseService, fileService } from '../services/platform'
import { appState, can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import { joinTags, splitTags } from '../utils/display'
import type { ResourceItem } from '../types'

const resources = ref<ResourceItem[]>([])
const allResources = ref<ResourceItem[]>([])
const resourceCategory = ref('')
const resourceTag = ref('')
const loading = ref(false)
const saving = ref(false)
const resourceDrawer = ref(false)
const editingResource = ref<ResourceItem | null>(null)
const resourceFile = ref<File | null>(null)
const resourceFileList = ref<UploadUserFile[]>([])
const resourceForm = ref({
  title: '',
  category: '课件',
  tags: [] as string[],
  status: 1
})
const canManageResource = computed(() => can('UPLOAD_RESOURCE'))
const hasActiveFilters = computed(() => Boolean(resourceCategory.value || resourceTag.value))
const tagOptions = computed(() => {
  const tags = new Set<string>()
  allResources.value.forEach((resource) => {
    splitTags(resource.tags).forEach((tag) => tags.add(tag))
  })
  resourceForm.value.tags.forEach((tag) => tags.add(tag))
  return [...tags].sort((a, b) => a.localeCompare(b, 'zh-CN'))
})
const categoryOptions = computed(() => {
  const categories = new Set(['课件', '实验', '参考资料'])
  allResources.value.forEach((resource) => {
    if (resource.category) categories.add(resource.category)
  })
  return [...categories]
})
const canSaveResource = computed(() => {
  const hasMetadata = Boolean(resourceForm.value.title.trim() && resourceForm.value.category.trim())
  return editingResource.value ? hasMetadata : Boolean(hasMetadata && resourceFile.value)
})

async function loadResources() {
  resources.value = await courseService.getResources({
    courseId: currentCourseId.value,
    category: resourceCategory.value || undefined,
    tag: resourceTag.value || undefined
  })
}

async function loadTagOptions() {
  allResources.value = await courseService.getResources({
    courseId: currentCourseId.value
  })
}

async function selectTag(tag: string) {
  resourceTag.value = resourceTag.value === tag ? '' : tag
  await loadResources()
}

async function clearFilters() {
  resourceCategory.value = ''
  resourceTag.value = ''
  await loadResources()
}

function openCreateResource() {
  resetResourceForm()
  resourceDrawer.value = true
}

function openEditResource(resource: ResourceItem) {
  editingResource.value = resource
  resourceForm.value = {
    title: resource.title,
    category: resource.category || '课件',
    tags: splitTags(resource.tags),
    status: 1
  }
  resourceDrawer.value = true
}

function resetResourceForm() {
  editingResource.value = null
  resourceFile.value = null
  resourceFileList.value = []
  resourceForm.value = { title: '', category: '课件', tags: [], status: 1 }
  saving.value = false
}

function handleResourceFileChange(file: UploadFile) {
  resourceFileList.value = [file]
  resourceFile.value = file.raw || null
  if (!resourceForm.value.title.trim() && file.name) {
    resourceForm.value.title = file.name
  }
}

function handleResourceFileRemove() {
  resourceFile.value = null
  resourceFileList.value = []
}

function resourcePayload(fileId?: number) {
  return {
    courseId: currentCourseId.value,
    fileId,
    title: resourceForm.value.title.trim(),
    category: resourceForm.value.category.trim(),
    tags: joinTags(resourceForm.value.tags),
    uploaderId: appState.session.userId,
    status: resourceForm.value.status
  }
}

async function saveResource() {
  saving.value = true
  try {
    if (editingResource.value) {
      await courseService.updateResource(editingResource.value.id, resourcePayload(editingResource.value.fileId))
      ElMessage.success('资源信息已更新')
    } else {
      if (!resourceFile.value) return
      const file = await fileService.upload(resourceFile.value, appState.session.userId, 'resource')
      await courseService.createResource(resourcePayload(file.id))
      ElMessage.success('资源已上传')
    }
    resourceDrawer.value = false
    await loadResourcePage()
  } finally {
    saving.value = false
  }
}

async function deleteResource(resource: ResourceItem) {
  await ElMessageBox.confirm(`确认删除资源「${resource.title}」？`, '删除资源', { type: 'warning' })
  await courseService.deleteResource(resource.id)
  ElMessage.success('资源已删除')
  await loadResourcePage()
}

async function loadResourcePage() {
  loading.value = true
  try {
    await loadTagOptions()
    await loadResources()
  } finally {
    loading.value = false
  }
}

onMounted(loadResourcePage)
watch(currentCourseId, async () => {
  resourceCategory.value = ''
  resourceTag.value = ''
  await loadResourcePage()
})
watch(refreshSignal, loadResourcePage)
</script>
