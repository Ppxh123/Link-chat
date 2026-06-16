<template>
  <Teleport to="body">
    <Transition name="lc-img-preview">
      <div v-if="visible" class="img-preview" @click.self="close">
        <button class="img-preview__close" @click="close">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>

        <img :src="src" class="img-preview__img" :alt="alt" @wheel="onWheel" />

        <div class="img-preview__actions">
          <button class="img-preview__action" @click="zoomOut" title="缩小">−</button>
          <span class="img-preview__zoom">{{ Math.round(scale * 100) }}%</span>
          <button class="img-preview__action" @click="zoomIn" title="放大">+</button>
          <button class="img-preview__action" @click="resetZoom" title="重置">↺</button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  visible: boolean
  src: string
  alt?: string
}>()

const emit = defineEmits<{ close: [] }>()

const scale = ref(1)
const MIN_SCALE = 0.25
const MAX_SCALE = 4

function close() { emit('close') }
function zoomIn() { scale.value = Math.min(scale.value + 0.25, MAX_SCALE) }
function zoomOut() { scale.value = Math.max(scale.value - 0.25, MIN_SCALE) }
function resetZoom() { scale.value = 1 }
function onWheel(e: WheelEvent) {
  e.preventDefault()
  scale.value = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale.value - e.deltaY * 0.001))
}

watch(() => props.visible, (v) => { if (v) scale.value = 1 })
</script>

<style scoped>
.img-preview {
  position: fixed;
  inset: 0;
  z-index: 10000;
  background: rgba(0, 0, 0, 0.92);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.img-preview__close {
  position: absolute;
  top: var(--lc-space-5);
  right: var(--lc-space-5);
  width: 40px;
  height: 40px;
  border-radius: var(--lc-radius-full);
  background: rgba(255,255,255,0.1);
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: none;
  transition: background var(--lc-duration-150) var(--lc-ease-out);
}
.img-preview__close:hover { background: rgba(255,255,255,0.2); }

.img-preview__img {
  max-width: 90vw;
  max-height: 80vh;
  object-fit: contain;
  transition: transform var(--lc-duration-200) var(--lc-ease-out);
  transform: scale(v-bind('scale'));
  cursor: grab;
}

.img-preview__actions {
  position: absolute;
  bottom: var(--lc-space-6);
  display: flex;
  align-items: center;
  gap: var(--lc-space-3);
  padding: var(--lc-space-2) var(--lc-space-4);
  border-radius: var(--lc-radius-full);
  background: rgba(255,255,255,0.1);
  backdrop-filter: blur(8px);
}

.img-preview__action {
  width: 32px;
  height: 32px;
  border-radius: var(--lc-radius-full);
  background: transparent;
  color: #FFFFFF;
  font-size: var(--lc-font-size-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: none;
}
.img-preview__action:hover { background: rgba(255,255,255,0.15); }

.img-preview__zoom {
  font-size: var(--lc-font-size-sm);
  color: #FFFFFF;
  min-width: 40px;
  text-align: center;
  font-family: var(--lc-font-mono);
}

/* 动画 */
.lc-img-preview-enter-active { transition: opacity var(--lc-duration-200) var(--lc-ease-out); }
.lc-img-preview-leave-active { transition: opacity var(--lc-duration-150) var(--lc-ease-in); }
.lc-img-preview-enter-from,
.lc-img-preview-leave-to { opacity: 0; }
</style>
