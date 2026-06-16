<template>
  <img
    :src="message.fileUrl"
    class="msg-image"
    :alt="message.fileName || '图片消息'"
    loading="lazy"
    @click="$emit('preview')"
  />
</template>

<script setup lang="ts">
defineProps<{ message: { fileUrl: string; fileName?: string } }>()
defineEmits<{ preview: [] }>()
</script>

<style scoped>
.msg-image {
  max-width: 320px;
  max-height: 320px;
  border-radius: var(--lc-radius-msg-image);
  cursor: pointer;
  display: block;
  transition: opacity var(--lc-duration-150) var(--lc-ease-out);
}
.msg-image:hover {
  opacity: 0.9;
}
</style>
