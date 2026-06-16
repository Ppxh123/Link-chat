<template>
  <div class="conversation-search">
    <LcSearchBox
      :model-value="modelValue"
      placeholder="搜索会话..."
      @update:model-value="$emit('update:modelValue', $event)"
      @search="$emit('search')"
    />
  </div>
</template>

<script setup lang="ts">
import LcSearchBox from '@/components/design/LcSearchBox.vue'

defineProps<{ modelValue: string }>()
defineEmits<{
  'update:modelValue': [v: string]
  search: []
}>()
</script>

<style scoped>
.conversation-search {
  padding: var(--lc-space-3) var(--lc-space-4);
  border-bottom: 1px solid var(--lc-border-light);
}
</style>
