import { ref } from 'vue'

export interface ToastItem {
  id: number
  type: 'success' | 'error' | 'warning' | 'info'
  message: string
}

export const toasts = ref<ToastItem[]>([])
let nextId = 0

function add(type: ToastItem['type'], message: string) {
  const id = nextId++
  toasts.value.push({ id, type, message })
  setTimeout(() => {
    const idx = toasts.value.findIndex(t => t.id === id)
    if (idx > -1) toasts.value.splice(idx, 1)
  }, 3000)
}

export const toast = {
  success(msg: string) { add('success', msg) },
  error(msg: string) { add('error', msg) },
  warning(msg: string) { add('warning', msg) },
  info(msg: string) { add('info', msg) }
}
