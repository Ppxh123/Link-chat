<template>
  <div class="chat-window">
    <!-- 头部 -->
    <div class="chat-window__header" :class="{ 'chat-window__header--clickable': !!currentGroupId }" @click="onHeaderClick">
      <div class="chat-window__header-info">
        <LcAvatar
          :src="peerAvatar"
          :name="chatTitle"
          size="sm"
          :status="peerOnline ? 'online' : 'offline'"
        />
        <div class="chat-window__header-text">
          <span class="chat-window__title">{{ chatTitle }}</span>
          <span v-if="currentGroupId" class="chat-window__member-hint">{{ groupMemberCount }} 名成员</span>
          <span v-if="peerTyping" class="chat-window__typing">对方正在输入...</span>
        </div>
      </div>
      <div class="chat-window__header-actions">
        <button v-if="currentPeerId" class="chat-window__action-btn" title="删除聊天" @click.stop="$emit('clear')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- 消息列表 -->
    <div ref="msgContainer" class="chat-window__messages" @scroll="handleScroll">
      <div v-if="hasMore" class="chat-window__load-more">
        <LcButton variant="ghost" size="sm" :loading="loadingMore" @click="handleLoadMore">加载更多</LcButton>
      </div>

      <TransitionGroup name="msg-anim">
        <template v-for="(msg, idx) in messages" :key="msg.id">
          <div v-if="shouldShowDivider(idx)" class="chat-window__divider">
            <span>{{ getDividerText(msg.createdAt) }}</span>
          </div>
          <MessageBubble
            :message="msg"
            :is-mine="String(msg.senderId) === String(userId)"
            :re-edited-ids="reEditedIds"
            @recall="handleRecall"
          />
        </template>
      </TransitionGroup>

      <LcEmpty v-if="messages.length === 0" description="发送一条消息开始聊天吧" />
    </div>

    <!-- 输入区域 -->
    <ChatInput
      ref="chatInputRef"
      :peer-id="currentPeerId"
      :group-id="currentGroupId"
      @send="handleSend"
      @typing="emitTyping"
      @stop-typing="emitStopTyping"
    />

  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import { useFriendStore } from '@/stores/friend'
import { useGroupStore } from '@/stores/group'
import { messageApi } from '@/api/message'
import { wsClient } from '@/websocket/WebSocketClient'
import { toast } from '@/components/design/LcToast.ts'
import { useTyping } from '@/composables/useTyping'
import { formatTimeDivider } from '@/utils/date'
import LcAvatar from '@/components/design/LcAvatar.vue'
import LcButton from '@/components/design/LcButton.vue'
import MessageBubble from './MessageBubble.vue'
import ChatInput from './ChatInput.vue'
import type { SendMessageRequest } from '@/types/message'

const emit = defineEmits<{ clear: []; 'manage-group': [groupId: string] }>()

const chatStore = useChatStore()
const authStore = useAuthStore()
const friendStore = useFriendStore()
const groupStore = useGroupStore()

const messages = computed(() => chatStore.messages)
const currentPeerId = computed(() => chatStore.currentPeerId)
const currentGroupId = computed(() => chatStore.currentGroupId)
const hasMore = computed(() => chatStore.hasMore)
const userId = computed(() => authStore.userInfo?.id)

let lastDividerDate = ''
const msgContainer = ref<HTMLElement>()
const chatInputRef = ref<InstanceType<typeof ChatInput>>()
const loadingMore = ref(false)

const reEditedIds = ref(new Set<string>())

const chatTitle = computed(() => {
  if (currentGroupId.value) {
    const g = groupStore.groups.find(g => Number(g.id) === currentGroupId.value)
    return g?.name || '群聊'
  }
  if (currentPeerId.value) {
    const f = friendStore.friends.find(f => Number(f.friendId) === currentPeerId.value)
    return f?.remark || f?.nickname || '聊天'
  }
  return '选择一个会话开始聊天'
})

