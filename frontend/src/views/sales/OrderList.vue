<script setup>
import { computed, onMounted, ref } from 'vue'
import { getMemberOrders } from '@/api/order'
//review-start，總共6次修改，第1次//
import { getOrderStars } from '@/api/review'
//review-end，總共6次修改，第1次//
import { getOrderDisplayStatus, isOrderInDisplayGroup } from '@/utils/orderDisplayStatus'

const orders = ref([])
const loading = ref(true)
const errorMessage = ref('')
const activeStatus = ref('ALL')
const keyword = ref('')
const sortOrder = ref('NEWEST')
//review-start，總共6次修改，第2次//
const starsByOrderItemId = ref({})
//review-end，總共6次修改，第2次//

const filters = [
  { value: 'ALL', label: '全部' },
  { value: 'PENDING_PAYMENT', label: '待付款' },
  { value: 'PENDING_SHIPMENT', label: '待出貨' },
  { value: 'PENDING_RECEIPT', label: '待收貨' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '不成立' },
]

const visibleOrders = computed(() => {
  const selectedFilter = filters.find((filter) => filter.value === activeStatus.value)
  const normalizedKeyword = keyword.value.trim().toLowerCase()

  return orders.value
    .filter((order) => {
      if (!selectedFilter || selectedFilter.value === 'ALL') return true
      return isOrderInDisplayGroup(order, selectedFilter.value)
    })
    .filter((order) => {
      if (!normalizedKeyword) return true
      const searchableText = [
        order.orderNo,
        order.sellerName,
        ...((order.items ?? []).flatMap((item) => [item.productName, item.skuSpec])),
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
      return searchableText.includes(normalizedKeyword)
    })
    .toSorted((left, right) => {
      const difference = new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()
      return sortOrder.value === 'NEWEST' ? difference : -difference
    })
})

async function loadOrders() {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await getMemberOrders()
    orders.value = Array.isArray(response.data) ? response.data : []
    //review-start，總共6次修改，第3次//
    const completedOrders = orders.value.filter((order) => order.status === 'COMPLETED')
    const starResponses = await Promise.allSettled(
      completedOrders.map((order) => getOrderStars(order.orderId)),
    )
    starsByOrderItemId.value = starResponses.reduce((result, response) => {
      if (response.status !== 'fulfilled' || !Array.isArray(response.value.data)) return result
      response.value.data.forEach((star) => {
        result[star.orderItemId] = star
      })
      return result
    }, {})
    //review-end，總共6次修改，第3次//
  } catch (error) {
    errorMessage.value = error.response?.data?.message ?? '訂單載入失敗，請稍後再試。'
  } finally {
    loading.value = false
  }
}

function firstItem(order) {
  return order.items?.[0] ?? null
}

function itemSummary(order) {
  const item = firstItem(order)
  if (!item) return '商品資料尚未提供'

  const product = [item.productName, item.skuSpec].filter(Boolean).join('・')
  const extraCount = Math.max((order.items?.length ?? 1) - 1, 0)
  return extraCount > 0 ? `${product}・另有 ${extraCount} 件商品` : product
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
  }).format(new Date(value))
}

//review-start，總共6次修改，第4次//
function reviewFor(item) {
  return starsByOrderItemId.value[item.orderItemId] ?? null
}

function isReviewed(item) {
  return Number(reviewFor(item)?.fiveStar ?? 0) > 0
}

function reviewRoute(order, item) {
  const star = reviewFor(item)
  return {
    name: 'MemberOrderItemReview',
    params: { orderId: order.orderId, orderItemId: item.orderItemId },
    query: star?.starId ? { starId: star.starId } : undefined,
  }
}
//review-end，總共6次修改，第4次//

onMounted(loadOrders)
</script>

