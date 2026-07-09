<template>
  <WorkspaceDrawer v-model="visible" :title="fileName" size="min(880px, calc(100vw - 28px))" @closed="resetTextPreview">
    <div class="file-preview-shell">
      <div class="file-preview-meta">
        <div>
          <span>{{ previewKindLabel }}</span>
          <strong>{{ fileName }}</strong>
        </div>
        <small v-if="sizeText">{{ sizeText }}</small>
      </div>

      <div class="file-preview-stage">
        <img v-if="previewKind === 'image'" class="image-preview" :src="previewUrl" :alt="fileName" />

        <iframe
          v-else-if="previewKind === 'pdf'"
          class="pdf-preview"
          :src="previewUrl"
          :title="fileName"
        />

        <div v-else-if="previewKind === 'text' || previewKind === 'markdown'" v-loading="textLoading" class="text-preview-pane">
          <el-alert v-if="textError" type="warning" :closable="false" :title="textError" />
          <template v-else>
            <div v-if="textTruncated" class="preview-note">文件较大，当前只显示前 {{ textLimitText }}。</div>
            <pre :class="{ 'markdown-source': previewKind === 'markdown' }">{{ textContent || '暂无可预览内容' }}</pre>
          </template>
        </div>

        <div v-else class="unsupported-preview">
          <el-icon><Document /></el-icon>
          <strong>该格式暂不支持内嵌预览</strong>
          <p>可以直接下载后使用本地软件打开。</p>
        </div>
      </div>

      <div class="file-preview-actions">
        <el-link :href="previewUrl" target="_blank">在新窗口打开</el-link>
        <el-link :href="downloadUrl">下载</el-link>
      </div>
    </div>
  </WorkspaceDrawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { Document } from '@element-plus/icons-vue'
import WorkspaceDrawer from './WorkspaceDrawer.vue'
import { TOKEN_KEY } from '../state/appState'
import { formatBytes } from '../utils/display'
import { previewKindForFile } from '../utils/filePreview'
import type { FileBrief } from '../types'

const TEXT_PREVIEW_LIMIT = 200 * 1024

const props = defineProps<{
  modelValue: boolean
  fileId?: number
  file?: FileBrief | null
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
}>()

const textState = reactive({
  fileId: undefined as number | undefined,
  content: '',
  error: '',
  truncated: false
})
const textLoading = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const fileName = computed(() => props.file?.originalName || `附件 #${props.fileId}`)
const previewUrl = computed(() => props.file?.previewUrl || `/api/files/preview/${props.fileId}`)
const downloadUrl = computed(() => props.fileId ? `/api/files/download/${props.fileId}` : '')
const sizeText = computed(() => props.file?.sizeBytes == null ? '' : formatBytes(props.file.sizeBytes))
const textLimitText = computed(() => formatBytes(TEXT_PREVIEW_LIMIT))
const textContent = computed(() => textState.content)
const textError = computed(() => textState.error)
const textTruncated = computed(() => textState.truncated)

const previewKind = computed(() => previewKindForFile(fileName.value, props.file?.contentType))

const previewKindLabel = computed(() => {
  const labels: Record<string, string> = {
    image: '图片预览',
    pdf: 'PDF 预览',
    text: '文本预览',
    markdown: 'Markdown 预览',
    unsupported: '附件预览'
  }
  return labels[previewKind.value]
})

watch(
  () => [visible.value, props.fileId, previewKind.value] as const,
  () => {
    if (!visible.value || !props.fileId) return
    if (previewKind.value === 'text' || previewKind.value === 'markdown') {
      void loadTextPreview()
    }
  },
  { immediate: true }
)

async function loadTextPreview() {
  if (!props.fileId || textState.fileId === props.fileId) return
  textLoading.value = true
  textState.fileId = props.fileId
  textState.content = ''
  textState.error = ''
  textState.truncated = false
  try {
    const token = localStorage.getItem(TOKEN_KEY)
    const response = await fetch(previewUrl.value, {
      headers: token ? { Authorization: `Bearer ${token}` } : undefined
    })
    if (!response.ok) {
      throw new Error(`预览失败：${response.status}`)
    }
    const body = await response.text()
    textState.truncated = body.length > TEXT_PREVIEW_LIMIT
    textState.content = textState.truncated ? body.slice(0, TEXT_PREVIEW_LIMIT) : body
  } catch (error) {
    textState.error = error instanceof Error ? error.message : '预览失败'
  } finally {
    textLoading.value = false
  }
}

function resetTextPreview() {
  textState.fileId = undefined
  textState.content = ''
  textState.error = ''
  textState.truncated = false
  textLoading.value = false
}
</script>

<style scoped>
.file-preview-shell {
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 14px;
}

.file-preview-meta {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-surface-soft);
}

.file-preview-meta div {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.file-preview-meta span,
.file-preview-meta small {
  color: var(--app-muted);
  font-size: 12px;
  font-weight: 600;
}

.file-preview-meta strong {
  overflow: hidden;
  color: var(--app-ink-strong);
  font-size: 14px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-preview-stage {
  min-height: 420px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-bg-warm);
}

.image-preview {
  max-width: 100%;
  max-height: 68vh;
  object-fit: contain;
}

.pdf-preview {
  width: 100%;
  height: 68vh;
  min-height: 500px;
  border: 0;
  background: #fff;
}

.text-preview-pane {
  width: 100%;
  height: 68vh;
  min-height: 500px;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 10px;
  padding: 12px;
  place-self: stretch;
}

.preview-note {
  padding: 8px 10px;
  border: 1px solid var(--app-brand-soft-strong);
  border-radius: 8px;
  color: var(--app-brand-deep);
  background: rgb(var(--app-brand-rgb) / 7%);
  font-size: 12px;
  font-weight: 600;
}

.text-preview-pane pre {
  min-height: 0;
  margin: 0;
  overflow: auto;
  padding: 14px;
  border: 1px solid var(--app-divider);
  border-radius: 8px;
  color: var(--app-ink);
  background: var(--app-surface);
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.text-preview-pane pre.markdown-source {
  font-family: var(--app-font-sans);
  font-size: 13px;
  line-height: 1.8;
}

.unsupported-preview {
  max-width: 360px;
  display: grid;
  justify-items: center;
  gap: 8px;
  padding: 28px;
  color: var(--app-muted);
  text-align: center;
}

.unsupported-preview .el-icon {
  color: var(--app-brand);
  font-size: 34px;
}

.unsupported-preview strong {
  color: var(--app-ink-strong);
}

.unsupported-preview p {
  margin: 0;
  line-height: 1.6;
}

.file-preview-actions {
  display: flex;
  justify-content: flex-end;
  gap: 14px;
}
</style>
