<script setup>
import { RouterLink } from 'vue-router'
import SearchBar from './SearchBar.vue'
</script>

<template>
  <header class="app-header">
    <div class="container d-flex align-items-center gap-4 py-3">
      <RouterLink class="brand-mark flex-shrink-0" to="/" aria-label="DinoGo 首頁">
        <span class="brand-mark__dino">Dino</span><span class="brand-mark__go">Go</span>
      </RouterLink>
      <div class="flex-grow-1 d-none d-md-block"><SearchBar /></div>
      <nav class="header-actions d-flex align-items-center gap-2" aria-label="Header actions">
        <RouterLink class="header-action" to="/member/favorites" aria-label="收藏"
          ><i class="bi bi-heart" aria-hidden="true"></i
        ></RouterLink>
        <RouterLink
          class="header-action header-action--badge"
          to="/member/messages"
          aria-label="通知"
          ><i class="bi bi-bell" aria-hidden="true"></i
          ><span class="notification-badge">2</span></RouterLink
        >
        <RouterLink class="header-action header-action--badge" to="/cart" aria-label="購物車"
          ><i class="bi bi-cart" aria-hidden="true"></i
          ><span class="notification-badge">0</span></RouterLink
        >
        <RouterLink
          class="member-action d-none d-lg-flex align-items-center gap-2"
          to="/member/overview"
          ><i class="bi bi-person-circle" aria-hidden="true"></i><span>會員中心</span></RouterLink
        >
      </nav>
    </div>
    <div class="container d-md-none pb-3"><SearchBar compact /></div>
  </header>
</template>

<style scoped>
.app-header {
  color: var(--color-text);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  box-shadow: var(--shadow-soft);
}
.brand-mark {
  font-family: var(--font-heading);
  font-size: var(--font-size-lg);
  font-weight: 700;
  line-height: 1;
  text-decoration: none;
  white-space: nowrap;
}
.brand-mark__dino {
  color: var(--color-primary-800);
}
.brand-mark__go {
  color: var(--color-primary);
}
.header-action,
.member-action {
  position: relative;
  color: var(--color-text-muted);
  text-decoration: none;
}
.header-action {
  display: inline-grid;
  width: 40px;
  height: 40px;
  place-items: center;
  font-size: 1.2rem;
  border-radius: var(--radius-md);
}
.header-action:hover,
.header-action:focus-visible,
.member-action:hover,
.member-action:focus-visible {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}
.header-action--badge .notification-badge {
  position: absolute;
  top: 2px;
  right: 0;
  min-width: 16px;
  padding: 1px 4px;
  color: var(--color-surface);
  font-size: 10px;
  line-height: 1.2;
  text-align: center;
  border-radius: var(--radius-pill);
  background: var(--color-danger);
}
.member-action {
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
}
</style>