<template>
  <section class="order-page">
    <div class="container order-container">
      <header class="page-header">
        <h1>我的訂單</h1>
        <p>追蹤您的所有交易與配送狀態</p>
      </header>

      <nav class="order-tabs" aria-label="訂單狀態篩選">
        <button
          v-for="filter in filters"
          :key="filter.value"
          type="button"
          :class="{ active: activeStatus === filter.value }"
          @click="activeStatus = filter.value"
        >
          {{ filter.label }}
        </button>
      </nav>

      <div class="filter-row">
        <label class="search-control">
          <i class="bi bi-search" aria-hidden="true"></i>
          <span class="visually-hidden">搜尋訂單</span>
          <input v-model="keyword" type="search" placeholder="搜尋訂單編號或商品" />
        </label>

        <label class="sort-control">
          <span class="visually-hidden">訂單排序</span>
          <select v-model="sortOrder">
            <option value="NEWEST">較新優先</option>
            <option value="OLDEST">較舊優先</option>
          </select>
          <i class="bi bi-chevron-down" aria-hidden="true"></i>
        </label>
      </div>

      <div v-if="loading" class="state-card" aria-live="polite">
        <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        <span>正在載入訂單...</span>
      </div>

      <div v-else-if="errorMessage" class="state-card state-error" role="alert">
        <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
        <strong>無法載入訂單</strong>
        <span>{{ errorMessage }}</span>
        <button type="button" @click="loadOrders">重新載入</button>
      </div>

      <div v-else-if="visibleOrders.length === 0" class="state-card">
        <i class="bi bi-receipt" aria-hidden="true"></i>
        <strong>找不到符合條件的訂單</strong>
        <span>請調整狀態、關鍵字或排序方式。</span>
      </div>

      <div v-else class="order-list">
        <article
          v-for="order in visibleOrders"
          :key="order.orderId"
          class="order-card"
        >
          <div class="order-card__heading">
            <RouterLink
              class="order-copy"
              :to="{ name: 'MemberOrderDetail', params: { id: order.orderId } }"
            >
              <strong>訂單 #{{ order.orderNo }}</strong>
              <span>{{ itemSummary(order) }}</span>
              <small>{{ formatDate(order.createdAt) }}</small>
            </RouterLink>

            <span
              class="status-badge"
              :class="`status-${getOrderDisplayStatus(order).key.toLowerCase()}`"
            >
              {{ getOrderDisplayStatus(order).label }}
            </span>

            <strong class="order-total">{{ formatCurrency(order.totalAmount) }}</strong>
          </div>

          <!-- //review-start，總共6次修改，第5次// -->
          <div v-if="order.status === 'COMPLETED'" class="completed-items">
            <div v-for="item in order.items" :key="item.orderItemId" class="completed-item">
              <div class="product-image">
                <img v-if="item.productImageUrl" :src="item.productImageUrl" :alt="item.productName" />
                <i v-else class="bi bi-image" aria-hidden="true"></i>
              </div>
              <div class="completed-item__copy">
                <strong>{{ item.productName }}</strong>
                <span>{{ item.skuSpec || '單一規格' }}・數量 {{ item.quantity }}</span>
              </div>
              <RouterLink
                :to="reviewRoute(order, item)"
                class="review-endcap"
                :class="isReviewed(item) ? 'review-endcap--reviewed' : 'review-endcap--pending'"
                :aria-label="isReviewed(item) ? `修改 ${item.productName} 的評價` : `評價 ${item.productName}`"
                :title="isReviewed(item) ? '已評價' : '未評價'"
              >
                <!-- //review-未評價// -->
                <i v-if="!isReviewed(item)" class="bi bi-star" aria-hidden="true"></i>
                <!-- //review-已評價// -->
                <i v-else class="bi bi-star-fill" aria-hidden="true"></i>
              </RouterLink>
            </div>
          </div>
          <!-- //review-end，總共6次修改，第5次// -->
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.order-page {
  min-height: 620px;
  padding: var(--space-5) 0 var(--space-8);
  background: var(--color-bg);
}

.order-container {
  max-width: 1440px;
  padding-inline: var(--space-8);
}

.page-header {
  display: flex;
  min-height: 68px;
  flex-direction: column;
  justify-content: center;
  gap: var(--space-1);
}

.page-header h1 {
  margin: 0;
  color: var(--color-text);
  font-family: var(--font-body);
  font-size: 26px;
  font-weight: 700;
  line-height: var(--line-height-heading);
}

.page-header p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 13px;
}

.order-tabs {
  display: flex;
  min-height: 42px;
  align-items: stretch;
  gap: var(--space-5);
  overflow-x: auto;
  border-bottom: 1px solid var(--color-border);
}

.order-tabs button {
  position: relative;
  flex: 0 0 auto;
  padding: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  background: transparent;
  border: 0;
  cursor: pointer;
}

.order-tabs button::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  content: '';
  background: transparent;
}

.order-tabs button:hover,
.order-tabs button:focus-visible,
.order-tabs button.active {
  color: var(--color-primary-active);
}

.order-tabs button.active {
  font-weight: 700;
}

.order-tabs button.active::after {
  background: var(--color-primary-active);
}

.order-tabs button:focus-visible,
.search-control:focus-within,
.sort-control:focus-within,
.order-card:focus-visible,
.state-card button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.filter-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 130px;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.search-control,
.sort-control {
  display: flex;
  height: 42px;
  align-items: center;
  color: var(--color-text-muted);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.search-control {
  gap: var(--space-2);
  padding: 0 var(--space-3);
}

.search-control input,
.sort-control select {
  width: 100%;
  color: var(--color-text);
  font: inherit;
  font-size: 11px;
  background: transparent;
  border: 0;
  outline: 0;
}

.search-control input::placeholder {
  color: var(--color-text-muted);
}

.sort-control {
  position: relative;
  padding: 0 var(--space-3);
}

.sort-control select {
  height: 100%;
  padding-right: var(--space-5);
  appearance: none;
  cursor: pointer;
}

.sort-control i {
  position: absolute;
  right: var(--space-3);
  pointer-events: none;
}

.order-list {
  display: grid;
  gap: var(--space-4);
  margin-top: var(--space-4);
}

.order-card {
  min-height: calc(var(--space-8) * 2);
  padding: var(--space-4);
  color: var(--color-text);
  text-decoration: none;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  transition: border-color 160ms ease, box-shadow 160ms ease, transform 160ms ease;
}

.order-card__heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: var(--space-4);
}

