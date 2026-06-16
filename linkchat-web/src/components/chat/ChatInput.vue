<template>
  <div class="chat-input">
    <!-- 工具栏 -->
    <div class="chat-input__toolbar">
      <button class="chat-input__tool-btn" title="表情" @click="showEmoji = !showEmoji">
        🙂
      </button>
      <button class="chat-input__tool-btn" :disabled="uploading" title="图片" @click="triggerFile('image/*', 'IMAGE')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/>
        </svg>
      </button>
      <button class="chat-input__tool-btn" :disabled="uploading" title="文件" @click="triggerFile('.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar,.7z,.csv,.md,.json,.xml', 'FILE')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/>
        </svg>
      </button>

      <input ref="fileInput" type="file" style="display:none" :disabled="uploading" @change="handleFileSelect" />

      <span v-if="uploading" class="chat-input__uploading">上传中...</span>

      <div class="chat-input__spacer" />

      <button class="chat-input__send-btn" :disabled="!text.trim() || uploading || sending" @click="handleSend">
        <svg v-if="!sending" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
        </svg>
        <span v-else class="chat-input__spinner"></span>
      </button>
    </div>

    <!-- 输入区域 -->
    <textarea
      v-model="text"
      class="chat-input__textarea"
      placeholder="输入消息..."
      rows="1"
      @keydown.enter.exact.prevent="handleSend"
      @input="onInput"
    />

    <!-- 表情面板 -->
    <div v-if="showEmoji" class="chat-input__emoji-panel">
      <button v-for="e in emojis" :key="e" class="chat-input__emoji" @click="insertEmoji(e)">{{ e }}</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { fileApi } from '@/api/file'
import { toast } from '@/components/design/LcToast.ts'
import type { SendMessageRequest } from '@/types/message'

const props = defineProps<{ peerId: number | null; groupId: number | null }>()
const emit = defineEmits<{
  send: [data: SendMessageRequest]
  typing: []
  stopTyping: []
}>()

const text = ref('')
const showEmoji = ref(false)
const uploading = ref(false)
const sending = ref(false)
const fileInput = ref<HTMLInputElement>()
let pendingFileType: 'IMAGE' | 'FILE' = 'FILE'
let typingHeartbeat: number | null = null

const emojis = ['😀','😂','😍','🤔','😎','👍','❤️','🔥','🎉','🙏','😢','😡','🥳','💪','👏','✨','🤗','🫡','😭','💀']

// 启动周期性 typing 心跳：输入框非空时每 3s 发一次，保持接收方活跃
function startTypingHeartbeat() {
  stopTypingHeartbeat()
  typingHeartbeat = window.setInterval(() => {
    if (text.value) {
      emit('typing')
    } else {
      stopTypingHeartbeat()
    }
  }, 3000)
}

function stopTypingHeartbeat() {
  if (typingHeartbeat) {
    clearInterval(typingHeartbeat)
    typingHeartbeat = null
  }
}

// 监听输入文本变化：变空时发 stopTyping + 停心跳
watch(text, (newVal, oldVal) => {
  if (!oldVal && newVal) {
    // 从空变非空 → 发 typing + 启心跳
    emit('typing')
    startTypingHeartbeat()
  } else if (oldVal && !newVal) {
    // 从非空变空 → 发 stopTyping + 停心跳
    emit('stopTyping')
    stopTypingHeartbeat()
  }
})

// 每次输入时持续发送 typing
function onInput() {
  if (text.value) {
    emit('typing')
  }
}

function insertEmoji(emoji: string) {
  text.value += emoji
  showEmoji.value = false
}

// 撤回重编辑：外部可调用，将内容回填到输入框
function setDraft(content: string) {
  text.value = content
}

function doneSending() {
  sending.value = false
}

defineExpose({ setDraft, doneSending })

onUnmounted(() => {
  stopTypingHeartbeat()
})

function triggerFile(accept: string, type: 'IMAGE' | 'FILE') {
  if (fileInput.value) {
    fileInput.value.accept = accept
    pendingFileType = type
    fileInput.value.click()
  }
}

async function handleFileSelect(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  uploading.value = true
  try {
    const res = await fileApi.uploadFile(file)
    emit('send', {
      receiverId: props.peerId!,
      groupId: props.groupId ?? undefined,
      messageType: pendingFileType,
      fileUrl: res.data.fileUrl,
      fileName: res.data.fileName,
      fileSize: parseInt(res.data.fileSize),
      fileMime: file.type
    })
  } catch (e: any) {
    toast.error(e?.message || (pendingFileType === 'IMAGE' ? '图片上传失败' : '文件上传失败'))
  } finally {
    uploading.value = false
  }
  target.value = ''
}

function handleSend() {
  if (!text.value.trim() || sending.value) return
  sending.value = true
  emit('send', {
    receiverId: props.peerId!,
    groupId: props.groupId ?? undefined,
    messageType: 'TEXT',
    content: text.value.trim()
  })
  text.value = ''
  emit('stopTyping')
  stopTypingHeartbeat()
}
</script>

<style scoped>
.chat-input {
  border-top: 1px solid var(--lc-border);
  background: var(--lc-bg-card);
  padding: var(--lc-space-3) var(--lc-space-4) var(--lc-space-4);
}

.chat-input__toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding-bottom: var(--lc-space-2);
}

.chat-input__tool-btn {
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
  font-size: var(--lc-font-size-lg);
  transition: all var(--lc-duration-100) var(--lc-ease-out);
}
.chat-input__tool-btn:hover:not(:disabled) {
  background: var(--lc-bg-hover);
  color: var(--lc-text-primary);
}
.chat-input__tool-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.chat-input__uploading { font-size: var(--lc-font-size-xs); color: var(--lc-primary); margin-left: 4px; }

.chat-input__spacer { flex: 1; }

.chat-input__send-btn {
  width: 34px;
  height: 34px;
  border-radius: var(--lc-radius-full);
  background: var(--lc-primary);
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: none;
  transition: all var(--lc-duration-150) var(--lc-ease-out);
}
.chat-input__send-btn:hover:not(:disabled) { background: var(--lc-primary-hover); }
.chat-input__send-btn:disabled { opacity: 0.35; cursor: not-allowed; background: var(--lc-border); }

.chat-input__spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  display: inline-block;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.chat-input__textarea {
  width: 100%;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-input);
  padding: var(--lc-space-3) var(--lc-space-4);
  font-family: var(--lc-font-family);
  font-size: var(--lc-font-size-base);
  color: var(--lc-text-primary);
  background: var(--lc-bg-input);
  resize: none;
  outline: none;
  min-height: 80px;
  max-height: 160px;
  line-height: var(--lc-line-height-normal);
  transition: border-color var(--lc-duration-150) var(--lc-ease-out);
  box-sizing: border-box;
}
.chat-input__textarea:focus {
  border-color: var(--lc-border-focus);
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}
.chat-input__textarea::placeholder { color: var(--lc-text-tertiary); }

.chat-input__emoji-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: var(--lc-space-2) 0 0;
  max-height: 140px;
  overflow-y: auto;
}

.chat-input__emoji {
  font-size: 22px;
  padding: 4px;
  border-radius: var(--lc-radius-sm);
  cursor: pointer;
  background: transparent;
  border: none;
  transition: background var(--lc-duration-100) var(--lc-ease-out);
}
.chat-input__emoji:hover { background: var(--lc-bg-hover); }
</style>
