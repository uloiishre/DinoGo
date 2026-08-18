<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api/axios'

const pageTitle = '購物車'
const router = useRouter()

const cart = ref(null)
const loading = ref(false)
const errorMessage = ref('')

// 正在修改規格的 cartItemId
const changingSkuId = ref(null)

// 勾選的商品 cartItemId
const selectedCartItemIds = ref([])

// ================================
// 商品是否可以購買
// ================================

const isItemAvailable = (item) => {
  return item.available !== false
}

// ================================
// 取得購物車
// ================================

const fetchCart = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await api.get('/cart')

    console.log('購物車 API：', response.data)

    cart.value = {
      ...response.data,

      items: (response.data.items || []).map((item) => ({
        ...item,

        // 只有可購買商品才保留勾選
        selected: false,
      })),
    }

    // 重新取得購物車後
    // 只保留「仍存在且可購買」的商品
    selectedCartItemIds.value = selectedCartItemIds.value.filter((id) => {
      const item = cart.value.items.find((item) => item.cartItemId === id)

      return item && isItemAvailable(item)
    })
  } catch (error) {
    console.error('取得購物車失敗:', error)

    errorMessage.value = error.response?.data?.message || '無法取得購物車資料，請稍後再試。'
  } finally {
    loading.value = false
  }
}

// ================================
// 依照 sellerId 分組
// ================================

const sellerGroups = computed(() => {
  if (!cart.value?.items) {
    return []
  }

  const groups = {}

  cart.value.items.forEach((item) => {
    // 不可購買商品不放進正常商品區
    if (!isItemAvailable(item)) {
      return
    }

    const sellerId = Number(item.sellerId)

    if (!groups[sellerId]) {
      groups[sellerId] = {
        sellerId,
        storeName: item.storeName,
        items: [],
      }
    }

    groups[sellerId].items.push(item)
  })

  return Object.values(groups)
})
// ================================
// 已失效商品
// ================================

const unavailableItems = computed(() => {
  if (!cart.value?.items) {
    return []
  }

  return cart.value.items.filter((item) => !isItemAvailable(item))
})
// ================================
// 失效商品是否全部勾選
// ================================

const isAllUnavailableSelected = computed(() => {
  if (!unavailableItems.value.length) {
    return false
  }

  return unavailableItems.value.every((item) => selectedCartItemIds.value.includes(item.cartItemId))
})

// ================================
// 失效商品全選 / 取消全選
// ================================

const toggleUnavailableSelectAll = () => {
  const unavailableIds = unavailableItems.value.map((item) => item.cartItemId)

  if (isAllUnavailableSelected.value) {
    // 取消勾選所有失效商品
    selectedCartItemIds.value = selectedCartItemIds.value.filter(
      (id) => !unavailableIds.includes(id),
    )
  } else {
    // 勾選所有失效商品
    const newIds = unavailableIds.filter((id) => !selectedCartItemIds.value.includes(id))

    selectedCartItemIds.value.push(...newIds)
  }
}

// ================================
// 一次刪除所有失效商品
// ================================

const deleteUnavailableItems = async () => {
  if (!unavailableItems.value.length) {
    return
  }

  const confirmed = confirm(`確定要刪除全部 ${unavailableItems.value.length} 件失效商品嗎？`)

  if (!confirmed) {
    return
  }

  try {
    loading.value = true
    errorMessage.value = ''

    const ids = unavailableItems.value.map((item) => item.cartItemId)

    // 逐筆刪除
    for (const cartItemId of ids) {
      await api.delete(`/cart/items/${cartItemId}`)
    }

    // 重新取得購物車
    await fetchCart()

    // 清除這些商品的勾選
    selectedCartItemIds.value = selectedCartItemIds.value.filter((id) => !ids.includes(id))
  } catch (error) {
    console.error('刪除失效商品失敗:', error)

    // 即使部分刪除成功，也重新取得最新資料
    await fetchCart()

    alert(error.response?.data?.message || '刪除失效商品失敗，已重新整理購物車資料。')
  } finally {
    loading.value = false
  }
}
// ================================
// 已選商品
// ================================

const selectedItems = computed(() => {
  if (!cart.value?.items) {
    return []
  }

  return cart.value.items.filter(
    (item) => selectedCartItemIds.value.includes(item.cartItemId) && isItemAvailable(item),
  )
})

// ================================
// 已選商品數量
// ================================

const selectedTotalQuantity = computed(() => {
  return selectedItems.value.reduce((total, item) => total + Number(item.quantity), 0)
})

// ================================
// 已選商品總金額
// ================================

const selectedTotalAmount = computed(() => {
  return selectedItems.value.reduce((total, item) => total + getItemSubtotal(item), 0)
})

// ================================
// 是否全部可購買商品都勾選
// ================================

const isAllSelected = computed(() => {
  const availableItems = cart.value?.items?.filter(isItemAvailable) || []

  if (!availableItems.length) {
    return false
  }

  return availableItems.every((item) => selectedCartItemIds.value.includes(item.cartItemId))
})

// ================================
// 全選 / 取消全選
// ================================

const toggleSelectAll = () => {
  if (!cart.value?.items?.length) {
    return
  }

  const availableItems = cart.value.items.filter(isItemAvailable)

  if (isAllSelected.value) {
    selectedCartItemIds.value = selectedCartItemIds.value.filter((id) => {
      return !availableItems.some((item) => item.cartItemId === id)
    })
  } else {
    const newIds = availableItems
      .map((item) => item.cartItemId)
      .filter((id) => !selectedCartItemIds.value.includes(id))

    selectedCartItemIds.value.push(...newIds)
  }
}

// ================================
// 某個賣家是否全部可購買商品都勾選
// ================================

