<template>
  <section class="panel split">
    <div class="content-column">
      <div class="section-heading compact">
        <div>
          <h2>成果展示</h2>
          <p>{{ currentCourseLabel }}</p>
        </div>
        <strong>{{ showcases.length }} 项</strong>
      </div>
      <el-table :data="showcases" height="calc(100vh - 270px)" empty-text="暂无成果">
        <el-table-column prop="title" label="成果" />
        <el-table-column prop="summary" label="摘要" />
        <el-table-column label="链接" width="90">
          <template #default="{ row }">
            <el-link v-if="row.linkUrl" :href="row.linkUrl" target="_blank">打开</el-link>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-form v-if="can('PUBLISH_SHOWCASE')" :model="showcaseForm" label-position="top" class="side-form">
      <h3>发布成果</h3>
      <el-form-item label="项目组ID"><el-input-number v-model="showcaseForm.groupId" :min="1" /></el-form-item>
      <el-form-item label="标题"><el-input v-model="showcaseForm.title" /></el-form-item>
      <el-form-item label="链接"><el-input v-model="showcaseForm.linkUrl" /></el-form-item>
      <el-form-item label="摘要"><el-input v-model="showcaseForm.summary" type="textarea" :rows="5" /></el-form-item>
      <el-button type="primary" :icon="Plus" :disabled="!canCreateShowcase" @click="createShowcase">发布成果</el-button>
    </el-form>
    <aside v-else class="side-form muted-panel">当前角色只能查看成果展示。</aside>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { collaborationService } from '../services/platform'
import { can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import type { Showcase } from '../types'

const showcases = ref<Showcase[]>([])
const showcaseForm = reactive({ groupId: 1, fileId: undefined as number | undefined, title: '', summary: '', linkUrl: '', status: 1 })
const canCreateShowcase = computed(() => Boolean(showcaseForm.groupId && showcaseForm.title.trim() && showcaseForm.summary.trim()))

async function loadShowcases() {
  showcases.value = await collaborationService.getShowcases(currentCourseId.value)
}

async function createShowcase() {
  await collaborationService.createShowcase({
    courseId: currentCourseId.value,
    ...showcaseForm
  })
  showcaseForm.title = ''
  showcaseForm.summary = ''
  showcaseForm.linkUrl = ''
  await loadShowcases()
}

onMounted(loadShowcases)
watch([currentCourseId, refreshSignal], loadShowcases)
</script>