const peerAvatar = computed(() => {
  if (currentPeerId.value) {
    const f = friendStore.friends.find(f => Number(f.friendId) === currentPeerId.value)
    return f?.avatarUrl || ''
  }
  if (currentGroupId.value) {
    const g = groupStore.groups.find(g => Number(g.id) === currentGroupId.value)
    return g?.avatarUrl || ''
  }
  return ''
})

const peerOnline = computed(() => {
  if (currentGroupId.value) return false
  if (currentPeerId.value) {
    const f = friendStore.friends.find(f => Number(f.friendId) === currentPeerId.value)
    return f?.status === 1
  }
  return false
})

const groupMemberCount = computed(() => {
  if (!currentGroupId.value) return 0
  const g = groupStore.groups.find(g => g.id === String(currentGroupId.value))
  return g?.memberCount || 0
})

const { peerTyping, onInput: emitTyping, onStopInput: emitStopTyping, handleTyping: handleTypingReceive, clearPeerTyping } = useTyping(currentPeerId as any, wsClient)

// 注册 typing 接收处理，切换会话时重新绑定
let currentTypingHandler: ((data: any) => void) | null = null
watch(currentPeerId, (newPeerId) => {
  // 先注销旧的
  if (currentTypingHandler) {
    wsClient.offTyping(currentTypingHandler)
    currentTypingHandler = null
  }
  // 绑定新的
  if (newPeerId) {
    currentTypingHandler = (data: any) => handleTypingReceive(data)
    wsClient.onTyping(currentTypingHandler)
  }
}, { immediate: true })

onUnmounted(() => {
  if (currentTypingHandler) {
    wsClient.offTyping(currentTypingHandler)
    currentTypingHandler = null
  }
})

function shouldShowDivider(idx: number): boolean {
  if (idx === 0) { lastDividerDate = getDateLabel(messages.value[idx].createdAt); return true }
  const prev = getDateLabel(messages.value[idx - 1].createdAt)
  const curr = getDateLabel(messages.value[idx].createdAt)
  return curr !== prev
}
function getDateLabel(d: string): string { return new Date(d).toDateString() }
function getDividerText(d: string): string { return formatTimeDivider(d) }

async function handleSend(data: SendMessageRequest) {
  try {
    const res = await messageApi.sendMessage(data)
    chatStore.addMessage(res.data)
    // 后端 sendMessage 已通过 WebSocket 推送消息给接收方/群聊，前端不再重复发送
  } catch {
    // API 失败不处理（toast 在 request 拦截器中已显示）
  } finally {
    chatInputRef.value?.doneSending()
  }
}

async function handleRecall(messageId: string, reEdit?: boolean) {
  // 超时检查
  const msg = chatStore.messages.find(m => m.id === messageId)
  if (msg) {
    const elapsed = Date.now() - new Date(msg.createdAt).getTime()
    if (elapsed > 120000) {
      toast.error('消息已超过2分钟，无法撤回')
      return
    }
  }

  // 撤回时先缓存原始内容
  const recalledContent = msg?.messageType === 'TEXT' ? msg.content : ''
  try {
    await messageApi.recallMessage(messageId)
    chatStore.markRecalled(messageId)
    // 撤回后把原内容回填输入框
    if (reEdit && recalledContent && chatInputRef.value) {
      reEditedIds.value.add(messageId)
      chatInputRef.value.setDraft(recalledContent)
    }
  } catch (e: any) {
    toast.error(e.message || e.response?.data?.message || '撤回失败')
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (msgContainer.value) {
      msgContainer.value.scrollTop = msgContainer.value.scrollHeight
    }
  })
}

async function handleLoadMore() {
  loadingMore.value = true
  const prevHeight = msgContainer.value?.scrollHeight || 0
  await chatStore.loadMore()
  await nextTick()
  if (msgContainer.value) {
    msgContainer.value.scrollTop = msgContainer.value.scrollHeight - prevHeight
  }
  loadingMore.value = false
}

