<script setup>
import { RouterLink } from 'vue-router'

const navItems = [
  { label: '全部分類', to: '/products', icon: 'bi-grid' },
  { label: '新品上市', to: '/products?sort=newest' },
  { label: '熱門推薦', to: '/products?sort=popular' },
  { label: '品牌與商家', to: '/products?filter=brand' },
  { label: '優惠活動', to: '/products?filter=offers' },
  { label: '主題企劃', to: '/products?filter=themes' },
  { label: '商家中心', to: '/seller/dashboard', icon: 'bi-shop' },
]
</script>

<template>
  <nav class="primary-nav" aria-label="Primary navigation">
    <div class="container">
      <button
        class="primary-nav__toggle d-flex d-md-none align-items-center justify-content-between"
        type="button"
        data-bs-toggle="collapse"
        data-bs-target="#primary-nav-menu"
        aria-controls="primary-nav-menu"
        aria-expanded="false"
      >
        <span><i class="bi bi-list me-2" aria-hidden="true"></i>商城導覽</span>
        <i class="bi bi-chevron-down" aria-hidden="true"></i>
      </button>
      <div
        id="primary-nav-menu"
        class="primary-nav__menu collapse d-md-flex align-items-md-center gap-md-1"
      >
        <RouterLink
          v-for="item in navItems"
          :key="item.label"
          class="primary-nav__link"
          :class="{
            'primary-nav__link--all': item.label === '全部分類',
            'primary-nav__link--seller': item.label === '商家中心',
          }"
          :to="item.to"
        >
          <i v-if="item.icon" class="bi" :class="item.icon" aria-hidden="true"></i
          ><span>{{ item.label }}</span>
        </RouterLink>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.primary-nav {
  color: var(--color-text-muted);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}
.primary-nav__menu {
  width: 100%;
}
.primary-nav__toggle {
  width: 100%;
  min-height: 46px;
  padding: 0;
  color: var(--color-primary-800);
  border: 0;
  background: transparent;
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
.primary-nav__link--all {
  min-height: 40px;
  margin: 3px 0;
  color: var(--color-surface);
  border-bottom-color: transparent;
  border-radius: var(--radius-md);
  background: var(--color-primary);
}
.primary-nav__link--all:hover,
.primary-nav__link--all:focus-visible,
.primary-nav__link--all.router-link-active {
  color: var(--color-surface);
  border-bottom-color: transparent;
  background: var(--color-primary-hover);
}
.primary-nav__link--seller {
  margin-left: auto;
}
.primary-nav__link:hover,
.primary-nav__link:focus-visible,
.primary-nav__link.router-link-active {
  color: var(--color-primary-800);
  background: var(--color-primary-soft);
  border-bottom-color: var(--color-primary);
}

@media (max-width: 767.98px) {
  .primary-nav__menu {
    padding-bottom: var(--space-2);
  }
  .primary-nav__link {
    width: 100%;
    min-height: 42px;
    border-bottom: 0;
    border-radius: var(--radius-md);
  }
  .primary-nav__link--all {
    margin: 0;
  }
  .primary-nav__link--seller {
    margin-left: 0;
  }
}
</style>
