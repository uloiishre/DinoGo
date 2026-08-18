<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getSellerOrder } from '@/api/sellerOrderApi'

const route = useRoute()
const order = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const orderId = computed(() => Number(route.params.id))

const orderStatusLabels = {
  PENDING_PAYMENT: '待付款', PAID: '已付款', PROCESSING: '備貨中',
  SHIPPED: '已出貨', COMPLETED: '已完成', CANCELLED: '已取消',
}
const paymentStatusLabels = {
  PENDING: '待付款', SUCCESS: '付款成功', FAILED: '付款失敗', CANCELLED: '付款已取消',
}
const shipmentStatusLabels = {
  PREPARING: '備貨中', SHIPPED: '已出貨', AVAILABLE_FOR_PICKUP: '可取貨', DELIVERED: '已送達',
}
const onlinePaymentProgressSteps = [
  { label: '訂單成立', statuses: ['PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'COMPLETED'] },
  {
    label: '付款完成',
    statuses: ['PAID', 'PROCESSING', 'SHIPPED', 'COMPLETED'],
    paymentStatus: 'SUCCESS',
  },
  { label: '備貨中', statuses: ['PROCESSING', 'SHIPPED', 'COMPLETED'] },
  { label: '已出貨', statuses: ['SHIPPED', 'COMPLETED'] },
  { label: '已完成', statuses: ['COMPLETED'] },
]
const cashOnDeliveryProgressSteps = [
  { label: '訂單成立', statuses: ['PROCESSING', 'SHIPPED', 'COMPLETED'] },
  { label: '備貨中', statuses: ['PROCESSING', 'SHIPPED', 'COMPLETED'] },
  { label: '已出貨', statuses: ['SHIPPED', 'COMPLETED'] },
  { label: '已送達', statuses: ['COMPLETED'], shipmentStatus: 'DELIVERED' },
  { label: '已完成', statuses: ['COMPLETED'] },
]
const isCashOnDelivery = computed(() =>
  order.value?.payment?.paymentMethodCode === 'CASH_ON_DELIVERY')
const progressSteps = computed(() =>
  isCashOnDelivery.value ? cashOnDeliveryProgressSteps : onlinePaymentProgressSteps)

const fullAddress = computed(() => {
  if (!order.value) return '-'
  return [order.value.shippingPostalCode, order.value.shippingCity,
    order.value.shippingDistrict, order.value.shippingDetailAddress]
    .filter(Boolean).join(' ')
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
    order.value = (await getSellerOrder(orderId.value)).data
  } catch (error) {
    errorMessage.value = error.response?.data?.message ?? '無法載入訂單詳情。'
  } finally {
    loading.value = false
  }
}

const isStepComplete = (step) =>
  order.value &&
  step.statuses.includes(order.value.status) &&
  (!step.paymentStatus || order.value.payment?.status === step.paymentStatus) &&
  (!step.shipmentStatus || order.value.shipment?.status === step.shipmentStatus)
const formatCurrency = (value) => new Intl.NumberFormat('zh-TW', {
  style: 'currency', currency: 'TWD', maximumFractionDigits: 0,
}).format(Number(value ?? 0))
const formatDate = (value) => value ? new Intl.DateTimeFormat('zh-TW', {
  dateStyle: 'medium', timeStyle: 'short',
}).format(new Date(value)) : '-'

onMounted(loadOrder)
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">訂單管理</p>
        <h1>商家訂單詳情</h1>
        <p class="page-description">核對買家、商品、付款與配送資訊。</p>
      </div>
      <RouterLink class="back-button" :to="{ name: 'SellerOrders' }">返回訂單列表</RouterLink>
    </header>

    <div v-if="loading" class="state-card" aria-live="polite">載入訂單中…</div>
    <div v-else-if="errorMessage" class="state-card state-error" role="alert">
      <strong>無法載入訂單</strong><span>{{ errorMessage }}</span>
      <button class="secondary-button" type="button" @click="loadOrder">重新載入</button>
    </div>

    <template v-else-if="order">
      <section class="status-card">
        <div class="status-copy">
          <p class="section-label">訂單 {{ order.orderNo }}</p>
          <strong>{{ orderStatusLabels[order.status] ?? order.status }}</strong>
          <small>{{ formatDate(order.createdAt) }}</small>
        </div>
        <div class="order-progress" aria-label="訂單進度">
          <div v-for="step in progressSteps" :key="step.label" class="progress-item"
            :class="{ completed: isStepComplete(step), cancelled: order.status === 'CANCELLED' }">
            <span class="progress-dot"></span><small>{{ step.label }}</small>
          </div>
        </div>
      </section>

      <div v-if="order.status === 'CANCELLED'" class="cancelled-notice" role="status">
        <strong>此訂單已取消</strong><span>{{ order.cancelReason || '未提供取消原因' }}</span>
      </div>

      <div class="detail-layout">
        <div class="detail-main">
          <section class="detail-card">
            <h2>商品明細</h2>
            <div class="item-list">
              <div v-for="item in order.items" :key="item.orderItemId" class="item-row">
                <div><strong>{{ item.productName }}</strong><p>{{ item.skuSpec || '單一規格' }} × {{ item.quantity }}</p></div>
                <span>{{ formatCurrency(item.subtotal) }}</span>
              </div>
            </div>
            <div class="total-row"><span>商品小計</span><strong>{{ formatCurrency(order.subtotalAmount) }}</strong></div>
            <div class="total-row compact-row"><span>運費</span><strong>{{ formatCurrency(order.shippingFee) }}</strong></div>
            <div v-if="Number(order.discountAmount) > 0" class="total-row discount-row">
              <span>優惠折抵</span><strong>-{{ formatCurrency(order.discountAmount) }}</strong>
            </div>
            <div class="total-row payable-row"><span>訂單總額</span><strong>{{ formatCurrency(order.totalAmount) }}</strong></div>
          </section>

          <section class="detail-card">
            <h2>買家與收件資料</h2>
            <div class="info-grid">
              <div><p class="section-label">收件人</p><strong>{{ order.receiverName }}</strong></div>
              <div><p class="section-label">電話</p><strong>{{ order.receiverPhone }}</strong></div>
              <div class="full-width"><p class="section-label">地址</p><strong>{{ fullAddress }}</strong></div>
              <div class="full-width"><p class="section-label">買家備註</p><strong>{{ order.buyerRemark || '無' }}</strong></div>
            </div>
          </section>

          <section class="detail-card">
            <h2>付款資訊</h2>
            <div v-if="order.payment" class="info-grid">
              <div><p class="section-label">付款方式</p><strong>{{ order.payment.paymentMethodName ?? order.payment.paymentMethodCode }}</strong></div>
              <div><p class="section-label">付款狀態</p><strong>{{ paymentStatusLabels[order.payment.status] ?? order.payment.status }}</strong></div>
              <div v-if="order.payment.paidAt" class="full-width"><p class="section-label">付款時間</p><strong>{{ formatDate(order.payment.paidAt) }}</strong></div>
              <div v-if="order.payment.failureReason" class="full-width failure-message"><p class="section-label">失敗原因</p><strong>{{ order.payment.failureReason }}</strong></div>
            </div>
            <p v-else class="empty-message">尚未建立付款紀錄。</p>
          </section>
        </div>

        <aside class="shipping-card">
          <h2>物流資訊</h2>
          <template v-if="order.shipment">
            <div><p class="section-label">物流狀態</p><strong>{{ shipmentStatusLabels[order.shipment.status] ?? order.shipment.status }}</strong></div>
            <div><p class="section-label">物流商</p><strong>{{ order.shipment.carrierName || '尚未填寫' }}</strong></div>
            <div><p class="section-label">物流單號</p><strong>{{ order.shipment.trackingNo || '尚未填寫' }}</strong></div>
            <div v-if="order.shipment.shippedAt"><p class="section-label">出貨時間</p><strong>{{ formatDate(order.shipment.shippedAt) }}</strong></div>
            <div v-if="order.shipment.availablePickupAt"><p class="section-label">可取貨時間</p><strong>{{ formatDate(order.shipment.availablePickupAt) }}</strong></div>
            <div v-if="order.shipment.deliveredAt"><p class="section-label">送達時間</p><strong>{{ formatDate(order.shipment.deliveredAt) }}</strong></div>
          </template>
          <p v-else class="empty-message">尚未建立物流資料。</p>
        </aside>
      </div>
    </template>
  </section>
</template>

<style scoped>
.seller-page { display: grid; gap: var(--space-5); }
.page-header { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); }
.eyebrow, .page-description, .section-label, .detail-card p, .shipping-card p, .status-card small {
  color: var(--color-text-muted); font-size: var(--font-size-sm);
}
.eyebrow, .page-description, .section-label { margin: 0 0 var(--space-1); }
h1, h2, p { margin-top: 0; }
h1 { margin-bottom: 0; font-family: var(--font-heading); font-size: var(--font-size-xl); }
h2 { margin-bottom: var(--space-4); font-family: var(--font-heading); font-size: var(--font-size-base); }
.back-button { min-height: 40px; display: inline-flex; align-items: center; justify-content: center;
  border: 1px solid var(--color-border-strong); border-radius: var(--radius-md); padding: 0 var(--space-4);
  background: var(--color-surface); color: var(--color-text-700); font-weight: 700; text-decoration: none; }
.state-card, .status-card, .detail-card, .shipping-card, .cancelled-notice {
  border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: var(--color-surface); padding: var(--space-5);
}
.state-card, .cancelled-notice { display: flex; align-items: center; gap: var(--space-3); }
.state-error, .failure-message, .cancelled-notice { color: var(--color-danger); }
.status-card { display: grid; gap: var(--space-5); }
.status-copy { display: grid; gap: var(--space-1); }
.status-copy > strong { color: var(--color-warning); font-size: var(--font-size-xl); }
.order-progress { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); }
.progress-item { position: relative; display: grid; justify-items: center; gap: var(--space-3); color: var(--color-text-muted); text-align: center; }
.progress-item::before, .progress-item::after { position: absolute; top: 5px; width: 50%; height: 2px; content: ''; background: var(--color-border); }
.progress-item::before { left: 0; } .progress-item::after { right: 0; }
.progress-item:first-child::before, .progress-item:last-child::after { display: none; }
.progress-item.completed::before, .progress-item.completed::after, .progress-item.completed .progress-dot { background: var(--color-primary); }
.progress-item.cancelled::before, .progress-item.cancelled::after, .progress-item.cancelled .progress-dot { background: var(--color-border); }
.progress-dot { position: relative; z-index: 1; width: 10px; height: 10px; box-shadow: 0 0 0 6px var(--color-surface); border-radius: 50%; background: var(--color-border-strong); }
.progress-item.completed { color: var(--color-text-700); font-weight: 700; }
.detail-layout { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: var(--space-5); align-items: start; }
.detail-main, .item-list, .shipping-card { display: grid; gap: var(--space-4); }
.item-row, .total-row { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); }
.item-row { min-height: 52px; border-bottom: 1px solid var(--color-border); padding-bottom: var(--space-3); }
.item-row p { margin: var(--space-1) 0 0; }
.total-row { padding-top: var(--space-4); } .compact-row { padding-top: var(--space-2); }
.total-row strong { font-size: var(--font-size-lg); } .discount-row { color: var(--color-success); }
.discount-row strong { font-size: var(--font-size-base); } .payable-row { border-top: 1px solid var(--color-border); }
.info-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-4); }
.full-width { grid-column: 1 / -1; }
.shipping-card { position: sticky; top: var(--space-5); }
.shipping-card h2, .shipping-card p, .empty-message { margin-bottom: 0; }
button { min-height: 42px; border-radius: var(--radius-md); padding: 0 var(--space-4); font-weight: 700; }
.secondary-button { border: 1px solid var(--color-border-strong); background: var(--color-surface); color: var(--color-text-700); }
@media (max-width: 1000px) { .detail-layout { grid-template-columns: 1fr; } .shipping-card { position: static; } }
@media (max-width: 720px) {
  .page-header, .state-card, .cancelled-notice { align-items: flex-start; flex-direction: column; }
  .order-progress, .info-grid { grid-template-columns: 1fr; }
  .progress-item { justify-items: start; text-align: left; }
  .progress-item::before, .progress-item::after { display: none; }
  .full-width { grid-column: auto; }
}
</style>
