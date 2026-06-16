<template>
  <svg
    :width="sizeMap[size]"
    :height="sizeMap[size]"
    viewBox="0 0 24 24"
    fill="none"
    class="lc-spinner"
    :class="`lc-spinner--${size}`"
  >
    <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2.5" opacity="0.2" />
    <path
      d="M12 2a10 10 0 0 1 10 10"
      stroke="currentColor"
      stroke-width="2.5"
      stroke-linecap="round"
    />
  </svg>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ size?: 'sm' | 'md' }>(), { size: 'md' })

const sizeMap: Record<string, number> = { sm: 16, md: 24 }
</script>

<style scoped>
.lc-spinner {
  animation: lc-spin 0.7s linear infinite;
  color: var(--lc-primary);
  display: block;
}

@keyframes lc-spin {
  to { transform: rotate(360deg); }
}
</style>
