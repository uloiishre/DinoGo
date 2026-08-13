<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getOrder } from '@/api/order'

const route = useRoute()
const order = ref(null)
const loading = ref(true)
const errorMessage = ref('')

const orderId = computed(() => Number(route.params.id ?? route.params.orderId))

const statusLabels = {
  PENDING_PAYMENT: '待付款',
  PAID: '已付款',
  PROCESSING: '處理中',
  SHIPPED: '已出貨',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

const progressSteps = [
  { label: '訂單成立', statuses: ['PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'COMPLETED'] },
  { label: '賣家處理', statuses: ['PAID', 'PROCESSING', 'SHIPPED', 'COMPLETED'] },
  { label: '商品出貨', statuses: ['SHIPPED', 'COMPLETED'] },
  { label: '完成訂單', statuses: ['COMPLETED'] },
]

const fullAddress = computed(() => {
  if (!order.value) return '—'
  return [
    order.value.shippingPostalCode,
    order.value.shippingCity,
    order.value.shippingDistrict,
    order.value.shippingDetailAddress,
  ]
    .filter(Boolean)
    .join(' ')
})

async function loadOrder() {
  loading.value = true
  errorMessage.value = ''

  if (!Number.isInteger(orderId.value) || orderId.value <= 0) {
    errorMessage.value = '訂單編號格式不正確。'
    loading.value = false
    return
  }

  try {
    order.value = (await getOrder(orderId.value)).data
  } catch (error) {
    errorMessage.value = error.response?.data?.message ?? '訂單詳情載入失敗，請稍後再試。'
  } finally {
    loading.value = false
  }
}

function isStepComplete(step) {
  return order.value && step.statuses.includes(order.value.status)
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

onMounted(loadOrder)
</script>

<template>
  <section class="order-detail-page">
    <div class="container detail-container">
      <header class="page-header">
        <div>
          <h1>訂單詳情</h1>
          <p v-if="order">
            訂單 #{{ order.orderNo }}・建立於 {{ formatDate(order.createdAt) }}
          </p>
          <p v-else>查看訂單商品、配送與付款資訊</p>
        </div>

        <RouterLink class="back-button" :to="{ name: 'MemberOrders' }">
          返回訂單列表
        </RouterLink>
      </header>

      <div v-if="loading" class="state-card" aria-live="polite">
        <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        <span>正在載入訂單詳情...</span>
      </div>

      <div v-else-if="errorMessage" class="state-card state-error" role="alert">
        <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
        <strong>無法載入訂單</strong>
        <span>{{ errorMessage }}</span>
        <button type="button" @click="loadOrder">重新載入</button>
      </div>

      <template v-else-if="order">
        <section class="progress-card" aria-label="訂單進度">
          <div
            v-for="(step, index) in progressSteps"
            :key="step.label"
            class="progress-step"
            :class="{ complete: isStepComplete(step), cancelled: order.status === 'CANCELLED' }"
          >
            <span class="step-marker">
              <i
                class="bi"
                :class="isStepComplete(step) ? 'bi-check-lg' : 'bi-circle'"
                aria-hidden="true"
              ></i>
            </span>
            <strong>{{ step.label }}</strong>
            <span v-if="index < progressSteps.length - 1" class="step-line" aria-hidden="true"></span>
          </div>
        </section>

        <div v-if="order.status === 'CANCELLED'" class="cancelled-notice" role="status">
          <i class="bi bi-x-circle" aria-hidden="true"></i>
          <div>
            <strong>此訂單已取消</strong>
            <span>{{ order.cancelReason || '未提供取消原因' }}</span>
          </div>
        </div>

        <div class="detail-columns">
          <section class="detail-card product-card">
            <div class="card-heading">
              <h2>商品明細</h2>
              <span class="status-badge" :class="`status-${order.status?.toLowerCase()}`">
                {{ statusLabels[order.status] ?? order.status }}
              </span>
            </div>

            <article v-for="item in order.items" :key="item.orderItemId" class="product-row">
              <div class="product-image">
                <img
                  v-if="item.productImageUrl"
                  :src="item.productImageUrl"
                  :alt="item.productName"
                />
                <i v-else class="bi bi-image" aria-hidden="true"></i>
              </div>

              <div class="product-copy">
                <strong>{{ item.productName }}</strong>
                <span>{{ item.skuSpec || '單一規格' }}・數量 {{ item.quantity }}</span>
                <small>{{ formatCurrency(item.unitPrice) }} × {{ item.quantity }}</small>
              </div>

              <strong class="product-subtotal">{{ formatCurrency(item.subtotal) }}</strong>
            </article>

            <div class="remark-row">
              <span>訂單備註</span>
              <strong>{{ order.buyerRemark || '無' }}</strong>
            </div>
          </section>

          <aside class="detail-card delivery-card">
            <h2>配送與付款</h2>

            <section class="info-section">
              <h3>收件資訊</h3>
              <p>{{ order.receiverName }}・{{ order.receiverPhone }}</p>
              <p>{{ fullAddress }}</p>
            </section>

            <section class="info-section">
              <h3>付款資訊</h3>
              <p>{{ order.status === 'PENDING_PAYMENT' ? '尚未付款' : statusLabels[order.status] }}</p>
              <p class="muted">付款方式將於付款功能串接後顯示</p>
            </section>

            <dl class="amount-summary">
              <div>
                <dt>商品小計</dt>
                <dd>{{ formatCurrency(order.subtotalAmount) }}</dd>
              </div>
              <div>
                <dt>運費</dt>
                <dd>{{ formatCurrency(order.shippingFee) }}</dd>
              </div>
              <div>
                <dt>折扣</dt>
                <dd>− {{ formatCurrency(order.discountAmount) }}</dd>
              </div>
              <div class="total-row">
                <dt>訂單總額</dt>
                <dd>{{ formatCurrency(order.totalAmount) }}</dd>
              </div>
            </dl>
          </aside>
        </div>
      </template>
    </div>
  </section>
</template>

<style scoped>
.order-detail-page {
  min-height: 620px;
  padding: var(--space-5) 0 var(--space-8);
  background: var(--color-bg);
}

.detail-container {
  max-width: 1440px;
  padding-inline: var(--space-8);
}

.page-header {
  display: flex;
  min-height: 68px;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-5);
}

.page-header h1 {
  margin: 0;
  color: var(--color-text);
  font-size: 26px;
  font-weight: 700;
  line-height: var(--line-height-heading);
}

.page-header p {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
  font-size: 13px;
}

.back-button {
  display: inline-flex;
  min-height: 42px;
  align-items: center;
  justify-content: center;
  padding: 0 var(--space-4);
  color: var(--color-surface);
  font-size: var(--font-size-xs);
  font-weight: 600;
  text-decoration: none;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
}

.back-button:hover {
  color: var(--color-surface);
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.back-button:focus-visible,
.state-card button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.progress-card {
  display: grid;
  min-height: 130px;
  grid-template-columns: repeat(4, 1fr);
  align-items: center;
  gap: 18px;
  margin-top: var(--space-4);
  padding: 22px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.progress-step {
  position: relative;
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: var(--space-2);
  color: var(--color-text-muted);
}

.progress-step strong {
  font-size: 11px;
  font-weight: 600;
}

.step-marker {
  z-index: 1;
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  color: var(--color-text-muted);
  background: var(--color-bg-muted);
  border-radius: var(--radius-pill);
}

.progress-step.complete {
  color: var(--color-text);
}

.progress-step.complete .step-marker {
  color: var(--color-surface);
  background: var(--color-primary);
}

.step-line {
  position: absolute;
  top: 16px;
  left: calc(50% + 26px);
  width: calc(100% - 34px);
  height: 2px;
  background: var(--color-border);
}

.progress-step.complete .step-line {
  background: var(--color-primary-300);
}

.cancelled-notice {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-4);
  padding: var(--space-4);
  color: var(--color-danger);
  background: var(--color-danger-soft);
  border-radius: var(--radius-lg);
}

.cancelled-notice > i {
  font-size: var(--font-size-lg);
}

.cancelled-notice div {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.cancelled-notice span {
  font-size: var(--font-size-xs);
}

.detail-columns {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 20px;
  margin-top: var(--space-4);
}

.detail-card {
  padding: 20px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.detail-card h2,
.detail-card h3 {
  margin: 0;
  color: var(--color-text);
}

.detail-card h2 {
  font-size: var(--font-size-base);
  font-weight: 700;
}

.detail-card h3 {
  font-size: 13px;
  font-weight: 700;
}

.card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-3);
}

.product-row {
  display: grid;
  min-height: 96px;
  grid-template-columns: 82px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: var(--space-3) 0;
  border-top: 1px solid var(--color-border);
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

.product-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}

.product-copy strong {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-copy span,
.product-copy small {
  color: var(--color-text-muted);
  font-size: 11px;
}

.product-subtotal {
  font-size: 13px;
  white-space: nowrap;
}

.remark-row {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  padding-top: var(--space-4);
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  border-top: 1px solid var(--color-border);
}

.remark-row strong {
  color: var(--color-text);
  text-align: right;
}

.status-badge {
  padding: 6px 10px;
  color: var(--color-info);
  font-size: 10px;
  font-weight: 600;
  background: var(--color-info-soft);
  border-radius: var(--radius-sm);
}

.status-pending_payment {
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

.delivery-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);
}

.info-section p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 11px;
  line-height: 1.7;
}

.info-section .muted {
  color: var(--color-text-subtle);
}

.amount-summary {
  display: grid;
  gap: var(--space-2);
  margin: auto 0 0;
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
}

.amount-summary div {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
}

.amount-summary dt,
.amount-summary dd {
  margin: 0;
  font-size: var(--font-size-xs);
}

.amount-summary dt {
  color: var(--color-text-muted);
  font-weight: 400;
}

.amount-summary dd {
  color: var(--color-text);
  font-weight: 600;
}

.amount-summary .total-row {
  margin-top: var(--space-2);
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);
}

.amount-summary .total-row dt,
.amount-summary .total-row dd {
  color: var(--color-primary-active);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.state-card {
  display: flex;
  min-height: 220px;
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

.state-card strong {
  color: var(--color-text);
}

.state-card > i {
  color: var(--color-danger);
  font-size: var(--font-size-xl);
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

@media (max-width: 991.98px) {
  .detail-columns {
    grid-template-columns: 1fr;
  }

  .delivery-card {
    min-height: auto;
  }
}

@media (max-width: 767.98px) {
  .detail-container {
    padding-inline: var(--space-4);
  }

  .page-header {
    align-items: flex-start;
    flex-direction: column;
    padding-bottom: var(--space-3);
  }

  .progress-card {
    grid-template-columns: repeat(2, 1fr);
    row-gap: var(--space-5);
  }

  .step-line {
    display: none;
  }
}

@media (max-width: 479.98px) {
  .back-button {
    width: 100%;
  }

  .product-row {
    grid-template-columns: 64px minmax(0, 1fr);
  }

  .product-image {
    width: 64px;
    height: 64px;
  }

  .product-subtotal {
    grid-column: 2;
  }
}
</style>
