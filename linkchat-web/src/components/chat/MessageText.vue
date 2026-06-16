<template>
  <div class="msg-text">{{ message.content }}</div>
</template>

<script setup lang="ts">
defineProps<{ message: { content: string } }>()
</script>

<style scoped>
.msg-text {
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: break-word;
  line-height: var(--lc-line-height-relaxed);
}
</style>
