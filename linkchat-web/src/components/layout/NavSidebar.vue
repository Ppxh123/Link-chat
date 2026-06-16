<template>
  <nav class="nav-sidebar lc-no-select">
    <!-- 顶部：用户头像 -->
    <div class="nav-sidebar__top">
      <div class="nav-sidebar__avatar" :class="{ 'nav-sidebar__avatar--active': activeTab === 'chat' }" @click="$emit('select-tab', 'chat')" title="消息">
        <LcAvatar :src="authStore.userInfo?.avatarUrl" :name="authStore.userInfo?.nickname" size="md" status="online" />
      </div>
    </div>

    <!-- 中间：导航图标 -->
    <div class="nav-sidebar__tabs">
      <NavItem icon="chat" label="消息" :active="activeTab === 'chat'" @click="$emit('select-tab', 'chat')" />
      <NavItem icon="friend" label="好友" :active="activeTab === 'friend'" @click="$emit('select-tab', 'friend')" />
      <NavItem icon="group" label="群聊" :active="activeTab === 'group'" @click="$emit('select-tab', 'group')" />
      <div class="nav-sidebar__divider" />
      <NavItem icon="settings" label="设置" :active="route.path === '/settings'" @click="goSettings" />
    </div>

    <!-- 底部：暗黑模式切换 -->
    <div class="nav-sidebar__bottom">
      <NavItem
        :icon="themeStore.isDark ? 'sun' : 'moon'"
        :label="themeStore.isDark ? '浅色' : '深色'"
        @click="themeStore.toggleTheme()"
      />
    </div>
  </nav>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import LcAvatar from '@/components/design/LcAvatar.vue'
import NavItem from './NavItem.vue'

defineProps<{ activeTab: string }>()
defineEmits<{ 'select-tab': [tab: string] }>()

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const themeStore = useThemeStore()

function goSettings() {
  router.push('/settings')
}
</script>

<style scoped>
.nav-sidebar {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  padding: var(--lc-space-4) 0;
  gap: var(--lc-space-2);
}

.nav-sidebar__top {
  padding-bottom: var(--lc-space-4);
}

.nav-sidebar__avatar {
  width: 44px;
  height: 44px;
  border-radius: var(--lc-radius-full);
  cursor: pointer;
  transition: box-shadow var(--lc-duration-150) var(--lc-ease-out);
  display: flex;
  align-items: center;
  justify-content: center;
}
.nav-sidebar__avatar:hover {
  box-shadow: var(--lc-shadow-md);
}
.nav-sidebar__avatar--active {
  box-shadow: 0 0 0 2px var(--lc-border-focus);
}

.nav-sidebar__tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  width: 100%;
}

.nav-sidebar__divider {
  width: 32px;
  height: 1px;
  background: var(--lc-border);
  margin: var(--lc-space-2) 0;
}

.nav-sidebar__bottom {
  padding-top: var(--lc-space-2);
}
</style>
