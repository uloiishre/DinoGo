<script setup>
const pageTitle = '購物車'
import axios from 'axios'
import { ref, computed, onMounted } from 'vue'
const cart = ref(null)
const loading = ref(false)
const errorMessage = ref('')
// 取得購物車
const fetchCart = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await axios.get('/api/cart')
    cart.value = response.data
  } catch (error) {
    console.error('取得購物車失敗:', error)
    errorMessage.value = error.response?.data?.message || '無法取得購物車資料，請稍後再試。'
  } finally {
    loading.value = false
  }
}
// 商品小計
const getItemSubtotal = (item) => {
  return Number(item.unitPrice) * Number(item.quantity)
}
// 購物車商品總數
const totalQuantity = computed(() => {
  if (!cart.value?.items) {
    return 0
  }
  return cart.value.items.reduce((total, item) => total + Number(item.quantity), 0)
})
// 商品總金額
const totalAmount = computed(() => {
  if (!cart.value?.items) {
    return 0
  }
  return cart.value.items.reduce((total, item) => total + getItemSubtotal(item), 0)
})
// 修改數量
const updateQuantity = async (item, quantity) => {
  if (quantity < 1) {
    return
  }
  try {
    await axios.put(`/api/cart/items/${item.cartItemId}`, { quantity: quantity })
    item.quantity = quantity
  } catch (error) {
    console.error('修改數量失敗:', error)
    alert(error.response?.data?.message || '修改商品數量失敗')
  }
}
// 增加數量
const increaseQuantity = (item) => {
  updateQuantity(item, Number(item.quantity) + 1)
}
// 減少數量
const decreaseQuantity = (item) => {
  const quantity = Number(item.quantity)
  if (quantity <= 1) {
    return
  }
  updateQuantity(item, quantity - 1)
}
// 刪除購物車商品
const removeItem = async (item) => {
  if (!confirm('確定要移除這個商品嗎？')) {
    return
  }
  try {
    await axios.delete(`/api/cart/items/${item.cartItemId}`)
    cart.value.items = cart.value.items.filter(
      (cartItem) => cartItem.cartItemId !== item.cartItemId,
    )
  } catch (error) {
    console.error('刪除商品失敗:', error)
    alert(error.response?.data?.message || '刪除商品失敗')
  }
}
// 格式化金額
const formatPrice = (price) => {
  return Number(price).toLocaleString('zh-TW')
}
// 前往結帳
const goToCheckout = () => {
  if (!cart.value?.items?.length) {
    return
  }
  window.location.href = '/checkout'
}
// 初始化
onMounted(() => {
  fetchCart()
})
</script>

<template>
  <main class="container py-5">
    <!-- 標題 -->
    <div class="mb-4">
      <h1 class="h3 mb-2">{{ pageTitle }}</h1>
      <p class="text-muted mb-0">共 {{ totalQuantity }} 件商品</p>
    </div>
    <!-- Loading -->
    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border" role="status">
        <span class="visually-hidden"> Loading... </span>
      </div>
      <p class="mt-3 text-muted">正在載入購物車...</p>
    </div>
    <!-- Error -->
    <div v-else-if="errorMessage" class="alert alert-danger">
      {{ errorMessage }}
      <button type="button" class="btn btn-sm btn-outline-danger ms-3" @click="fetchCart">
        重新載入
      </button>
    </div>
    <!-- 空購物車 -->
    <div v-else-if="!cart?.items?.length" class="text-center py-5">
      <div class="fs-1 mb-3">🛒</div>
      <h2 class="h5">購物車目前是空的</h2>
      <p class="text-muted">快去挑選你喜歡的商品吧！</p>
      <RouterLink to="/products" class="btn btn-dark"> 開始購物 </RouterLink>
    </div>
    <!-- 購物車 -->
    <div v-else class="row g-4">
      <!-- 左邊商品列表 -->
      <div class="col-12 col-lg-8">
        <div v-for="item in cart.items" :key="item.cartItemId" class="card border-0 shadow-sm mb-3">
          <div class="card-body">
            <div class="row align-items-center g-3">
              <!-- 商品圖片 -->
              <div class="col-4 col-md-2">
                <img :src="item.productImage" :alt="item.productName" class="cart-image" />
              </div>
              <!-- 商品資訊 -->
              <div class="col-8 col-md-4">
                <h2 class="h6 mb-2">{{ item.productName }}</h2>
                <p class="text-muted small mb-1">SKU：{{ item.skuId }}</p>
                <p class="mb-0 fw-semibold">NT$ {{ formatPrice(item.unitPrice) }}</p>
              </div>
              <!-- 數量 -->
              <div class="col-6 col-md-3">
                <div class="quantity-control">
                  <button
                    type="button"
                    class="quantity-btn"
                    :disabled="item.quantity <= 1"
                    @click="decreaseQuantity(item)"
                  >
                    −
                  </button>
                  <span class="quantity"> {{ item.quantity }} </span>
                  <button type="button" class="quantity-btn" @click="increaseQuantity(item)">
                    +
                  </button>
                </div>
              </div>
              <!-- 小計 -->
              <div class="col-6 col-md-3 text-md-end">
                <div class="fw-bold mb-2">NT$ {{ formatPrice(getItemSubtotal(item)) }}</div>
                <button
                  type="button"
                  class="btn btn-sm btn-outline-danger"
                  @click="removeItem(item)"
                >
                  移除
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <!-- 右邊訂單摘要 -->
      <div class="col-12 col-lg-4">
        <div class="card border-0 shadow-sm">
          <div class="card-body">
            <h2 class="h5 mb-4">訂單摘要</h2>
            <div class="d-flex justify-content-between mb-3">
              <span class="text-muted"> 商品數量 </span> <span> {{ totalQuantity }} 件 </span>
            </div>
            <div class="d-flex justify-content-between mb-3">
              <span class="text-muted"> 商品小計 </span>
              <span> NT$ {{ formatPrice(totalAmount) }} </span>
            </div>
            <hr />
            <div class="d-flex justify-content-between mb-4">
              <span class="fw-bold"> 商品總計 </span>
              <span class="fw-bold fs-5"> NT$ {{ formatPrice(totalAmount) }} </span>
            </div>
            <button type="button" class="btn btn-dark w-100" @click="goToCheckout">前往結帳</button>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>
<style scoped>
.cart-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border-radius: 8px;
  background: #f8f9fa;
}
.quantity-control {
  display: inline-flex;
  align-items: center;
  border: 1px solid #dee2e6;
  border-radius: 6px;
  overflow: hidden;
}
.quantity-btn {
  width: 34px;
  height: 34px;
  border: 0;
  background: white;
  font-size: 18px;
  cursor: pointer;
}
.quantity-btn:hover:not(:disabled) {
  background: #f8f9fa;
}
.quantity-btn:disabled {
  color: #adb5bd;
  cursor: not-allowed;
}
.quantity {
  min-width: 40px;
  text-align: center;
  font-size: 14px;
}
@media (max-width: 767px) {
  .quantity-control {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
