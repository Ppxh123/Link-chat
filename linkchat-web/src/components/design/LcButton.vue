<template>
  <button
    class="lc-btn"
    :class="[`lc-btn--${variant}`, `lc-btn--${size}`, { 'lc-btn--block': block, 'lc-btn--round': round, 'lc-btn--loading': loading }]"
    :disabled="disabled || loading"
    @click="$emit('click')"
  >
    <LcSpinner v-if="loading" size="sm" />
    <span v-if="loading" class="lc-btn__loading-text">处理中...</span>
    <slot v-else />
  </button>
</template>

<script setup lang="ts">
import LcSpinner from './LcSpinner.vue'

withDefaults(defineProps<{
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  disabled?: boolean
  block?: boolean
  round?: boolean
}>(), {
  variant: 'primary',
  size: 'md',
  loading: false,
  disabled: false,
  block: false,
  round: false
})

defineEmits<{ click: [] }>()
</script>

<style scoped>
.lc-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid transparent;
  border-radius: var(--lc-radius-btn);
  font-family: var(--lc-font-family);
  font-weight: var(--lc-font-weight-medium);
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
  transition:
    background-color var(--lc-duration-150) var(--lc-ease-out),
    border-color var(--lc-duration-150) var(--lc-ease-out),
    color var(--lc-duration-150) var(--lc-ease-out),
    box-shadow var(--lc-duration-150) var(--lc-ease-out),
    opacity var(--lc-duration-150) var(--lc-ease-out);
  outline: none;
}

.lc-btn:focus-visible {
  box-shadow: 0 0 0 2px var(--lc-bg-card), 0 0 0 4px var(--lc-border-focus);
}

/* Sizes */
.lc-btn--sm { padding: 6px 12px; font-size: var(--lc-font-size-sm); border-radius: var(--lc-radius-sm); }
.lc-btn--md { padding: 8px 16px; font-size: var(--lc-font-size-base); }
.lc-btn--lg { padding: 12px 24px; font-size: var(--lc-font-size-lg); }

/* Variants */
.lc-btn--primary {
  background: var(--lc-primary);
  color: #FFFFFF;
  border-color: var(--lc-primary);
}
.lc-btn--primary:hover:not(:disabled) { background: var(--lc-primary-hover); border-color: var(--lc-primary-hover); }
.lc-btn--primary:active:not(:disabled) { background: var(--lc-primary-active); }

.lc-btn--secondary {
  background: var(--lc-bg-card);
  color: var(--lc-text-primary);
  border-color: var(--lc-border);
}
.lc-btn--secondary:hover:not(:disabled) { background: var(--lc-bg-hover); }
.lc-btn--secondary:active:not(:disabled) { background: var(--lc-bg-active); }

.lc-btn--ghost {
  background: transparent;
  color: var(--lc-text-primary);
}
.lc-btn--ghost:hover:not(:disabled) { background: var(--lc-bg-hover); }

.lc-btn--danger {
  background: var(--lc-danger);
  color: #FFFFFF;
  border-color: var(--lc-danger);
}
.lc-btn--danger:hover:not(:disabled) { background: #DC2626; }

/* Modifiers */
.lc-btn--block { width: 100%; }
.lc-btn--round { border-radius: var(--lc-radius-full); }

.lc-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.lc-btn--loading { pointer-events: none; }

.lc-btn__loading-text {
  color: inherit;
  opacity: 0.7;
}
</style>
