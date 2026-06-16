<template>
  <Teleport to="body">
    <Transition name="lc-modal">
      <div v-if="modelValue" class="lc-modal" @click.self="onOverlayClick">
        <div class="lc-modal__panel" :style="{ maxWidth: width }">
          <!-- 头部 -->
          <div v-if="title || closable" class="lc-modal__header">
            <h3 v-if="title" class="lc-modal__title">{{ title }}</h3>
            <button v-if="closable" class="lc-modal__close" @click="close">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>

          <!-- 内容 -->
          <div class="lc-modal__body">
            <slot />
          </div>

          <!-- 底部 -->
          <div v-if="$slots.footer" class="lc-modal__footer">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{
  modelValue: boolean
  title?: string
  width?: string
  closable?: boolean
  closeOnOverlay?: boolean
}>(), {
  width: '440px',
  closable: true,
  closeOnOverlay: true
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  closed: []
}>()

function close() {
  emit('update:modelValue', false)
  emit('closed')
}

function onOverlayClick() {
  if (props.closeOnOverlay) close()
}
</script>

<style scoped>
.lc-modal {
  position: fixed;
  inset: 0;
  z-index: 9000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--lc-space-6);
  background: var(--lc-bg-overlay);
  backdrop-filter: blur(4px);
}

.lc-modal__panel {
  width: 100%;
  background: var(--lc-bg-card);
  border-radius: var(--lc-radius-modal);
  box-shadow: var(--lc-shadow-modal);
  display: flex;
  flex-direction: column;
  max-height: 85vh;
}

.lc-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--lc-space-5) var(--lc-space-6);
  border-bottom: 1px solid var(--lc-border-light);
}

.lc-modal__title {
  font-size: var(--lc-font-size-lg);
  font-weight: var(--lc-font-weight-semibold);
  margin: 0;
}

.lc-modal__close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: var(--lc-radius-sm);
  color: var(--lc-text-tertiary);
  cursor: pointer;
  transition: all var(--lc-duration-150) var(--lc-ease-out);
}
.lc-modal__close:hover {
  background: var(--lc-bg-hover);
  color: var(--lc-text-primary);
}

.lc-modal__body {
  padding: var(--lc-space-6);
  overflow-y: auto;
  flex: 1;
}

.lc-modal__footer {
  padding: var(--lc-space-4) var(--lc-space-6);
  border-top: 1px solid var(--lc-border-light);
  display: flex;
  justify-content: flex-end;
  gap: var(--lc-space-2);
}

/* 动画 */
.lc-modal-enter-active {
  transition: opacity var(--lc-modal-anim-duration) var(--lc-ease-out);
}
.lc-modal-enter-active .lc-modal__panel {
  transition: transform var(--lc-modal-anim-duration) var(--lc-ease-out),
              opacity var(--lc-modal-anim-duration) var(--lc-ease-out);
}
.lc-modal-leave-active {
  transition: opacity var(--lc-duration-150) var(--lc-ease-in);
}
.lc-modal-leave-active .lc-modal__panel {
  transition: transform var(--lc-duration-150) var(--lc-ease-in),
              opacity var(--lc-duration-150) var(--lc-ease-in);
}

.lc-modal-enter-from {
  opacity: 0;
}
.lc-modal-enter-from .lc-modal__panel {
  opacity: 0;
  transform: scale(var(--lc-modal-anim-scale)) translateY(8px);
}
.lc-modal-leave-to {
  opacity: 0;
}
.lc-modal-leave-to .lc-modal__panel {
  opacity: 0;
  transform: scale(var(--lc-modal-anim-scale)) translateY(-8px);
}
</style>
