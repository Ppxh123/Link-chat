<template>
  <div class="file-card lc-interactive">
    <div class="file-card__icon">
      <!-- 图片类型 -->
      <svg v-if="isImage" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
        <circle cx="8.5" cy="8.5" r="1.5"/>
        <polyline points="21 15 16 10 5 21"/>
      </svg>
      <!-- PDF 类型 -->
      <svg v-else-if="isPdf" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
        <polyline points="14 2 14 8 20 8"/>
        <line x1="8" y1="13" x2="14" y2="13"/>
        <line x1="8" y1="17" x2="12" y2="17"/>
      </svg>
      <!-- 其他文件 -->
      <svg v-else width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
        <polyline points="14 2 14 8 20 8"/>
      </svg>
    </div>
    <div class="file-card__info">
      <span class="file-card__name lc-truncate">{{ displayName }}</span>
      <span class="file-card__size">{{ formatSize(fileSize) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { isImageFile, isPdfFile, formatFileSize } from '@/utils/file'

const props = defineProps<{
  fileUrl: string
  fileName: string
  fileSize?: number
}>()

const isImage = computed(() => isImageFile(props.fileName))
const isPdf = computed(() => isPdfFile(props.fileName))
const displayName = computed(() => props.fileName || '未知文件')

function formatSize(bytes?: number): string {
  return formatFileSize(bytes)
}
</script>

<style scoped>
.file-card {
  display: flex;
  align-items: center;
  gap: var(--lc-space-3);
  padding: var(--lc-space-2) 0;
  cursor: pointer;
  max-width: 280px;
}

.file-card__icon {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: var(--lc-radius-md);
  background: var(--lc-bg-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--lc-text-secondary);
}

.file-card__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.file-card__name {
  font-size: var(--lc-font-size-sm);
  font-weight: var(--lc-font-weight-medium);
  color: var(--lc-text-primary);
  max-width: 200px;
}

.file-card__size {
  font-size: var(--lc-font-size-xs);
  color: var(--lc-text-tertiary);
}
</style>
