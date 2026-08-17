<script setup>
import { computed, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()
const trackingNumber = ref('')

// TODO: 等 D 模組提供賣家訂單詳情 API 後，改成依 orderId 載入。
const orders = [
  {
    id: 18,
    orderNo: 'DG240826-018',
    statusText: '待出貨',
    createdAt: '2026-08-14 10:30',
    buyer: {
      name: '陳怡安',
      phone: '0912-345-678',
      address: '台北市中山區南京東路三段 100 號',
    },
    payment: {
      method: 'LINE Pay',
      status: '已付款',
    },
    coupon: {
      name: '新會員首購折抵',
      code: 'WELCOME100',
      discountAmount: 100,
    },
    shipping: {
      method: '宅配',
      status: '待出貨',
    },
    items: [
      { id: 1, name: '苔色日常托特包', specification: '苔綠', quantity: 1, price: 1680 },
      { id: 2, name: '山影收納袋', specification: '霧灰', quantity: 2, price: 800 },
    ],
  },
  {
    id: 19,
    orderNo: 'DG240826-019',
    statusText: '備貨中',
    createdAt: '2026-08-14 11:15',
    buyer: {
      name: '李小華',
      phone: '0988-123-456',
      address: '新北市板橋區文化路一段 20 號',
    },
    payment: {
      method: '信用卡',
      status: '已付款',
    },
    coupon: null,
    shipping: {
      method: '超商取貨',
      status: '備貨中',
    },
    items: [{ id: 1, name: '晨光陶杯組', specification: '米白 / 2 入', quantity: 1, price: 2460 }],
  },
]

const order = computed(() => {
  return orders.find((item) => item.id === Number(route.params.id)) ?? orders[0]
})

const steps = computed(() => [
  { label: '訂單成立', completed: true },
  { label: '付款完成', completed: true },
  { label: '備貨中', completed: true },
  {
    label: '已出貨',
    completed: order.value.shipping.status === '已出貨' || order.value.shipping.status === '已送達',
  },
  { label: '已完成', completed: order.value.statusText === '已完成' },
])

const totalAmount = computed(() => {
  return order.value.items.reduce((total, item) => total + item.price * item.quantity, 0)
})

const payableAmount = computed(() => {
  return Math.max(totalAmount.value - (order.value.coupon?.discountAmount ?? 0), 0)
})

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'TWD',
    maximumFractionDigits: 0,
  }).format(amount)
}

const handleShipment = () => {
  alert('目前為前端展示版本，出貨 API 尚未串接。')
}
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">訂單管理</p>
        <h1>商家訂單詳情</h1>
        <p class="page-description">核對買家、商品與配送資訊後完成出貨。</p>
      </div>

      <RouterLink class="back-button" to="/seller/orders">返回訂單列表</RouterLink>
    </header>

    <section class="status-card">
      <div>
        <p class="section-label">訂單 {{ order.orderNo }}</p>
        <strong>{{ order.statusText }}</strong>
      </div>

      <div class="order-progress">
        <div
          v-for="step in steps"
          :key="step.label"
          class="progress-item"
          :class="{ completed: step.completed }"
        >
          <span class="progress-dot"></span>
          <small>{{ step.label }}</small>
        </div>
      </div>
    </section>

    <div class="detail-layout">
      <div class="detail-main">
        <section class="detail-card">
          <h2>商品明細</h2>

          <div class="item-list">
            <div v-for="item in order.items" :key="item.id" class="item-row">
              <div>
                <strong>{{ item.name }}</strong>
                <p>{{ item.specification }} × {{ item.quantity }}</p>
              </div>
              <span>{{ formatCurrency(item.price * item.quantity) }}</span>
            </div>
          </div>

          <div class="total-row">
            <span>訂單總額</span>
            <strong>{{ formatCurrency(totalAmount) }}</strong>
          </div>

          <div v-if="order.coupon" class="total-row discount-row">
            <span>優惠券折抵</span>
            <strong>-{{ formatCurrency(order.coupon.discountAmount) }}</strong>
          </div>

          <div class="total-row payable-row">
            <span>實付金額</span>
            <strong>{{ formatCurrency(payableAmount) }}</strong>
          </div>
        </section>

        <section class="detail-card">
          <h2>買家與收件資料</h2>

          <div class="info-grid">
            <div>
              <p class="section-label">收件人</p>
              <strong>{{ order.buyer.name }}</strong>
            </div>
            <div>
              <p class="section-label">電話</p>
              <strong>{{ order.buyer.phone }}</strong>
            </div>
            <div class="full-width">
              <p class="section-label">地址</p>
              <strong>{{ order.buyer.address }}</strong>
            </div>
          </div>
        </section>

        <section class="detail-card">
          <h2>付款與配送</h2>

          <div class="info-grid">
            <div>
              <p class="section-label">付款方式</p>
              <strong>{{ order.payment.method }}</strong>
            </div>
            <div>
              <p class="section-label">付款狀態</p>
              <strong>{{ order.payment.status }}</strong>
            </div>
            <div>
              <p class="section-label">配送方式</p>
              <strong>{{ order.shipping.method }}</strong>
            </div>
            <div>
              <p class="section-label">物流狀態</p>
              <strong>{{ order.shipping.status }}</strong>
            </div>
            <div class="coupon-box full-width" :class="{ empty: !order.coupon }">
              <p class="section-label">優惠券使用</p>
              <template v-if="order.coupon">
                <strong>{{ order.coupon.name }}</strong>
                <span>{{ order.coupon.code }} · 折抵 {{ formatCurrency(order.coupon.discountAmount) }}</span>
              </template>
              <template v-else>
                <strong>未使用優惠券</strong>
                <span>此訂單沒有套用賣家優惠券。</span>
              </template>
            </div>
          </div>
        </section>
      </div>

      <aside class="shipping-card">
        <h2>出貨操作</h2>
        <p>確認商品已交付物流商後，再將訂單標記為已出貨。</p>

        <label class="form-field">
          物流單號
          <input v-model="trackingNumber" type="text" placeholder="請輸入物流單號" />
        </label>

        <button class="primary-button" type="button" @click="handleShipment">標記為已出貨</button>
        <button class="secondary-button" type="button">聯絡平台客服</button>
      </aside>
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
}

