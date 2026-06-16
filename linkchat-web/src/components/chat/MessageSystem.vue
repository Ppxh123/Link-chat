<template>
  <div class="msg-system">
    <span class="msg-system__text">{{ text }}</span>
    <button v-if="isRecalled && isMine && !reEdited" class="msg-system__re-edit" @click="$emit('re-edit')">重新编辑</button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ isMine: boolean; isRecalled?: number; senderName?: string; reEdited?: boolean }>()
defineEmits<{ 're-edit': [] }>()

const text = computed(() => {
  if (props.isRecalled) {
    return props.isMine ? '你撤回了一条消息' : `「${props.senderName || '对方'}」撤回了一条消息`
  }
  return ''
})
</script>

<style scoped>
.msg-system {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 4px 0;
}

.msg-system__text {
  font-size: var(--lc-font-size-xs);
  color: var(--lc-text-tertiary);
}

.msg-system__re-edit {
  font-size: var(--lc-font-size-xs);
  color: #1890ff;
  cursor: pointer;
  background: none;
  border: none;
  padding: 0;
  text-decoration: none;
  font-family: inherit;
}

.msg-system__re-edit:hover {
  text-decoration: underline;
  color: #40a9ff;
}
</style>
