<script setup>
const pageTitle = '結帳'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api/axios'
const router = useRouter()
// 狀態
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
// 結帳商品
const checkoutItems = ref([])
// 收件地址
const addresses = ref([])
const selectedAddressId = ref(null)
// 配送方式
const shippingMethod = ref('HOME_DELIVERY')
// 付款方式
const paymentMethod = ref('CREDIT_CARD')
// 金額
const subtotal = ref(0)
const shippingFee = ref(0)
const discount = ref(0)
const totalAmount = ref(0)
// 目前選擇的地址
const selectedAddress = computed(() => {
  return addresses.value.find((address) => address.addressId === selectedAddressId.value)
})
// 金額格式
const formatPrice = (price) => {
  return Number(price || 0).toLocaleString('zh-TW')
}
// 取得 Token
const getToken = () => {
  return localStorage.getItem('token')
}
// 設定 JWT
api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
// 取得購物車勾選商品
const loadCheckoutItems = () => {
  const data = localStorage.getItem('checkoutData')
  if (!data) {
    router.push({
      name: 'Cart',
    })
    return false
  }
  const checkoutData = JSON.parse(data)
  checkoutItems.value = checkoutData.items || []
  return true
}
// 取得會員地址
const loadAddresses = async () => {
  try {
    const response = await api.get('/addresses')
    addresses.value = response.data
    // 預設選第一個地址
    if (addresses.value.length > 0) {
      selectedAddressId.value = addresses.value[0].addressId
    }
  } catch (error) {
    console.error('取得地址失敗：', error)
    errorMessage.value = '無法取得收件地址'
  }
}
// 建立 Checkout Preview Request
const buildPreviewRequest = () => {
  return {
    items: checkoutItems.value.map((item) => ({
      skuId: item.skuId,
      quantity: Number(item.quantity),
    })),
    addressId: selectedAddressId.value,
    shippingMethod: shippingMethod.value,
    paymentMethod: paymentMethod.value,
  }
}
// 取得結帳金額
const loadCheckoutPreview = async () => {
  if (!selectedAddressId.value) {
    return
  }
  try {
    loading.value = true
    const request = buildPreviewRequest()
    const response = await api.post('/checkout/preview', request)
    const data = response.data
    subtotal.value = Number(data.subtotal || 0)
    shippingFee.value = Number(data.shippingFee || 0)
    discount.value = Number(data.discount || 0)
    totalAmount.value = Number(data.totalAmount || 0)
  } catch (error) {
    console.error('取得結帳金額失敗：', error)
    errorMessage.value = error.response?.data?.message || '無法取得訂單金額'
  } finally {
    loading.value = false
  }
}
// 配送方式改變
const changeShippingMethod = async () => {
  await loadCheckoutPreview()
}
// 付款方式改變
const changePaymentMethod = async () => {
  await loadCheckoutPreview()
}
// 地址改變
const changeAddress = async () => {
  await loadCheckoutPreview()
}
// 建立正式訂單資料
const buildOrderRequest = () => {
  const member = JSON.parse(localStorage.getItem('member') || 'null')

  return {
    buyerId: member?.memberId,
    addressId: selectedAddressId.value,
    shippingFee: shippingFee.value,
    buyerRemark: '',
    items: checkoutItems.value.map((item) => ({
      skuId: item.skuId,
      quantity: Number(item.quantity),
    })),
  }
}
// 送出訂單
const submitOrder = async () => {
  if (!selectedAddressId.value) {
    alert('請選擇收件地址')
    return
  }

  if (checkoutItems.value.length === 0) {
    alert('沒有可結帳的商品')
    return
  }

  try {
    submitting.value = true
    errorMessage.value = ''

    const request = buildOrderRequest()

    console.log('送給 D 組 Order API：', request)

    const response = await api.post('/orders', request)

    console.log('建立訂單成功：', response.data)

    alert('訂單建立成功！')

    localStorage.removeItem('checkoutData')

    router.push({
      name: 'Home',
    })
  } catch (error) {
    console.error('建立訂單失敗：', error)

    errorMessage.value = error.response?.data?.message || '訂單建立失敗，請稍後再試'
  } finally {
    submitting.value = false
  }
}
// 初始化
const init = async () => {
  const hasItems = loadCheckoutItems()
  if (!hasItems) {
    return
  }
  await loadAddresses()
  await loadCheckoutPreview()
}
onMounted(() => {
  init()
})
</script>

