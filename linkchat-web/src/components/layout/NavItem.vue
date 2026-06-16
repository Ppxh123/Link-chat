<template>
  <button
    class="nav-item lc-interactive"
    :class="{
      'nav-item--active': active,
      'nav-item--disabled': disabled
    }"
    :title="label"
    @click="!disabled && $emit('click')"
  >
    <div class="nav-item__icon-wrapper">
      <!-- 消息 -->
      <svg v-if="icon === 'chat'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
      </svg>
      <!-- 好友 -->
      <svg v-else-if="icon === 'friend'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
        <circle cx="9" cy="7" r="4"/>
        <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
        <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
      </svg>
      <!-- 群聊 -->
      <svg v-else-if="icon === 'group'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
        <circle cx="9" cy="7" r="4"/>
        <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
        <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
      </svg>
      <!-- AI -->
      <svg v-else-if="icon === 'ai'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
      </svg>
      <!-- 设置 -->
      <svg v-else-if="icon === 'settings'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="3"/>
        <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
      </svg>
      <!-- 太阳（亮色模式） -->
      <svg v-else-if="icon === 'sun'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="5"/>
        <line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
        <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
        <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
        <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
      </svg>
      <!-- 月亮（暗色模式） -->
      <svg v-else-if="icon === 'moon'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
      </svg>
    </div>

    <span class="nav-item__label">{{ label }}</span>
  </button>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  icon: string
  label: string
  active?: boolean
  disabled?: boolean
  badge?: number
}>(), {
  active: false,
  disabled: false,
  badge: 0
})

defineEmits<{ click: [] }>()
</script>

<style scoped>
.nav-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  width: 56px;
  padding: 6px 4px;
  border-radius: var(--lc-radius-lg);
  cursor: pointer;
  border: none;
  background: transparent;
  color: var(--lc-text-tertiary);
  font-family: var(--lc-font-family);
}

.nav-item:hover {
  color: var(--lc-text-primary);
  background: var(--lc-bg-hover);
}

.nav-item--active {
  color: var(--lc-primary);
  background: var(--lc-primary-subtle);
}

.nav-item--disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.nav-item--disabled:hover {
  color: var(--lc-text-tertiary);
  background: transparent;
}

.nav-item__icon-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--lc-radius-md);
}

.nav-item--active .nav-item__icon-wrapper {
  color: var(--lc-primary);
}

.nav-item__label {
  font-size: 10px;
  font-weight: var(--lc-font-weight-medium);
  line-height: 1;
  letter-spacing: 0.01em;
}
</style>
