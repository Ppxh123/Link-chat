import { onMounted, onUnmounted } from 'vue'
import { wsClient } from '@/websocket/WebSocketClient'
import { useAuthStore } from '@/stores/auth'

export function useWebSocket() {
  const authStore = useAuthStore()

  onMounted(() => {
    console.log('[WS] useWebSocket.onMounted, isLoggedIn:', authStore.isLoggedIn(), 'token:', !!localStorage.getItem('token'))
    if (authStore.isLoggedIn()) {
      console.log('[WS] 准备调用 wsClient.connect()')
      wsClient.connect()
    } else {
      console.warn('[WS] 未登录，跳过 WebSocket 连接')
    }
  })

  onUnmounted(() => {
    // 不在这里断开，因为WS是全局单例
  })

  return { wsClient }
}