<template>
  <!-- <main class="container py-5">
    <h1 class="h3">{{ pageTitle }}</h1>
    <p class="text-muted">此頁為 router placeholder，後續由對應功能分支實作。</p>
  </main> -->
  <main class="checkout-page">
    <div class="container py-5">
      <!-- =================================
           頁面標題
      ================================= -->

      <div class="checkout-header">
        <h1>結帳</h1>

        <p>請確認收件、配送、付款方式與訂單摘要</p>
      </div>

      <div class="checkout-layout">
        <!-- =================================
             左側
        ================================= -->

        <div class="checkout-left">
          <!-- =================================
               收件資料
          ================================= -->

          <section class="checkout-card">
            <h2>收件資料</h2>

            <!-- 沒有地址 -->

            <div v-if="addresses.length === 0" class="empty-address">
              <p>目前沒有收件地址</p>

              <button type="button" class="btn-outline">新增地址</button>
            </div>

            <!-- 地址 -->

            <label
              v-for="address in addresses"
              :key="address.addressId"
              class="option-card"
              :class="{
                selected: selectedAddressId === address.addressId,
              }"
            >
              <input
                v-model="selectedAddressId"
                type="radio"
                name="address"
                :value="address.addressId"
                @change="changeAddress"
              />

              <div class="radio-dot"></div>

              <div class="option-content">
                <strong>
                  {{ address.receiverName }}
                </strong>

                <span>
                  {{ address.detailAddress }}
                </span>

                <span v-if="address.phone" class="option-phone">
                  {{ address.receiverPhone }}
                </span>
              </div>
            </label>
          </section>

          <!-- =================================
               配送方式
          ================================= -->

          <section class="checkout-card">
            <h2>配送方式</h2>

            <label
              class="option-card"
              :class="{
                selected: shippingMethod === 'HOME_DELIVERY',
              }"
            >
              <input
                v-model="shippingMethod"
                type="radio"
                value="HOME_DELIVERY"
                name="shipping"
                @change="changeShippingMethod"
              />

              <div class="radio-dot"></div>

              <div class="option-content">
                <strong> 宅配 </strong>

                <span> 預計 2-3 個工作天 </span>
              </div>
            </label>

            <label
              class="option-card"
              :class="{
                selected: shippingMethod === 'CONVENIENCE_STORE',
              }"
            >
              <input
                v-model="shippingMethod"
                type="radio"
                value="CONVENIENCE_STORE"
                name="shipping"
                @change="changeShippingMethod"
              />

              <div class="radio-dot"></div>

              <div class="option-content">
                <strong> 超商取貨 </strong>

                <span> 取貨時付款 </span>
              </div>
            </label>
          </section>

          <!-- =================================
               付款方式
          ================================= -->

          <section class="checkout-card">
            <h2>付款方式</h2>

            <label
              class="option-card"
              :class="{
                selected: paymentMethod === 'CREDIT_CARD',
              }"
            >
              <input
                v-model="paymentMethod"
                type="radio"
                value="CREDIT_CARD"
                name="payment"
                @change="changePaymentMethod"
              />

              <div class="radio-dot"></div>

              <div class="option-content">
                <strong> 信用卡 </strong>

                <span> Visa •••• 0182 </span>
              </div>
            </label>

            <label
              class="option-card"
              :class="{
                selected: paymentMethod === 'ATM',
              }"
            >
              <input
                v-model="paymentMethod"
                type="radio"
                value="ATM"
                name="payment"
                @change="changePaymentMethod"
              />

              <div class="radio-dot"></div>

              <div class="option-content">
                <strong> ATM 轉帳 </strong>

                <span> 訂單成立後提供轉帳資訊 </span>
              </div>
            </label>

            <label
              class="option-card"
              :class="{
                selected: paymentMethod === 'COD',
              }"
            >
              <input
                v-model="paymentMethod"
                type="radio"
                value="COD"
                name="payment"
                @change="changePaymentMethod"
              />

              <div class="radio-dot"></div>

              <div class="option-content">
                <strong> 貨到付款 </strong>

                <span> 商品送達時付款 </span>
              </div>
            </label>
          </section>
        </div>

        <!-- =================================
             右側：金額摘要
        ================================= -->

        <aside class="summary-card">
          <h2>金額摘要</h2>

          <!-- 商品小計 -->

          <div class="summary-row">
            <span> 商品小計 </span>

            <span>
              NT$
              {{ formatPrice(subtotal) }}
            </span>
          </div>

          <!-- 運費 -->

          <div class="summary-row">
            <span> 運費 </span>

            <span>
              NT$
              {{ formatPrice(shippingFee) }}
            </span>
          </div>

          <!-- 折扣 -->

          <div v-if="discount > 0" class="summary-row discount">
            <span> 折扣 </span>

            <span>
              - NT$
              {{ formatPrice(discount) }}
            </span>
          </div>

          <div class="summary-divider"></div>

          <!-- 總額 -->

          <div class="summary-total">
            <span> 訂單總額 </span>

            <strong>
              NT$
              {{ formatPrice(totalAmount) }}
            </strong>
          </div>

          <!-- Loading -->

          <div v-if="loading" class="summary-loading">正在更新金額...</div>

          <!-- 錯誤 -->

          <div v-if="errorMessage" class="error-message">
            {{ errorMessage }}
          </div>

          <!-- 送出 -->

          <button
            type="button"
            class="submit-button"
            :disabled="submitting || loading || !selectedAddressId || checkoutItems.length === 0"
            @click="submitOrder"
          >
            <span v-if="submitting"> 建立訂單中... </span>

            <span v-else> 送出訂單 </span>
          </button>
        </aside>
      </div>
    </div>
  </main>
