<template>
  <el-drawer
    v-model="visible"
    :title="title"
    append-to-body
    class="workspace-drawer"
    direction="rtl"
    :size="size"
    @closed="emit('closed')"
  >
    <slot />
    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  modelValue: boolean
  title: string
  size?: string
}>(), {
  size: '440px'
})

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'closed'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})
</script>