const isSellerAllSelected = (group) => {
  const availableItems = group.items.filter(isItemAvailable)

  if (!availableItems.length) {
    return false
  }

  return availableItems.every((item) => selectedCartItemIds.value.includes(item.cartItemId))
}

// ================================
// 某個賣家全選 / 取消全選
// ================================

const toggleSellerSelectAll = (group) => {
  const availableItems = group.items.filter(isItemAvailable)

  if (!availableItems.length) {
    return
  }

  const itemIds = availableItems.map((item) => item.cartItemId)

  const allSelected = isSellerAllSelected(group)

  if (allSelected) {
    selectedCartItemIds.value = selectedCartItemIds.value.filter((id) => !itemIds.includes(id))
  } else {
    const newIds = itemIds.filter((id) => !selectedCartItemIds.value.includes(id))

    selectedCartItemIds.value.push(...newIds)
  }
}

// ================================
// 修改數量
// ================================

const updateQuantity = async (item, quantity) => {
  if (!isItemAvailable(item)) {
    return
  }

  if (quantity < 1) {
    return
  }

  try {
    const response = await api.put(`/cart/items/${item.cartItemId}`, {
      skuId: item.skuId,
      quantity: Number(quantity),
    })

    console.log('修改數量成功：', response.data)

    // 直接更新目前畫面的商品
    Object.assign(item, response.data)
  } catch (error) {
    console.error('修改數量失敗:', error)

    alert(error.response?.data?.message || '修改商品數量失敗')

    // 失敗時才重新取得購物車
    await fetchCart()
  }
}

// ================================
// 修改商品規格 SKU
// ================================

const changeSku = async (item, newSkuId) => {
  if (!isItemAvailable(item)) {
    return
  }

  if (!newSkuId || Number(newSkuId) === Number(item.skuId)) {
    return
  }

  const oldSkuId = item.skuId

  changingSkuId.value = item.cartItemId

  try {
    const response = await api.put(`/cart/items/${item.cartItemId}`, {
      skuId: Number(newSkuId),
      quantity: Number(item.quantity),
    })

    console.log('修改 SKU 成功：', response.data)

    // 直接更新目前商品
    Object.assign(item, response.data)
  } catch (error) {
    console.error('修改商品規格失敗:', error)

    alert(error.response?.data?.message || '修改商品規格失敗')

    // 恢復原本 SKU
    item.skuId = oldSkuId

    // 失敗時才重新取得購物車
    await fetchCart()
  } finally {
    changingSkuId.value = null
  }
}
// ================================
// 增加數量
// ================================

const increaseQuantity = (item) => {
  if (!isItemAvailable(item)) {
    return
  }

  updateQuantity(item, Number(item.quantity) + 1)
}

// ================================
// 減少數量
// ================================

const decreaseQuantity = (item) => {
  if (!isItemAvailable(item)) {
    return
  }

  const quantity = Number(item.quantity)

  if (quantity <= 1) {
    return
  }

  updateQuantity(item, quantity - 1)
}

// ================================
// 刪除商品
// ================================

const removeItem = async (item) => {
  if (!confirm('確定要移除這個商品嗎？')) {
    return
  }

  try {
    await api.delete(`/cart/items/${item.cartItemId}`)

    cart.value.items = cart.value.items.filter(
      (cartItem) => cartItem.cartItemId !== item.cartItemId,
    )

    selectedCartItemIds.value = selectedCartItemIds.value.filter((id) => id !== item.cartItemId)
  } catch (error) {
    console.error('刪除商品失敗:', error)

    alert(error.response?.data?.message || '刪除商品失敗')
  }
}

// ================================
// 刪除選取商品
// ================================

const deleteSelectedItems = async () => {
  if (!selectedCartItemIds.value.length) {
    alert('請先勾選要刪除的商品')

    return
  }

  const confirmed = confirm(`確定要刪除選取的 ${selectedCartItemIds.value.length} 件商品嗎？`)

  if (!confirmed) {
    return
  }

  try {
    loading.value = true
    errorMessage.value = ''

    const ids = [...selectedCartItemIds.value]

    for (const cartItemId of ids) {
      await api.delete(`/cart/items/${cartItemId}`)
    }

    await fetchCart()

    selectedCartItemIds.value = []
  } catch (error) {
    console.error('刪除選取商品失敗:', error)

    await fetchCart()

    selectedCartItemIds.value = []

    alert(error.response?.data?.message || '部分商品刪除失敗，已重新整理購物車資料。')
  } finally {
    loading.value = false
  }
}

// ================================
// 格式化金額
// ================================

const formatPrice = (price) => {
  const number = Number(price)

  if (isNaN(number)) {
    return '0'
  }

  return number.toLocaleString('zh-TW')
}

// ================================
// 商品小計
// ================================

const getItemSubtotal = (item) => {
  return Number(item.price || 0) * Number(item.quantity || 0)
}

// ================================
// 前往結帳
// ================================

