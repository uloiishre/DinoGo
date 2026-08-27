<script setup>
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getCurrentSellerId } from '@/utils/seller-session'

// 沒有 sellerId 時，不要自動變成 1
const sellerId = computed(() => getCurrentSellerId())

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const navItems = [
  { label: '營運總覽', to: '/seller/dashboard', icon: 'bi-speedometer2' },
  {
    label: '商品管理',
    to: '/seller/products',
    icon: 'bi-box-seam',
  },
  { label: '訂單管理', to: '/seller/orders', icon: 'bi-receipt' },
  { label: '批次出貨', to: '/seller/orders/batch-shipment', icon: 'bi-truck' },
  { label: '優惠券管理', to: '/seller/coupons', icon: 'bi-ticket-perforated' },
  { label: '賣家錢包', to: '/seller/wallet', icon: 'bi-wallet2' },
  { label: '訊息中心', to: '/seller/messages', icon: 'bi-chat-left-text' },
  { label: '店鋪資料', to: '/seller/profile', icon: 'bi-shop' },
]

const plannedItems = [{ label: '銷售分析', icon: 'bi-graph-up-arrow' }]

const isItemActive = (item) => {
  if (item.to === '/seller/products') {
    return route.path.startsWith('/seller/products')
  }

  if (item.to === '/seller/orders') {
    return route.path === item.to
  }

  return route.path === item.to
}

function logout() {
  authStore.signOut()
  router.replace({ name: 'Login' })
}
</script>

<template>
  <aside class="seller-nav-shell">
    <div class="seller-nav-header">
      <RouterLink class="brand-link" to="/seller/dashboard">
        <strong>DINO-GO</strong>
        <span>商家中心</span>
      </RouterLink>

      <RouterLink
        v-if="sellerId"
        class="store-link"
        :to="`/products?sellerId=${sellerId}`"
        aria-label="查看店鋪"
        title="查看店鋪"
      >
        <i class="bi bi-shop-window" aria-hidden="true"></i>
      </RouterLink>
    </div>

    <section class="seller-card">
      <span class="seller-card-icon">店</span>
      <div>
        <strong>森日選物</strong>
        <span>今日營運狀態</span>
      </div>
    </section>

    <nav class="seller-nav" aria-label="賣家中心導覽">
      <div v-for="item in navItems" :key="item.to" class="seller-nav-group">
        <RouterLink v-slot="{ href, navigate }" custom :to="item.to">
          <a
            class="seller-nav-link"
            :class="{ 'router-link-active': isItemActive(item) }"
            :href="href"
            @click="navigate"
          >
            <i class="nav-mark bi" :class="item.icon" aria-hidden="true"></i>
            <span>{{ item.label }}</span>
          </a>
        </RouterLink>
      </div>

      <div
        v-for="item in plannedItems"
        :key="item.label"
        class="seller-nav-link is-planned"
        :title="`${item.label}功能規劃中`"
        aria-disabled="true"
      >
        <i class="nav-mark bi" :class="item.icon" aria-hidden="true"></i>
        <span>{{ item.label }}</span>
        <small>規劃中</small>
      </div>
    </nav>

    <div class="seller-nav-footer">
      <button type="button" class="seller-logout-button" @click="logout">
        <i class="nav-mark bi bi-box-arrow-right" aria-hidden="true"></i>
        <span>登出</span>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.seller-nav-shell {
  position: sticky;
  top: 0;
  width: 240px;
  height: 100vh;
  min-height: 100vh;
  display: grid;
  grid-template-rows: auto auto 1fr auto;
  background: var(--color-primary-800);
  color: var(--color-text-50);
}

.seller-nav-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-5);
}

.brand-link {
  display: grid;
  min-width: 0;
  gap: var(--space-1);
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
.nav-mark {
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

.seller-nav-group {
  display: grid;
  gap: 2px;
}

.seller-nav-link {
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

.seller-nav > .seller-nav-group > .seller-nav-link.router-link-active::before {
  width: 3px;
  height: 26px;
  content: '';
  border-radius: var(--radius-pill);
  background: var(--color-surface);
}

.seller-nav a.router-link-active .nav-mark {
  background: rgba(255, 255, 255, 0.12);
}

.seller-nav-link small {
  margin-left: auto;
  color: var(--color-text-200);
  font-size: var(--font-size-xs);
}

.seller-nav-link.is-planned {
  cursor: not-allowed;
  opacity: 0.58;
}

.store-link {
  display: inline-grid;
  place-items: center;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.08);
  color: var(--color-surface);
  text-decoration: none;
  transition:
    background 0.15s ease,
    border-color 0.15s ease;
}

.store-link:hover {
  border-color: rgba(255, 255, 255, 0.32);
  background: rgba(255, 255, 255, 0.14);
}

.store-link i {
  font-size: var(--font-size-base);
}

.seller-nav-footer {
  padding: var(--space-4);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.seller-logout-button {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  min-height: 40px;
  padding: 0 var(--space-3);
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-100);
  font: inherit;
  font-size: var(--font-size-sm);
  font-weight: 600;
  text-align: left;
  cursor: pointer;
}

.seller-logout-button:hover {
  background: rgba(255, 255, 255, 0.08);
  color: var(--color-surface);
}

.seller-nav a:focus-visible,
.brand-link:focus-visible,
.seller-logout-button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.store-link:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

@media (max-width: 760px) {
  .seller-nav-shell {
    position: static;
    width: 100%;
    height: auto;
    border-right: 0;
    border-bottom: 1px solid var(--color-border);
  }

  .seller-nav {
    grid-template-columns: repeat(7, minmax(0, 1fr));
  }

  .seller-nav a.router-link-active::before {
    display: none;
  }

  .seller-nav-link {
    min-width: 0;
    justify-content: center;
    padding: 0 var(--space-2);
  }

  .seller-nav-link small {
    display: none;
  }

  .seller-nav-footer {
    padding-top: 0;
  }
}

@media (max-width: 560px) {
  .seller-nav {
    display: flex;
    overflow-x: auto;
  }

  .seller-nav-link {
    min-width: max-content;
  }
}
</style>
