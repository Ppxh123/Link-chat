<template>
  <div class="app-layout">
    <aside class="app-layout__nav">
      <slot name="nav" />
    </aside>
    <aside class="app-layout__conversations">
      <slot name="conversations" />
    </aside>
    <main class="app-layout__chat">
      <slot name="chat" />
    </main>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background: var(--lc-bg-app);
}

.app-layout__nav {
  width: var(--lc-nav-width);
  min-width: var(--lc-nav-width);
  max-width: var(--lc-nav-width);
  display: flex;
  flex-direction: column;
  background: var(--lc-bg-nav);
  border-right: 1px solid var(--lc-border-light);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.app-layout__conversations {
  width: var(--lc-conv-width);
  min-width: var(--lc-conv-width);
  max-width: var(--lc-conv-width);
  display: flex;
  flex-direction: column;
  background: var(--lc-bg-card);
  border-right: 1px solid var(--lc-border);
}

.app-layout__chat {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--lc-bg-card);
}

/* 响应式：小屏幕隐藏会话列表 */
@media (max-width: 1024px) {
  .app-layout__conversations { display: none; }
}
@media (max-width: 768px) {
  .app-layout__nav { display: none; }
}
</style>
