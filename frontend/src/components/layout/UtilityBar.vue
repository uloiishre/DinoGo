<template>
  <div class="utility-bar">
    <div class="container d-flex align-items-center justify-content-between gap-3 py-1">
      <span class="small">DinoGo，讓每一次選物都更有意義</span>
      <nav class="d-flex align-items-center gap-3" aria-label="Utility navigation">
        <a href="#latest" class="utility-link">最新消息</a>
        <a href="#support" class="utility-link">客服中心</a>
        <a href="#help" class="utility-link">幫助中心</a>
        <button type="button" class="language-button">繁體中文</button>
      </nav>
    </div>
  </div>
</template>

<style scoped>
.utility-bar {
  min-height: 34px;
  color: var(--color-secondary-100);
  background: var(--color-primary-900);
}
.utility-bar .container {
  min-height: inherit;
}
.utility-link,
.language-button {
  color: inherit;
  font-size: var(--font-size-xs);
  text-decoration: none;
  white-space: nowrap;
}
.utility-link:hover,
.utility-link:focus-visible,
.language-button:hover,
.language-button:focus-visible {
  color: var(--color-surface);
  text-decoration: underline;
}
.language-button {
  padding: 0;
  border: 0;
  background: transparent;
}
</style>
