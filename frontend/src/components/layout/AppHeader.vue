<script setup>
import { RouterLink } from 'vue-router'
import SearchBar from './SearchBar.vue'
import { useCartStore } from '@/stores/cart'

const cartStore = useCartStore()
</script>

<template>
  <header class="app-header">
    <div class="container app-header__main d-flex align-items-center">
      <RouterLink class="brand-mark flex-shrink-0" to="/" aria-label="DinoGo 首頁">
        <span class="brand-mark__badge" aria-hidden="true">D</span>
        <span class="brand-mark__copy">
          <span class="brand-mark__name">DINO-GO</span>
          <span class="brand-mark__tagline">好物，慢慢挑</span>
        </span>
      </RouterLink>
      <div class="flex-grow-1 d-none d-md-block"><SearchBar /></div>
      <nav class="header-actions d-flex align-items-center gap-2" aria-label="Header actions">
        <RouterLink class="header-action" to="/member/favorites" aria-label="收藏"
          ><i class="bi bi-heart" aria-hidden="true"></i
          ><span class="header-action__label">收藏</span></RouterLink
        >
        <RouterLink
          class="header-action header-action--badge"
          to="/member/messages"
          aria-label="通知"
          ><i class="bi bi-bell" aria-hidden="true"></i><span class="notification-badge">2</span
          ><span class="header-action__label">通知</span></RouterLink
        >
        <RouterLink class="header-action header-action--badge" to="/cart" aria-label="購物車"
          ><i class="bi bi-cart" aria-hidden="true"></i
          ><span class="notification-badge">{{ cartStore.totalQuantity }}</span
          ><span class="header-action__label">購物車</span></RouterLink
        >
        <RouterLink
          class="header-action member-action d-none d-lg-inline-flex"
          to="/member/overview"
        >
          <i class="bi bi-person-circle" aria-hidden="true"></i
          ><span class="header-action__label">會員中心</span>
        </RouterLink>
      </nav>
    </div>
    <div class="container app-header__search-mobile d-md-none"><SearchBar compact /></div>
  </header>
</template>

<style scoped>
.app-header {
  color: var(--color-text);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  box-shadow: var(--shadow-soft);
}
.app-header__main,
.app-header__search-mobile {
  max-width: 1440px;
}
.app-header__main {
  min-height: 92px;
  gap: 36px;
}
.app-header__search-mobile {
  padding-bottom: var(--space-4);
}
.brand-mark {
  display: flex;
  width: 250px;
  flex: 0 0 250px;
  align-items: center;
  gap: var(--space-3);
  font-family: var(--font-heading);
  font-weight: 700;
  text-decoration: none;
  white-space: nowrap;
}
.brand-mark__badge {
  display: grid;
  width: 48px;
  height: 48px;
  color: var(--color-surface);
  font-family: var(--font-body);
  font-size: 24px;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-primary-700);
}
.brand-mark__copy {
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.brand-mark__name {
  color: var(--color-text);
  font-family: var(--font-body);
  font-size: var(--font-size-lg);
  line-height: 1.2;
}
.brand-mark__tagline {
  color: var(--color-text-subtle);
  font-family: var(--font-body);
  font-size: var(--font-size-xs);
  font-weight: 400;
}
.header-action,
.member-action {
  position: relative;
  color: var(--color-text-muted);
  text-decoration: none;
}
.header-action {
  display: inline-flex;
  width: 64px;
  min-height: 64px;
  flex: 0 0 64px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  font-size: 21px;
  border-radius: var(--radius-md);
}
.header-action__label {
  font-size: 11px;
  line-height: 1.2;
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
  top: 10px;
  right: 16px;
  width: 16px;
  min-height: 16px;
  padding: 1px 0;
  color: var(--color-surface);
  font-size: 9px;
  line-height: 1.2;
  text-align: center;
  border-radius: var(--radius-pill);
  background: var(--color-danger);
}
.member-action {
  padding: 0;
  border-radius: var(--radius-md);
}

@media (max-width: 575.98px) {
  .brand-mark__badge {
    width: 40px;
    height: 40px;
  }
  .brand-mark__copy {
    display: none;
  }
  .header-action {
    min-width: 40px;
    min-height: 48px;
  }
  .header-action__label {
    display: none;
  }
}

@media (max-width: 767.98px) {
  .app-header__main {
    min-height: 72px;
  }
}
</style>