const goToCheckout = () => {
  const items = cart.value?.items || []

  // 只允許可購買商品
  const selectedItems = items.filter(
    (item) => selectedCartItemIds.value.includes(item.cartItemId) && isItemAvailable(item),
  )

  if (!selectedItems.length) {
    alert('請先選擇可以購買的商品')

    return
  }

  // ================================
  // 確認庫存
  // ================================

  const unavailableItem = selectedItems.find((item) => !isItemAvailable(item))

  if (unavailableItem) {
    alert(unavailableItem.unavailableReason || '部分商品目前無法購買')

    return
  }

  // ================================
  // 檢查是否包含不同賣家
  // ================================

  const sellerIds = [...new Set(selectedItems.map((item) => Number(item.sellerId)))]

  if (sellerIds.length > 1) {
    alert('不同賣家的商品不能一起結帳，請只選擇同一個賣家的商品。')

    return
  }

  // ================================
  // 相同賣家 → Checkout
  // ================================

  localStorage.setItem(
    'checkoutData',
    JSON.stringify({
      sellerId: sellerIds[0],

      items: selectedItems.map((item) => ({
        cartItemId: item.cartItemId,

        skuId: item.skuId,

        quantity: Number(item.quantity),

        productName: item.productName,

        price: item.price,

        productImage: item.productImage,

        sellerId: Number(item.sellerId),

        storeName: item.storeName,

        skus: item.skus,
      })),
    }),
  )

  router.push('/checkout')
}

// ================================
// 初始化
// ================================

onMounted(() => {
  fetchCart()
})
</script>
<template>
  <main class="cart-page">
    <div class="cart-container">
      <!-- ================================
           Page Header
      ================================= -->

      <header class="cart-header">
        <div>
          <h1 class="cart-title">
            {{ pageTitle }}
          </h1>

          <p class="cart-description">確認商品、數量與總金額</p>
        </div>

        <span v-if="cart?.items?.length" class="cart-count"> {{ cart.items.length }} 件商品 </span>
      </header>

      <!-- ================================
           Loading
      ================================= -->

      <div v-if="loading" class="state-message" role="status">
        <i class="bi bi-arrow-repeat loading-icon"></i>

        <span> 正在載入購物車... </span>
      </div>

      <!-- ================================
           Error
      ================================= -->

      <div v-else-if="errorMessage" class="state-message state-message-error" role="alert">
        <i class="bi bi-exclamation-circle"></i>

        <span>
          {{ errorMessage }}
        </span>

        <button type="button" class="retry-button" @click="fetchCart">重新載入</button>
      </div>

      <!-- ================================
           Empty Cart
      ================================= -->

      <div v-else-if="!cart?.items?.length" class="empty-cart">
        <div class="empty-cart-icon">
          <i class="bi bi-cart3"></i>
        </div>

        <h2>購物車目前是空的</h2>

        <p>快去挑選你喜歡的商品吧！</p>

        <RouterLink to="/products" class="shopping-button"> 開始購物 </RouterLink>
      </div>

      <!-- ================================
           Cart
      ================================= -->

      <div v-else class="cart-layout">
        <!-- ================================
             左側商品列表
        ================================= -->

        <section class="cart-products">
          <div class="select-all-card">
            <label for="selectAll" class="select-all-label">
              <input
                id="selectAll"
                class="cart-checkbox"
                type="checkbox"
                :checked="isAllSelected"
                @change="toggleSelectAll"
              />

              <span>全選</span>
            </label>

            <div class="cart-selection-info">
              <span> 已選 {{ selectedCartItemIds.length }} 件商品 </span>

              <button
                type="button"
                class="delete-selected-button"
                :disabled="selectedCartItemIds.length === 0 || loading"
                @click="deleteSelectedItems"
              >
                刪除選取
              </button>
            </div>
          </div>
          <!-- ================================
       不同賣家分組
  ================================= -->

          <div v-for="group in sellerGroups" :key="group.sellerId" class="seller-group">
            <!-- ================================
         賣家標題
    ================================= -->

            <div class="seller-header">
              <label class="seller-select-label">
                <input
                  class="cart-checkbox"
                  type="checkbox"
                  :checked="isSellerAllSelected(group)"
                  @change="toggleSellerSelectAll(group)"
                />

                <!-- 這裡改成店家名稱 -->
                <span>{{ group.storeName }}</span>
              </label>

              <span class="seller-item-count"> {{ group.items.length }} 件商品 </span>
            </div>

            <!-- ================================
         商品
    ================================= -->

            <article
              v-for="item in group.items"
              :key="item.cartItemId"
              class="cart-item-card"
              :class="{
                'cart-item-unavailable': !isItemAvailable(item),
              }"
            >
              <!-- 勾選 -->

              <div class="item-checkbox">
                <input
                  v-model="selectedCartItemIds"
                  class="cart-checkbox"
                  type="checkbox"
                  :value="item.cartItemId"
                  :disabled="!isItemAvailable(item)"
                />
              </div>

              <!-- 商品圖片 -->

              <div class="item-image-wrapper">
                <img
                  v-if="item.productImage"
                  :src="item.productImage"
                  :alt="item.productName"
                  class="cart-image"
                />

                <div v-else class="cart-image-placeholder">
                  <i class="bi bi-image"></i>
                </div>
              </div>

              <!-- 商品資訊 -->

              <div class="item-info">
                <h2 class="item-name">
                  {{ item.productName }}
                </h2>
                <!-- 商品不可購買提示 -->
                <div v-if="!isItemAvailable(item)" class="item-unavailable-message">
                  <i class="bi bi-exclamation-circle"></i>

                  <span>
                    {{ item.unavailableReason }}
                  </span>
                </div>
                <!-- SKU -->

                <div class="item-sku-select">
                  <label class="sku-label">規格</label>

                  <select
                    :value="item.skuId"
                    :disabled="!isItemAvailable(item) || changingSkuId === item.cartItemId"
                    @change="changeSku(item, Number($event.target.value))"
                  >
                    <option v-for="sku in item.skus" :key="sku.skuId" :value="sku.skuId">
                      {{ sku.skuName }}
                    </option>
                  </select>

                  <span v-if="changingSkuId === item.cartItemId" class="sku-loading">
                    更新中...
                  </span>
                </div>

                <p class="item-price">NT$ {{ formatPrice(item.price) }}</p>
              </div>

              <!-- 數量 -->

              <div class="item-quantity">
                <span class="quantity-label"> 數量 </span>

                <div class="quantity-control">
                  <button
                    type="button"
                    class="quantity-button"
                    :disabled="!isItemAvailable(item) || item.quantity <= 1"
                    @click="decreaseQuantity(item)"
                  >
                    <i class="bi bi-dash"></i>
                  </button>

                  <span class="quantity-value">
                    {{ item.quantity }}
                  </span>

                  <button
                    type="button"
                    class="quantity-button"
                    :disabled="!isItemAvailable(item)"
                    @click="increaseQuantity(item)"
                  >
                    <i class="bi bi-plus"></i>
                  </button>
                </div>
              </div>

              <!-- 小計 -->

              <div class="item-total">
                <span class="item-total-label"> 小計 </span>

                <strong>
                  NT$
                  {{ formatPrice(getItemSubtotal(item)) }}
                </strong>

                <span
                  v-if="!isItemAvailable(item) && item.stock !== null"
                  class="stock-info stock-warning"
                >
                  目前庫存：{{ item.stock }} 件
                </span>

                <button type="button" class="remove-button" @click="removeItem(item)">
                  <i class="bi bi-trash3"></i>

                  <span>移除</span>
                </button>
              </div>
            </article>
          </div>
          <!-- ================================
     已失效商品
