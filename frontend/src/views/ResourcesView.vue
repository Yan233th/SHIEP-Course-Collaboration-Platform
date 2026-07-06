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
      <el-upload v-if="can('UPLOAD_RESOURCE')" :http-request="uploadFile" :show-file-list="false">
        <el-button :icon="Upload">上传文件</el-button>
      </el-upload>
    </div>

    <el-table :data="resources" height="calc(100vh - 270px)" empty-text="暂无课程资源">
      <el-table-column prop="title" label="资源" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column label="标签">
        <template #default="{ row }">
          <el-tag v-for="tag in tagList(row.tags)" :key="tag" effect="plain" class="tag-chip">{{ tag }}</el-tag>
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
import { onMounted, ref, watch } from 'vue'
import type { UploadRequestOptions } from 'element-plus'
import { Search, Upload } from '@element-plus/icons-vue'
import { courseService, fileService } from '../services/platform'
import { appState, can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import type { ResourceItem } from '../types'

const resources = ref<ResourceItem[]>([])
const resourceCategory = ref('')
const resourceTag = ref('')

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
  await loadResources()
}

onMounted(loadResources)
watch([currentCourseId, refreshSignal], loadResources)
</script>
