import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/user'
import type { User } from '@/types/user'
import router from '@/router'
import { wsClient } from '@/websocket/WebSocketClient'

export const useAuthStore = defineStore('auth', () => {
  // 从 localStorage 恢复用户信息（解决刷新后昵称消失的问题）
  const cachedUserInfo = (() => {
    try {
      const cached = localStorage.getItem('userInfo')
      return cached ? JSON.parse(cached) : null
    } catch {
      return null
    }
  })()

  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<User | null>(cachedUserInfo)

  const isLoggedIn = () => !!token.value

  /**
   * 初始化：如果已有 token，自动拉取最新的用户信息
   * 在应用启动时调用，确保刷新后用户信息不丢失
   */
  async function init() {
    if (token.value) {
      try {
        await fetchUserInfo()
      } catch {
        // 如果 token 已过期，清除并跳转到登录页
        token.value = ''
        userInfo.value = null
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
      }
    }
  }

  async function login(email: string, password: string) {
    const res = await authApi.login({ email, password })
    token.value = res.data.token
    localStorage.setItem('token', res.data.token)
    await fetchUserInfo()
    router.push('/')
  }

  async function register(email: string, password: string, nickname: string) {
    await authApi.register({ email, password, nickname })
    router.push('/login')
  }

  async function fetchUserInfo() {
    const res = await userApi.getProfile()
    userInfo.value = res.data
    // 持久化用户信息到 localStorage，确保刷新后立即可用
    localStorage.setItem('userInfo', JSON.stringify(res.data))
  }

  function logout() {
    // 先断开 WebSocket 连接
    wsClient.disconnect()
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    router.push('/login')
  }

  return { token, userInfo, isLoggedIn, init, login, register, fetchUserInfo, logout }
})