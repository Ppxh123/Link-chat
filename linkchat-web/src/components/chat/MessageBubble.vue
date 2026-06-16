<template>
  <div
    class="msg-bubble"
    :class="[`msg-bubble--${isMine ? 'self' : 'other'}`, { 'msg-bubble--system': message.messageType === 'SYSTEM' || message.isRecalled }]"
  >
    <!-- 系统/撤回消息 -->
    <MessageSystem v-if="message.isRecalled || message.messageType === 'SYSTEM'" :is-mine="isMine" :is-recalled="message.isRecalled" :sender-name="message.senderName" :re-edited="reEditedIds?.has(message.id)" @re-edit="emit('recall', message.id, true)" />

    <template v-else>
      <LcAvatar :src="message.senderAvatar" :name="message.senderName" size="sm" class="msg-bubble__avatar" />

      <div class="msg-bubble__content-wrapper">
        <span v-if="!isMine" class="msg-bubble__sender">{{ message.senderName }}</span>

        <div class="msg-bubble__content" @contextmenu.prevent="onContextMenu">
          <div v-if="message.quotedMsgId" class="msg-bubble__quote">引用消息 #{{ message.quotedMsgId }}</div>

          <MessageText v-if="message.messageType === 'TEXT'" :message="message" />
          <div v-else-if="message.messageType === 'EMOJI'" class="msg-bubble__emoji">{{ message.content }}</div>
          <MessageImage v-else-if="message.messageType === 'IMAGE' && message.fileUrl" :message="message" @preview="showPreview = true" />
          <MessageFile v-else-if="message.messageType === 'FILE' && message.fileUrl" :message="message" @preview="showFilePreview = true" />
          <div v-else class="msg-bubble__unknown">[不支持的消息类型]</div>
        </div>

        <span class="msg-bubble__time">{{ formatTime(message.createdAt) }}</span>

        <div v-if="isMine" class="msg-bubble__actions">
          <button class="msg-bubble__more-btn" @click="showMenu = !showMenu" title="更多">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="1"/><circle cx="12" cy="5" r="1"/><circle cx="12" cy="19" r="1"/></svg>
          </button>

          <LcContextMenu
            v-if="showMenu"
            :model-value="showMenu"
            :items="menuItems"
            @update:model-value="showMenu = $event"
            @select="onMenuSelect"
          />
        </div>
      </div>
    </template>

    <ImagePreview :visible="showPreview" :src="message.fileUrl" :alt="message.fileName" @close="showPreview = false" />
    <FilePreviewDialog :visible="showFilePreview" :message="message" @close="showFilePreview = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import LcAvatar from '@/components/design/LcAvatar.vue'
import LcContextMenu from '@/components/design/LcContextMenu.vue'
import type { ContextMenuItem } from '@/components/design/LcContextMenu.vue'
import MessageText from './MessageText.vue'
import MessageImage from './MessageImage.vue'
import MessageFile from './MessageFile.vue'
import MessageSystem from './MessageSystem.vue'
import ImagePreview from '@/components/file/ImagePreview.vue'
import FilePreviewDialog from './FilePreviewDialog.vue'
import { formatTime } from '@/utils/date'

const props = defineProps<{ message: any; isMine: boolean; reEditedIds?: Set<string> }>()
const emit = defineEmits<{ recall: [id: string, reEdit?: boolean] }>()

const showMenu = ref(false)
const showPreview = ref(false)
const showFilePreview = ref(false)

const nowMs = computed(() => Date.now())

const menuItems = computed<ContextMenuItem[]>(() => {
  const items: ContextMenuItem[] = []
  if (props.isMine && !props.message.isRecalled) {
    items.push({ label: '撤回' })
  }
  return items
})

function onMenuSelect(item: ContextMenuItem) {
  showMenu.value = false
  if (item.label === '撤回') emit('recall', props.message.id)
}

function onContextMenu(e: MouseEvent) {
  e.preventDefault()
  showMenu.value = true
}
</script>

<style scoped>
.msg-bubble {
  display: flex;
  align-items: flex-start;
  gap: var(--lc-space-2);
  margin-bottom: var(--lc-space-4);
  padding: 0 var(--lc-space-4);
}

.msg-bubble--self { flex-direction: row-reverse; }

.msg-bubble--system {
  justify-content: center;
  margin-bottom: var(--lc-space-3);
}

.msg-bubble__avatar { flex-shrink: 0; margin-top: 2px; }

.msg-bubble__content-wrapper {
  max-width: 60%;
  min-width: 40px;
  position: relative;
}

.msg-bubble__sender {
  display: block;
  font-size: var(--lc-font-size-xs);
  color: var(--lc-text-secondary);
  margin-bottom: 3px;
  padding-left: 2px;
}

.msg-bubble__content {
  display: inline-block;
  padding: 10px 14px;
  border-radius: var(--lc-radius-msg);
  font-size: var(--lc-font-size-base);
  line-height: var(--lc-line-height-normal);
  word-break: break-word;
  position: relative;
}

.msg-bubble--self .msg-bubble__content {
  background: var(--lc-msg-self-bg);
  color: var(--lc-msg-self-text);
  border-bottom-right-radius: var(--lc-radius-msg-tail);
}

.msg-bubble--other .msg-bubble__content {
  background: var(--lc-msg-other-bg);
  color: var(--lc-msg-other-text);
  border-bottom-left-radius: var(--lc-radius-msg-tail);
}

.msg-bubble__quote {
  font-size: var(--lc-font-size-xs);
  opacity: 0.6;
  padding-bottom: 6px;
  margin-bottom: 6px;
  border-bottom: 1px solid rgba(0,0,0,0.08);
}

.msg-bubble__emoji { font-size: 44px; line-height: 1; padding: 0; }

.msg-bubble__unknown { font-size: var(--lc-font-size-sm); opacity: 0.6; }

.msg-bubble--self .msg-bubble__content-wrapper {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.msg-bubble--other .msg-bubble__content-wrapper {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.msg-bubble__time {
  font-size: var(--lc-font-size-xs);
  color: var(--lc-text-tertiary);
  margin-top: 2px;
  padding: 0 2px;
}

.msg-bubble__actions {
  margin-top: 2px;
  opacity: 0;
  transition: opacity var(--lc-duration-100) var(--lc-ease-out);
}

.msg-bubble:hover .msg-bubble__actions { opacity: 1; }

.msg-bubble__more-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: var(--lc-radius-sm);
  cursor: pointer;
  color: var(--lc-text-tertiary);
  background: transparent;
  border: none;
}
.msg-bubble__more-btn:hover { background: var(--lc-bg-hover); color: var(--lc-text-primary); }
</style>
