<script setup>
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()

const navItems = [
  {
    label: '全部分類',
    to: { name: 'ProductList' },
    icon: 'bi-grid',
    activeKey: 'all',
  },
  {
    label: '新品上市',
    to: { name: 'ProductList', query: { sort: 'newest' } },
    activeKey: 'newest',
  },
  {
    label: '熱門推薦',
    to: { name: 'ProductList', query: { sort: 'popular' } },
    activeKey: 'popular',
  },
  {
    label: '品牌與商家',
    to: { name: 'ProductList', query: { filter: 'brand' } },
    activeKey: 'brand',
  },
  {
    label: '優惠活動',
    to: { name: 'ProductList', query: { filter: 'offers' } },
    activeKey: 'offers',
  },
  {
    label: '主題企劃',
    to: { name: 'ProductList', query: { filter: 'themes' } },
    activeKey: 'themes',
  },
  {
    label: '商家中心',
    to: { name: 'SellerDashboard' },
    icon: 'bi-shop',
    activeKey: 'seller',
  },
]

const isActive = (item) => {
  if (item.activeKey === 'seller') {
    return route.name === 'SellerDashboard'
  }

  if (route.name !== 'ProductList') {
    return false
  }

  if (item.activeKey === 'all') {
    return !route.query.sort && !route.query.filter
  }

  if (item.activeKey === 'newest') {
    return route.query.sort === 'newest'
  }

  if (item.activeKey === 'popular') {
    return route.query.sort === 'popular'
  }

  return route.query.filter === item.activeKey
}
</script>

<template>
  <nav class="primary-nav" aria-label="Primary navigation">
    <div class="container primary-nav__inner">
      <button
        class="primary-nav__toggle d-flex d-lg-none align-items-center justify-content-between"
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
        class="primary-nav__menu collapse d-lg-flex align-items-lg-center gap-lg-1"
      >
        <RouterLink
          v-for="item in navItems"
          :key="item.label"
          class="primary-nav__link"
          :class="{
            'primary-nav__link--all': item.label === '全部分類',
            'primary-nav__link--seller': item.label === '商家中心',
            'primary-nav__link--active': isActive(item),
          }"
          :to="item.to"
        >
          <i v-if="item.icon" class="bi" :class="item.icon" aria-hidden="true"></i>

          <span>{{ item.label }}</span>
        </RouterLink>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.primary-nav {
  min-height: 72px;
  color: var(--color-text-muted);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}
.primary-nav__inner {
  max-width: 1440px;
  min-height: inherit;
}
.primary-nav__menu {
  min-height: inherit;
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
  min-height: 52px;
  padding: 0 var(--space-4);
  color: inherit;
  font-size: var(--font-size-base);
  text-decoration: none;
  border-bottom: 2px solid transparent;
}
.primary-nav__link--all {
  width: 160px;
  min-height: 52px;
  margin: 0;
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
.primary-nav__link:focus-visible {
  color: var(--color-primary-800);
  background: var(--color-primary-soft);
  border-bottom-color: var(--color-primary);
}

.primary-nav__link--active {
  color: var(--color-primary-800);
  background: var(--color-primary-soft);
  border-bottom-color: var(--color-primary);
}

@media (max-width: 991.98px) {
  .primary-nav {
    min-height: 0;
  }
  .primary-nav__inner {
    min-height: 0;
  }
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
    width: 100%;
    margin: 0;
  }
  .primary-nav__link--seller {
    margin-left: 0;
  }
}
</style>
