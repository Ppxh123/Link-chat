import { ref, type Ref, onUnmounted } from 'vue'

export function useTyping(peerId: Ref<number>, wsClient: any) {
  const isTyping = ref(false)
  const peerTyping = ref(false)
  let typingTimer: number | null = null
  let peerResetTimer: number | null = null

  function onInput() {
    isTyping.value = true
    if (typingTimer) clearTimeout(typingTimer)
    typingTimer = window.setTimeout(() => {
      isTyping.value = false
      typingTimer = null
    }, 2000)
    // 每次按键都发 typing start（由 ChatInput 的 3s 心跳保证持续活跃）
    console.log('[typing] 发送 typing → receiverId:', peerId.value, 'active: true')
    wsClient.send({ type: 'typing', payload: { receiverId: peerId.value, active: true } })
  }

  function onStopInput() {
    console.log('[typing] 发送 stopTyping → receiverId:', peerId.value)
    wsClient.send({ type: 'typing', payload: { receiverId: peerId.value, active: false } })
    isTyping.value = false
    if (typingTimer) {
      clearTimeout(typingTimer)
      typingTimer = null
    }
  }

  function handleTyping(data: any) {
    console.log('[typing] 收到 typing 事件:', JSON.stringify(data.payload), '| 当前 peerId:', peerId.value, '(type:', typeof peerId.value, ')')
    if (String(data.payload?.senderId) === String(peerId.value)) {
      console.log('[typing] ✅ 匹配当前会话，显示"对方正在输入"')
      const active = data.payload?.active !== false // 默认为 true（兼容旧版）
      if (active) {
        peerTyping.value = true
        if (peerResetTimer) clearTimeout(peerResetTimer)
        // 5s 超时兜底（略长于发送方 3s 心跳间隔，确保不断档）
        peerResetTimer = window.setTimeout(() => { peerTyping.value = false }, 5000)
      } else {
        peerTyping.value = false
        if (peerResetTimer) {
          clearTimeout(peerResetTimer)
          peerResetTimer = null
        }
      }
    } else {
      console.log('[typing] ⏭️ 不匹配: senderId=', data.payload?.senderId, '(type:', typeof data.payload?.senderId, ') !== peerId=', peerId.value, '(type:', typeof peerId.value, ')')
    }
  }

  // 强制清除对方 typing 指示器（收到对方消息时调用）
  function clearPeerTyping() {
    peerTyping.value = false
    if (peerResetTimer) {
      clearTimeout(peerResetTimer)
      peerResetTimer = null
    }
  }

  onUnmounted(() => {
    if (typingTimer) clearTimeout(typingTimer)
    if (peerResetTimer) clearTimeout(peerResetTimer)
  })

  return { isTyping, peerTyping, onInput, onStopInput, handleTyping, clearPeerTyping }
}
