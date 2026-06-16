import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { guest: true }
  },
  {
    path: '/',
    name: 'Chat',
    component: () => import('@/views/ChatView.vue'),
    meta: { auth: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/SettingsView.vue'),
    meta: { auth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 标记是否已初始化 auth store（避免重复调用 fetchUserInfo）
let authInitialized = false

router.beforeEach(async (to, _from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.auth && !token) {
    next('/login')
    return
  }

  if (to.meta.guest && token) {
    next('/')
    return
  }

  // 进入需要认证的页面时，自动拉取用户信息（解决刷新后昵称消失）
  if (to.meta.auth && token && !authInitialized) {
    authInitialized = true
    try {
      const authStore = useAuthStore()
      await authStore.init()
    } catch {
      // init 失败时内部已处理（清除 token 并跳转）
    }
  }

  next()
})

export default router