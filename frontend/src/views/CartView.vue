<script setup>
const pageTitle = '購物車'
import axios from 'axios'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})
const router = useRouter()
const cart = ref(null)
const loading = ref(false)
const errorMessage = ref('')
// 勾選的商品 cartItemId
const selectedCartItemIds = ref([])
// 取得購物車
const fetchCart = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await api.get('/cart')
    cart.value = response.data
  } catch (error) {
    console.error('取得購物車失敗:', error)
    errorMessage.value = error.response?.data?.message || '無法取得購物車資料，請稍後再試。'
  } finally {
    loading.value = false
  }
}
// 勾選的商品
const selectedItems = computed(() => {
  if (!cart.value?.items) {
    return []
  }
  return cart.value.items.filter((item) => selectedCartItemIds.value.includes(item.cartItemId))
})
// 是否全部勾選
const isAllSelected = computed(() => {
  if (!cart.value?.items?.length) {
    return false
  }
  return selectedCartItemIds.value.length === cart.value.items.length
})
// 全選 / 取消全選
const toggleSelectAll = () => {
  if (!cart.value?.items?.length) {
    return
  }
  if (isAllSelected.value) {
    // 取消全選
    selectedCartItemIds.value = []
  } else {
    // 全選
    selectedCartItemIds.value = cart.value.items.map((item) => item.cartItemId)
  }
}
// 購物車「勾選商品」數量
const selectedTotalQuantity = computed(() => {
  return selectedItems.value.reduce((total, item) => total + Number(item.quantity), 0)
})
// 購物車「勾選商品」總金額
const selectedTotalAmount = computed(() => {
  return selectedItems.value.reduce((total, item) => total + getItemSubtotal(item), 0)
})
// 修改數量
const updateQuantity = async (item, quantity) => {
  if (quantity < 1) {
    return
  }
  try {
    await api.put(`/api/cart/items/${item.cartItemId}`, {
      quantity: quantity,
    })
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
    await api.delete(`/cart/items/${item.cartItemId}`)
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
  // 沒有勾選商品
  if (selectedItems.value.length === 0) {
    alert('請先勾選要結帳的商品')
    return
  }

  // 建立結帳資料
  const checkoutData = {
    cartId: cart.value.cartId,
    cartItemIds: selectedItems.value.map((item) => item.cartItemId),
  }

  // 儲存到 localStorage
  localStorage.setItem('checkoutData', JSON.stringify(checkoutData))

  // Vue Router 前往結帳頁
  router.push('/checkout')
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
      <p class="text-muted mb-0">確認商品、數量與總金額</p>
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
        <!-- 全選 -->
        <div class="card border-0 shadow-sm mb-3">
          <div class="card-body">
            <div class="form-check">
              <input
                id="selectAll"
                class="form-check-input"
                type="checkbox"
                :checked="isAllSelected"
                @change="toggleSelectAll"
              />
              <label for="selectAll" class="form-check-label fw-semibold"> 全選 </label>
              <span class="text-muted ms-2">
                已選
                {{ selectedItems.length }}
                件商品
              </span>
            </div>
          </div>
        </div>
        <div v-for="item in cart.items" :key="item.cartItemId" class="card border-0 shadow-sm mb-3">
          <div class="card-body">
            <div class="row align-items-center g-3">
              <!-- 勾選 -->
              <div class="col-auto">
                <input
                  v-model="selectedCartItemIds"
                  class="form-check-input"
                  type="checkbox"
                  :value="item.cartItemId"
                />
              </div>
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
              <span class="text-muted"> 已選商品數量 </span>
              <span> {{ selectedTotalQuantity }} 件 </span>
            </div>
            <div class="d-flex justify-content-between mb-3">
              <span class="text-muted"> 商品小計 </span>
              <span> NT$ {{ formatPrice(selectedTotalAmount) }} </span>
            </div>
            <hr />
            <div class="d-flex justify-content-between mb-4">
              <span class="fw-bold"> 商品總計 </span>
              <span class="fw-bold fs-5"> NT$ {{ formatPrice(selectedTotalAmount) }} </span>
            </div>
            <!-- 前往結帳 -->
            <button
              type="button"
              class="btn btn-dark w-100"
              :disabled="selectedItems.length === 0"
              @click="goToCheckout"
            >
              {{ selectedItems.length === 0 ? '請先選擇商品' : '前往結帳' }}
            </button>
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
