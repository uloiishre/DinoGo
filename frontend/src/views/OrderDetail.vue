 ㄎ<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getOrder } from '@/api/order'

const route = useRoute()
const order = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const orderId = computed(() => Number(route.params.orderId))

const statusLabels = {
  PENDING_PAYMENT: '待付款', PAID: '已付款', PROCESSING: '處理中',
  SHIPPED: '已出貨', COMPLETED: '已完成', CANCELLED: '已取消',
}

const fullAddress = computed(() => {
  if (!order.value) return '—'
  return [order.value.shippingPostalCode, order.value.shippingCity,
    order.value.shippingDistrict, order.value.shippingDetailAddress]
    .filter(Boolean).join(' ')
})

async function loadOrder() {
  loading.value = true
  errorMessage.value = ''
  if (!Number.isInteger(orderId.value) || orderId.value <= 0) {
    errorMessage.value = '訂單編號無效。'
    loading.value = false
    return
  }
  try {
    order.value = (await getOrder(orderId.value)).data
  } catch (error) {
    errorMessage.value = error.response?.data?.message ?? '目前無法取得訂單詳情，請稍後再試。'
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
  if (!value) return '—'
  return new Intl.DateTimeFormat('zh-TW', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hour12: false,
  }).format(new Date(value))
}

onMounted(loadOrder)
</script>

<template>
  <div class="detail-page">
    <header class="simple-header">
      <RouterLink class="brand" to="/"><span>D</span><strong>DINO-GO 都能購</strong></RouterLink>
      <RouterLink class="back-link" to="/orders">← 返回我的訂單</RouterLink>
    </header>

    <main class="detail-wrap">
      <div class="title-row">
        <div><p>會員中心／我的訂單</p><h1>訂單詳情</h1></div>
        <span v-if="order" class="status-pill">{{ statusLabels[order.status] ?? order.status }}</span>
      </div>

      <section v-if="loading" class="state-card">正在載入訂單詳情…</section>
      <section v-else-if="errorMessage" class="state-card" role="alert">
        <strong>訂單載入失敗</strong><p>{{ errorMessage }}</p>
        <button type="button" @click="loadOrder">再試一次</button>
      </section>

      <template v-else-if="order">
        <section class="summary-card">
          <div><small>訂單編號</small><strong>{{ order.orderNo }}</strong></div>
          <div><small>成立時間</small><strong>{{ formatDate(order.createdAt) }}</strong></div>
          <div><small>賣家</small><strong>{{ order.sellerName ?? `賣家 #${order.sellerId}` }}</strong></div>
        </section>

        <div class="content-grid">
          <section class="panel">
            <h2>商品明細</h2>
            <article v-for="item in order.items" :key="item.orderItemId" class="product-row">
              <div class="product-image"><img v-if="item.productImageUrl" :src="item.productImageUrl" :alt="item.productName"></div>
              <div class="product-copy">
                <strong>{{ item.productName }}</strong><small>{{ item.skuSpec || '一般規格' }}</small>
                <span>{{ formatCurrency(item.unitPrice) }} × {{ item.quantity }}</span>
              </div>
              <strong>{{ formatCurrency(item.subtotal) }}</strong>
            </article>
          </section>

          <aside class="panel amount-panel">
            <h2>金額明細</h2>
            <div><span>商品小計</span><strong>{{ formatCurrency(order.subtotalAmount) }}</strong></div>
            <div><span>運費</span><strong>{{ formatCurrency(order.shippingFee) }}</strong></div>
            <div><span>優惠折抵</span><strong>− {{ formatCurrency(order.discountAmount) }}</strong></div>
            <div class="total"><span>訂單總額</span><strong>{{ formatCurrency(order.totalAmount) }}</strong></div>
          </aside>
        </div>

        <section class="panel receiver-panel">
          <h2>收件資訊</h2>
          <dl>
            <div><dt>收件人</dt><dd>{{ order.receiverName }}</dd></div>
            <div><dt>聯絡電話</dt><dd>{{ order.receiverPhone }}</dd></div>
            <div><dt>配送地址</dt><dd>{{ fullAddress }}</dd></div>
            <div><dt>訂單備註</dt><dd>{{ order.buyerRemark || '無' }}</dd></div>
          </dl>
        </section>

        <div class="page-actions">
          <RouterLink to="/orders">返回訂單列表</RouterLink>
          <button v-if="order.status === 'PENDING_PAYMENT'" type="button">立即付款</button>
          <button v-else-if="order.status === 'SHIPPED'" type="button">查看物流</button>
        </div>
      </template>
    </main>
  </div>
