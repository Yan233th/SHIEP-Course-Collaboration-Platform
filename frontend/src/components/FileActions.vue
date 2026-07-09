<template>
  <div v-if="fileId" class="file-actions" :class="`file-actions--${variant}`">
    <div class="file-actions-meta">
      <el-icon><Document /></el-icon>
      <span>{{ fileName }}</span>
      <small v-if="sizeText">{{ sizeText }}</small>
    </div>
    <div class="file-actions-buttons">
      <el-button v-if="previewSupported" size="small" text @click="previewOpen = true">预览</el-button>
      <el-link :href="downloadUrl">下载</el-link>
    </div>
    <FilePreviewDrawer v-model="previewOpen" :file-id="fileId" :file="file" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Document } from '@element-plus/icons-vue'
import FilePreviewDrawer from './FilePreviewDrawer.vue'
import { formatBytes } from '../utils/display'
import { previewKindForFile } from '../utils/filePreview'
import type { FileBrief } from '../types'

const props = withDefaults(defineProps<{
  fileId?: number
  file?: FileBrief | null
  variant?: 'card' | 'inline'
}>(), {
  variant: 'card'
})

const previewOpen = ref(false)
const fileName = computed(() => props.file?.originalName || `附件 #${props.fileId}`)
const previewSupported = computed(() => previewKindForFile(fileName.value, props.file?.contentType) !== 'unsupported')
const downloadUrl = computed(() => props.fileId ? `/api/files/download/${props.fileId}` : '')
const sizeText = computed(() => props.file?.sizeBytes == null ? '' : formatBytes(props.file.sizeBytes))
</script>
