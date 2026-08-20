<script setup>
const alertItems = [
  { label: '待付款', value: '5', note: '提醒買家完成付款', tone: 'warning', icon: 'bi-credit-card' },
  { label: '待出貨', value: '3', note: '今日優先處理', tone: 'danger', icon: 'bi-truck' },
  { label: '退貨/退款', value: '1', note: '需在 24 小時內回覆', tone: 'danger', icon: 'bi-arrow-counterclockwise' },
  { label: '未回覆訊息', value: '7', note: '平均等待 18 分鐘', tone: 'warning', icon: 'bi-chat-left-text' },
  { label: '低庫存商品', value: '2', note: '建議立即補貨', tone: 'warning', icon: 'bi-box-seam' },
  { label: '審核未通過', value: '1', note: '商品需重新送審', tone: 'neutral', icon: 'bi-shield-exclamation' },
]

const salesMetrics = [
  { label: '今日銷售額', value: 'NT$4,722', change: '+12%', trend: 'up', note: '較昨日同時段' },
  { label: '訂單數', value: '8', change: '+2 筆', trend: 'up', note: '較昨日同時段' },
  { label: '轉換率', value: '3.8%', change: '-0.4%', trend: 'down', note: '較上週同期' },
  { label: '平均客單價', value: 'NT$590', change: '+8%', trend: 'up', note: '較昨日同時段' },
]

const revenueItems = [
  { label: '已完結金額', value: 'NT$18,420', note: '完成訂單可計入收益' },
  { label: '預計撥款', value: 'NT$12,880', note: '下一批撥款預估' },
]

const insightItems = [
  { title: '托特包流量升高但庫存偏低', action: '建議補貨 30 件並提高曝光商品排序。' },
  { title: '新會員 9 折券使用率 42%', action: '建議延長活動 3 天，提高首次下單轉換。' },
  { title: '收納袋曝光高但轉換率偏低', action: '建議優化首圖與商品描述。' },
]

const productHighlights = [
  { label: '最高銷售額商品', value: '苔色日常托特包', note: 'NT$8,400 / 5 筆訂單' },
  { label: '潛力商品', value: '山影收納袋', note: '流量 +38%，庫存剩 6 件' },
]

const trafficSources = [
  { label: '站內搜尋', value: '46%' },
  { label: '優惠券入口', value: '28%' },
  { label: '收藏回訪', value: '16%' },
  { label: '其他', value: '10%' },
]

const platformNotices = [
  { title: '8/25 夏末大促報名截止', note: '剩 5 天，符合資格商品 12 件。' },
  { title: '物流費率將於 9/1 調整', note: '宅配大型材積將新增級距。' },
]
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <h1>商家營運總覽</h1>
        <p class="eyebrow">森日選物 · 今日營運狀態</p>
      </div>
    </header>

    <section class="priority-panel">
      <div class="panel-heading">
        <div>
          <p class="section-kicker">Priority</p>
          <h2>營運待辦與即時警訊</h2>
        </div>
        <span>需優先處理</span>
      </div>

      <div class="alert-grid">
        <article v-for="item in alertItems" :key="item.label" class="alert-card" :class="item.tone">
          <i class="bi" :class="item.icon" aria-hidden="true"></i>
          <div>
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.note }}</p>
          </div>
        </article>
      </div>
    </section>

    <section class="metric-panel">
      <div class="panel-heading">
        <div>
          <p class="section-kicker">Sales Pulse</p>
          <h2>核心銷售數據</h2>
        </div>
      </div>

      <div class="metric-grid">
        <article v-for="item in salesMetrics" :key="item.label" class="metric-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em :class="item.trend">
            <i class="bi" :class="item.trend === 'up' ? 'bi-arrow-up-right' : 'bi-arrow-down-right'" aria-hidden="true"></i>
            {{ item.change }}
          </em>
          <small>{{ item.note }}</small>
        </article>
      </div>

      <div class="revenue-grid">
        <article v-for="item in revenueItems" :key="item.label" class="revenue-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.note }}</p>
        </article>
      </div>
    </section>

    <div class="insight-layout">
      <section class="insight-panel">
        <div class="panel-heading">
          <div>
            <p class="section-kicker">Insights</p>
            <h2>商業洞察與行動建議</h2>
          </div>
        </div>

        <div class="highlight-grid">
          <article v-for="item in productHighlights" :key="item.label" class="highlight-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.note }}</p>
          </article>
        </div>

        <div class="suggestion-list">
          <article v-for="item in insightItems" :key="item.title" class="suggestion-card">
            <i class="bi bi-stars" aria-hidden="true"></i>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.action }}</p>
            </div>
          </article>
        </div>
      </section>

      <aside class="side-panel">
        <section class="traffic-card">
          <h2>行銷與流量</h2>
          <div v-for="item in trafficSources" :key="item.label" class="traffic-row">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </section>

        <section class="notice-card">
          <h2>平台重要通知</h2>
          <article v-for="notice in platformNotices" :key="notice.title">
            <strong>{{ notice.title }}</strong>
            <p>{{ notice.note }}</p>
          </article>
        </section>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.seller-page {
  display: grid;
  gap: var(--space-5);
  max-width: 1280px;
}

