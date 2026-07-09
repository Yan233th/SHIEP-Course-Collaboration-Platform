<template>
  <div v-if="fileId" class="file-actions">
    <div class="file-actions-meta">
      <el-icon><Document /></el-icon>
      <span>{{ fileName }}</span>
      <small v-if="sizeText">{{ sizeText }}</small>
    </div>
    <div class="file-actions-buttons">
      <el-link :href="previewUrl" target="_blank">预览</el-link>
      <el-link :href="downloadUrl">下载</el-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Document } from '@element-plus/icons-vue'
import { formatBytes } from '../utils/display'
import type { FileBrief } from '../types'

const props = defineProps<{
  fileId?: number
  file?: FileBrief | null
}>()

const fileName = computed(() => props.file?.originalName || `附件 #${props.fileId}`)
const previewUrl = computed(() => props.file?.previewUrl || `/api/files/preview/${props.fileId}`)
const downloadUrl = computed(() => props.fileId ? `/api/files/download/${props.fileId}` : '')
const sizeText = computed(() => props.file?.sizeBytes == null ? '' : formatBytes(props.file.sizeBytes))
</script>