</template>
<style scoped>
.checkout-page {
  min-height: 100vh;
  background: #f7f7f5;
  color: #202622;
}

/* ========================================
   Header
======================================== */

.checkout-header {
  margin-bottom: 24px;
}

.checkout-header h1 {
  margin: 0 0 6px;
  font-size: 28px;
  font-weight: 700;
}

.checkout-header p {
  margin: 0;
  color: #8a918c;
  font-size: 13px;
}

/* ========================================
   Layout
======================================== */

.checkout-layout {
  display: grid;
  grid-template-columns:
    minmax(0, 1fr)
    280px;

  gap: 16px;

  align-items: stretch;
}

/* ========================================
   Left
======================================== */

.checkout-left {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* ========================================
   Card
======================================== */

.checkout-card,
.summary-card {
  background: #ffffff;
  border: 1px solid #dedfdb;
  border-radius: 7px;
}

.checkout-card {
  padding: 16px;
}

.checkout-card h2,
.summary-card h2 {
  margin: 0 0 14px;

  font-size: 14px;
  font-weight: 700;
}

/* ========================================
   Option
======================================== */

.option-card {
  position: relative;

  display: flex;
  align-items: center;

  gap: 12px;

  min-height: 55px;

  padding: 10px;

  margin-bottom: 8px;

  border: 1px solid #dedfdb;
  border-radius: 5px;

  cursor: pointer;

  transition:
    background-color 0.15s,
    border-color 0.15s;
}

.option-card:last-child {
  margin-bottom: 0;
}

.option-card:hover {
  border-color: #aab6ac;
}

.option-card.selected {
  background: #c1cdbf;
  border-color: #8ea18f;
}

/* 隱藏原本 radio */

.option-card input {
  position: absolute;

  width: 1px;
  height: 1px;

  opacity: 0;
}

/* 自訂圓點 */

.radio-dot {
  width: 10px;
  height: 10px;

  flex: 0 0 auto;

  border-radius: 50%;

  background: #d6dcd7;

  border: 1px solid #b3beb6;
}

.option-card.selected .radio-dot {
  background: #81968a;
  border-color: #81968a;

  box-shadow: inset 0 0 0 3px #c1cdbf;
}

.option-content {
  display: flex;
  flex-direction: column;

  gap: 3px;
}

.option-content strong {
  font-size: 12px;
  font-weight: 700;
}

.option-content span {
  color: #737a75;
  font-size: 10px;
}

.option-phone {
  font-size: 10px;
}

/* ========================================
   Summary
======================================== */

.summary-card {
  display: flex;
  flex-direction: column;

  min-height: 396px;

  padding: 18px 15px;
}

.summary-row {
  display: flex;

  justify-content: space-between;

  margin-bottom: 16px;

  color: #7a817c;

  font-size: 11px;
}

.summary-row span:last-child {
  color: #303631;
}

.summary-row.discount span:last-child {
  color: #6f8c77;
}

.summary-divider {
  height: 1px;

  margin: 2px 0 14px;

  background: #e1e2de;
}

.summary-total {
  display: flex;

  align-items: center;

  justify-content: space-between;

  margin-bottom: 20px;
}

.summary-total span {
  font-size: 12px;
  font-weight: 700;
}

.summary-total strong {
  color: #66796d;

  font-size: 17px;
}

.summary-loading {
  margin-bottom: 12px;

  color: #89918c;

  font-size: 11px;
}

.error-message {
  padding: 8px;

  margin-bottom: 12px;

  color: #9b4d4d;

  background: #f9eeee;

  border-radius: 4px;

  font-size: 11px;
}

.submit-button {
  width: 100%;

  margin-top: auto;

  padding: 10px;

  border: 0;
  border-radius: 5px;

  color: #ffffff;

  background: #81968a;

  font-size: 12px;

  cursor: pointer;
}

.submit-button:hover:not(:disabled) {
  background: #718579;
}

.submit-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* ========================================
   Empty
======================================== */

.empty-address {
  padding: 20px;

  text-align: center;

  color: #7a817c;

  font-size: 12px;
}

.btn-outline {
  padding: 7px 14px;

  border: 1px solid #aeb6b0;
  border-radius: 4px;

  background: #ffffff;

  cursor: pointer;
}

/* ========================================
   RWD
======================================== */

@media (max-width: 768px) {
  .checkout-layout {
    grid-template-columns: 1fr;
  }

  .summary-card {
    min-height: auto;
  }
}
</style>
