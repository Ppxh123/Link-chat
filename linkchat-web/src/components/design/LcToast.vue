<template>
  <Teleport to="body">
    <TransitionGroup name="lc-toast" tag="div" class="lc-toast-container">
      <div
        v-for="item in toasts"
        :key="item.id"
        class="lc-toast"
        :class="`lc-toast--${item.type}`"
      >
        <span class="lc-toast__icon">
          <svg v-if="item.type === 'success'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
          <svg v-else-if="item.type === 'error'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          <svg v-else-if="item.type === 'warning'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
        </span>
        <span class="lc-toast__text">{{ item.message }}</span>
      </div>
    </TransitionGroup>
  </Teleport>
</template>

<script setup lang="ts">
import { toasts } from './LcToast.ts'
</script>

<style scoped>
.lc-toast-container {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10000;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}

.lc-toast {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: var(--lc-radius-md);
  background: var(--lc-bg-card);
  color: var(--lc-text-primary);
  font-size: var(--lc-font-size-base);
  box-shadow: var(--lc-shadow-lg);
  border: 1px solid var(--lc-border);
  pointer-events: auto;
  min-width: 200px;
}

.lc-toast--success { border-left: 3px solid var(--lc-success); }
.lc-toast--error { border-left: 3px solid var(--lc-danger); }
.lc-toast--warning { border-left: 3px solid var(--lc-warning); }
.lc-toast--info { border-left: 3px solid var(--lc-info); }

.lc-toast__icon { flex-shrink: 0; display: flex; }
.lc-toast--success .lc-toast__icon { color: var(--lc-success); }
.lc-toast--error .lc-toast__icon { color: var(--lc-danger); }
.lc-toast--warning .lc-toast__icon { color: var(--lc-warning); }
.lc-toast--info .lc-toast__icon { color: var(--lc-info); }

.lc-toast__text { flex: 1; }

.lc-toast-enter-active { transition: all var(--lc-duration-300) var(--lc-ease-spring); }
.lc-toast-leave-active { transition: all var(--lc-duration-200) var(--lc-ease-in); }
.lc-toast-enter-from { opacity: 0; transform: translateY(-8px); }
.lc-toast-leave-to { opacity: 0; transform: translateY(-4px); }
</style>
