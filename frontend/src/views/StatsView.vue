<template>
  <section class="panel">
    <div class="section-heading">
      <div>
        <h2>课程统计</h2>
        <p>按课程汇总成员、资源、作业和项目组。</p>
      </div>
      <strong>{{ stats.length }} 条</strong>
    </div>
    <div class="panel-toolbar">
      <el-button :icon="Refresh" @click="loadStats">刷新统计</el-button>
    </div>
    <el-table :data="stats" height="calc(100vh - 250px)">
      <el-table-column prop="course_name" label="课程" />
      <el-table-column prop="member_count" label="成员" width="90" />
      <el-table-column prop="resource_count" label="资源" width="90" />
      <el-table-column prop="assignment_count" label="作业" width="90" />
      <el-table-column prop="project_group_count" label="项目组" width="100" />
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { courseService } from '../services/platform'
import type { CourseStats } from '../types'

const stats = ref<CourseStats[]>([])

async function loadStats() {
  stats.value = await courseService.getStats()
}

onMounted(loadStats)
</script>