================================= -->

          <div v-if="unavailableItems.length > 0" class="unavailable-section">
            <!-- 區塊標題 -->

            <div class="unavailable-header">
              <div class="unavailable-header-left">
                <label class="unavailable-select-label">
                  <span class="unavailable-title">
                    <i class="bi bi-exclamation-triangle"></i>
                    失效商品
                  </span>
                </label>

                <p class="unavailable-description">以下商品目前無法購買，請移除後再進行結帳</p>
              </div>

              <div class="unavailable-header-right">
                <span class="unavailable-count"> {{ unavailableItems.length }} 件 </span>

                <button
                  type="button"
                  class="delete-unavailable-button"
                  :disabled="loading || unavailableItems.length === 0"
                  @click="deleteUnavailableItems"
                >
                  <i class="bi bi-trash3"></i>

                  <span> 一次刪除失效商品 </span>
                </button>
              </div>
            </div>

            <!-- 失效商品 -->

            <article
              v-for="item in unavailableItems"
              :key="item.cartItemId"
              class="cart-item-card cart-item-unavailable"
            >
              <!-- 勾選 -->

              <div class="item-checkbox">
                <input class="cart-checkbox" type="checkbox" disabled />
              </div>

              <!-- 商品圖片 -->

              <div class="item-image-wrapper">
                <img
                  v-if="item.productImage"
                  :src="item.productImage"
                  :alt="item.productName"
                  class="cart-image"
                />

                <div v-else class="cart-image-placeholder">
                  <i class="bi bi-image"></i>
                </div>
              </div>

              <!-- 商品資訊 -->

              <div class="item-info">
                <h2 class="item-name">
                  {{ item.productName }}
                </h2>

                <!-- 失效原因 -->

                <div class="item-unavailable-message">
                  <i class="bi bi-exclamation-circle"></i>

                  <span>
                    {{ item.unavailableReason }}
                  </span>
                </div>

                <!-- SKU -->

                <div class="item-sku-select">
                  <label class="sku-label"> 規格 </label>

                  <select :value="item.skuId" disabled>
                    <option v-for="sku in item.skus" :key="sku.skuId" :value="sku.skuId">
                      {{ sku.skuName }}
                    </option>
                  </select>
                </div>

                <p class="item-price">NT$ {{ formatPrice(item.price) }}</p>
              </div>

              <!-- 數量 -->

              <div class="item-quantity">
                <span class="quantity-label"> 數量 </span>

                <div class="quantity-control">
                  <button type="button" class="quantity-button" disabled>
                    <i class="bi bi-dash"></i>
                  </button>

                  <span class="quantity-value">
                    {{ item.quantity }}
                  </span>

                  <button type="button" class="quantity-button" disabled>
                    <i class="bi bi-plus"></i>
                  </button>
                </div>
              </div>

              <!-- 小計 / 庫存 / 移除 -->

              <div class="item-total">
                <span class="item-total-label"> 小計 </span>

                <strong>
                  NT$
                  {{ formatPrice(getItemSubtotal(item)) }}
                </strong>

                <span
                  v-if="item.stock !== null && item.stock !== undefined"
                  class="stock-info stock-warning"
                >
                  目前庫存：{{ item.stock }} 件
                </span>

                <button type="button" class="remove-button" @click="removeItem(item)">
                  <i class="bi bi-trash3"></i>

                  <span> 移除 </span>
                </button>
              </div>
            </article>
          </div>
        </section>

        <!-- ================================
             右側訂單摘要
        ================================= -->

        <aside class="summary-wrapper">
          <div class="summary-card">
            <h2 class="summary-title">訂單摘要</h2>

            <div class="summary-row">
              <span> 已選商品數量 </span>

              <strong> {{ selectedTotalQuantity }} 件 </strong>
            </div>

            <div class="summary-row">
              <span> 商品小計 </span>

              <strong>
                NT$
                {{ formatPrice(selectedTotalAmount) }}
              </strong>
            </div>

            <div class="summary-divider"></div>

            <div class="summary-total">
              <span> 商品總計 </span>

              <strong>
                NT$
                {{ formatPrice(selectedTotalAmount) }}
              </strong>
            </div>

            <!-- 前往結帳 -->

            <button
              type="button"
              class="checkout-button"
              :disabled="selectedItems.length === 0"
              @click="goToCheckout"
            >
              <i class="bi bi-credit-card"></i>

              <span>
                {{ selectedItems.length === 0 ? '請先選擇商品' : '前往結帳' }}
              </span>
            </button>
          </div>
        </aside>
      </div>
    </div>
  </main>
