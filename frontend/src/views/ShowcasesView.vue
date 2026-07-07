<template>
  <section class="panel">
    <div class="section-heading compact">
      <div>
        <h2>成果展示</h2>
        <p>{{ currentCourseLabel }}</p>
      </div>
      <div class="heading-actions">
        <strong>{{ showcases.length }} 项</strong>
        <el-button v-if="can('PUBLISH_SHOWCASE')" type="primary" :icon="Plus" @click="openShowcaseDrawer">
          发布成果
        </el-button>
      </div>
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

    <el-drawer
      v-model="showcaseDrawer"
      title="发布成果"
      append-to-body
      class="workspace-drawer"
      direction="rtl"
      size="440px"
      @closed="resetShowcaseForm"
    >
      <el-form :model="showcaseForm" label-position="top" class="drawer-form">
        <el-form-item label="项目组ID"><el-input-number v-model="showcaseForm.groupId" :min="1" /></el-form-item>
        <el-form-item label="标题"><el-input v-model="showcaseForm.title" /></el-form-item>
        <el-form-item label="链接"><el-input v-model="showcaseForm.linkUrl" /></el-form-item>
        <el-form-item label="摘要"><el-input v-model="showcaseForm.summary" type="textarea" :rows="6" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="showcaseDrawer = false">取消</el-button>
          <el-button type="primary" :icon="Plus" :disabled="!canCreateShowcase" @click="createShowcase">发布</el-button>
        </div>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { collaborationService } from '../services/platform'
import { can, currentCourseId, currentCourseLabel, refreshSignal } from '../state/appState'
import type { Showcase } from '../types'

const showcases = ref<Showcase[]>([])
const showcaseDrawer = ref(false)
const showcaseForm = reactive({ groupId: 1, fileId: undefined as number | undefined, title: '', summary: '', linkUrl: '', status: 1 })
const canCreateShowcase = computed(() => Boolean(showcaseForm.groupId && showcaseForm.title.trim() && showcaseForm.summary.trim()))

async function loadShowcases() {
  showcases.value = await collaborationService.getShowcases(currentCourseId.value)
}

function openShowcaseDrawer() {
  resetShowcaseForm()
  showcaseDrawer.value = true
}

function resetShowcaseForm() {
  showcaseForm.groupId = 1
  showcaseForm.fileId = undefined
  showcaseForm.title = ''
  showcaseForm.summary = ''
  showcaseForm.linkUrl = ''
  showcaseForm.status = 1
}

async function createShowcase() {
  await collaborationService.createShowcase({
    courseId: currentCourseId.value,
    ...showcaseForm
  })
  showcaseDrawer.value = false
  resetShowcaseForm()
  await loadShowcases()
}

onMounted(loadShowcases)
watch([currentCourseId, refreshSignal], () => {
  showcaseDrawer.value = false
  resetShowcaseForm()
  void loadShowcases()
})
</script>
