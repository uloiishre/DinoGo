<script setup>
const summaryItems = [
  { label: '今日訂單', value: '8', note: '較昨日 +2', icon: 'cal' },
  { label: '待出貨', value: '3', note: '所有今日出貨單', icon: 'ship' },
  { label: '上架商品', value: '24', note: '2 件庫存不足', icon: 'box' },
  { label: '店鋪狀態', value: '營運中', note: '貨架完整度 90%', icon: 'shop' },
]

const recentOrders = [
  { orderNo: '#DG-0182', buyer: 'Dino', status: '待出貨', amount: 'NT$1,280' },
  { orderNo: '#DG-0180', buyer: 'Aki', status: '已出貨', amount: 'NT$2,462' },
  { orderNo: '#DG-0179', buyer: 'Hina', status: '已完成', amount: 'NT$980' },
]

const quickActions = [
  { label: '新增商品', to: '/seller/products/new' },
  { label: '查看待出貨', to: '/seller/orders' },
  { label: '查看庫存警報', to: '/seller/products' },
  { label: '編輯店鋪資料', to: '/seller/profile' },
]
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <h1>商家營運總覽</h1>
        <p class="eyebrow">咬日雜物 · 今日營運狀態</p>
      </div>

      <button class="page-action" type="button">查看店鋪</button>
    </header>

    <div class="summary-grid">
      <article v-for="item in summaryItems" :key="item.label" class="summary-card">
        <div class="card-topline">
          <span>{{ item.label }}</span>
          <small>{{ item.icon }}</small>
        </div>
        <strong>{{ item.value }}</strong>
        <em>{{ item.note }}</em>
      </article>
    </div>

    <div class="dashboard-grid">
      <section class="orders-panel">
        <div class="panel-header">
          <h2>最近訂單</h2>
          <button type="button">查看全部</button>
        </div>

        <div class="order-table">
          <div class="order-row order-head">
            <span>訂單</span>
            <span>買家</span>
            <span>狀態</span>
            <span>金額</span>
          </div>

          <div v-for="order in recentOrders" :key="order.orderNo" class="order-row">
            <span>{{ order.orderNo }}</span>
            <span>{{ order.buyer }}</span>
            <span>{{ order.status }}</span>
            <span>{{ order.amount }}</span>
          </div>
        </div>
      </section>

      <section class="actions-panel">
        <h2>快捷操作</h2>
        <RouterLink
          v-for="action in quickActions"
          :key="action.label"
          class="panel-action"
          :to="action.to"
        >
          <span>+</span>
          {{ action.label }}
        </RouterLink>
      </section>
    </div>
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
  margin-bottom: var(--space-1);
}

.eyebrow {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

h1,
h2 {
  margin: 0;
}

h1 {
  color: var(--color-text-900);
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}

h2 {
  color: var(--color-text-800);
  font-size: var(--font-size-base);
}

.page-action {
  min-height: 40px;
  border: 0;
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  background: var(--color-primary);
  color: var(--color-surface);
  font-weight: 600;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
}

.summary-card {
  min-height: 104px;
  display: grid;
  gap: var(--space-2);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.card-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-topline span,
.summary-card em {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.card-topline small {
  color: var(--color-primary-700);
  font-size: var(--font-size-xs);
  font-style: normal;
}

.summary-card strong {
  color: var(--color-text-900);
  font-size: var(--font-size-xl);
  line-height: 1;
}

.summary-card em {
  font-style: normal;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: var(--space-4);
}

.orders-panel,
.actions-panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4);
}

.panel-header button {
  border: 0;
  background: transparent;
  color: var(--color-primary-700);
  font-size: var(--font-size-xs);
}

.order-table {
  display: grid;
}

.order-row {
  min-height: 112px;
  display: grid;
  grid-template-columns: 1.2fr 1.2fr 1fr 1fr;
  align-items: center;
  gap: var(--space-3);
  padding: 0 var(--space-4);
  border-top: 1px solid var(--color-border);
  font-size: var(--font-size-sm);
}

.order-head {
  min-height: 36px;
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.actions-panel {
  padding: var(--space-4);
}

.actions-panel h2 {
  margin-bottom: var(--space-4);
}

.panel-action {
  text-decoration: none;
  box-sizing: border-box;
  width: 100%;
  min-height: 48px;
  display: grid;
  grid-template-columns: 24px 1fr auto;
  align-items: center;
  gap: var(--space-2);
  border: 0;
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-700);
  text-align: left;
  font-weight: 600;
}

.panel-action:first-of-type {
  border-top: 0;
}

.actions-panel small {
  color: var(--color-text-subtle);
  font-size: var(--font-size-base);
}

@media (max-width: 960px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .order-row {
    min-height: auto;
    grid-template-columns: 1fr 1fr;
    padding: var(--space-4);
  }
}
</style>
