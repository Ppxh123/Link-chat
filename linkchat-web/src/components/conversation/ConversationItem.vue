<template>
  <div
    class="conv-item lc-interactive lc-no-select"
    :class="{ 'conv-item--active': active }"
    @click="$emit('click')"
  >
    <div class="conv-item__avatar" @click.stop="$emit('avatar-click')">
      <LcAvatar
        :src="avatar"
        :name="name"
        size="md"
        :status="status === 1 ? 'online' : status === 2 ? 'busy' : 'offline'"
      />
    </div>
    <div class="conv-item__content">
      <div class="conv-item__top">
        <span class="conv-item__name lc-truncate">{{ name }}</span>
        <span class="conv-item__time">{{ timeText }}</span>
      </div>
      <div class="conv-item__bottom">
        <span class="conv-item__preview lc-truncate">{{ preview }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import LcAvatar from '@/components/design/LcAvatar.vue'

defineProps<{
  avatar: string
  name: string
  preview: string
  timeText: string
  unread: number
  status?: number
  active?: boolean
}>()

defineEmits<{ click: []; 'avatar-click': [] }>()
</script>

<style scoped>
.conv-item {
  display: flex;
  align-items: center;
  gap: var(--lc-space-3);
  padding: var(--lc-space-3) var(--lc-space-4);
  cursor: pointer;
  border-radius: 0;
  margin: 2px var(--lc-space-2);
  border-radius: var(--lc-radius-md);
}

.conv-item:hover {
  background: var(--lc-bg-hover);
}

.conv-item--active {
  background: var(--lc-primary-subtle);
}
.conv-item--active:hover {
  background: rgba(16, 185, 129, 0.12);
}

.conv-item__avatar {
  flex-shrink: 0;
}

.conv-item__content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.conv-item__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--lc-space-2);
}

.conv-item__name {
  font-size: var(--lc-font-size-md);
  font-weight: var(--lc-font-weight-medium);
  color: var(--lc-text-primary);
  flex: 1;
  min-width: 0;
}

.conv-item__time {
  font-size: var(--lc-font-size-xs);
  color: var(--lc-text-tertiary);
  flex-shrink: 0;
  white-space: nowrap;
}

.conv-item__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--lc-space-2);
}

.conv-item__preview {
  font-size: var(--lc-font-size-sm);
  color: var(--lc-text-secondary);
  flex: 1;
  min-width: 0;
}
</style>
