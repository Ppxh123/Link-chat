<template>
  <div class="msg-file">
    <div class="msg-file__card" @click="$emit('preview')">
      <FileCard
        :file-url="message.fileUrl"
        :file-name="message.fileName || '未知文件'"
        :file-size="message.fileSize"
      />
    </div>
    <button class="msg-file__download" title="下载文件" @click.stop="handleDownload">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
      </svg>
    </button>
  </div>
</template>

<script setup lang="ts">
import FileCard from '@/components/file/FileCard.vue'

const props = defineProps<{ message: { fileUrl: string; fileName?: string; fileSize?: number; fileMime?: string } }>()
defineEmits<{ preview: [] }>()

async function handleDownload() {
  try {
    const res = await fetch(props.message.fileUrl)
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = props.message.fileName || 'file'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch {
    window.open(props.message.fileUrl, '_blank')
  }
}
</script>

<style scoped>
.msg-file {
  display: flex;
  align-items: center;
  gap: 8px;
}
.msg-file__card {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}
.msg-file__download {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: var(--lc-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--lc-text-tertiary);
  background: transparent;
  border: none;
  transition: all var(--lc-duration-100) var(--lc-ease-out);
}
.msg-file__download:hover {
  background: var(--lc-bg-hover);
  color: var(--lc-primary);
}
</style>
