<template>
  <div class="lc-input-wrapper" :class="{ 'lc-input-wrapper--error': error }">
    <div
      class="lc-input-container"
      :class="[`lc-input-container--${size}`, { 'lc-input-container--focused': focused }]"
      @click.self="focusInput"
    >
      <span v-if="$slots.prefix" class="lc-input__prefix">
        <slot name="prefix" />
      </span>

      <input
        ref="inputEl"
        v-model="localValue"
        :type="type"
        :placeholder="placeholder"
        :disabled="disabled"
        :maxlength="maxlength"
        :name="name"
        :autocomplete="autocomplete"
        class="lc-input__field"
        @focus="onFocus"
        @blur="onBlur"
        @keydown.enter="onEnter"
      />

      <span v-if="clearable && modelValue" class="lc-input__clear" @click.stop="handleClear">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      </span>
      <span v-if="showWordLimit && maxlength" class="lc-input__count">
        {{ modelValue.length }}/{{ maxlength }}
      </span>
    </div>
    <p v-if="error" class="lc-input__error">{{ error }}</p>
    <p v-else-if="hint" class="lc-input__hint">{{ hint }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const props = withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
  type?: string
  disabled?: boolean
  error?: string
  hint?: string
  size?: 'sm' | 'md' | 'lg'
  maxlength?: number
  showWordLimit?: boolean
  clearable?: boolean
  name?: string
  autocomplete?: string
}>(), {
  type: 'text',
  size: 'md',
  placeholder: '',
  disabled: false,
  error: '',
  hint: '',
  maxlength: 0,
  showWordLimit: false,
  clearable: false,
  name: '',
  autocomplete: ''
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  focus: []
  blur: []
  enter: []
  clear: []
}>()

const inputEl = ref<HTMLInputElement>()
const focused = ref(false)

// 用 computed get/set 做双向绑定，和原生 v-model 行为完全一致
const localValue = computed({
  get: () => props.modelValue,
  set: (val: string) => emit('update:modelValue', val)
})

// ====== 其他交互处理 ======

function focusInput() {
  inputEl.value?.focus()
}

function onFocus() {
  focused.value = true
  emit('focus')
}

function onBlur() {
  focused.value = false
  emit('blur')
}

function onEnter() {
  emit('enter')
}

function handleClear() {
  emit('update:modelValue', '')
  emit('clear')
  inputEl.value?.focus()
}
</script>

<style scoped>
.lc-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.lc-input-container {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-input);
  background: var(--lc-bg-input);
  cursor: text;
  transition:
    border-color var(--lc-duration-150) var(--lc-ease-out),
    box-shadow var(--lc-duration-150) var(--lc-ease-out);
}

.lc-input-container--focused {
  border-color: var(--lc-border-focus);
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.lc-input-wrapper--error .lc-input-container {
  border-color: var(--lc-danger);
}
.lc-input-wrapper--error .lc-input-container--focused {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
}

.lc-input-container--sm { padding: 6px 10px; font-size: var(--lc-font-size-sm); }
.lc-input-container--md { padding: 8px 12px; font-size: var(--lc-font-size-base); }
.lc-input-container--lg { padding: 10px 14px; font-size: var(--lc-font-size-lg); }

.lc-input__field {
  flex: 1;
  width: 100%;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-family: var(--lc-font-family);
  font-size: inherit;
  color: var(--lc-text-primary);
  line-height: var(--lc-line-height-normal);
  padding: 0;
  margin: 0;
}

.lc-input__field::placeholder {
  color: var(--lc-text-tertiary);
}

.lc-input__field:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.lc-input__prefix {
  display: flex;
  align-items: center;
  color: var(--lc-text-tertiary);
  flex-shrink: 0;
}

.lc-input__clear {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: var(--lc-text-tertiary);
  flex-shrink: 0;
  padding: 2px;
  border-radius: 4px;
}
.lc-input__clear:hover {
  color: var(--lc-text-secondary);
  background: var(--lc-bg-hover);
}

.lc-input__count {
  font-size: var(--lc-font-size-xs);
  color: var(--lc-text-tertiary);
  flex-shrink: 0;
}

.lc-input__error {
  margin: 0;
  font-size: var(--lc-font-size-xs);
  color: var(--lc-danger);
  padding-left: 2px;
}

.lc-input__hint {
  margin: 0;
  font-size: var(--lc-font-size-xs);
  color: var(--lc-text-tertiary);
  padding-left: 2px;
}
</style>
