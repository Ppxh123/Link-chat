<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-brand">
      <div class="login-brand__content">
        <div class="login-brand__logo">
          <svg width="48" height="48" viewBox="0 0 64 64" fill="none">
            <rect width="64" height="64" rx="16" fill="#10B981"/>
            <text x="50%" y="54%" dominant-baseline="middle" text-anchor="middle" fill="#FFFFFF" font-size="28" font-weight="700" font-family="Inter, sans-serif">LC</text>
          </svg>
        </div>
        <h1 class="login-brand__title">LinkChat</h1>
        <p class="login-brand__subtitle">企业级即时通讯平台</p>
        <p class="login-brand__desc">高效沟通，协作无间</p>
        <div class="login-brand__decor">
          <span v-for="i in 16" :key="i" class="login-brand__dot" :style="dotStyle(i)" />
        </div>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="login-form-side">
      <div class="login-card">
        <div class="login-card__header">
          <h2 class="login-card__title">欢迎回来</h2>
          <p class="login-card__hint">登录您的 LinkChat 账号</p>
        </div>

        <form class="login-card__form" @submit.prevent="handleLogin">
          <div class="input-group">
            <input v-model="email" type="email" placeholder="请输入邮箱" autocomplete="email" name="email" class="native-input" :class="{ 'native-input--error': errors.email }" />
            <p v-if="errors.email" class="input-error">{{ errors.email }}</p>
          </div>
          <div class="input-group">
            <input v-model="password" type="password" placeholder="请输入密码" autocomplete="current-password" name="password" class="native-input" :class="{ 'native-input--error': errors.password }" @keydown.enter="handleLogin" />
            <p v-if="errors.password" class="input-error">{{ errors.password }}</p>
          </div>
          <LcButton variant="primary" size="lg" block :loading="loading" type="submit">登 录</LcButton>
        </form>

        <div class="login-card__footer">
          还没有账号？
          <router-link to="/register" class="login-card__link">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { toast } from '@/components/design/LcToast.ts'
import LcButton from '@/components/design/LcButton.vue'

const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const loading = ref(false)
const errors = reactive({ email: '', password: '' })

function validate(): boolean {
  errors.email = ''
  errors.password = ''
  if (!email.value.trim()) { errors.email = '请输入邮箱'; return false }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) { errors.email = '邮箱格式不正确'; return false }
  if (!password.value) { errors.password = '请输入密码'; return false }
  if (password.value.length < 6) { errors.password = '密码至少 6 位'; return false }
  return true
}

async function handleLogin() {
  if (!validate()) return
  loading.value = true
  try {
    await authStore.login(email.value, password.value)
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '登录失败'
    toast.error(msg)
  } finally {
    loading.value = false
  }
}

function dotStyle(i: number) {
  const colors = ['#10B981', '#34D399', '#6EE7B7', '#A7F3D0', '#D1FAE5']
  return {
    width: `${4 + (i % 3) * 2}px`,
    height: `${4 + (i % 3) * 2}px`,
    background: colors[i % colors.length],
    borderRadius: '50%',
    opacity: 0.3 + (i % 4) * 0.15
  }
}
</script>

<style scoped>
.login-page { display: flex; height: 100vh; width: 100vw; overflow: hidden; }

.login-brand {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ECFDF5 0%, #D1FAE5 40%, #A7F3D0 100%);
  overflow: hidden;
}
.login-brand__content { text-align: center; z-index: 1; }
.login-brand__logo { margin: 0 auto var(--lc-space-6); display: inline-block; }
.login-brand__title {
  font-size: var(--lc-font-size-5xl);
  font-weight: var(--lc-font-weight-bold);
  color: #047857;
  margin: 0 0 var(--lc-space-2);
  letter-spacing: var(--lc-letter-spacing-tight);
}
.login-brand__subtitle { font-size: var(--lc-font-size-xl); color: #065F46; margin: 0 0 4px; font-weight: var(--lc-font-weight-medium); }
.login-brand__desc { font-size: var(--lc-font-size-base); color: #6B7280; margin: 0; }
.login-brand__decor { display: flex; flex-wrap: wrap; gap: 8px; margin-top: var(--lc-space-10); justify-content: center; max-width: 280px; }

.login-form-side {
  width: 480px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--lc-bg-card);
  padding: var(--lc-space-10);
}
.login-card { width: 100%; max-width: 360px; }
.login-card__header { margin-bottom: var(--lc-space-8); }
.login-card__title { font-size: var(--lc-font-size-3xl); font-weight: var(--lc-font-weight-bold); color: var(--lc-text-primary); margin: 0 0 4px; }
.login-card__hint { font-size: var(--lc-font-size-base); color: var(--lc-text-secondary); margin: 0; }
.login-card__form { display: flex; flex-direction: column; gap: var(--lc-space-4); }
.login-card__footer { text-align: center; margin-top: var(--lc-space-8); font-size: var(--lc-font-size-base); color: var(--lc-text-secondary); }
.login-card__link { color: var(--lc-primary); font-weight: var(--lc-font-weight-medium); text-decoration: none; }
.login-card__link:hover { color: var(--lc-primary-hover); }

/* 原生 input 样式 */
.input-group { display: flex; flex-direction: column; gap: 4px; }
.native-input {
  padding: 10px 14px;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-input);
  background: var(--lc-bg-input);
  font-family: var(--lc-font-family);
  font-size: var(--lc-font-size-lg);
  color: var(--lc-text-primary);
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.native-input:focus { border-color: var(--lc-border-focus); box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1); }
.native-input::placeholder { color: var(--lc-text-tertiary); }
.native-input--error { border-color: var(--lc-danger); }
.native-input--error:focus { box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1); }
.input-error { margin: 0; font-size: var(--lc-font-size-xs); color: var(--lc-danger); padding-left: 2px; }

@media (max-width: 768px) {
  .login-brand { display: none; }
  .login-form-side { width: 100%; }
}
</style>
