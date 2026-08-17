<script setup>
import { computed } from 'vue'
import { useRoute, RouterLink } from 'vue-router'

const route = useRoute()

// 從網址 /seller/orders/:id 取得訂單 id
const orderId = computed(() => route.params.id)

// TODO：等待 D 模組訂單 API 完成後，改成依 orderId 呼叫 API
const order = {
  orderNo: 'DG240826-018',
  status: 'SHIPPING',
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

  shipping: {
    method: '宅配',
    status: '待出貨',
    trackingNumber: '',
  },

  items: [
    {
      id: 1,
      name: 'DinoGo 無線藍牙耳機',
      specification: '黑色',
      quantity: 1,
      price: 1280,
    },
    {
      id: 2,
      name: 'Type-C 快充線',
      specification: '1.5 公尺',
      quantity: 1,
      price: 350,
    },
  ],
}

const steps = [
  {
    label: '訂單成立',
    completed: true,
  },
  {
    label: '付款完成',
    completed: true,
  },
  {
    label: '備貨中',
    completed: true,
  },
  {
    label: '已出貨',
    completed: false,
  },
  {
    label: '已完成',
    completed: false,
  },
]

const totalAmount = computed(() => {
  return order.items.reduce((total, item) => {
    return total + item.price * item.quantity
  }, 0)
})

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'TWD',
    maximumFractionDigits: 0,
  }).format(amount)
}

const handleShipment = () => {
  // TODO：等待 D 模組提供更新物流狀態 API
  alert('目前為前端展示版本，出貨 API 尚未串接。')
}
</script>

<template>
  <section class="seller-page">
    <!-- 頁面標題 -->
    <header class="page-header">
      <div>
        <p class="eyebrow">訂單管理</p>
        <h1>商家訂單詳情</h1>
      </div>

      <RouterLink class="back-button" to="/seller/orders"> 返回訂單列表 </RouterLink>
    </header>

    <!-- 訂單狀態 -->
    <section class="status-card">
      <div class="status-header">
        <div>
          <p class="section-label">訂單編號</p>
          <h2>{{ order.orderNo }}</h2>
          <p class="order-id">系統 ID：{{ orderId }}</p>
        </div>

        <span class="status-badge">
          {{ order.statusText }}
        </span>
      </div>

      <!-- 訂單流程 -->
      <div class="order-progress">
        <div
          v-for="(step, index) in steps"
          :key="step.label"
          class="progress-item"
          :class="{ completed: step.completed }"
        >
          <div class="progress-marker">
            {{ step.completed ? '✓' : index + 1 }}
          </div>

          <span>{{ step.label }}</span>
        </div>
      </div>
    </section>

    <!-- 左右兩欄 -->
    <div class="detail-columns">
      <!-- 左側 -->
      <div class="detail-main">
        <!-- 商品明細 -->
        <section class="detail-card">
          <div class="card-header">
            <h2>商品明細</h2>
          </div>

          <div class="product-table">
            <div class="product-row product-head">
              <span>商品</span>
              <span>單價</span>
              <span>數量</span>
              <span>小計</span>
            </div>

            <div v-for="item in order.items" :key="item.id" class="product-row">
              <div>
                <strong>{{ item.name }}</strong>
                <p class="muted-text">
                  {{ item.specification }}
                </p>
              </div>

              <span>{{ formatCurrency(item.price) }}</span>

              <span>{{ item.quantity }}</span>

              <strong>
                {{ formatCurrency(item.price * item.quantity) }}
              </strong>
            </div>
          </div>

          <div class="order-total">
            <span>訂單總額</span>
            <strong>{{ formatCurrency(totalAmount) }}</strong>
          </div>
        </section>

        <!-- 買家與收件資料 -->
        <section class="detail-card">
          <div class="card-header">
            <h2>買家與收件資料</h2>
          </div>

          <div class="information-grid">
            <div>
              <p class="section-label">收件人</p>
              <strong>{{ order.buyer.name }}</strong>
            </div>

            <div>
              <p class="section-label">聯絡電話</p>
              <strong>{{ order.buyer.phone }}</strong>
            </div>

            <div class="full-width">
              <p class="section-label">收件地址</p>
              <strong>{{ order.buyer.address }}</strong>
            </div>
          </div>
        </section>

        <!-- 付款與配送 -->
        <section class="detail-card">
          <div class="card-header">
            <h2>付款與配送</h2>
          </div>

          <div class="information-grid">
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
          </div>
        </section>
      </div>

      <!-- 右側出貨操作 -->
      <aside class="shipping-card">
        <div>
          <p class="eyebrow">Shipping Action</p>
          <h2>出貨操作</h2>
        </div>

        <div class="shipping-status">
          <span>目前狀態</span>
          <strong>{{ order.shipping.status }}</strong>
        </div>

        <label class="form-field">
          物流單號

          <input type="text" placeholder="請輸入物流單號" />
        </label>

        <button class="primary-button" type="button" @click="handleShipment">標記為已出貨</button>

        <button class="secondary-button" type="button">聯絡平台客服</button>

        <p class="helper-text">確認商品已交付物流商後，再將訂單標記為已出貨。</p>
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

