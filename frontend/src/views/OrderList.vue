<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { getMemberOrders } from '@/api/order'

const route = useRoute()
const orders = ref([])
const loading = ref(true)
const errorMessage = ref('')
const activeStatus = ref('ALL')

const memberId = computed(() => {
  const value = route.query.memberId ?? sessionStorage.getItem('memberId') ?? 1
  return Number(value)
})

const filters = [
  { value: 'ALL', label: '全部' },
  { value: 'PENDING_PAYMENT', label: '待付款' },
  { value: 'PAID', label: '已付款' },
  { value: 'PROCESSING', label: '處理中' },
  { value: 'SHIPPED', label: '已出貨' },
  { value: 'DELIVERED', label: '待取貨' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
]

const statusLabels = Object.fromEntries(filters.map((filter) => [filter.value, filter.label]))

const visibleOrders = computed(() => {
  if (activeStatus.value === 'ALL') return orders.value
  return orders.value.filter((order) => order.status === activeStatus.value)
})

async function loadOrders() {
  loading.value = true
  errorMessage.value = ''

  if (!Number.isInteger(memberId.value) || memberId.value <= 0) {
    errorMessage.value = '會員資料無效，請重新登入後再試。'
    loading.value = false
    return
  }

  try {
    const response = await getMemberOrders(memberId.value)
    orders.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    errorMessage.value = error.response?.data?.message ?? '目前無法取得訂單資料，請稍後再試。'
  } finally {
    loading.value = false
  }
}

function statusLabel(status) {
  return statusLabels[status] ?? status
}

function sellerName(order) {
  return order.sellerName ?? `賣家 #${order.sellerId}`
}

function firstItem(order) {
  return order.items?.[0] ?? null
}

function productDescription(item) {
  if (!item) return '商品資料準備中'
  return [item.productName, item.skuSpec].filter(Boolean).join('｜')
}

function formatCurrency(value) {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'TWD',
    maximumFractionDigits: 0,
  }).format(Number(value ?? 0))
}