</template>

<style scoped>
/* ========================================
   Cart Page
======================================== */

.cart-page {
  width: 100%;
  min-height: 600px;

  background: var(--color-bg);
  color: var(--color-text);
}

.cart-container {
  width: 100%;
  max-width: 1440px;

  margin: 0 auto;

  padding: var(--space-7) var(--space-6);

  box-sizing: border-box;
}

/* ========================================
   Header
======================================== */

.cart-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;

  gap: var(--space-5);

  margin-bottom: var(--space-6);
}

.cart-title {
  margin: 0 0 var(--space-2);

  color: var(--color-text);

  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
  font-weight: 700;

  line-height: 1.3;
}

.cart-description {
  margin: 0;

  color: var(--color-text-muted);

  font-family: var(--font-body);
  font-size: var(--font-size-sm);

  line-height: 1.5;
}

.cart-count {
  flex-shrink: 0;

  padding: var(--space-2) var(--space-3);

  color: var(--color-primary);

  background: var(--color-primary-soft);

  border-radius: var(--radius-pill);

  font-family: var(--font-body);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

/* ========================================
   Cart Layout
======================================== */

.cart-layout {
  display: grid;

  grid-template-columns: minmax(0, 2fr) minmax(300px, 1fr);

  gap: var(--space-5);

  align-items: start;
}

/* ========================================
   Products
======================================== */

.cart-products {
  min-width: 0;
}

/* ========================================
   Select All
======================================== */

.select-all-card {
  display: flex;
  align-items: center;
  justify-content: space-between;

  gap: var(--space-3);

  margin-bottom: var(--space-3);

  padding: var(--space-4);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.select-all-label {
  display: inline-flex;
  align-items: center;

  gap: var(--space-2);

  margin: 0;

  color: var(--color-text);

  font-family: var(--font-body);
  font-size: var(--font-size-base);
  font-weight: 600;

  cursor: pointer;
}

.selected-count {
  color: var(--color-text-muted);

  font-family: var(--font-body);
  font-size: var(--font-size-sm);
}

/* ========================================
   Checkbox
======================================== */

.cart-checkbox {
  width: 18px;
  height: 18px;

  margin: 0;

  accent-color: var(--color-primary);

  cursor: pointer;
}

.cart-checkbox:focus-visible {
  outline: none;

  box-shadow: var(--shadow-focus);
}

/* ========================================
   Cart Item
======================================== */

.cart-item-card {
  display: grid;

  grid-template-columns:
    auto
    120px
    minmax(160px, 1fr)
    auto
    auto;

  align-items: center;

  gap: var(--space-4);

  margin-bottom: var(--space-3);

  padding: var(--space-4);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);

  transition:
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.cart-item-card:hover {
  border-color: var(--color-primary);

  box-shadow: var(--shadow-sm);
}

/* ========================================
   Checkbox
======================================== */

.item-checkbox {
  display: flex;

  align-items: center;
  justify-content: center;
}

/* ========================================
   Product Image
======================================== */

.item-image-wrapper {
  width: 120px;
  height: 120px;

  display: flex;

  align-items: center;
  justify-content: center;

  overflow: hidden;

  background: var(--color-surface-soft);

  border-radius: var(--radius-md);
}

.cart-image {
  width: 100%;
  height: 100%;

  display: block;

  object-fit: contain;
}

.cart-image-placeholder {
  width: 100%;
  height: 100%;

  display: flex;

  align-items: center;
  justify-content: center;

  color: var(--color-text-subtle);

  font-size: var(--font-size-xl);
}

/* ========================================
   Product Info
======================================== */

.item-info {
  min-width: 0;
}

.item-name {
  margin: 0 0 var(--space-2);

  color: var(--color-text);

  font-family: var(--font-body);
  font-size: var(--font-size-base);
  font-weight: 600;

  line-height: 1.5;

  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;

  overflow: hidden;
}

.item-sku {
  margin: 0 0 var(--space-2);

  color: var(--color-text-subtle);

  font-family: var(--font-body);
  font-size: var(--font-size-xs);
}

.item-price {
  margin: 0;

  color: var(--color-text);

  font-family: var(--font-body);
  font-size: var(--font-size-base);
  font-weight: 700;
}

/* ========================================
   Quantity
======================================== */

.item-quantity {
  display: flex;

  flex-direction: column;

  align-items: center;

  gap: var(--space-2);
}

.quantity-label {
  color: var(--color-text-subtle);

  font-family: var(--font-body);
  font-size: var(--font-size-xs);
}

.quantity-control {
  display: inline-flex;

  align-items: center;

  overflow: hidden;

  border: 1px solid var(--color-border);

  border-radius: var(--radius-md);
}

.quantity-button {
  width: 36px;
  height: 36px;

  display: inline-flex;

  align-items: center;
  justify-content: center;

  padding: 0;

  color: var(--color-text);

  background: var(--color-surface);

  border: 0;

  font-size: var(--font-size-base);

  cursor: pointer;

  transition:
    color 0.15s ease,
    background-color 0.15s ease;
}

.quantity-button:hover:not(:disabled) {
  color: var(--color-primary);

  background: var(--color-primary-soft);
}

.quantity-button:active:not(:disabled) {
  color: var(--color-primary-active);
}

.quantity-button:focus-visible {
  outline: none;

  box-shadow: var(--shadow-focus);
}

.quantity-button:disabled {
  color: var(--color-text-subtle);

  cursor: not-allowed;
}

.quantity-value {
  min-width: 40px;

  color: var(--color-text);

  text-align: center;

  font-family: var(--font-body);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

/* ========================================
   Item Total
======================================== */

.item-total {
  display: flex;

  flex-direction: column;

  align-items: flex-end;

  gap: var(--space-2);
}

.item-total-label {
  color: var(--color-text-subtle);

  font-family: var(--font-body);
  font-size: var(--font-size-xs);
}

.item-total strong {
  color: var(--color-text);

  font-family: var(--font-body);
  font-size: var(--font-size-md);
  font-weight: 700;

  white-space: nowrap;
}

/* ========================================
   Remove
======================================== */

.remove-button {
  display: inline-flex;

  align-items: center;
  justify-content: center;

  gap: var(--space-1);

  padding: var(--space-1) var(--space-2);

  color: var(--color-danger);

  background: transparent;

  border: 0;
  border-radius: var(--radius-sm);

  font-family: var(--font-body);
  font-size: var(--font-size-xs);

  cursor: pointer;

  transition:
    color 0.15s ease,
    background-color 0.15s ease;
}

.remove-button:hover {
  color: var(--color-danger);

  background: var(--color-surface-soft);
}

.remove-button:focus-visible {
  outline: none;

  box-shadow: var(--shadow-focus);
}

/* ========================================
   Summary
======================================== */

.summary-wrapper {
  position: sticky;

  top: var(--space-5);
}

.summary-card {
  padding: var(--space-5);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);

  box-shadow: var(--shadow-sm);
}

.summary-title {
  margin: 0 0 var(--space-5);

  color: var(--color-text);

  font-family: var(--font-heading);
  font-size: var(--font-size-lg);
  font-weight: 700;
}

.summary-row {
  display: flex;

  align-items: center;
  justify-content: space-between;

  gap: var(--space-3);

  margin-bottom: var(--space-3);

  font-family: var(--font-body);
  font-size: var(--font-size-sm);
}

.summary-row span {
  color: var(--color-text-muted);
}

.summary-row strong {
  color: var(--color-text);
}

.summary-divider {
  height: 1px;

  margin: var(--space-4) 0;

  background: var(--color-border);
}

.summary-total {
  display: flex;

  align-items: center;
  justify-content: space-between;

  gap: var(--space-3);

  margin-bottom: var(--space-5);

  color: var(--color-text);

  font-family: var(--font-body);
  font-size: var(--font-size-base);
  font-weight: 700;
}

.summary-total strong {
  color: var(--color-primary);

  font-size: var(--font-size-lg);
}

/* ========================================
   Checkout Button
======================================== */

.checkout-button {
  width: 100%;
  height: 48px;

  display: inline-flex;

  align-items: center;
  justify-content: center;

  gap: var(--space-2);

  padding: 0 var(--space-4);

  color: var(--color-surface);

  background: var(--color-primary);

  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);

  font-family: var(--font-body);
  font-size: var(--font-size-base);
  font-weight: 600;

  cursor: pointer;

  transition:
    background-color 0.15s ease,
    border-color 0.15s ease;
}

.checkout-button:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.checkout-button:active:not(:disabled) {
  background: var(--color-primary-active);
  border-color: var(--color-primary-active);
}

.checkout-button:focus-visible {
  outline: none;

  box-shadow: var(--shadow-focus);
}

.checkout-button:disabled {
  opacity: 0.55;

  cursor: not-allowed;
}

/* ========================================
   Empty Cart
======================================== */

.empty-cart {
  min-height: 420px;

  display: flex;

  flex-direction: column;

  align-items: center;
  justify-content: center;

  padding: var(--space-7);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);

  text-align: center;
}

