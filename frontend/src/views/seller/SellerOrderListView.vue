<script setup>
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'

// TODO: 等 D 模組提供賣家訂單列表 API 後，改由 API 載入。
const orders = [
  {
    id: 18,
    orderNo: 'DG240826-018',
    buyer: '陳怡安',
    amount: 1680,
    paymentStatus: '已付款',
    shippingStatus: '待出貨',
    orderStatus: '處理中',
    createdAt: '2026-08-14 10:30',
  },
  {
    id: 19,
    orderNo: 'DG240826-019',
    buyer: '李小華',
    amount: 2460,
    paymentStatus: '已付款',
    shippingStatus: '備貨中',
    orderStatus: '處理中',
    createdAt: '2026-08-14 11:15',
  },
  {
    id: 20,
    orderNo: 'DG240813-003',
    buyer: '陳美玲',
    amount: 980,
    paymentStatus: '已付款',
    shippingStatus: '已送達',
    orderStatus: '已完成',
    createdAt: '2026-08-13 16:40',
  },
]

const activeStatus = ref('ALL')

const statusTabs = [
  { label: '全部', value: 'ALL' },
  { label: '待出貨', value: '待出貨' },
  { label: '備貨中', value: '備貨中' },
  { label: '已完成', value: '已完成' },
]

const filteredOrders = computed(() => {
  if (activeStatus.value === 'ALL') {
    return orders
  }

  return orders.filter(
    (order) =>
      order.shippingStatus === activeStatus.value || order.orderStatus === activeStatus.value,
  )
})

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'TWD',
    maximumFractionDigits: 0,
  }).format(amount)
}

const statusClass = (status) => {
  if (status === '已完成' || status === '已送達') {
    return 'is-done'
  }

  if (status === '待出貨') {
    return 'is-warning'
  }

  return 'is-processing'
}
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">訂單管理</p>
        <h1>賣家訂單列表</h1>
        <p class="page-description">集中檢視每筆訂單狀態，點選查看進入出貨與明細處理。</p>
      </div>
    </header>

    <section class="list-panel">
      <div class="list-toolbar">
        <input class="search-input" type="search" placeholder="搜尋訂單編號或買家" />

        <div class="status-tabs" aria-label="訂單狀態篩選">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            type="button"
            :class="{ active: activeStatus === tab.value }"
            @click="activeStatus = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>

      <div class="order-table">
        <div class="table-header">
          <span>訂單編號</span>
          <span>買家</span>
          <span>訂單金額</span>
          <span>付款</span>
          <span>配送</span>
          <span>建立時間</span>
          <span>操作</span>
        </div>

        <article v-for="order in filteredOrders" :key="order.id" class="order-row">
          <RouterLink class="order-no" :to="`/seller/orders/${order.id}`">
            {{ order.orderNo }}
          </RouterLink>
          <span>{{ order.buyer }}</span>
          <strong>{{ formatCurrency(order.amount) }}</strong>
          <span class="status-badge is-paid">{{ order.paymentStatus }}</span>
          <span class="status-badge" :class="statusClass(order.shippingStatus)">
            {{ order.shippingStatus }}
          </span>
          <span>{{ order.createdAt }}</span>
          <RouterLink class="view-button" :to="`/seller/orders/${order.id}`">查看</RouterLink>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.seller-page {
  display: grid;
  gap: var(--space-5);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.eyebrow,
.page-description {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.eyebrow {
  margin: 0 0 var(--space-1);
}

.page-description {
  margin: var(--space-1) 0 0;
}

h1 {
  margin: 0;
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}

.list-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  overflow: hidden;
}

.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.status-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.status-tabs button {
  min-height: 36px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--color-surface);
  color: var(--color-text-700);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.status-tabs button.active {
  border-color: var(--color-primary-800);
  background: var(--color-primary-800);
  color: var(--color-surface);
}

.search-input {
  flex: 1;
  width: 100%;
  min-height: 40px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--color-surface);
  color: var(--color-text);
}

.order-table {
  display: grid;
}

.table-header,
.order-row {
  display: grid;
  grid-template-columns: minmax(160px, 1.2fr) 0.9fr 0.9fr 0.8fr 0.8fr 1fr 68px;
  align-items: center;
  gap: var(--space-4);
}

.table-header {
  min-height: 44px;
  padding: 0 var(--space-5);
  background: #050505;
  color: var(--color-surface);
  font-size: var(--font-size-xs);
  font-weight: 700;
}

.order-row {
  min-height: 64px;
  padding: 0 var(--space-5);
  border-top: 1px solid var(--color-border);
  color: var(--color-text-700);
  font-size: var(--font-size-sm);
}

.order-no {
  color: var(--color-text);
  font-weight: 800;
  text-decoration: none;
}

.order-no:hover {
  color: var(--color-primary);
}

.status-badge {
  width: fit-content;
  min-height: 26px;
  display: inline-flex;
  align-items: center;
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  font-size: var(--font-size-xs);
  font-weight: 700;
}

.status-badge.is-paid,
.status-badge.is-done {
  background: var(--color-success-soft);
  color: var(--color-success);
}

.status-badge.is-warning {
  background: var(--color-warning-soft);
  color: var(--color-warning);
}

.status-badge.is-processing {
  background: var(--color-primary-soft);
  color: var(--color-primary-700);
}

.view-button {
  width: fit-content;
  min-height: auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  color: var(--color-primary-700);
  font-weight: 700;
  text-decoration: none;
}

.view-button:hover {
  color: var(--color-primary);
}

@media (max-width: 1100px) {
  .table-header {
    display: none;
  }

  .order-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: var(--space-3);
    padding: var(--space-4) var(--space-5);
  }

  .order-no,
  .view-button {
    grid-column: 1 / -1;
  }
}

@media (max-width: 720px) {
  .list-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }
}
</style>
