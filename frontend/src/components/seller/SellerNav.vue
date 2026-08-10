<script setup>
import { RouterLink } from 'vue-router'

const sellerItems = [
  { label: '儀表板', to: '/seller/dashboard', icon: 'bi-speedometer2' },
  { label: '商品管理', to: '/seller/products', icon: 'bi-box-seam' },
  { label: '訂單管理', to: '/seller/orders', icon: 'bi-receipt' },
]
</script>

<template>
  <aside class="seller-nav-shell">
    <div class="nav-topline">D-01 商家中心首頁</div>

    <RouterLink class="brand-link" to="/seller/dashboard">
      <strong>DINO-GO</strong>
      <span>商家中心</span>
    </RouterLink>

    <section class="seller-card">
      <span class="seller-card-icon">店</span>
      <div>
        <strong>森日選物</strong>
        <span>今日營運狀態</span>
      </div>
    </section>

    <nav class="seller-nav" aria-label="賣家中心導覽">
      <RouterLink v-for="item in navItems" :key="item.to" :to="item.to">
        <span class="nav-mark">{{ item.mark }}</span>
        {{ item.label }}
      </RouterLink>
    </nav>

    <RouterLink class="support-link" to="/seller/dashboard">
      <span class="support-mark">?</span>
      商家協助中心
    </RouterLink>
  </aside>
</template>

<style scoped>
.seller-nav-shell {
  min-height: 100vh;
  display: grid;
  grid-template-rows: auto auto auto 1fr auto;
  background: var(--color-primary-800);
  color: var(--color-text-50);
}

.nav-topline {
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  font-size: var(--font-size-xs);
  font-weight: 700;
}

.brand-link {
  display: grid;
  gap: var(--space-1);
  padding: var(--space-5);
  color: var(--color-surface);
  text-decoration: none;
}

.brand-link strong {
  font-size: var(--font-size-base);
  letter-spacing: 0;
}

.brand-link span,
.seller-card span {
  color: var(--color-text-200);
  font-size: var(--font-size-xs);
}

.seller-card {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-5);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.seller-card-icon,
.nav-mark,
.support-mark {
  display: inline-grid;
  place-items: center;
  width: 24px;
  height: 24px;
  flex: 0 0 auto;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: var(--radius-sm);
  color: var(--color-surface);
  font-size: var(--font-size-xs);
}

.seller-card div {
  display: grid;
  gap: 2px;
}

.seller-nav {
  display: grid;
  align-content: start;
  gap: var(--space-1);
  padding: var(--space-4);
}

.seller-nav a {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-height: 40px;
  color: var(--color-text-100);
  text-decoration: none;
  padding: 0 var(--space-3);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
}

.seller-nav a:hover {
  background: rgba(255, 255, 255, 0.08);
  color: var(--color-surface);
}

.seller-nav a.router-link-active {
  background: rgba(255, 255, 255, 0.14);
  color: var(--color-surface);
  font-weight: 600;
}

.seller-nav a.router-link-active::before {
  width: 3px;
  height: 26px;
  content: '';
  border-radius: var(--radius-pill);
  background: var(--color-surface);
}

.seller-nav a.router-link-active .nav-mark {
  background: rgba(255, 255, 255, 0.12);
}

.support-link {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-4) var(--space-5);
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  color: var(--color-text-100);
  text-decoration: none;
  font-size: var(--font-size-sm);
}

.seller-nav a:focus-visible,
.brand-link:focus-visible,
.support-link:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

@media (max-width: 760px) {
  .seller-nav-shell {
    border-right: 0;
    border-bottom: 1px solid var(--color-border);
  }

  .seller-nav {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .seller-nav a.router-link-active::before {
    display: none;
  }

  .seller-nav a {
    text-align: center;
  }

  .support-link {
    display: none;
  }
}
</style>
