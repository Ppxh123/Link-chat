<template>
  <Teleport to="body">
    <div v-if="visible" class="lc-context-menu-overlay" @click.self="close" @keydown.escape="close">
      <div ref="menuEl" class="lc-context-menu" :style="menuStyle">
        <button
          v-for="(item, idx) in items"
          :key="idx"
          class="lc-context-menu__item"
          :class="{ 'lc-context-menu__item--danger': item.danger }"
          @click="select(item)"
        >
          {{ item.label }}
        </button>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

export interface ContextMenuItem {
  label: string
  action?: string
  danger?: boolean
}

const props = defineProps<{
  items: ContextMenuItem[]
  modelValue: boolean
  x?: number
  y?: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  select: [item: ContextMenuItem]
}>()

const menuEl = ref<HTMLElement>()
const visible = computed(() => props.modelValue)

const menuStyle = computed(() => {
  if (props.x && props.y) {
    return { left: `${props.x}px`, top: `${props.y}px`, position: 'fixed' as const }
  }
  return {}
})

function select(item: ContextMenuItem) {
  emit('select', item)
  close()
}

function close() {
  emit('update:modelValue', false)
}

function onEscape(e: KeyboardEvent) {
  if (e.key === 'Escape') close()
}

onMounted(() => document.addEventListener('keydown', onEscape))
onUnmounted(() => document.removeEventListener('keydown', onEscape))
</script>

<style scoped>
.lc-context-menu-overlay {
  position: fixed;
  inset: 0;
  z-index: 9500;
}

.lc-context-menu {
  background: var(--lc-bg-card);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-md);
  box-shadow: var(--lc-shadow-lg);
  padding: 4px;
  min-width: 140px;
  backdrop-filter: blur(12px);
}

.lc-context-menu__item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 8px 12px;
  border-radius: var(--lc-radius-sm);
  font-size: var(--lc-font-size-base);
  color: var(--lc-text-primary);
  cursor: pointer;
  transition: background-color var(--lc-duration-100) var(--lc-ease-out);
}
.lc-context-menu__item:hover {
  background: var(--lc-bg-hover);
}
.lc-context-menu__item--danger {
  color: var(--lc-danger);
}
.lc-context-menu__item--danger:hover {
  background: rgba(239, 68, 68, 0.08);
}
</style>
