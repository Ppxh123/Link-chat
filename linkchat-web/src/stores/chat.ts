import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { messageApi } from '@/api/message'
import type { Message } from '@/types/message'

export const useChatStore = defineStore('chat', () => {
  const messages = ref<Message[]>([])
  const currentPeerId = ref<number | null>(null)
  const currentGroupId = ref<number | null>(null)
  const currentPage = ref(1)
  const hasMore = ref(true)
  const unreadCounts = ref<Record<string, number>>({})
  const peerMessages = ref<Record<number, Message[]>>({})

  async function loadMessages(peerId?: number, groupId?: number) {
    if (peerId) {
      currentPeerId.value = Number(peerId)
      currentGroupId.value = null
    }
    if (groupId) {
      currentGroupId.value = Number(groupId)
      currentPeerId.value = null
    }
    currentPage.value = 1
    messages.value = []

    const res = await messageApi.getChatHistory({
      peerId: currentPeerId.value ?? undefined,
      groupId: currentGroupId.value ?? undefined,
      page: currentPage.value,
      size: 20
    })
    messages.value = res.data.reverse()
    hasMore.value = res.data.length === 20

    if (currentPeerId.value) {
      clearUnread(currentPeerId.value)
    }
  }

  async function loadMore() {
    if (!hasMore.value) return
    currentPage.value++
    const res = await messageApi.getChatHistory({
      peerId: currentPeerId.value ?? undefined,
      groupId: currentGroupId.value ?? undefined,
      page: currentPage.value,
      size: 20
    })
    if (res.data.length < 20) hasMore.value = false
    messages.value = [...res.data.reverse(), ...messages.value]
  }

  function addMessage(msg: Message) {
    if (messages.value.some(m => m.id === msg.id)) return
    messages.value.push(msg)
    cacheToPeer(msg)
  }

  function cacheMessage(msg: Message) {
    const peerId = getPeerIdFromMessage(msg)
    if (peerId === null) return
    if (peerId === currentPeerId.value && !msg.groupId) return
    if (msg.groupId && Number(msg.groupId) === currentGroupId.value) return
    cacheToPeer(msg)
    incrementUnread(peerId)
  }

  function getPeerIdFromMessage(msg: Message): number | null {
    if (msg.groupId) return null
    // 消息中 senderId 和 receiverId 从 Jackson 序列化后都是字符串
    const senderId = Number(msg.senderId)
    const receiverId = Number(msg.receiverId)
    const authStore = useAuthStore()
    const myId = authStore.userInfo?.id
    if (myId == null) return receiverId || senderId || null
    // 返回对话中对方的 ID（非当前用户的那个）
    if (String(senderId) === String(myId)) return receiverId || null
    return senderId || null
  }

  function cacheToPeer(msg: Message) {
    const senderId = Number(msg.senderId)
    const receiverId = Number(msg.receiverId)
    const authStore = useAuthStore()
    const myId = authStore.userInfo?.id
    let peerId: number
    if (myId != null && String(senderId) === String(myId)) {
      peerId = receiverId
    } else {
      peerId = senderId
    }
    if (!peerId || isNaN(peerId)) return
    if (!peerMessages.value[peerId]) {
      peerMessages.value[peerId] = []
    }
    const exists = peerMessages.value[peerId].some(m => m.id === msg.id)
    if (!exists) {
      peerMessages.value[peerId].push(msg)
    }
  }

  function incrementUnread(peerId: number) {
    const key = String(peerId)
    if (!unreadCounts.value[key]) {
      unreadCounts.value[key] = 0
    }
    unreadCounts.value[key]++
  }

  function clearUnread(peerId: number) {
    const key = String(peerId)
    unreadCounts.value[key] = 0
  }

  function updateMessageStatus(messageId: string, status: string) {
    const msg = messages.value.find(m => String(m.id) === String(messageId))
    if (msg) msg.ackStatus = status as Message['ackStatus']
  }

  function markRecalled(messageId: string) {
    const id = String(messageId)
    console.log('[recall] markRecalled:', id, 'messages count:', messages.value.length, 'sample id:', messages.value[0]?.id, typeof messages.value[0]?.id)
    const msg = messages.value.find(m => String(m.id) === id)
    if (!msg) {
      // also check cached peer messages
      for (const [peerId, msgs] of Object.entries(peerMessages.value)) {
        const found = msgs.find(m => String(m.id) === id)
        if (found) { found.isRecalled = 1; console.log('[recall] found in peer cache:', peerId); return }
      }
      console.warn('[recall] message not found in store:', id)
    }
    if (msg) msg.isRecalled = 1
  }

  function reset() {
    messages.value = []
    currentPeerId.value = null
    currentGroupId.value = null
    currentPage.value = 1
    hasMore.value = true
  }

  return { messages, currentPeerId, currentGroupId, hasMore, unreadCounts, peerMessages,
    loadMessages, loadMore, addMessage, cacheMessage, updateMessageStatus, markRecalled, reset,
    incrementUnread, clearUnread }
})