.eyebrow {
  margin: 0 0 var(--space-1);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
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
  margin-bottom: 0;
  font-family: var(--font-heading);
}

.back-button {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  padding: 0 var(--space-4);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-700);
  font-weight: 700;
  text-decoration: none;
}

.status-card,
.detail-card,
.shipping-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.status-card {
  padding: var(--space-5);
}

.status-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.section-label {
  margin-bottom: var(--space-1);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.order-id {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.status-badge {
  border-radius: 999px;
  padding: 6px 12px;
  background: #fff3d6;
  color: #8a5a00;
  font-weight: 700;
}

.order-progress {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  margin-top: var(--space-5);
}

.progress-item {
  position: relative;
  display: grid;
  justify-items: center;
  gap: var(--space-2);
  color: var(--color-text-muted);
  text-align: center;
  font-size: var(--font-size-sm);
}

.progress-item::before {
  content: '';
  position: absolute;
  top: 16px;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--color-border);
  z-index: 0;
}

.progress-item:first-child::before {
  left: 50%;
  width: 50%;
}

.progress-item:last-child::before {
  width: 50%;
}

.progress-marker {
  position: relative;
  z-index: 1;
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border: 2px solid var(--color-border-strong);
  border-radius: 50%;
  background: var(--color-surface);
  font-weight: 700;
}

.progress-item.completed {
  color: var(--color-primary);
  font-weight: 700;
}

.progress-item.completed .progress-marker {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: white;
}

.detail-columns {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--space-5);
  align-items: start;
}

.detail-main {
  display: grid;
  gap: var(--space-5);
}

.detail-card {
  overflow: hidden;
}

.card-header {
  padding: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.product-row {
  display: grid;
  grid-template-columns: minmax(0, 2fr) 0.8fr 0.5fr 0.8fr;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.product-head {
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.muted-text {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.order-total {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-4);
  padding: var(--space-4);
}

.order-total strong {
  font-size: var(--font-size-lg);
}

.information-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-5);
  padding: var(--space-4);
}

.full-width {
  grid-column: 1 / -1;
}

.shipping-card {
  position: sticky;
  top: var(--space-5);
  display: grid;
  gap: var(--space-4);
  padding: var(--space-5);
}

.shipping-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
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
  cursor: pointer;
}

.primary-button {
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: white;
}

.secondary-button {
  border: 1px solid var(--color-border-strong);
  background: var(--color-surface);
  color: var(--color-text-700);
}

.helper-text {
  margin-bottom: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: 1.6;
}

@media (max-width: 1000px) {
  .detail-columns {
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

  .order-progress {
    grid-template-columns: 1fr;
    gap: var(--space-3);
  }

  .progress-item {
    grid-template-columns: 32px 1fr;
    justify-items: start;
    align-items: center;
    text-align: left;
  }

  .progress-item::before {
    display: none;
  }

  .information-grid {
    grid-template-columns: 1fr;
  }

  .full-width {
    grid-column: auto;
  }

  .product-table {
    overflow-x: auto;
  }

  .product-row {
    min-width: 620px;
  }
}
</style>
