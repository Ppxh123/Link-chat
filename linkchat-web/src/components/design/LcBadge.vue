<template>
  <div
    v-if="shouldShow"
    class="lc-badge"
    :class="{
      'lc-badge--dot': dot,
      'lc-badge--danger': variant === 'danger'
    }"
  >
    <template v-if="!dot">{{ displayCount }}</template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  count?: number
  max?: number
  dot?: boolean
  variant?: 'primary' | 'danger'
}>(), {
  count: 0,
  max: 99,
  dot: false,
  variant: 'primary'
})

const shouldShow = computed(() => props.dot ? true : props.count > 0)
const displayCount = computed(() => {
  if (props.count > props.max) return `${props.max}+`
  return String(props.count)
})
</script>

<style scoped>
.lc-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: var(--lc-radius-badge);
  background: var(--lc-primary);
  color: var(--lc-text-inverse);
  font-size: var(--lc-font-size-xs);
  font-weight: var(--lc-font-weight-semibold);
  line-height: 1;
  letter-spacing: 0.02em;
}

.lc-badge--danger {
  background: var(--lc-danger);
}

.lc-badge--dot {
  min-width: 8px;
  width: 8px;
  height: 8px;
  padding: 0;
  border-radius: 50%;
}
</style>
