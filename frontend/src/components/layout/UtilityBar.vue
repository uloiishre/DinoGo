<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="utility-bar">
    <div
      class="container utility-bar__inner d-flex align-items-center justify-content-between gap-3"
    >
      <span class="utility-tagline">安心選物・透明交易・平台保障</span>
      <nav class="utility-nav d-flex align-items-center gap-3" aria-label="Utility navigation">
        <a href="#latest" class="utility-link">最新消息</a>
        <a href="#support" class="utility-link">客服中心</a>
        <a href="#help" class="utility-link">幫助中心</a>
        <RouterLink to="/register" class="utility-link">註冊</RouterLink>
        <span class="utility-separator" aria-hidden="true">|</span>
        <RouterLink to="/login" class="utility-link">登入</RouterLink>
      </nav>
    </div>
  </div>
</template>

<style scoped>
.utility-bar {
  min-height: 48px;
  color: var(--color-secondary-100);
  background: var(--color-primary-900);
}
.utility-bar .container {
  min-height: inherit;
  max-width: 1440px;
}
.utility-tagline {
  font-size: var(--font-size-sm);
  white-space: nowrap;
}
.utility-link,
.language-button,
.utility-separator {
  color: inherit;
  font-size: var(--font-size-sm);
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
.utility-separator {
  opacity: 0.65;
}

@media (max-width: 575.98px) {
  .utility-tagline {
    display: none;
  }
  .utility-nav {
    width: 100%;
    justify-content: flex-end;
    gap: var(--space-3) !important;
  }
}
</style>
