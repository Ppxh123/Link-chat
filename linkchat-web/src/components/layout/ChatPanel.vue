<template>
  <div class="chat-panel">
    <slot />
  </div>
</template>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--lc-bg-card);
}
</style>
