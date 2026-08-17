<script setup>
const summaryItems = [
  { label: '今日訂單', value: '8', note: '較昨日 +2', icon: 'cal' },
  { label: '待出貨', value: '3', note: '所有今日出貨單', icon: 'ship' },
  { label: '上架商品', value: '24', note: '2 件庫存不足', icon: 'box' },
  { label: '店鋪狀態', value: '營運中', note: '貨架完整度 90%', icon: 'shop' },
]

const salesOverview = [
  { label: '今日營收', value: 'NT$4,722', note: '來自 8 筆訂單' },
  { label: '平均客單價', value: 'NT$590', note: '較昨日 +8%' },
  { label: '轉換提醒', value: '3 筆', note: '待出貨優先處理' },
]

const focusItems = [
  { label: '待出貨訂單', value: '3', tone: 'warning' },
  { label: '低庫存商品', value: '2', tone: 'danger' },
  { label: '草稿商品', value: '5', tone: 'neutral' },
]

const trendBars = [42, 58, 38, 74, 62, 88, 70]

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
      <section class="overview-panel">
        <div class="panel-header">
          <div>
            <h2>銷售儀錶板</h2>
            <p>快速掌握今日營收、待辦與庫存狀態。</p>
          </div>
        </div>

        <div class="overview-grid">
          <article v-for="item in salesOverview" :key="item.label" class="overview-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <em>{{ item.note }}</em>
          </article>
        </div>

        <div class="insight-row">
          <section class="focus-list">
            <h3>今日待辦</h3>
            <div v-for="item in focusItems" :key="item.label" class="focus-item" :class="item.tone">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </section>

          <section class="trend-panel">
            <h3>近 7 日銷售趨勢</h3>
            <div class="trend-chart" aria-label="近 7 日銷售趨勢">
              <span
                v-for="(height, index) in trendBars"
                :key="index"
                class="trend-bar"
                :style="{ height: `${height}%` }"
              ></span>
            </div>
          </section>
        </div>
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
  grid-template-columns: 1fr;
  gap: var(--space-4);
}

.overview-panel {
  width: 100%;
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

.panel-header p {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-3);
  padding: 0 var(--space-4) var(--space-4);
}

.overview-card {
  min-height: 110px;
  display: grid;
  align-content: center;
  gap: var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
  padding: var(--space-4);
}

.overview-card span,
.overview-card em {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-style: normal;
}

.overview-card strong {
  color: var(--color-text-900);
  font-size: 28px;
  line-height: 1;
}

.insight-row {
  display: grid;
  grid-template-columns: minmax(240px, 0.8fr) minmax(0, 1.2fr);
  gap: var(--space-4);
  padding: 0 var(--space-4) var(--space-4);
}

.focus-list,
.trend-panel {
  display: grid;
  gap: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-4);
}

h3 {
  margin: 0;
  color: var(--color-text-800);
  font-size: var(--font-size-sm);
}

.focus-item {
  min-height: 42px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--color-bg-muted);
  color: var(--color-text-700);
  font-size: var(--font-size-sm);
}

.focus-item strong {
  font-size: var(--font-size-lg);
}

.focus-item.warning strong {
  color: var(--color-warning);
}

.focus-item.danger strong {
  color: var(--color-danger);
}

.trend-chart {
  min-height: 160px;
  display: flex;
  align-items: end;
  gap: var(--space-3);
  padding-top: var(--space-3);
}

.trend-bar {
  flex: 1;
  min-height: 24px;
  border-radius: var(--radius-sm) var(--radius-sm) 0 0;
  background: var(--color-primary);
  opacity: 0.72;
}

@media (max-width: 960px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .insight-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .summary-grid,
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
