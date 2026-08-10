<script setup>
import { RouterLink } from 'vue-router'

const memberItems = [
  { label: '總覽', to: '/member/overview', icon: 'bi-layout-dashboard' },
  { label: '訂單', to: '/member/orders', icon: 'bi-receipt' },
  { label: '收藏', to: '/member/favorites', icon: 'bi-heart' },
  { label: '優惠券', to: '/member/coupons', icon: 'bi-ticket-perforated' },
  { label: '訊息', to: '/member/messages', icon: 'bi-chat-circle' },
  { label: '帳戶設定', to: '/member/profile', icon: 'bi-person-gear' },
]
</script>

<template>
  <aside class="member-nav" aria-label="Member navigation">
    <div class="list-group">
      <RouterLink
        v-for="item in memberItems"
        :key="item.label"
        class="member-nav__link list-group-item list-group-item-action"
        :to="item.to"
      >
        <i class="bi" :class="item.icon" aria-hidden="true"></i><span>{{ item.label }}</span>
      </RouterLink>
    </div>
  </aside>
</template>

<style scoped>
.member-nav {
  padding: var(--space-4);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.member-nav__link {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-muted);
  border: 0;
  border-radius: var(--radius-md) !important;
}
.member-nav__link:hover,
.member-nav__link:focus-visible,
.member-nav__link.router-link-active {
  color: var(--color-primary-700);
  background: var(--color-primary-soft);
}
</style>