function formatDate(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

onMounted(loadOrders)
</script>

<template>
  <div class="site-shell">
    <header class="global-header">
      <div class="top-bar">
        <div class="header-width">
          <span>DINO-GO 都能購｜簡約採購・質感生活・一次到位</span>
          <nav>企業採購　 客服服務　 最新消息　 幫助中心　 繁體中文⌄</nav>
        </div>
      </div>

      <div class="main-header header-width">
        <RouterLink class="brand" to="/">
          <span class="brand-mark">D</span>
          <strong>DINO-GO 都能購</strong>
        </RouterLink>
        <div class="search-shell">
          <button type="button">全部分類⌄</button>
          <span>搜尋商品、品牌、型號...</span>
          <b>⌕</b>
        </div>
        <nav class="quick-links" aria-label="會員功能">
          <span>♡<small>收藏</small></span>
          <span>🛒<small>購物車</small></span>
          <span>♙<small>會員中心</small></span>
          <span>▤<small>賣家中心</small></span>
        </nav>
      </div>

      <nav class="category-nav">
        <div class="header-width">
          <button type="button">☰　全部分類</button>
          <div class="category-links">
            <span>品牌館</span><span>包包專區</span><span>配件專區</span><span>生活選物</span>
            <span>企業採購</span><span>新品上市</span><span>熱銷排行</span>
          </div>
          <b>◇ 優惠與活動</b>
        </div>
      </nav>
    </header>

    <main class="page-wrap">
      <h1>16 我的訂單</h1>

      <div class="member-layout">
        <aside class="member-sidebar">
          <div class="avatar">D</div>
          <strong>您好，王小明</strong>
          <nav>
            <a href="#">會員總覽</a>
            <a href="#">個人資料</a>
            <a href="#">地址管理</a>
            <RouterLink class="active" to="/member/orders">我的訂單</RouterLink>
            <a href="#">我的收藏</a>
            <a href="#">修改密碼</a>
          </nav>
        </aside>

        <section class="orders-content">
          <div class="filter-area">
            <strong>訂單狀態</strong>
            <div class="filter-list" role="tablist" aria-label="訂單狀態篩選">
              <button
                v-for="filter in filters"
                :key="filter.value"
                type="button"
                :class="{ active: activeStatus === filter.value }"
                @click="activeStatus = filter.value"
              >
                {{ filter.label }}
              </button>
            </div>
          </div>

          <section v-if="loading" class="state-box" aria-live="polite">
            <span class="spinner"></span>
            <p>正在整理你的訂單…</p>
          </section>

          <section v-else-if="errorMessage" class="state-box error-box" role="alert">
            <strong>訂單載入失敗</strong>
            <p>{{ errorMessage }}</p>
            <button type="button" @click="loadOrders">再試一次</button>
          </section>

          <section v-else-if="visibleOrders.length === 0" class="state-box">
            <strong>目前沒有{{ activeStatus === 'ALL' ? '' : statusLabel(activeStatus) }}訂單</strong>
            <p>符合條件的訂單會顯示在這裡。</p>
          </section>

          <section v-else class="order-list">
            <article v-for="order in visibleOrders" :key="order.orderId" class="order-card">
              <div class="card-topline">
                <div>
                  <strong>{{ sellerName(order) }}</strong>
                  <p>訂單編號 {{ order.orderNo }}　 {{ formatDate(order.createdAt) }}</p>
                </div>
                <span class="status-pill" :class="`status-${order.status?.toLowerCase()}`">
                  {{ statusLabel(order.status) }}
                </span>
              </div>

              <div class="card-product">
                <div class="product-image">
                  <img
                    v-if="firstItem(order)?.productImageUrl"
                    :src="firstItem(order).productImageUrl"
                    :alt="firstItem(order).productName"
                  />
                </div>
                <div class="product-copy">
                  <strong>{{ productDescription(firstItem(order)) }}</strong>
                  <span>×{{ firstItem(order)?.quantity ?? 0 }}</span>
                  <small v-if="order.items?.length > 1">另有 {{ order.items.length - 1 }} 件商品</small>
                </div>
                <strong class="order-amount">合計 {{ formatCurrency(order.totalAmount) }}</strong>
              </div>

              <div class="card-actions">
                <RouterLink
                  class="outline action-link"
                  :to="{ name: 'MemberOrderDetail', params: { orderId: order.orderId } }"
                  :aria-label="`查看訂單 ${order.orderNo} 詳情`"
                >
                  訂單詳情
                </RouterLink>
                <button v-if="order.status === 'PENDING_PAYMENT'" class="primary" type="button">立即付款</button>
                <button v-else-if="order.status === 'SHIPPED'" class="primary" type="button">查看物流</button>
              </div>
            </article>
          </section>

          <aside class="developer-note">
            <strong>開發提示</strong>
            <p>查詢 API 必須以 JWT memberId 篩選 buyer_id，不接受前端傳入其他會員 ID。</p>
          </aside>
        </section>
      </div>
    </main>

    <footer class="site-footer">
      <div class="header-width">
        <div class="footer-brand"><span>D</span><strong>DINO-GO 都能購</strong></div>
        <nav>購物指南　 客戶服務　 關於我們　 追蹤我們</nav>
        <small>SSL 安全加密　　多元付款　　發票開立</small>
        <p>© 2026 DINO-GO 都能購 版權所有</p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
:global(*) { box-sizing: border-box; }
:global(body) { margin: 0; min-width: 320px; color: #141a17; background: #fbf8f0; font-family: Inter, "Noto Sans TC", system-ui, sans-serif; }
:global(button), :global(a) { font: inherit; }
.site-shell { min-height: 100vh; background: #fbf8f0; }
.header-width { width: min(1268px, calc(100% - 48px)); margin: 0 auto; }
.top-bar { height: 34px; color: white; background: #14572e; font-size: 12px; }
.top-bar .header-width { display: flex; height: 100%; align-items: center; justify-content: space-between; }
.top-bar nav { white-space: nowrap; }
.main-header { display: grid; height: 86px; grid-template-columns: 285px minmax(320px, 510px) 1fr; align-items: center; gap: 38px; }
.brand { display: flex; align-items: center; gap: 12px; color: #14572e; text-decoration: none; }
.brand-mark { display: grid; width: 48px; height: 48px; place-items: center; border: 1px solid #14572e; border-radius: 50%; background: #def0db; font-size: 24px; font-weight: 800; }
.brand strong { font-size: 28px; white-space: nowrap; }
.search-shell { display: grid; height: 44px; grid-template-columns: 95px 1fr 42px; align-items: center; overflow: hidden; border: 1px solid #c7ccc2; border-radius: 9px; background: white; }
.search-shell button { height: 100%; border: 0; border-right: 1px solid #c7ccc2; color: #14572e; background: #def0db; font-size: 13px; }
.search-shell span { padding-left: 20px; color: #6b756e; font-size: 13px; }
.search-shell b { color: #14572e; font-size: 26px; font-weight: 400; }
.quick-links { display: flex; justify-content: flex-end; gap: 28px; }
.quick-links > span { display: grid; min-width: 48px; justify-items: center; font-size: 17px; }
.quick-links small { margin-top: 3px; font-size: 11px; }
.category-nav { height: 47px; border-block: 1px solid #c7ccc2; background: white; }
.category-nav .header-width { display: flex; height: 100%; align-items: center; }
.category-nav button { align-self: stretch; width: 134px; border: 0; border-radius: 5px; color: white; background: #14572e; font-size: 14px; font-weight: 700; }
.category-links { display: flex; flex: 1; justify-content: space-evenly; font-size: 13px; font-weight: 700; }
.category-nav b { color: #f2521f; font-size: 13px; }
.page-wrap { width: min(1268px, calc(100% - 48px)); min-height: 848px; margin: 0 auto; padding: 15px 0 70px; }
h1 { margin: 0 0 10px; color: #14572e; font-size: 28px; }
.member-layout { display: grid; grid-template-columns: 225px minmax(0, 1fr); gap: 34px; }
.member-sidebar { min-height: 700px; padding: 27px 16px; border: 1px solid #c7ccc2; border-radius: 10px; background: white; text-align: center; }
.avatar { display: grid; width: 68px; height: 68px; margin: 0 auto 16px; place-items: center; border: 1px solid #14572e; border-radius: 50%; color: #14572e; background: #def0db; font-size: 28px; font-weight: 800; }
.member-sidebar > strong { display: block; margin-bottom: 28px; font-size: 17px; }
.member-sidebar nav { display: grid; gap: 9px; }
.member-sidebar a { padding: 12px 24px; border-radius: 7px; color: #141a17; font-size: 14px; font-weight: 600; text-align: left; text-decoration: none; }
.member-sidebar a.active { color: #14572e; background: #def0db; font-weight: 800; }
.orders-content { min-width: 0; }
.filter-area > strong { display: block; margin: 7px 0 10px; font-size: 16px; }
.filter-list { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 20px; }
.filter-list button { min-width: 72px; height: 32px; padding: 0 14px; border: 0; border-radius: 16px; color: #14572e; background: white; font-size: 12px; font-weight: 800; cursor: pointer; }
.filter-list button.active { color: white; background: #14572e; }
.order-list { display: grid; gap: 18px; }
.order-card { min-height: 220px; padding: 19px 23px 10px; border: 1px solid #c7ccc2; border-radius: 10px; background: white; }
.card-topline { display: flex; align-items: start; justify-content: space-between; }
.card-topline strong { font-size: 15px; }
.card-topline p { margin: 14px 0 0; color: #6b756e; font-size: 12px; }
.status-pill { min-width: 90px; padding: 8px 14px; border-radius: 16px; color: #14572e; background: #def0db; font-size: 12px; font-weight: 800; text-align: center; }
.status-pending_payment { color: #f2521f; background: #fff0d1; }
.status-cancelled { color: #8a3434; background: #f6dddd; }
.card-product { display: grid; grid-template-columns: 72px minmax(0, 1fr) auto; align-items: center; gap: 22px; margin-top: 22px; }
.product-image { width: 72px; height: 72px; overflow: hidden; border: 1px solid #c7ccc2; border-radius: 6px; background: #e8ebe3; }
.product-image img { width: 100%; height: 100%; object-fit: cover; }
.product-copy { display: grid; gap: 8px; }
.product-copy strong { font-size: 14px; }
.product-copy span, .product-copy small { color: #6b756e; font-size: 12px; }
.order-amount { padding-right: 120px; font-size: 15px; }
.card-actions { display: flex; justify-content: flex-end; gap: 14px; margin-top: 3px; }
.card-actions button { width: 140px; height: 42px; border-radius: 7px; font-size: 14px; font-weight: 800; cursor: pointer; }
.card-actions .action-link { display: grid; width: 140px; height: 42px; place-items: center; border-radius: 7px; font-size: 14px; font-weight: 800; text-decoration: none; }
.card-actions .primary { border: 1px solid #14572e; color: white; background: #14572e; }
.card-actions .outline { border: 1px solid #14572e; color: #14572e; background: white; }
.state-box { display: grid; min-height: 220px; place-content: center; justify-items: center; border: 1px solid #c7ccc2; border-radius: 10px; background: white; text-align: center; }
.state-box p { color: #6b756e; }
.state-box button { border: 1px solid #14572e; border-radius: 7px; padding: 9px 18px; color: white; background: #14572e; }
.spinner { width: 34px; height: 34px; border: 4px solid #def0db; border-top-color: #14572e; border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.developer-note { margin-top: 32px; padding: 11px 14px; border: 1px solid #e0c259; border-radius: 8px; background: #fff7d1; font-size: 12px; }
.developer-note strong { color: #14572e; }
.developer-note p { margin: 5px 0 0; }
.site-footer { min-height: 85px; border: 1px solid #c7ccc2; background: white; }
.site-footer .header-width { display: grid; grid-template-columns: 1fr 1fr 1fr; align-items: center; padding-top: 17px; }
.footer-brand { display: flex; align-items: center; gap: 12px; color: #14572e; }
.footer-brand span { display: grid; width: 34px; height: 34px; place-items: center; border: 1px solid #14572e; border-radius: 50%; background: #def0db; }
.site-footer nav { font-size: 13px; }
.site-footer small { color: #6b756e; text-align: right; }
.site-footer p { margin: -3px 0 0; color: #6b756e; font-size: 11px; }
@media (max-width: 980px) {
  .top-bar nav, .search-shell, .category-links, .category-nav b { display: none; }
  .main-header { grid-template-columns: 1fr auto; gap: 16px; }
  .quick-links > span:nth-child(-n + 2) { display: none; }
  .member-layout { grid-template-columns: 1fr; }
  .member-sidebar { display: none; }
  .order-amount { padding-right: 0; }
}
@media (max-width: 620px) {
  .header-width, .page-wrap { width: min(100% - 24px, 1268px); }
  .top-bar span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .brand strong { font-size: 20px; }
  .brand-mark { width: 42px; height: 42px; }
  .quick-links { gap: 10px; }
  .category-nav button { width: 126px; }
  h1 { font-size: 24px; }
  .filter-list { gap: 7px; }
  .filter-list button { min-width: auto; }
  .order-card { padding: 16px; }
  .card-product { grid-template-columns: 60px 1fr; }
  .product-image { width: 60px; height: 60px; }
  .order-amount { grid-column: 2; }
  .card-actions button { flex: 1; width: auto; }
  .site-footer .header-width { grid-template-columns: 1fr; gap: 10px; padding-bottom: 17px; }
  .site-footer small { text-align: left; }
}
</style>
