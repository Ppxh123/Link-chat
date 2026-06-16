<template>
  <div class="lc-search-box">
    <span class="lc-search-box__icon">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="11" cy="11" r="8"/>
        <path d="m21 21-4.35-4.35"/>
      </svg>
    </span>
    <input
      :value="modelValue"
      :placeholder="placeholder"
      class="lc-search-box__input"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      @keydown.enter="$emit('search')"
    />
    <button v-if="modelValue" class="lc-search-box__clear" @click="$emit('update:modelValue', ''); $emit('clear')">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
    </button>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
}>(), { placeholder: '搜索...' })

defineEmits<{
  'update:modelValue': [value: string]
  search: []
  clear: []
}>()
</script>

<style scoped>
.lc-search-box {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-input);
  background: var(--lc-bg-input);
  transition: border-color 0.15s ease;
}
.lc-search-box:focus-within {
  border-color: var(--lc-border-focus);
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}
.lc-search-box__icon {
  flex-shrink: 0;
  color: var(--lc-text-tertiary);
  display: flex;
}
.lc-search-box__input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-family: var(--lc-font-family);
  font-size: var(--lc-font-size-sm);
  color: var(--lc-text-primary);
  padding: 0;
}
.lc-search-box__input::placeholder { color: var(--lc-text-tertiary); }
.lc-search-box__clear {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--lc-text-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}
.lc-search-box__clear:hover { background: var(--lc-bg-hover); color: var(--lc-text-primary); }
</style>
