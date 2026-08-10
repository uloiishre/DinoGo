<script setup>
import { RouterLink } from 'vue-router'

const navItems = [
  { label: '全部分類', to: '/products', icon: 'bi-grid' },
  { label: '新品上市', to: '/products?sort=newest' },
  { label: '熱門推薦', to: '/products?sort=popular' },
  { label: '品牌與商家', to: '/products?filter=brand' },
  { label: '優惠活動', to: '/products?filter=offers' },
  { label: '主題企劃', to: '/products?filter=themes' },
  { label: '商家中心', to: '/seller/dashboard' },
]
</script>

<template>
  <nav class="primary-nav" aria-label="Primary navigation">
    <div class="container d-flex align-items-center gap-1 overflow-auto">
      <RouterLink
        v-for="item in navItems"
        :key="item.label"
        class="primary-nav__link"
        :to="item.to"
      >
        <i v-if="item.icon" class="bi" :class="item.icon" aria-hidden="true"></i
        ><span>{{ item.label }}</span>
      </RouterLink>
    </div>
  </nav>
</template>

<style scoped>
.primary-nav {
  color: var(--color-text-muted);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}
.primary-nav__link {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: var(--space-1);
  min-height: 46px;
  padding: 0 var(--space-3);
  color: inherit;
  font-size: var(--font-size-sm);
  text-decoration: none;
  border-bottom: 2px solid transparent;
}
.primary-nav__link:hover,
.primary-nav__link:focus-visible,
.primary-nav__link.router-link-active {
  color: var(--color-primary-800);
  background: var(--color-primary-soft);
  border-bottom-color: var(--color-primary);
}
</style>
