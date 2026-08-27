<script setup>
import { computed, onMounted, ref } from 'vue'
import { getMemberOrders } from '@/api/order'
import { getOrderDisplayStatus, isOrderInDisplayGroup } from '@/utils/orderDisplayStatus'
import { getImageUrl } from '@/utils/imageUrl'

const orders = ref([])
const loading = ref(true)
const errorMessage = ref('')
const activeStatus = ref('ALL')
const keyword = ref('')
const sortOrder = ref('NEWEST')

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
        ...(order.items ?? []).flatMap((item) => [item.productName, item.skuSpec]),
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
        <RouterLink
          v-for="order in visibleOrders"
          :key="order.orderId"
          class="order-card"
          :data-order-id="order.orderId"
          :to="{ name: 'MemberOrderDetail', params: { id: order.orderId } }"
          :aria-label="`查看訂單 ${order.orderNo}`"
        >
          <div class="product-image">
            <img
              v-if="firstItem(order)?.productImageUrl"
              :src="getImageUrl(firstItem(order).productImageUrl)"
              :alt="firstItem(order).productName"
            />
            <i v-else class="bi bi-image" aria-hidden="true"></i>
          </div>

          <div class="order-copy">
            <!-- <strong>訂單 #{{ order.orderNo }}</strong> -->
            <strong>{{ itemSummary(order) }}</strong>
            <!-- <span>{{ itemSummary(order) }}</span> -->
            <small>{{ formatDate(order.createdAt) }}</small>
          </div>

          <span
            class="status-badge"
            :class="`status-${getOrderDisplayStatus(order).key.toLowerCase()}`"
          >
            {{ getOrderDisplayStatus(order).label }}
          </span>

          <strong class="order-total">{{ formatCurrency(order.totalAmount) }}</strong>
        </RouterLink>
      </div>
    </div>
  </section>
</template>

<style scoped>
.order-page {
  min-height: 620px;
  padding: 40px 0;
  background: var(--color-bg);
}

.order-container {
  --bs-gutter-x: var(--space-6);
  max-width: 1232px;
}

.page-header {
  display: flex;
  min-height: 68px;
  flex-direction: column;
  justify-content: center;
  gap: var(--space-1);
  margin-bottom: var(--space-5);
}

.page-header h1 {
  margin: 0;
  color: var(--color-text);
  font-family: var(--font-body);
  font-size: var(--font-size-xl);
  font-weight: 700;
  line-height: var(--line-height-heading);
}

.page-header p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-base);
}

.order-tabs {
  display: flex;
  min-height: 42px;
  align-items: stretch;
  gap: var(--space-5);
  overflow-y: hidden;
  border-bottom: 1px solid var(--color-border);
}

.order-tabs button {
  position: relative;
  flex: 0 0 auto;
  padding: 0;
  color: var(--color-text-muted);
  font-size: 15px;
  font-weight: 600;
  line-height: var(--line-height-base);
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
  font-size: var(--font-size-base);
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
  display: grid;
  min-height: 126px;
  grid-template-columns: 82px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: var(--space-4);
  padding: 18px;
  color: var(--color-text);
  text-decoration: none;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease,
    transform 160ms ease;
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
}

.order-copy strong {
  overflow: hidden;
  font-size: var(--font-size-base);
  font-weight: 600;
  line-height: 1.45;
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
  font-size: var(--font-size-sm);
  line-height: var(--line-height-base);
}

.status-badge {
  display: inline-flex;
  min-height: 32px;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  padding: var(--space-1) var(--space-3);
  color: var(--color-info);
  font-size: var(--font-size-sm);
  font-weight: 600;
  line-height: 1.3;
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
  font-size: var(--font-size-md);
  font-weight: 700;
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
  font-size: var(--font-size-base);
  line-height: var(--line-height-base);
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
  font-size: var(--font-size-md);
  font-weight: 700;
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
  .filter-row {
    grid-template-columns: 1fr;
  }

  .order-card {
    grid-template-columns: 64px minmax(0, 1fr) auto;
    padding: var(--space-4);
  }

  .product-image {
    width: 64px;
    height: 64px;
  }

  .status-badge {
    align-self: start;
  }

  .order-total {
    grid-column: 2 / -1;
    min-width: 0;
    text-align: left;
  }
}

@media (max-width: 575.98px) {
  .order-page {
    padding: var(--space-6) 0;
  }
}

@media (max-width: 479.98px) {
  .order-tabs {
    gap: var(--space-4);
  }

  .order-card {
    grid-template-columns: 56px minmax(0, 1fr);
  }

  .product-image {
    width: 56px;
    height: 56px;
  }

  .status-badge,
  .order-total {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