.empty-cart-icon {
  margin-bottom: var(--space-4);

  color: var(--color-primary);

  font-size: 48px;
}

.empty-cart h2 {
  margin: 0 0 var(--space-2);

  color: var(--color-text);

  font-family: var(--font-heading);
  font-size: var(--font-size-lg);
  font-weight: 700;
}

.empty-cart p {
  margin: 0 0 var(--space-5);

  color: var(--color-text-muted);

  font-family: var(--font-body);
  font-size: var(--font-size-sm);
}

/* ========================================
   Shopping Button
======================================== */

.shopping-button {
  min-height: 44px;

  display: inline-flex;

  align-items: center;
  justify-content: center;

  padding: 0 var(--space-5);

  color: var(--color-surface);

  background: var(--color-primary);

  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);

  font-family: var(--font-body);
  font-size: var(--font-size-base);
  font-weight: 600;

  text-decoration: none;

  transition:
    background-color 0.15s ease,
    border-color 0.15s ease;
}

.shopping-button:hover {
  color: var(--color-surface);

  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.shopping-button:active {
  background: var(--color-primary-active);
  border-color: var(--color-primary-active);
}

.shopping-button:focus-visible {
  outline: none;

  box-shadow: var(--shadow-focus);
}

/* ========================================
   Loading / Error
======================================== */

.state-message {
  min-height: 240px;

  display: flex;

  align-items: center;
  justify-content: center;

  gap: var(--space-2);

  padding: var(--space-6);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);

  color: var(--color-text-muted);

  font-family: var(--font-body);
  font-size: var(--font-size-base);

  text-align: center;
}

.state-message-error {
  color: var(--color-danger);
}

.retry-button {
  min-height: 36px;

  padding: 0 var(--space-3);

  color: var(--color-danger);

  background: var(--color-surface);

  border: 1px solid var(--color-danger);
  border-radius: var(--radius-md);

  font-family: var(--font-body);
  font-size: var(--font-size-sm);

  cursor: pointer;
}

.retry-button:hover {
  background: var(--color-surface-soft);
}

