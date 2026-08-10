<script setup>
import { RouterLink } from 'vue-router'

const memberItems = [
  { label: '總覽', to: '/member/overview', icon: 'bi-grid' },
  { label: '訂單', to: '/member/orders', icon: 'bi-box-seam' },
  { label: '收藏', to: '/member/favorites', icon: 'bi-heart' },
  { label: '優惠券', to: '/member/coupons', icon: 'bi-ticket-perforated' },
  { label: '訊息', to: '/member/messages', icon: 'bi-bell' },
  { label: '帳戶設定', to: '/member/profile', icon: 'bi-person' },
]
</script>

<template>
  <nav class="dg-member-nav" aria-label="會員中心導覽">
    <div class="container dg-member-nav-inner">
      <div class="dg-member-profile">
        <span class="dg-member-avatar" aria-hidden="true">D</span>
        <span class="dg-member-profile-copy">
          <strong class="dg-member-name">Dino 會員</strong>
          <span class="dg-member-level">一般會員</span>
        </span>
      </div>

      <div class="dg-member-menu" role="list">
        <RouterLink
          v-for="item in memberItems"
          :key="item.label"
          :to="item.to"
          class="dg-member-nav-link"
          :class="{ 'dg-member-account-button': item.label === '帳戶設定' }"
          role="listitem"
        >
          <i class="bi" :class="item.icon" aria-hidden="true"></i>
          <span>{{ item.label }}</span>
          <i
            v-if="item.label === '帳戶設定'"
            class="bi bi-chevron-down dg-member-account-arrow"
            aria-hidden="true"
          ></i>
        </RouterLink>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.dg-member-nav {
  --member-nav-height: 120px;
  min-height: var(--member-nav-height);
  color: var(--color-text);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}

.dg-member-nav-inner {
  display: flex;
  min-height: inherit;
  max-width: 1440px;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
}

.dg-member-profile {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: var(--space-3);
}

.dg-member-avatar {
  display: inline-grid;
  width: 56px;
  height: 56px;
  flex: 0 0 56px;
  color: var(--color-primary-700);
  font-size: var(--font-size-md);
  font-weight: 700;
  place-items: center;
  border-radius: var(--radius-pill);
  background: var(--color-primary-100);
}

.dg-member-profile-copy {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.dg-member-name {
  color: var(--color-text);
  font-size: var(--font-size-md);
  line-height: var(--line-height-heading);
}

.dg-member-level {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.dg-member-menu {
  display: flex;
  min-width: 0;
  align-items: stretch;
  justify-content: flex-end;
  gap: var(--space-2);
}

.dg-member-nav-link {
  position: relative;
  display: inline-flex;
  min-width: 92px;
  min-height: 56px;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: 0 var(--space-4);
  color: var(--color-text-muted);
  font-size: var(--font-size-base);
  text-decoration: none;
  white-space: nowrap;
  border-radius: var(--radius-md);
}

.dg-member-nav-link::after {
  position: absolute;
  right: var(--space-2);
  bottom: 0;
  left: var(--space-2);
  height: 0;
  content: '';
  background: var(--color-primary);
}

.dg-member-nav-link:hover,
.dg-member-nav-link:focus-visible {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.dg-member-nav-link:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.dg-member-nav-link.router-link-active {
  color: var(--color-primary);
}

.dg-member-nav-link.router-link-active::after {
  bottom: calc((56px - var(--member-nav-height)) / 2);
  height: 4px;
}

.dg-member-account-button {
  min-width: 148px;
  justify-content: flex-start;
  padding-inline: var(--space-5);
  background: var(--color-secondary-100);
}

.dg-member-account-arrow {
  margin-left: auto;
  color: var(--color-text-subtle);
  font-size: var(--font-size-sm);
}

@media (max-width: 991.98px) {
  .dg-member-nav-inner {
    align-items: flex-start;
    flex-direction: column;
    gap: var(--space-3);
    padding-top: var(--space-4);
    padding-bottom: var(--space-3);
  }

  .dg-member-menu {
    width: 100%;
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: var(--space-1);
  }

  .dg-member-nav-link.router-link-active::after {
    bottom: 0;
  }
}

@media (max-width: 575.98px) {
  .dg-member-nav-inner {
    padding-top: var(--space-3);
  }

  .dg-member-nav-link {
    min-width: 84px;
    padding-inline: var(--space-3);
  }

  .dg-member-account-button {
    min-width: 132px;
    padding-inline: var(--space-3);
  }
}
</style>
