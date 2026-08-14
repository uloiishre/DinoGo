<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()

const memberItems = [
  { label: '總覽', to: '/member/overview', icon: 'bi-grid' },
  { label: '訂單', to: '/member/orders', icon: 'bi-box-seam' },
  { label: '收藏', to: '/member/favorites', icon: 'bi-heart' },
  { label: '優惠券', to: '/member/coupons', icon: 'bi-ticket-perforated' },
  { label: '訊息', to: '/member/messages', icon: 'bi-bell' },
]

// 帳戶設定包含個人資料與地址管理，任一子頁啟用時父選單保持 active。
const accountItems = [
  { label: '個人資料', routeName: 'MemberProfile', icon: 'bi-person-vcard' },
  { label: '地址管理', routeName: 'MemberAddresses', icon: 'bi-geo-alt' },
]
const isAccountActive = computed(() => accountItems.some((item) => item.routeName === route.name))
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
          role="listitem"
        >
          <i class="bi" :class="item.icon" aria-hidden="true"></i>
          <span>{{ item.label }}</span>
        </RouterLink>

        <!-- 帳戶設定使用下拉選單切換個人資料與地址管理。 -->
        <div class="dropdown dg-member-account-menu" role="listitem">
          <button
            class="dg-member-nav-link dg-member-account-button"
            :class="{ 'is-active': isAccountActive }"
            type="button"
            data-bs-toggle="dropdown"
            data-bs-auto-close="true"
            aria-expanded="false"
          >
            <i class="bi bi-person" aria-hidden="true"></i>
            <span>帳戶設定</span>
            <i class="bi bi-chevron-down dg-member-account-arrow" aria-hidden="true"></i>
          </button>

          <ul class="dropdown-menu dropdown-menu-end dg-member-account-dropdown">
            <li v-for="item in accountItems" :key="item.routeName">
              <RouterLink
                :to="{ name: item.routeName }"
                class="dropdown-item dg-member-account-item"
                active-class="is-active"
              >
                <i class="bi" :class="item.icon" aria-hidden="true"></i>
                <span>{{ item.label }}</span>
              </RouterLink>
            </li>
          </ul>
        </div>
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

.dg-member-nav-link.router-link-active::after,
.dg-member-nav-link.is-active::after {
  bottom: calc((56px - var(--member-nav-height)) / 2);
  height: 4px;
}

.dg-member-account-button {
  min-width: 148px;
  justify-content: flex-start;
  padding-inline: var(--space-5);
  background: var(--color-secondary-100);
  border: 0;
}

.dg-member-account-button.is-active {
  color: var(--color-primary);
}

.dg-member-account-arrow {
  margin-left: auto;
  color: var(--color-text-subtle);
  font-size: var(--font-size-sm);
  transition: transform 160ms ease;
}

.dg-member-account-button[aria-expanded='true'] .dg-member-account-arrow {
  transform: rotate(180deg);
}

/* 帳戶設定下拉選單。 */
.dg-member-account-menu {
  flex: 0 0 auto;
}

.dg-member-account-dropdown {
  min-width: 180px;
  margin-top: var(--space-2) !important;
  padding: var(--space-2);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.dg-member-account-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  border-radius: var(--radius-md);
}

.dg-member-account-item:hover,
.dg-member-account-item:focus,
.dg-member-account-item.is-active {
  color: var(--color-primary-active);
  background: var(--color-primary-soft);
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
    flex-wrap: wrap;
    justify-content: flex-start;
    padding-bottom: var(--space-1);
  }

  .dg-member-nav-link.router-link-active::after,
  .dg-member-nav-link.is-active::after {
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