.eyebrow,
.page-description,
.section-label,
.detail-card p,
.shipping-card p {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.eyebrow {
  margin: 0 0 var(--space-1);
}

.page-description {
  margin: var(--space-1) 0 0;
}

h1,
h2,
p {
  margin-top: 0;
}

h1 {
  margin-bottom: 0;
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}

h2 {
  margin-bottom: var(--space-4);
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
}

.back-button {
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  background: var(--color-surface);
  color: var(--color-text-700);
  font-weight: 700;
  text-decoration: none;
}

.status-card,
.detail-card,
.shipping-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.status-card {
  display: grid;
  gap: var(--space-5);
  padding: var(--space-5);
}

.status-card strong {
  color: var(--color-warning);
  font-size: var(--font-size-xl);
}

.order-progress {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0;
}

.progress-item {
  position: relative;
  display: grid;
  justify-items: center;
  gap: var(--space-3);
  color: var(--color-text-muted);
  text-align: center;
}

.progress-item::before,
.progress-item::after {
  position: absolute;
  top: 5px;
  width: 50%;
  height: 2px;
  content: '';
  background: var(--color-border);
}

.progress-item::before {
  left: 0;
}

.progress-item::after {
  right: 0;
}

.progress-item:first-child::before,
.progress-item:last-child::after {
  display: none;
}

.progress-item.completed::before,
.progress-item.completed::after {
  background: var(--color-primary);
}

.progress-dot {
  position: relative;
  z-index: 1;
  width: 10px;
  height: 10px;
  box-shadow: 0 0 0 6px var(--color-surface);
  border-radius: 50%;
  background: var(--color-border-strong);
}

.progress-item.completed .progress-dot {
  background: var(--color-primary);
}

.progress-item.completed {
  color: var(--color-text-700);
  font-weight: 700;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--space-5);
  align-items: start;
}

.detail-main {
  display: grid;
  gap: var(--space-4);
}

.detail-card,
.shipping-card {
  padding: var(--space-5);
}

.item-list {
  display: grid;
  gap: var(--space-3);
}

.item-row,
.total-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.item-row {
  min-height: 52px;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: var(--space-3);
}

.item-row p {
  margin: var(--space-1) 0 0;
}

.total-row {
  padding-top: var(--space-4);
}

.total-row strong {
  font-size: var(--font-size-lg);
}

.discount-row {
  padding-top: var(--space-2);
  color: var(--color-success);
}

.discount-row strong {
  font-size: var(--font-size-base);
}

.payable-row {
  border-top: 1px solid var(--color-border);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

.section-label {
  margin-bottom: var(--space-1);
}

.full-width {
  grid-column: 1 / -1;
}

.coupon-box {
  display: grid;
  gap: var(--space-1);
  border: 1px solid var(--color-success);
  border-radius: var(--radius-md);
  background: var(--color-success-soft);
  padding: var(--space-4);
}

.coupon-box span {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.coupon-box.empty {
  border-color: var(--color-border);
  background: var(--color-bg-muted);
}

.shipping-card {
  position: sticky;
  top: var(--space-5);
  display: grid;
  gap: var(--space-4);
}

.shipping-card h2,
.shipping-card p {
  margin-bottom: 0;
}

.form-field {
  display: grid;
  gap: var(--space-2);
  color: var(--color-text-700);
  font-weight: 700;
}

input {
  width: 100%;
  min-height: 40px;
  box-sizing: border-box;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--color-surface);
  color: var(--color-text);
}

button {
  min-height: 42px;
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  font-weight: 700;
}

.primary-button {
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: var(--color-surface);
}

.secondary-button {
  border: 1px solid var(--color-border-strong);
  background: var(--color-surface);
  color: var(--color-text-700);
}

@media (max-width: 1000px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .shipping-card {
    position: static;
  }
}

@media (max-width: 720px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .order-progress,
  .info-grid {
    grid-template-columns: 1fr;
  }

  .progress-item {
    justify-items: start;
    text-align: left;
  }

  .progress-item::before,
  .progress-item::after {
    display: none;
  }

  .full-width {
    grid-column: auto;
  }
}
</style>
