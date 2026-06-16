import { config } from '@vue/test-utils'
import { createPinia } from 'pinia'

// Mock localStorage
const localStorageMock = {
  store: {} as Record<string, string>,
  getItem(key: string) {
    return this.store[key] || null
  },
  setItem(key: string, value: string) {
    this.store[key] = value
  },
  removeItem(key: string) {
    delete this.store[key]
  },
  clear() {
    this.store = {}
  }
}
Object.defineProperty(window, 'localStorage', { value: localStorageMock })

// Mock WebSocket
class MockWebSocket {
  onopen: (() => void) | null = null
  onmessage: ((event: any) => void) | null = null
  onclose: (() => void) | null = null
  onerror: ((event: any) => void) | null = null
  readyState = 1
  static OPEN = 1
  send = vi.fn()
  close = vi.fn()
}
Object.defineProperty(window, 'WebSocket', { value: MockWebSocket })