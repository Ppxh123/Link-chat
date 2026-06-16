<template>
  <div class="lc-avatar" :class="`lc-avatar--${size}`">
    <img
      v-if="src && !imgError"
      :src="src"
      :alt="name"
      class="lc-avatar__img"
      @error="onError"
    />
    <span v-else class="lc-avatar__fallback" :style="fallbackStyle">
      {{ initials }}
    </span>

    <!-- 在线状态点 -->
    <span v-if="status" class="lc-avatar__status" :class="`lc-avatar__status--${status}`" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = withDefaults(defineProps<{
  src?: string
  name?: string
  size?: 'sm' | 'md' | 'lg'
  status?: 'online' | 'offline' | 'busy' | 'away'
}>(), {
  size: 'md',
  name: ''
})

const imgError = ref(false)

const initials = computed(() => {
  if (!props.name) return ''
  return props.name.slice(0, 1).toUpperCase()
})

const fallbackStyle = computed(() => {
  // 基于名字生成稳定的背景色
  const colors = [
    '#10B981', '#3B82F6', '#8B5CF6', '#F59E0B',
    '#EF4444', '#EC4899', '#06B6D4', '#F97316'
  ]
  const idx = props.name ? props.name.charCodeAt(0) % colors.length : 0
  return { background: colors[idx] }
})

function onError() {
  imgError.value = true
}
</script>

<style scoped>
.lc-avatar {
  position: relative;
  flex-shrink: 0;
  border-radius: var(--lc-radius-avatar);
  overflow: hidden;
}

.lc-avatar--sm { width: 32px; height: 32px; }
.lc-avatar--md { width: 40px; height: 40px; }
.lc-avatar--lg { width: 48px; height: 48px; }

.lc-avatar__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.lc-avatar__fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #FFFFFF;
  font-weight: var(--lc-font-weight-semibold);
  font-size: calc(var(--lc-font-size-base) + 2px);
  user-select: none;
}

.lc-avatar--sm .lc-avatar__fallback { font-size: var(--lc-font-size-sm); }
.lc-avatar--lg .lc-avatar__fallback { font-size: var(--lc-font-size-xl); }

/* 状态指示器 */
.lc-avatar__status {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid var(--lc-bg-card);
  box-sizing: border-box;
}

.lc-avatar--sm .lc-avatar__status { width: 8px; height: 8px; }
.lc-avatar--lg .lc-avatar__status { width: 12px; height: 12px; }

.lc-avatar__status--online { background: var(--lc-success); }
.lc-avatar__status--busy { background: var(--lc-danger); }
.lc-avatar__status--away { background: var(--lc-warning); }
.lc-avatar__status--offline { background: var(--lc-text-tertiary); }
</style>
