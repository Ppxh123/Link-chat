import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../auth'

describe('AuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('初始状态应为未登录', () => {
    const store = useAuthStore()
    expect(store.isLoggedIn()).toBe(false)
    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
  })

  it('logout应清除状态', () => {
    const store = useAuthStore()
    store.token = 'test-token'
    store.userInfo = {
      id: 1,
      email: 'test@example.com',
      nickname: 'Test',
      avatarUrl: null,
      userCode: '1234567890',
      signature: null,
      status: 0
    }

    expect(store.isLoggedIn()).toBe(true)

    store.logout()
    expect(store.isLoggedIn()).toBe(false)
    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
  })

  it('token设置后isLoggedIn返回true', () => {
    const store = useAuthStore()
    store.token = 'valid-token'
    expect(store.isLoggedIn()).toBe(true)
  })
})