.order-card:hover {
  color: var(--color-text);
  border-color: var(--color-primary-300);
  box-shadow: var(--shadow-card);
  transform: translateY(-1px);
}

.product-image {
  display: grid;
  width: 82px;
  height: 82px;
  overflow: hidden;
  place-items: center;
  color: var(--color-text-subtle);
  background: var(--color-bg-muted);
  border-radius: var(--radius-sm);
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.order-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
  color: var(--color-text);
  text-decoration: none;
}

/* //review-start，總共6次修改，第6次// */
.completed-items {
  display: grid;
  gap: var(--space-3);
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
}

.completed-item {
  display: grid;
  min-height: calc(var(--space-8) + var(--space-5));
  grid-template-columns: var(--space-8) minmax(0, 1fr) calc(var(--space-8) + var(--space-2));
  align-items: stretch;
  overflow: hidden;
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.completed-item .product-image {
  width: var(--space-8);
  height: 100%;
  min-height: calc(var(--space-8) + var(--space-4));
  border-radius: 0;
}

.completed-item__copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  padding: var(--space-3);
}

.completed-item__copy strong,
.completed-item__copy span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.completed-item__copy span {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}

.review-endcap {
  display: grid;
  min-width: calc(var(--space-8) + var(--space-2));
  place-items: center;
  color: var(--color-surface);
  font-size: var(--font-size-lg);
  text-decoration: none;
  border-left: 1px solid transparent;
  transition: filter 160ms ease, transform 160ms ease;
}

.review-endcap--pending {
  color: var(--color-text-muted);
  background: var(--color-disabled-bg);
  border-left-color: var(--color-border-strong);
}

.review-endcap--reviewed {
  color: var(--color-surface);
  background: var(--color-warning);
  border-left-color: var(--color-warning);
}

.review-endcap:hover,
.review-endcap:focus-visible {
  color: inherit;
  filter: brightness(0.92);
}

.review-endcap:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}
/* //review-end，總共6次修改，第6次// */

.order-copy strong {
  overflow: hidden;
  font-size: var(--font-size-xs);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-copy span,
.order-copy small {
  overflow: hidden;
  color: var(--color-text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-copy span {
  font-size: var(--font-size-xs);
}

.order-copy small {
  font-size: 10px;
}

.status-badge {
  min-width: 58px;
  padding: 6px 10px;
  color: var(--color-info);
  font-size: 10px;
  font-weight: 600;
  text-align: center;
  background: var(--color-info-soft);
  border-radius: var(--radius-sm);
}

.status-pending_payment,
.status-pending_shipment,
.status-pending_pickup {
  color: var(--color-warning);
  background: var(--color-warning-soft);
}

.status-completed {
  color: var(--color-success);
  background: var(--color-success-soft);
}

.status-cancelled {
  color: var(--color-danger);
  background: var(--color-danger-soft);
}

.order-total {
  min-width: 100px;
  font-size: 13px;
  text-align: right;
}

.state-card {
  display: flex;
  min-height: 180px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: var(--space-2);
  margin-top: var(--space-4);
  padding: var(--space-6);
  color: var(--color-text-muted);
  text-align: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.state-card > i {
  color: var(--color-primary);
  font-size: var(--font-size-xl);
}

.state-card strong {
  color: var(--color-text);
}

.state-card button {
  margin-top: var(--space-2);
  padding: var(--space-2) var(--space-4);
  color: var(--color-surface);
  font: inherit;
  font-weight: 600;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
}

.state-error > i {
  color: var(--color-danger);
}

@media (max-width: 767.98px) {
  .order-page {
    padding-top: var(--space-4);
  }

  .order-container {
    padding-inline: var(--space-4);
  }

  .filter-row {
    grid-template-columns: 1fr;
  }

  .order-card {
    padding: var(--space-4);
  }

  .order-card__heading {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .product-image {
    width: 64px;
    height: 64px;
  }

  .status-badge {
    align-self: start;
  }

  .order-total {
    grid-column: 1 / -1;
    min-width: 0;
    text-align: left;
  }
}

@media (max-width: 479.98px) {
  .order-tabs {
    gap: var(--space-4);
  }

  .order-card {
    padding: var(--space-3);
  }

  .product-image {
    width: 56px;
    height: 56px;
  }

  .status-badge,
  .order-total {
    grid-column: 1;
    justify-self: start;
  }

  .completed-item {
    grid-template-columns: var(--space-7) minmax(0, 1fr) var(--space-8);
  }

  .completed-item .product-image {
    width: var(--space-7);
  }
}
</style>
