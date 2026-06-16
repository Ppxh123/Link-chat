<template>
  <div class="lc-tooltip-wrapper" @mouseenter="show" @mouseleave="hide">
    <slot />
    <Teleport to="body">
      <Transition name="lc-tooltip">
        <div v-if="visible" class="lc-tooltip" :class="`lc-tooltip--${position}`">
          {{ text }}
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

withDefaults(defineProps<{
  text: string
  position?: 'top' | 'bottom' | 'right'
}>(), { position: 'top' })

const visible = ref(false)
let timer: ReturnType<typeof setTimeout>

function show() {
  timer = setTimeout(() => { visible.value = true }, 300)
}
function hide() {
  clearTimeout(timer)
  visible.value = false
}
</script>

<style scoped>
.lc-tooltip-wrapper {
  display: inline-flex;
  position: relative;
}

.lc-tooltip {
  position: fixed;
  z-index: 10000;
  padding: 6px 10px;
  border-radius: var(--lc-radius-sm);
  background: var(--lc-text-primary);
  color: var(--lc-text-inverse);
  font-size: var(--lc-font-size-xs);
  font-weight: var(--lc-font-weight-medium);
  line-height: var(--lc-line-height-tight);
  white-space: nowrap;
  pointer-events: none;
  box-shadow: var(--lc-shadow-md);
}
</style>