.page-header,
.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.eyebrow,
.section-kicker,
.panel-heading span,
.alert-card span,
.alert-card p,
.metric-card span,
.metric-card small,
.revenue-card span,
.revenue-card p,
.highlight-card span,
.highlight-card p,
.suggestion-card p,
.traffic-row span,
.notice-card p {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.eyebrow,
.section-kicker,
h1,
h2,
p {
  margin-top: 0;
}

.eyebrow,
.section-kicker {
  margin-bottom: var(--space-1);
}

h1,
h2 {
  margin-bottom: 0;
  color: var(--color-text-900);
  font-family: var(--font-heading);
}

h1 {
  font-size: var(--font-size-xl);
}

h2 {
  font-size: var(--font-size-base);
}

.priority-panel,
.metric-panel,
.insight-panel,
.traffic-card,
.notice-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  padding: var(--space-5);
}

.priority-panel {
  border-left: 4px solid var(--color-danger);
}

.alert-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.alert-card {
  min-height: 96px;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  background: var(--color-bg-muted);
}

.alert-card i {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-primary);
}

.alert-card div,
.metric-card,
.revenue-card,
.highlight-card,
.suggestion-card div,
.traffic-card,
.notice-card {
  display: grid;
  gap: var(--space-2);
}

.alert-card strong,
.metric-card strong,
.revenue-card strong {
  color: var(--color-text-900);
  font-size: 26px;
  line-height: 1;
}

.alert-card.danger strong {
  color: var(--color-danger);
}

.alert-card.warning strong {
  color: var(--color-warning);
}

.alert-card p,
.revenue-card p,
.highlight-card p,
.suggestion-card p,
.notice-card p {
  margin-bottom: 0;
  line-height: 1.6;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.metric-card,
.revenue-card,
.highlight-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
  padding: var(--space-4);
}

.metric-card em {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border-radius: var(--radius-pill);
  padding: 3px 9px;
  font-size: var(--font-size-xs);
  font-style: normal;
  font-weight: 800;
}

.metric-card em.up {
  background: var(--color-success-soft);
  color: var(--color-success);
}

.metric-card em.down {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.revenue-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
  margin-top: var(--space-3);
}

.insight-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: start;
  gap: var(--space-5);
}

.side-panel {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.highlight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.highlight-card strong,
.suggestion-card strong,
.traffic-row strong,
.notice-card strong {
  color: var(--color-text-900);
}

.suggestion-list {
  display: grid;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.suggestion-card {
  display: flex;
  gap: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-4);
}

.suggestion-card i {
  color: var(--color-primary);
}

.side-panel { display: grid; gap: var(--space-4); }

.traffic-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: var(--radius-md);
  padding: var(--space-3);
  background: var(--color-bg-muted);
}

.notice-card article {
  border-top: 1px solid var(--color-border);
  padding-top: var(--space-3);
}

@media (max-width: 1120px) {
  .side-panel { grid-template-columns: 1fr; }
}

@media (max-width: 680px) {
  .alert-grid,
  .metric-grid,
  .revenue-grid,
  .highlight-grid {
    grid-template-columns: 1fr;
  }
}
</style>
