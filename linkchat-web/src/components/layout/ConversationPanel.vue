<template>
  <div class="conversation-panel">
    <slot />
  </div>
</template>

<style scoped>
.conversation-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}
</style>
