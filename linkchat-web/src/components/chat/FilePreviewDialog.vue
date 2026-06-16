<template>
  <LcModal :model-value="visible" :title="message.fileName || '文件预览'" width="800px" @update:model-value="$emit('close')" @closed="$emit('close')">
    <div class="preview-container">
      <!-- 图片 -->
      <template v-if="previewType === 'image'">
        <img v-if="!imageError" :src="message.fileUrl" class="preview-image" @error="onImageError" />
        <div v-else class="preview-error">
          <p>图片加载失败</p>
          <p class="preview-hint">请下载后查看</p>
        </div>
      </template>
      <!-- PDF -->
      <template v-else-if="previewType === 'pdf'">
        <iframe v-if="!pdfError" :src="message.fileUrl" class="preview-iframe" @error="onPdfError" />
        <div v-else class="preview-error">
          <p>PDF 预览失败</p>
          <p class="preview-hint">请下载后查看</p>
        </div>
      </template>
      <!-- Office 文档：不尝试在线预览（需公网URL），直接引导下载 -->
      <div v-else-if="previewType === 'office'" class="preview-office">
        <div class="preview-office__icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
        </div>
        <p class="preview-office__name">{{ message.fileName }}</p>
        <p class="preview-hint">Office 文档请下载后查看</p>
      </div>
      <!-- 文本 -->
      <div v-else-if="previewType === 'text'" class="preview-text-wrapper">
        <pre v-if="!loadingText" class="preview-text" :class="{ 'preview-text--error': textError }">{{ textContent }}</pre>
        <div v-else class="preview-loading">加载中...</div>
      </div>
      <!-- 不支持 -->
      <div v-else class="preview-unsupported">
        <p>该文件类型不支持预览</p>
        <p class="preview-hint">请下载后查看</p>
      </div>
    </div>
    <template #footer>
      <LcButton variant="secondary" @click="$emit('close')">关闭</LcButton>
      <LcButton variant="primary" :loading="downloading" @click="download">下载文件</LcButton>
    </template>
  </LcModal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import LcModal from '@/components/design/LcModal.vue'
import LcButton from '@/components/design/LcButton.vue'
import { getFileExtension, isImageFile, isPdfFile, isOfficeFile, isTextFile } from '@/utils/file'

const props = defineProps<{
  visible: boolean
  message: { fileUrl: string; fileName?: string; fileMime?: string }
}>()

defineEmits<{ close: [] }>()

const textContent = ref('')
const loadingText = ref(false)
const textError = ref(false)
const imageError = ref(false)
const pdfError = ref(false)
const downloading = ref(false)

const extension = computed(() => getFileExtension(props.message.fileName || ''))
const previewType = computed(() => {
  const name = props.message.fileName || ''
  if (isImageFile(name)) return 'image'
  if (isPdfFile(name)) return 'pdf'
  if (isOfficeFile(name)) return 'office'
  if (isTextFile(name)) return 'text'
  return 'unsupported'
})

watch(() => props.visible, async (v) => {
  // 重置所有错误状态
  imageError.value = false
  pdfError.value = false
  textError.value = false

  if (v && previewType.value === 'text') {
    loadingText.value = true
    textError.value = false
    try {
      const res = await fetch(props.message.fileUrl)
      if (res.ok) {
        textContent.value = await res.text()
      } else {
        textError.value = true
        textContent.value = '[无法加载文本内容，请下载后查看]'
      }
    } catch {
      textError.value = true
      textContent.value = '[无法加载文本内容，请下载后查看]'
    }
    loadingText.value = false
  }
})

function onImageError() {
  imageError.value = true
}

function onPdfError() {
  pdfError.value = true
}

async function download() {
  downloading.value = true
  try {
    // 使用 fetch + Blob 方式下载，绕开迅雷等第三方下载工具
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
    // fetch 失败时降级为直接打开（可能触发浏览器原生下载或新标签页）
    window.open(props.message.fileUrl, '_blank')
  } finally {
    downloading.value = false
  }
}
</script>

<style scoped>
.preview-container { display: flex; justify-content: center; align-items: center; min-height: 200px; max-height: 60vh; overflow: auto; background: var(--lc-bg-app); border-radius: var(--lc-radius-md); }
.preview-image { max-width: 100%; max-height: 60vh; object-fit: contain; }
.preview-iframe { width: 100%; height: 60vh; border: none; border-radius: var(--lc-radius-md); }
.preview-text-wrapper { width: 100%; padding: var(--lc-space-4); }
.preview-text { white-space: pre-wrap; word-break: break-all; font-size: var(--lc-font-size-sm); line-height: 1.6; color: var(--lc-text-primary); margin: 0; max-height: 55vh; overflow: auto; font-family: var(--lc-font-mono); }
.preview-text--error { color: var(--lc-text-secondary); }
.preview-loading { text-align: center; color: var(--lc-text-secondary); padding: var(--lc-space-10); }
.preview-unsupported { text-align: center; padding: var(--lc-space-10); color: var(--lc-text-secondary); }
.preview-error { text-align: center; padding: var(--lc-space-10); color: var(--lc-text-secondary); }
.preview-office { text-align: center; padding: var(--lc-space-10); }
.preview-office__icon { color: var(--lc-text-tertiary); margin-bottom: var(--lc-space-4); }
.preview-office__name { color: var(--lc-text-primary); font-weight: var(--lc-font-weight-medium); font-size: var(--lc-font-size-lg); margin: 0 0 var(--lc-space-2); word-break: break-all; }
.preview-hint { font-size: var(--lc-font-size-sm); color: var(--lc-text-tertiary); margin-top: var(--lc-space-2); }
</style>
