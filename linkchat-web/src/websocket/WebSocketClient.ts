import { useChatStore } from '@/stores/chat'
import { useFriendStore } from '@/stores/friend'
import type { Message } from '@/types/message'

type MessageHandler = (msg: Message) => void
type TypingHandler = (data: any) => void

class WebSocketClient {
  private ws: WebSocket | null = null
  private reconnectTimer: number | null = null
  private heartbeatTimer: number | null = null
  private reconnectDelay = 1000
  private maxReconnectDelay = 16000
  private messageHandlers: MessageHandler[] = []
  private typingHandlers: TypingHandler[] = []

  connect() {
    try {
    const token = localStorage.getItem('token')
      if (!token) {
        console.warn('[WS] connect() 跳过：没有 token')
        return
      }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const url = `${protocol}//${window.location.host}/ws/${token}`

    console.log('[WS] 正在连接:', url)
    console.log('[WS] token 前8位:', token.substring(0, 8) + '...')

    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      console.log('[WS] ✅ 连接成功')
      this.reconnectDelay = 1000
      this.startHeartbeat()
    }

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        console.log('[WS] 📩 收到消息:', data.type, data.type === 'message' ? '(消息内容)' : '')
        this.handleMessage(data)
      } catch (e) {
        console.error('[WS] 消息解析失败:', e, 'raw:', event.data.substring(0, 200))
      }
    }

    this.ws.onclose = () => {
      const ce = event as CloseEvent
      console.log('[WS] ❌ 连接断开, code:', ce.code, 'reason:', ce.reason, 'wasClean:', ce.wasClean)
      this.stopHeartbeat()
      this.reconnect()
    }

    this.ws.onerror = (err) => {
      console.error('[WS] ⚠️ 连接错误')
    }
    } catch (e) {
      console.error('[WS] connect() 异常:', e)
      this.ws = null
    }
  }

  private handleMessage(data: any) {
    console.log('[WS] handleMessage type:', data.type)
    switch (data.type) {
      case 'message':
        const chatStore = useChatStore()
        const msg = data.payload as Message
        console.log('[WS] 收到聊天消息:', {
          id: msg.id,
          senderId: msg.senderId,
          receiverId: msg.receiverId,
          groupId: msg.groupId,
          content: (msg.content || '').substring(0, 50),
          currentPeerId: chatStore.currentPeerId,
          currentGroupId: chatStore.currentGroupId
        })
        // 先尝试缓存所有消息（用于未读计数和跨会话缓存）
        chatStore.cacheMessage(msg)
        // 如果正在查看该会话，直接加入消息列表
        const matchesPeer = Number(msg.receiverId) === chatStore.currentPeerId ||
            Number(msg.senderId) === chatStore.currentPeerId ||
            (msg.groupId && Number(msg.groupId) === chatStore.currentGroupId)
        console.log('[WS] 是否匹配当前会话:', matchesPeer,
          '| receiverId:', Number(msg.receiverId), '=== peerId:', chatStore.currentPeerId, '→', Number(msg.receiverId) === chatStore.currentPeerId,
          '| senderId:', Number(msg.senderId), '=== peerId:', chatStore.currentPeerId, '→', Number(msg.senderId) === chatStore.currentPeerId,
          '| groupId:', msg.groupId, '=== groupId:', chatStore.currentGroupId, '→', !!(msg.groupId && Number(msg.groupId) === chatStore.currentGroupId))
        if (matchesPeer) {
          chatStore.addMessage(msg)
          console.log('[WS] ✅ 消息已添加到当前会话')
        } else {
          console.log('[WS] ⏭️ 消息不属于当前会话，仅缓存')
        }
        this.messageHandlers.forEach(h => h(msg))
        break
      case 'ack':
        const store = useChatStore()
        store.updateMessageStatus(String(data.payload.messageId), data.payload.status)
        break
      case 'recall':
        const s = useChatStore()
        console.log('[ws] recall received:', data.payload)
        s.markRecalled(String(data.payload?.messageId))
        break
      case 'typing':
        this.typingHandlers.forEach(h => h(data))
        break
      case 'status_change':
        const friendStore = useFriendStore()
        friendStore.updateFriendStatus(data.payload.userId, data.payload.status)
        break
      case 'friend_request':
        // 收到新的好友申请通知
        const fs = useFriendStore()
        fs.onNewFriendRequest()
        break
    }
  }

  send(data: object) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data))
    } else {
      console.warn('WebSocket 未连接，消息未发送:', this.ws?.readyState)
    }
  }

  onMessage(handler: MessageHandler) {
    this.messageHandlers.push(handler)
  }

  offMessage(handler: MessageHandler) {
    this.messageHandlers = this.messageHandlers.filter(h => h !== handler)
  }

  onTyping(handler: TypingHandler) {
    this.typingHandlers.push(handler)
  }

  offTyping(handler: TypingHandler) {
    this.typingHandlers = this.typingHandlers.filter(h => h !== handler)
  }

  private startHeartbeat() {
    this.heartbeatTimer = window.setInterval(() => {
      this.send({ type: 'ping' })
    }, 30000)
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  private reconnect() {
    if (this.reconnectTimer) return
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null
      this.connect()
    }, this.reconnectDelay)
    this.reconnectDelay = Math.min(this.reconnectDelay * 2, this.maxReconnectDelay)
  }

  disconnect() {
    this.stopHeartbeat()
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.ws?.close()
    this.ws = null
  }

  isConnected() {
    return this.ws?.readyState === WebSocket.OPEN
  }
}

export const wsClient = new WebSocketClient()