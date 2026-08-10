<script setup>
import { RouterLink } from 'vue-router'

const sellerItems = [
  { label: '儀表板', to: '/seller/dashboard', icon: 'bi-speedometer2' },
  { label: '商品管理', to: '/seller/products', icon: 'bi-box-seam' },
  { label: '訂單管理', to: '/seller/orders', icon: 'bi-receipt' },
]
</script>

<template>
  <aside class="seller-nav" aria-label="Seller navigation">
    <div class="seller-nav__brand">商家中心</div>
    <nav class="nav flex-column gap-1">
      <RouterLink
        v-for="item in sellerItems"
        :key="item.label"
        class="seller-nav__link"
        :to="item.to"
      >
        <i class="bi" :class="item.icon" aria-hidden="true"></i><span>{{ item.label }}</span>
      </RouterLink>
    </nav>
  </aside>
</template>

<style scoped>
.seller-nav {
  min-height: 100%;
  padding: var(--space-5) var(--space-3);
  color: var(--color-secondary-100);
  background: var(--color-primary-900);
}
.seller-nav__brand {
  padding: 0 var(--space-3) var(--space-4);
  color: var(--color-surface);
  font-family: var(--font-heading);
  font-size: var(--font-size-lg);
  font-weight: 700;
}
.seller-nav__link {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3);
  color: var(--color-secondary-100);
  text-decoration: none;
  border-radius: var(--radius-md);
}
.seller-nav__link:hover,
.seller-nav__link:focus-visible,
.seller-nav__link.router-link-active {
  color: var(--color-surface);
  background: var(--color-primary-700);
}

@media (max-width: 767.98px) {
  .seller-nav {
    min-height: auto;
    padding: var(--space-3);
  }
  .seller-nav__brand {
    padding: 0 0 var(--space-2);
    font-size: var(--font-size-md);
  }
  .seller-nav .nav {
    flex-flow: row nowrap !important;
    overflow-x: auto;
  }
  .seller-nav__link {
    flex: 0 0 auto;
    white-space: nowrap;
  }
}
</style>