.retry-button:focus-visible {
  outline: none;

  box-shadow: var(--shadow-focus);
}

.loading-icon {
  animation: loading-spin 1s linear infinite;
}

@keyframes loading-spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

/* ========================================
   1024px
======================================== */

@media (max-width: 1024px) {
  .cart-container {
    padding: var(--space-6) var(--space-5);
  }

  .cart-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .summary-wrapper {
    position: static;
  }

  .cart-item-card {
    grid-template-columns:
      auto
      100px
      minmax(150px, 1fr)
      auto;
  }

  .item-total {
    grid-column: 3 / -1;

    flex-direction: row;

    align-items: center;

    justify-content: flex-end;
  }
}

/* ========================================
   768px
======================================== */

@media (max-width: 768px) {
  .cart-container {
    padding: var(--space-5) var(--space-4);
  }

  .cart-header {
    align-items: flex-start;

    flex-direction: column;

    margin-bottom: var(--space-5);
  }

  .cart-item-card {
    grid-template-columns:
      auto
      96px
      minmax(0, 1fr);

    gap: var(--space-3);
  }

  .item-image-wrapper {
    width: 96px;
    height: 96px;
  }

  .item-quantity {
    grid-column: 2 / -1;

    align-items: flex-start;

    flex-direction: row;
  }

  .item-total {
    grid-column: 2 / -1;

    flex-direction: row;

    align-items: center;

    justify-content: space-between;
  }

  .summary-card {
    padding: var(--space-4);
  }
}

/* ========================================
   480px
======================================== */

@media (max-width: 480px) {
  .cart-container {
    padding: var(--space-4) var(--space-3);
  }

  .cart-title {
    font-size: var(--font-size-lg);
  }

  .cart-layout {
    gap: var(--space-4);
  }

  .select-all-card {
    padding: var(--space-3);
  }

  .cart-item-card {
    grid-template-columns:
      auto
      80px
      minmax(0, 1fr);

    padding: var(--space-3);
  }

  .item-image-wrapper {
    width: 80px;
    height: 80px;
  }

  .item-name {
    font-size: var(--font-size-sm);
  }

  .item-price {
    font-size: var(--font-size-sm);
  }

  .item-quantity {
    grid-column: 2 / -1;
  }

  .item-total {
    grid-column: 2 / -1;

    flex-wrap: wrap;

    justify-content: space-between;
  }

  .summary-total {
    margin-bottom: var(--space-4);
  }

  .empty-cart {
    min-height: 360px;

    padding: var(--space-5);
  }
}
/* ========================================
   SKU Select
======================================== */

.item-sku-select {
  display: flex;

  flex-direction: column;

  align-items: flex-start;

  gap: var(--space-1);

  margin-bottom: var(--space-2);
}

.sku-label {
  color: var(--color-text-subtle);

  font-family: var(--font-body);
  font-size: var(--font-size-xs);
}

.item-sku-select select {
  min-width: 150px;

  height: 36px;

  padding: 0 var(--space-3);

  color: var(--color-text);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  font-family: var(--font-body);
  font-size: var(--font-size-sm);

  cursor: pointer;
}

.item-sku-select select:hover:not(:disabled) {
  border-color: var(--color-primary);
}

.item-sku-select select:focus {
  outline: none;

  border-color: var(--color-primary);

  box-shadow: var(--shadow-focus);
}

.item-sku-select select:disabled {
  opacity: 0.6;

  cursor: not-allowed;
}

.sku-loading {
  color: var(--color-text-subtle);

  font-family: var(--font-body);
  font-size: var(--font-size-xs);
}
.cart-selection-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.delete-selected-button {
  padding: 3px 8px;

  border: 1px solid #c9cfca;
  border-radius: 4px;

  background: #ffffff;
  color: #737a75;

  font-size: 10px;

  cursor: pointer;
}

.delete-selected-button:hover:not(:disabled) {
  background: #f5f6f4;
  color: #9b4d4d;
}

.delete-selected-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
/* ========================================
   Seller Group
======================================== */

.seller-group {
  margin-bottom: var(--space-5);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);

  overflow: hidden;
}

/* ========================================
   Seller Header
======================================== */

.seller-header {
  display: flex;

  align-items: center;
  justify-content: space-between;

  gap: var(--space-3);

  padding: var(--space-4);

  background: var(--color-surface-soft);

  border-bottom: 1px solid var(--color-border);
}

.seller-select-label {
  display: inline-flex;

  align-items: center;

  gap: var(--space-2);

  margin: 0;

  color: var(--color-text);

  font-family: var(--font-body);

  font-size: var(--font-size-base);

  font-weight: 600;

  cursor: pointer;
}

.seller-item-count {
  color: var(--color-text-muted);

  font-family: var(--font-body);

  font-size: var(--font-size-sm);
}

/* ========================================
   Seller Group Item
======================================== */

.seller-group .cart-item-card {
  margin-bottom: 0;

  border: 0;

  border-radius: 0;

  border-bottom: 1px solid var(--color-border);
}

.seller-group .cart-item-card:last-child {
  border-bottom: 0;
}

.seller-group .cart-item-card:hover {
  border-color: var(--color-border);

  box-shadow: none;

  background: var(--color-surface-soft);
}

/* ========================================
   Selection Bar
======================================== */

.cart-selection-bar {
  display: flex;

  align-items: center;
  justify-content: space-between;

  gap: var(--space-3);

  margin-bottom: var(--space-3);

  padding: var(--space-3) var(--space-4);

  background: var(--color-surface);

  border: 1px solid var(--color-border);

  border-radius: var(--radius-lg);

  color: var(--color-text-muted);

  font-family: var(--font-body);

  font-size: var(--font-size-sm);
}
/* ========================================
   商品不可購買
======================================== */