</template>

<style scoped>
:global(*) { box-sizing: border-box; }
:global(body) { margin: 0; color: #141a17; background: #fbf8f0; font-family: Inter, "Noto Sans TC", system-ui, sans-serif; }
.detail-page { min-height: 100vh; background: #fbf8f0; }
.simple-header { display: flex; min-height: 88px; align-items: center; justify-content: space-between; padding: 0 max(24px, calc((100% - 1268px) / 2)); border-top: 34px solid #14572e; border-bottom: 1px solid #c7ccc2; background: white; }
.brand { display: flex; align-items: center; gap: 12px; color: #14572e; text-decoration: none; }
.brand span { display: grid; width: 46px; height: 46px; place-items: center; border: 1px solid #14572e; border-radius: 50%; background: #def0db; font-weight: 800; }
.brand strong { font-size: 24px; }.back-link { color: #14572e; font-weight: 700; text-decoration: none; }
.detail-wrap { width: min(1010px, calc(100% - 48px)); margin: auto; padding: 38px 0 80px; }
.title-row { display: flex; align-items: end; justify-content: space-between; margin-bottom: 22px; }
.title-row p { margin: 0 0 8px; color: #6b756e; font-size: 13px; }
h1 { margin: 0; color: #14572e; font-size: 30px; } h2 { margin: 0 0 20px; color: #14572e; font-size: 18px; }
.status-pill { padding: 9px 18px; border-radius: 18px; color: #14572e; background: #def0db; font-weight: 800; }
.summary-card, .panel, .state-card { border: 1px solid #c7ccc2; border-radius: 10px; background: white; }
.summary-card { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; padding: 22px 26px; }
.summary-card div, .product-copy { display: grid; gap: 7px; }.summary-card small, .product-copy small, .product-copy span { color: #6b756e; }
.content-grid { display: grid; grid-template-columns: 2fr minmax(270px, 1fr); gap: 20px; margin-top: 20px; }.panel { padding: 24px 26px; }
.product-row { display: grid; grid-template-columns: 72px 1fr auto; align-items: center; gap: 18px; padding: 16px 0; border-top: 1px solid #e3e6df; }
.product-image { width: 72px; height: 72px; overflow: hidden; border: 1px solid #c7ccc2; border-radius: 7px; background: #e8ebe3; }.product-image img { width: 100%; height: 100%; object-fit: cover; }
.amount-panel > div { display: flex; justify-content: space-between; padding: 10px 0; }.amount-panel .total { margin-top: 8px; padding-top: 18px; border-top: 1px solid #c7ccc2; color: #14572e; }
.receiver-panel { margin-top: 20px; }.receiver-panel dl { display: grid; grid-template-columns: repeat(2, 1fr); gap: 18px 36px; }.receiver-panel dl div { display: grid; grid-template-columns: 80px 1fr; }.receiver-panel dt { color: #6b756e; }.receiver-panel dd { margin: 0; }
.state-card { display: grid; min-height: 220px; place-content: center; justify-items: center; padding: 30px; }.state-card button, .page-actions button { border: 1px solid #14572e; border-radius: 7px; padding: 11px 24px; color: white; background: #14572e; font-weight: 800; }
.page-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px; }.page-actions a { border: 1px solid #14572e; border-radius: 7px; padding: 11px 24px; color: #14572e; background: white; font-weight: 800; text-decoration: none; }
@media (max-width: 760px) { .brand strong { font-size: 18px; }.detail-wrap { width: calc(100% - 24px); }.summary-card, .content-grid, .receiver-panel dl { grid-template-columns: 1fr; }.product-row { grid-template-columns: 60px 1fr; }.product-image { width: 60px; height: 60px; }.product-row > strong { grid-column: 2; } }
</style>
