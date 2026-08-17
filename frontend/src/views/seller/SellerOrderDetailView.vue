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
  PENDING_PAYMENT: '待付款', PAID: '已付款', PROCESSING: '處理中',
  SHIPPED: '已出貨', COMPLETED: '已完成', CANCELLED: '已取消',
}
const paymentStatusLabels = {
  PENDING: '待付款', SUCCESS: '已付款', FAILED: '付款失敗', CANCELLED: '付款已取消',
}
const shipmentStatusLabels = {
  PREPARING: '備貨中', SHIPPED: '已出貨',
  AVAILABLE_FOR_PICKUP: '可取貨', DELIVERED: '已送達',
}

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
    errorMessage.value = '訂單編號格式不正確'
    loading.value = false
    return
  }
  try {
    order.value = (await getSellerOrder(orderId.value)).data
  } catch (error) {
    errorMessage.value = error.response?.data?.message ?? '無法載入訂單詳情'
  } finally {
    loading.value = false
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency', currency: 'TWD', maximumFractionDigits: 0,
  }).format(Number(value ?? 0))
}

function formatDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-TW', {
    dateStyle: 'medium', timeStyle: 'short',
  }).format(new Date(value))
}

onMounted(loadOrder)
</script>

<template>
  <section class="seller-order-detail">
    <header class="page-header">
      <div><p class="eyebrow">訂單管理</p><h1>訂單詳情</h1></div>
      <RouterLink class="btn btn-outline-secondary" :to="{ name: 'SellerOrders' }">
        返回訂單列表
      </RouterLink>
    </header>

    <div v-if="loading" class="state-card">載入訂單中...</div>
    <div v-else-if="errorMessage" class="state-card text-danger" role="alert">
      <p>{{ errorMessage }}</p>
      <button class="btn btn-outline-danger" type="button" @click="loadOrder">重新載入</button>
    </div>

    <template v-else-if="order">
      <section class="detail-card order-heading">
        <div><span class="label">訂單編號</span><h2>{{ order.orderNo }}</h2><small>{{ formatDate(order.createdAt) }}</small></div>
        <span class="status-badge">{{ orderStatusLabels[order.status] ?? order.status }}</span>
      </section>

      <div class="detail-grid">
        <section class="detail-card products-card">
          <h2>商品明細</h2>
          <article v-for="item in order.items" :key="item.orderItemId" class="product-row">
            <div><strong>{{ item.productName }}</strong><p>{{ item.skuSpec || '一般規格' }}</p></div>
            <span>{{ formatCurrency(item.unitPrice) }} × {{ item.quantity }}</span>
            <strong>{{ formatCurrency(item.subtotal) }}</strong>
          </article>
          <div class="total-row"><span>訂單總額</span><strong>{{ formatCurrency(order.totalAmount) }}</strong></div>
        </section>

        <aside class="detail-card summary-card">
          <section>
            <h2>收件資訊</h2>
            <p>{{ order.receiverName }}／{{ order.receiverPhone }}</p>
            <p class="muted">{{ fullAddress }}</p>
          </section>
          <section>
            <h2>付款資訊</h2>
            <template v-if="order.payment">
              <p>{{ paymentStatusLabels[order.payment.status] ?? order.payment.status }}</p>
              <p class="muted">{{ order.payment.paymentMethodName ?? order.payment.paymentMethodCode }}</p>
              <p v-if="order.payment.failureReason" class="text-danger">{{ order.payment.failureReason }}</p>
            </template>
            <p v-else class="muted">尚未建立付款資料</p>
          </section>
          <section>
            <h2>物流資訊</h2>
            <template v-if="order.shipment">
              <p>{{ shipmentStatusLabels[order.shipment.status] ?? order.shipment.status }}</p>
              <p class="muted">{{ order.shipment.carrierName || '物流商待確認' }}</p>
              <p v-if="order.shipment.trackingNo" class="muted">追蹤編號：{{ order.shipment.trackingNo }}</p>
            </template>
            <p v-else class="muted">尚未建立物流資料</p>
          </section>
        </aside>
      </div>
    </template>
  </section>
</template>

<style scoped>
.seller-order-detail { display: grid; gap: var(--space-5); }
.page-header, .order-heading, .product-row, .total-row { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); }
.eyebrow, .label, .muted, small { color: var(--color-text-muted); }
h1, h2, p { margin-top: 0; }
h1, .order-heading h2 { margin-bottom: 0; }
.state-card, .detail-card { padding: var(--space-5); border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: var(--color-surface); }
.detail-grid { display: grid; grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr); gap: var(--space-5); }
.status-badge { padding: var(--space-2) var(--space-3); border-radius: 999px; background: var(--color-primary-100); color: var(--color-primary-700); font-weight: 700; }
.product-row { padding: var(--space-4) 0; border-bottom: 1px solid var(--color-border); }
.product-row p { margin: var(--space-1) 0 0; color: var(--color-text-muted); }
.total-row { padding-top: var(--space-4); font-size: var(--font-size-lg); }
.summary-card { display: grid; gap: var(--space-5); }
.summary-card section + section { padding-top: var(--space-4); border-top: 1px solid var(--color-border); }
.summary-card h2 { font-size: var(--font-size-lg); }
@media (max-width: 768px) { .detail-grid { grid-template-columns: 1fr; } .page-header, .order-heading { align-items: flex-start; flex-direction: column; } }
</style>
