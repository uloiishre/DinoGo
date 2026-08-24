<script setup>
//sysmsg-start，總共6次修改，第1次//
import { computed, ref } from 'vue'
//sysmsg-end，總共6次修改，第1次//
import { RouterLink } from 'vue-router'
import SearchBar from './SearchBar.vue'
import { useCartStore } from '@/stores/cart'
//sysmsg-start，總共6次修改，第2次//
import { useAuthStore } from '@/stores/auth'
import { getMemberInboxPreview, getMemberUnreadCount } from '@/api/memberMessageApi'
//sysmsg-end，總共6次修改，第2次//

const cartStore = useCartStore()
//sysmsg-start，總共6次修改，第3次//
const authStore = useAuthStore()
const notificationItems = ref([])
const unreadCount = ref(0)
const notificationsLoading = ref(false)
const notificationsLoaded = ref(false)
const hasMoreNotifications = computed(() => notificationItems.value.length > 3)
const latestNotifications = computed(() => notificationItems.value.slice(0, 3))

async function loadNotificationPreview() {
  if (!authStore.isAuthenticated || notificationsLoading.value || notificationsLoaded.value) return
  notificationsLoading.value = true
  try {
    const categories = ['SYSTEM_INBOX', 'ORDER_INBOX', 'SELLER_INBOX']
    const [inboxResults, countResult] = await Promise.all([
      Promise.allSettled(categories.map((category) => getMemberInboxPreview(category))),
      getMemberUnreadCount().catch(() => null),
    ])
    const uniqueItems = new Map()
    inboxResults.forEach((result) => {
      if (result.status !== 'fulfilled') return
      ;(result.value.data?.items ?? []).forEach((item) => uniqueItems.set(item.recordId, item))
    })
    notificationItems.value = [...uniqueItems.values()].sort(
      (left, right) => new Date(right.recordCreatedAt).getTime() - new Date(left.recordCreatedAt).getTime(),
    )
    unreadCount.value = Number(countResult?.data?.unreadCount ?? countResult?.data?.count ?? 0)
    notificationsLoaded.value = true
  } finally {
    notificationsLoading.value = false
  }
}
//sysmsg-end，總共6次修改，第3次//
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
        <!-- //sysmsg-start，總共6次修改，第4次// -->
        <div class="notification-region" @mouseenter="loadNotificationPreview">
          <RouterLink
            class="header-action header-action--badge"
            to="/member/messages"
            aria-label="通知"
          >
            <i class="bi bi-bell" aria-hidden="true"></i>
            <span v-if="unreadCount > 0" class="notification-badge">
              <span
                class="notification-badge__value"
                :class="{
                  'notification-badge__value--compact': unreadCount > 9,
                  'notification-badge__value--dense': unreadCount > 99,
                }"
              >{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
            </span>
            <span class="header-action__label">通知</span>
          </RouterLink>

          <div class="notification-popover" role="status" aria-label="最新通知">
            <div v-if="!authStore.isAuthenticated" class="notification-empty">登入後查看最新通知</div>
            <div v-else-if="notificationsLoading && !notificationsLoaded" class="notification-empty">正在載入通知...</div>
            <div v-else-if="latestNotifications.length === 0" class="notification-empty">目前沒有通知</div>
            <template v-else>
              <RouterLink
                v-for="item in latestNotifications"
                :key="item.recordId"
                class="notification-preview"
                to="/member/messages"
              >
                <span
                  class="notification-read-dot"
                  :class="{ 'notification-read-dot--read': item.recordStatus === 'READ' }"
                  :aria-label="item.recordStatus === 'READ' ? '已讀' : '未讀'"
                ></span>
                <span class="notification-preview__copy">
                  <!-- //msg-title// -->
                  <strong>{{ item.sendTitle }}</strong>
                  <!-- //msg-content// -->
                  <small>{{ item.sendContent }}</small>
                </span>
              </RouterLink>
              <RouterLink v-if="hasMoreNotifications" class="notification-more" to="/member/messages" aria-label="查看更多通知">
                <span aria-hidden="true">…</span>
              </RouterLink>
            </template>
          </div>
        </div>
        <!-- //sysmsg-end，總共6次修改，第4次// -->
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
  margin: 0;
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

/* //sysmsg-start，總共6次修改，第5次// */
.notification-region {
  position: relative;
  display: inline-flex;
}

.notification-popover {
  position: absolute;
  z-index: 1040;
  top: 100%;
  right: 0;
  display: none;
  width: calc(var(--space-8) + var(--space-8) + var(--space-8) + var(--space-8) + var(--space-8) + var(--space-8));
  overflow: hidden;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.notification-region:hover .notification-popover,
.notification-region:focus-within .notification-popover {
  display: block;
}

.notification-preview {
  display: grid;
  min-height: calc(var(--space-8) + var(--space-5));
  grid-template-columns: var(--space-3) minmax(0, 1fr);
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  color: var(--color-text);
  text-decoration: none;
  border-bottom: 1px solid var(--color-border);
}

.notification-preview:hover,
.notification-preview:active {
  color: var(--color-primary-active);
  background: var(--color-primary-soft);
}

.notification-preview:focus-visible,
.notification-more:focus-visible {
  position: relative;
  outline: none;
  box-shadow: var(--shadow-focus);
}

.notification-read-dot {
  width: var(--space-2);
  height: var(--space-2);
  background: var(--color-primary);
  border-radius: var(--radius-pill);
}

.notification-read-dot--read {
  background: var(--color-disabled);
}

.notification-preview__copy {
  display: grid;
  min-width: 0;
  gap: var(--space-1);
}

.notification-preview__copy strong,
.notification-preview__copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-preview__copy strong {
  font-size: var(--font-size-sm);
}

.notification-preview__copy small {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}

.notification-more {
  display: grid;
  min-height: var(--space-6);
  place-items: center;
  color: var(--color-text-muted);
  font-size: var(--font-size-lg);
  text-decoration: none;
  background: var(--color-surface-soft);
}

.notification-more:hover,
.notification-more:active {
  color: var(--color-primary-active);
  background: var(--color-primary-soft);
}

.notification-empty {
  padding: var(--space-5);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  text-align: center;
}

@media (max-width: 575.98px) {
  .notification-popover {
    position: fixed;
    top: calc(var(--space-8) + var(--space-2));
    right: var(--space-3);
    left: var(--space-3);
    width: auto;
  }
}
/* //sysmsg-end，總共6次修改，第5次// */
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
  /* //sysmsg-start，總共6次修改，第6次// */
  position: absolute;
  top: 10px;
  right: 16px;
  display: inline-flex;
  min-width: 16px;
  height: 16px;
  min-height: 16px;
  align-items: center;
  justify-content: center;
  padding: 0 var(--space-1);
  color: var(--color-surface);
  font-size: 9px;
  line-height: 1;
  text-align: center;
  border-radius: var(--radius-pill);
  background: var(--color-primary);
}

.notification-badge__value {
  display: inline-block;
  line-height: 1;
  transform-origin: center;
}

.notification-badge__value--compact {
  transform: scale(0.85);
}

.notification-badge__value--dense {
  transform: scale(0.7);
}
/* //sysmsg-end，總共6次修改，第6次// */
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
