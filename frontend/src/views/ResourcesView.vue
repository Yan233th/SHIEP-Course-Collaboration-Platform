<template>
  <section class="panel">
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
      <el-upload v-if="can('UPLOAD_RESOURCE')" :http-request="uploadFile" :show-file-list="false">
        <el-button :icon="Upload">上传文件</el-button>
      </el-upload>
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
            v-for="tag in tagList(row.tags)"
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
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import type { UploadRequestOptions } from 'element-plus'
import { Close, Search, Upload } from '@element-plus/icons-vue'
import { courseService, fileService } from '../services/platform'
import { appState, can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import type { ResourceItem } from '../types'

const resources = ref<ResourceItem[]>([])
const allResources = ref<ResourceItem[]>([])
const resourceCategory = ref('')
const resourceTag = ref('')
const hasActiveFilters = computed(() => Boolean(resourceCategory.value || resourceTag.value))
const tagOptions = computed(() => {
  const tags = new Set<string>()
  allResources.value.forEach((resource) => {
    tagList(resource.tags).forEach((tag) => tags.add(tag))
  })
  return [...tags].sort((a, b) => a.localeCompare(b, 'zh-CN'))
})

function tagList(tags?: string) {
  return (tags || '')
    .split(/[,，\s]+/)
    .map((tag) => tag.trim())
    .filter(Boolean)
}

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

async function uploadFile(option: UploadRequestOptions) {
  const file = await fileService.upload(option.file, appState.session.userId, 'resource')
  await courseService.createResource({
    courseId: currentCourseId.value,
    fileId: file.id,
    title: file.originalName,
    category: resourceCategory.value || '课件',
    tags: resourceTag.value,
    uploaderId: appState.session.userId,
    status: 1
  })
  await loadTagOptions()
  await loadResources()
}

async function loadResourcePage() {
  await loadTagOptions()
  await loadResources()
}

onMounted(loadResourcePage)
watch(currentCourseId, async () => {
  resourceCategory.value = ''
  resourceTag.value = ''
  await loadResourcePage()
})
watch(refreshSignal, loadResourcePage)
</script>