function handleScroll() {
  if (msgContainer.value && msgContainer.value.scrollTop < 50 && hasMore.value) {
    handleLoadMore()
  }
}

async function onHeaderClick() {
  if (!currentGroupId.value) return
  emit('manage-group', String(currentGroupId.value))
}

// Auto-scroll on new message
watch(() => messages.value.length, () => {
  scrollToBottom()
  // 收到对方新消息 → 强制清除 typing 指示器（后端也发 stop-typing，双重保障）
  const latest = messages.value[messages.value.length - 1]
  if (latest && String(latest.senderId) !== String(userId.value)) {
    clearPeerTyping()
  }
})

// 切换会话时滚动到底部
watch([() => chatStore.currentPeerId, () => chatStore.currentGroupId], () => {
  scrollToBottom()
})

// 初始进入时滚动到底部
onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
/* 原生 input 样式（替换 LcInput，修复 IME 输入 bug） */
.native-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-input);
  background: var(--lc-bg-input);
  font-family: var(--lc-font-family);
  font-size: var(--lc-font-size-lg);
  color: var(--lc-text-primary);
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  box-sizing: border-box;
}
.native-input:focus { border-color: var(--lc-border-focus); box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1); }
.native-input::placeholder { color: var(--lc-text-tertiary); }

.chat-window {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-window__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--lc-space-3) var(--lc-space-5);
  border-bottom: 1px solid var(--lc-border);
  background: var(--lc-bg-card);
  min-height: 56px;
}

.chat-window__header--clickable {
  cursor: pointer;
}
.chat-window__header--clickable:hover {
  background: var(--lc-bg-hover);
}

.chat-window__header-info {
  display: flex;
  align-items: center;
  gap: var(--lc-space-3);
}

.chat-window__header-text {
  display: flex;
  flex-direction: column;
}

.chat-window__title {
  font-size: var(--lc-font-size-lg);
  font-weight: var(--lc-font-weight-semibold);
  color: var(--lc-text-primary);
}

.chat-window__typing {
  font-size: var(--lc-font-size-xs);
  color: var(--lc-primary);
}

.chat-window__member-hint {
  font-size: var(--lc-font-size-xs);
  color: var(--lc-text-tertiary);
}

.chat-window__header-actions {
  display: flex;
  align-items: center;
  gap: var(--lc-space-2);
}

.chat-window__action-btn {
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
.chat-window__action-btn:hover { background: var(--lc-bg-hover); color: var(--lc-text-primary); }

.chat-window__messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--lc-space-3) 0;
}

.chat-window__load-more {
  text-align: center;
  padding: var(--lc-space-2);
}

.chat-window__divider {
  text-align: center;
  padding: var(--lc-space-2) 0;
}
.chat-window__divider span {
  font-size: var(--lc-font-size-xs);
  color: var(--lc-text-tertiary);
  background: var(--lc-bg-card);
  padding: 2px 12px;
  border-radius: var(--lc-radius-sm);
}

/* 消息入场动画 */
.msg-anim-enter-active {
  transition: all var(--lc-msg-anim-duration) var(--lc-ease-out);
}
.msg-anim-enter-from {
  opacity: 0;
  transform: translateY(var(--lc-msg-anim-distance));
}

/* 群成员列表 */
.member-list {
  max-height: 360px;
  overflow-y: auto;
}
.member-list__item {
  display: flex;
  align-items: center;
  gap: var(--lc-space-3);
  padding: var(--lc-space-2) 0;
}
.member-list__info {
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.member-list__name {
  font-size: var(--lc-font-size-sm);
  color: var(--lc-text-primary);
  font-weight: var(--lc-font-weight-medium);
}
.member-list__role {
  font-size: var(--lc-font-size-xs);
  color: var(--lc-text-tertiary);
}
</style>