.cart-item-unavailable {
  opacity: 0.55;

  background: #eeeeee;

  border-color: #d5d5d5;

  filter: grayscale(100%);
}

/* 不可購買商品 hover 不變色 */
.seller-group .cart-item-card.cart-item-unavailable:hover {
  background: #eeeeee;

  border-color: #d5d5d5;

  box-shadow: none;
}

/* 不可購買商品圖片 */
.cart-item-unavailable .cart-image {
  opacity: 0.6;
}

/* 不可購買提示 */
.item-unavailable-message {
  display: flex;

  align-items: center;

  gap: 6px;

  margin-bottom: var(--space-2);

  padding: 6px 8px;

  color: #777777;

  background: #e5e5e5;

  border-radius: var(--radius-sm);

  font-family: var(--font-body);

  font-size: var(--font-size-xs);

  font-weight: 600;
}

.item-unavailable-message i {
  font-size: 14px;
}

/* 庫存提示 */
.stock-info {
  color: var(--color-text-subtle);

  font-family: var(--font-body);

  font-size: var(--font-size-xs);
}

.stock-warning {
  color: #777777;

  font-weight: 600;
}
/* ========================================
   已失效商品區塊
======================================== */

.unavailable-section {
  margin-top: var(--space-6);

  background: var(--color-surface);

  border: 1px solid #d8d8d8;

  border-radius: var(--radius-lg);

  overflow: hidden;
}

/* ========================================
   已失效商品標題
======================================== */

.unavailable-header {
  display: flex;

  align-items: center;
  justify-content: space-between;

  gap: var(--space-3);

  padding: var(--space-4);

  background: #eeeeee;

  border-bottom: 1px solid #d8d8d8;
}

.unavailable-title-wrapper {
  display: flex;

  align-items: center;

  gap: var(--space-3);
}

.unavailable-title-wrapper > i {
  color: #777777;

  font-size: 20px;
}

.unavailable-title {
  margin: 0;

  color: #666666;

  font-family: var(--font-heading);

  font-size: var(--font-size-base);

  font-weight: 700;
}

.unavailable-description {
  margin: 2px 0 0;

  color: #999999;

  font-family: var(--font-body);

  font-size: var(--font-size-xs);
}

.unavailable-count {
  flex-shrink: 0;

  padding: 4px 10px;

  color: #777777;

  background: #dddddd;

  border-radius: var(--radius-pill);

  font-family: var(--font-body);

  font-size: var(--font-size-xs);

  font-weight: 600;
}
/* ========================================
   Unavailable Header Layout
======================================== */

.unavailable-header-left {
  display: flex;

  flex-direction: column;

  align-items: flex-start;

  gap: var(--space-1);
}

.unavailable-select-label {
  display: inline-flex;

  align-items: center;

  gap: var(--space-2);

  cursor: pointer;
}

.unavailable-select-label .unavailable-title {
  margin: 0;
}

/* ========================================
   Unavailable Header Right
======================================== */

.unavailable-header-right {
  display: flex;

  align-items: center;

  gap: var(--space-3);
}

/* ========================================
   Delete Unavailable Button
======================================== */

.delete-unavailable-button {
  display: inline-flex;

  align-items: center;
  justify-content: center;

  gap: var(--space-1);

  min-height: 34px;

  padding: 0 var(--space-3);

  color: var(--color-danger);

  background: #ffffff;

  border: 1px solid var(--color-danger);

  border-radius: var(--radius-md);

  font-family: var(--font-body);

  font-size: var(--font-size-xs);

  font-weight: 600;

  cursor: pointer;

  transition:
    color 0.15s ease,
    background-color 0.15s ease,
    opacity 0.15s ease;
}

.delete-unavailable-button:hover:not(:disabled) {
  color: #ffffff;

  background: var(--color-danger);
}

.delete-unavailable-button:focus-visible {
  outline: none;

  box-shadow: var(--shadow-focus);
}

.delete-unavailable-button:disabled {
  opacity: 0.5;

  cursor: not-allowed;
}
@media (max-width: 768px) {
  .unavailable-header {
    align-items: flex-start;

    flex-direction: column;
  }

  .unavailable-header-right {
    width: 100%;

    justify-content: space-between;
  }
}

@media (max-width: 480px) {
  .delete-unavailable-button {
    padding: 0 var(--space-2);

    font-size: 11px;
  }
}
/* ========================================
   失效商品卡片
======================================== */

.unavailable-section .cart-item-card {
  margin-bottom: 0;

  border: 0;

  border-bottom: 1px solid #dddddd;

  border-radius: 0;

  opacity: 0.55;

  background: #eeeeee;

  filter: grayscale(100%);
}

.unavailable-section .cart-item-card:last-child {
  border-bottom: 0;
}

.unavailable-section .cart-item-card:hover {
  border-color: #dddddd;

  background: #eeeeee;

  box-shadow: none;
}

/* ========================================
   失效商品提示
======================================== */

.item-unavailable-message {
  display: inline-flex;

  align-items: center;

  gap: 6px;

  margin-bottom: var(--space-2);

  padding: 5px 8px;

  color: #777777;

  background: #e1e1e1;

  border-radius: var(--radius-sm);

  font-family: var(--font-body);

  font-size: var(--font-size-xs);

  font-weight: 600;
}

.item-unavailable-message i {
  font-size: 13px;
}

/* ========================================
   庫存警告
======================================== */

.stock-warning {
  color: #777777;

  font-family: var(--font-body);

  font-size: var(--font-size-xs);

  font-weight: 600;
}
</style>
