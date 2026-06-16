<template>
  <label class="lc-checkbox" :class="{ 'lc-checkbox--disabled': disabled }">
    <input
      type="checkbox"
      class="lc-checkbox__input"
      :checked="modelValue"
      :disabled="disabled"
      @change="$emit('update:modelValue', ($event.target as HTMLInputElement).checked)"
    />
    <span class="lc-checkbox__box">
      <svg v-if="modelValue" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="20 6 9 17 4 12"/>
      </svg>
    </span>
    <span v-if="label" class="lc-checkbox__label">{{ label }}</span>
  </label>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  modelValue: boolean
  label?: string
  disabled?: boolean
}>(), {
  disabled: false
})

defineEmits<{ 'update:modelValue': [value: boolean] }>()
</script>

<style scoped>
.lc-checkbox {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.lc-checkbox--disabled { opacity: 0.45; cursor: not-allowed; }

.lc-checkbox__input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.lc-checkbox__box {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  border: 1.5px solid var(--lc-border);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--lc-duration-150) var(--lc-ease-out);
  color: #FFFFFF;
  background: var(--lc-bg-card);
}

.lc-checkbox__input:checked + .lc-checkbox__box {
  background: var(--lc-primary);
  border-color: var(--lc-primary);
}

.lc-checkbox__input:focus-visible + .lc-checkbox__box {
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
}

.lc-checkbox__label {
  font-size: var(--lc-font-size-base);
  color: var(--lc-text-primary);
}
</style>
