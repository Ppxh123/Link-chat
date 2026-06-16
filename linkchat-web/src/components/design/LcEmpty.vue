<template>
  <div class="lc-empty">
    <div class="lc-empty__icon">
      <slot name="icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
        </svg>
      </slot>
    </div>
    <p class="lc-empty__text">{{ description }}</p>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  description?: string
}>(), { description: '暂无数据' })
</script>

<style scoped>
.lc-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--lc-space-10) var(--lc-space-4);
  gap: var(--lc-space-3);
}

.lc-empty__icon {
  color: var(--lc-text-tertiary);
  opacity: 0.4;
}

.lc-empty__text {
  font-size: var(--lc-font-size-base);
  color: var(--lc-text-tertiary);
  margin: 0;
}
</style>
