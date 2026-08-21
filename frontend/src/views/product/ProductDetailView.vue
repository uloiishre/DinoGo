<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api/axios'
import { logSafeError } from '@/utils/safeError'
import { getImageUrl } from '@/utils/imageUrl'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const loading = ref(true)
const errorMessage = ref('')

const selectedImage = ref('')
const selectedSpec1 = ref('')
const selectedSpec2 = ref('')
const quantity = ref(1)
const isFavorite = ref(false)
const favoriteLoading = ref(false)
const favoriteMessage = ref('')

/**
 * 取得商品詳情
 */
const fetchProductDetail = async () => {
  try {
    const productId = route.params.id

    const response = await api.get(`/products/${productId}`)

    product.value = response.data

    if (product.value.images?.length) {
      product.value.images.sort((a, b) => (a.sortOrder ?? 999) - (b.sortOrder ?? 999))
    }
    // 預設第一張圖片為主圖
    if (product.value.images?.length) {
      const mainImage =
        product.value.images.find((image) => image.isMain) ?? product.value.images[0]

      selectedImage.value = mainImage.imageUrl
    }

    // 預設第一個 SKU
    if (product.value.skus?.length) {
      const firstSku = product.value.skus[0]

      selectedSpec1.value = firstSku.spec1Value || ''
      selectedSpec2.value = firstSku.spec2Value || ''
    }
  } catch (error) {
    logSafeError('取得商品詳情失敗：', error)
    errorMessage.value = '商品資料載入失敗'
  } finally {
    loading.value = false
  }
}
/**
 * 判斷這個商品是不是雙規格商品
 *
 * 規則：
 * 同一商品所有 SKU 要嘛都有 spec2
 * 要嘛全部沒有 spec2
 */
const hasSpec2 = computed(() => {
  if (!product.value?.skus?.length) {
    return false
  }

  return product.value.skus.some((sku) => sku.spec2Name && sku.spec2Value)
})

/**
 * 規格一名稱
 * 例如：顏色
 */
const spec1Name = computed(() => {
  return product.value?.skus?.[0]?.spec1Name || ''
})

/**
 * 規格一所有選項
 *
 * 例如：
 * 黑色
 * 白色
 * 紅色
 */
const spec1Values = computed(() => {
  if (!product.value?.skus?.length) {
    return []
  }

  return [...new Set(product.value.skus.map((sku) => sku.spec1Value).filter(Boolean))]
})

/**
 * 規格二名稱
 * 例如：尺寸
 */
const spec2Name = computed(() => {
  if (!hasSpec2.value) {
    return ''
  }

  return product.value?.skus?.[0]?.spec2Name || ''
})

/**
 * 根據目前選到的 spec1
 * 找出對應的 spec2 選項
 *
 * 例如目前：
 * selectedSpec1 = 黑色
 *
 * SKU：
 * 黑色 / M
 * 黑色 / L
 * 白色 / M
 *
 * 結果：
 * M
 * L
 */
const spec2Values = computed(() => {
  if (!hasSpec2.value) {
    return []
  }

  return [
    ...new Set(
      product.value.skus
        .filter((sku) => sku.spec1Value === selectedSpec1.value)
        .map((sku) => sku.spec2Value)
        .filter(Boolean),
    ),
  ]
})

/**
 * 目前選中的 SKU
 */
const selectedSku = computed(() => {
  if (!product.value?.skus?.length) {
    return null
  }

  // =========================
  // 雙規格商品
  // =========================
  if (hasSpec2.value) {
    return (
      product.value.skus.find(
        (sku) => sku.spec1Value === selectedSpec1.value && sku.spec2Value === selectedSpec2.value,
      ) || null
    )
  }

  // =========================
  // 單規格商品
  // =========================
  return product.value.skus.find((sku) => sku.spec1Value === selectedSpec1.value) || null
})

/**
 * 點擊規格一
 */
const selectSpec1 = (value) => {
  selectedSpec1.value = value

  // 單規格商品不需要 spec2
  if (!hasSpec2.value) {
    selectedSpec2.value = ''
    return
  }

  // 雙規格商品：
  // 切換 spec1 後，自動選該規格下第一個 spec2
  const firstMatchingSku = product.value.skus.find(
    (sku) => sku.spec1Value === value && sku.spec2Value,
  )

  selectedSpec2.value = firstMatchingSku?.spec2Value || ''
}

/**
 * 點擊規格二
 */
const selectSpec2 = (value) => {
  selectedSpec2.value = value
}

const cartMessage = ref('')
const addingToCart = ref(false)
const buyingNow = ref(false)

const addToCart = async () => {
  cartMessage.value = ''

  if (!selectedSku.value) {
    cartMessage.value = '請先選擇商品規格。'
    return
  }

  if (selectedSku.value.stock <= 0) {
    cartMessage.value = '此商品目前沒有庫存。'
    return
  }

  try {
    addingToCart.value = true

    await api.post('/cart/items', {
      skuId: selectedSku.value.skuId,
      quantity: quantity.value,
    })

    // 只加入購物車，不跳頁
    cartMessage.value = '已加入購物車！'
  } catch (error) {
    console.error('加入購物車失敗：', error)

    if (error.response?.status === 401) {
      cartMessage.value = '請先登入後再加入購物車。'
    } else {
      cartMessage.value = error.response?.data?.message || '加入購物車失敗，請稍後再試。'
    }
  } finally {
    addingToCart.value = false
  }
}

const buyNow = async () => {
  cartMessage.value = ''

  if (!selectedSku.value) {
    cartMessage.value = '請先選擇商品規格。'
    return
  }

  if (selectedSku.value.stock <= 0) {
    cartMessage.value = '此商品目前沒有庫存。'
    return
  }

  try {
    buyingNow.value = true

    // 先加入購物車
    await api.post('/cart/items', {
      skuId: selectedSku.value.skuId,
      quantity: quantity.value,
    })

    // 成功後跳到購物車結帳頁
    router.push('/cart')
  } catch (error) {
    console.error('立即結帳失敗：', error)

    if (error.response?.status === 401) {
      cartMessage.value = '請先登入後再結帳。'
    } else {
      cartMessage.value = error.response?.data?.message || '加入購物車失敗，請稍後再試。'
    }
  } finally {
    buyingNow.value = false
  }
}
const fetchFavoriteStatus = async () => {
  if (!product.value) {
    return
  }

  try {
    const response = await api.get('/favorites')

    isFavorite.value = response.data.some(
      (favorite) => favorite.productId === product.value.productId,
    )
  } catch (error) {
    console.error('取得收藏狀態失敗：', error)
  }
}
const toggleFavorite = async () => {
  if (!product.value || favoriteLoading.value) {
    return
  }

  favoriteMessage.value = ''

  try {
    favoriteLoading.value = true

    if (isFavorite.value) {
      await api.delete(`/favorites/${product.value.productId}`)

      isFavorite.value = false
      favoriteMessage.value = '已取消收藏'
    } else {
      await api.post('/favorites', {
        productId: product.value.productId,
      })

      isFavorite.value = true
      favoriteMessage.value = '已加入收藏'
    }
  } catch (error) {
    console.error('收藏操作失敗：', error)

    if (error.response?.status === 401) {
      favoriteMessage.value = '請先登入後再收藏商品'
    } else {
      favoriteMessage.value = error.response?.data?.message || '收藏操作失敗，請稍後再試'
    }
  } finally {
    favoriteLoading.value = false
  }
}
/**
 * SKU 改變時，數量回到 1
 */
watch(selectedSku, () => {
  quantity.value = 1
})

onMounted(async () => {
  await fetchProductDetail()
  await fetchFavoriteStatus()
})
</script>

<template>
  <main class="product-detail-page">
    <div class="container py-5">
      <!-- Loading -->
      <div v-if="loading" class="text-center py-5">商品載入中...</div>

      <!-- Error -->
      <div v-else-if="errorMessage" class="error-message text-center py-5">
        {{ errorMessage }}
      </div>

      <!-- 商品內容 -->
      <div v-else-if="product">
        <div class="row g-5">
          <!-- =========================
               商品圖片
          ========================== -->
          <div class="col-md-6">
            <!-- 主圖 -->
            <div class="main-image-wrapper">
              <img
                v-if="selectedImage"
                :src="getImageUrl(selectedImage)"
                :alt="product.productName"
                class="product-main-image"
              />

              <div v-else class="product-image-placeholder">暫無圖片</div>
            </div>

            <!-- 縮圖 -->
            <div v-if="product.images?.length > 1" class="thumbnail-list">
              <button
                v-for="image in product.images"
                :key="image.imageId"
                type="button"
                class="thumbnail-button"
                :class="{
                  active: selectedImage === image.imageUrl,
                }"
                @click="selectedImage = image.imageUrl"
              >
                <img
                  :src="getImageUrl(image.imageUrl)"
                  :alt="product.productName"
                  class="thumbnail-image"
                />
              </button>
            </div>
          </div>

          <!-- =========================
               商品資訊
          ========================== -->
          <div class="col-md-6">
            <!-- 分類 -->
            <div class="product-category mb-2">
              {{ product.categoryName }}
              /
              {{ product.subcategoryName }}
            </div>

            <!-- 商品名稱 -->
            <h1 class="product-title">
              {{ product.productName }}
            </h1>

            <!-- 品牌 -->
            <div class="product-brand mb-3">品牌：{{ product.brandName }}</div>

            <!-- 價格 -->
            <div class="product-price mb-4">
              NT$
              {{ selectedSku ? selectedSku.price : product.basePrice }}
            </div>

            <!-- 商品描述 -->
            <div class="product-description mb-4">
              {{ product.description }}
            </div>

            <!-- =========================
                 SKU 規格========================== -->
            <div v-if="product.skus?.length" class="product-specs">
              <!-- =========================
                   規格一
              ========================== -->
              <div v-if="spec1Values.length" class="spec-group">
                <div class="spec-title">
                  {{ spec1Name }}
                </div>

                <div class="spec-options">
                  <button
                    v-for="value in spec1Values"
                    :key="value"
                    type="button"
                    class="spec-button"
                    :class="{
                      active: selectedSpec1 === value,
                    }"
                    @click="selectSpec1(value)"
                  >
                    {{ value }}
                  </button>
                </div>
              </div>

              <!-- =========================
                   規格二
                   只有雙規格商品才顯示
              ========================== -->
              <div v-if="hasSpec2 && spec2Values.length" class="spec-group">
                <div class="spec-title">
                  {{ spec2Name }}
                </div>

                <div class="spec-options">
                  <button
                    v-for="value in spec2Values"
                    :key="value"
                    type="button"
                    class="spec-button"
                    :class="{
                      active: selectedSpec2 === value,
                    }"
                    @click="selectSpec2(value)"
                  >
                    {{ value }}
                  </button>
                </div>
              </div>

              <!-- =========================
                   選中的 SKU
              ========================== -->
              <div v-if="selectedSku" class="selected-sku-info">
                <!-- 庫存 -->
                <div class="sku-stock">庫存：{{ selectedSku.stock }}</div>

                <!-- 數量 -->
                <div class="quantity-area">
                  <span>數量</span>

                  <button type="button" :disabled="quantity <= 1" @click="quantity--">-</button>

                  <span>{{ quantity }}</span>

                  <button
                    type="button"
                    :disabled="selectedSku.stock <= 0 || quantity >= selectedSku.stock"
                    @click="quantity++"
                  >
                    +
                  </button>

                  <span> 剩餘 {{ selectedSku.stock }} 件 </span>
                </div>

                <div class="purchase-actions">
                  <div class="favorite-action">
                    <button
                      type="button"
                      class="favorite-button"
                      :class="{ active: isFavorite }"
                      :disabled="favoriteLoading"
                      @click="toggleFavorite"
                    >
                      {{ favoriteLoading ? '處理中...' : isFavorite ? '♥ 已收藏' : '♡ 收藏商品' }}
                    </button>
                  </div>
                  <!-- 加入購物車 -->
                  <button
                    type="button"
                    class="add-cart-button"
                    :disabled="addingToCart || buyingNow || selectedSku.stock <= 0"
                    @click="addToCart"
                  >
                    {{ addingToCart ? '加入中...' : '加入購物車' }}
                  </button>

                  <!-- 立即結帳 -->
                  <button
                    type="button"
                    class="checkout-button"
                    :disabled="addingToCart || buyingNow || selectedSku.stock <= 0"
                    @click="buyNow"
                  >
                    {{ buyingNow ? '處理中...' : '立即結帳' }}
                  </button>
                </div>

                <div v-if="cartMessage" class="cart-message">
                  {{ cartMessage }}
                </div>
              </div>

              <!-- 找不到 SKU -->
              <div v-else class="sku-unavailable">此規格目前無法選購</div>
            </div>

            <!-- 完全沒有 SKU -->
            <div v-else class="empty-sku-message">此商品目前沒有規格資料</div>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.product-detail-page {
  color: var(--color-text);
  background: var(--color-bg);
}

.product-category {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.product-title {
  color: var(--color-text);
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
  font-weight: 600;
}

.product-brand {
  color: var(--color-text-muted);
}

.product-price {
  color: var(--color-primary);
  font-size: var(--font-size-xl);
  font-weight: 700;
}

.product-description {
  line-height: 1.8;
  white-space: pre-line;
}

.product-specs {
  margin-top: var(--space-5);
}

.spec-group {
  margin-bottom: var(--space-5);
}

.spec-title {
  margin-bottom: var(--space-3);
  font-weight: 600;
}

.spec-options {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.spec-button {
  min-width: 80px;

  padding: var(--space-2) var(--space-4);

  color: var(--color-text);
  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  cursor: pointer;

  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    border-color 0.2s ease;
}

.spec-button:hover {
  color: var(--color-primary);
  background: var(--color-primary-soft);
  border-color: var(--color-primary);
}

.spec-button.active {
  color: var(--color-primary);
  background: var(--color-primary-soft);
  border-color: var(--color-primary);
}

.spec-button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.selected-sku-info {
  margin-top: var(--space-5);
}

.sku-stock {
  margin-top: var(--space-2);
  color: var(--color-text-muted);
}

.sku-unavailable {
  margin-top: var(--space-5);
  color: var(--color-danger);
}

.empty-sku-message {
  color: var(--color-text-muted);
}

.main-image-wrapper {
  width: 100%;
  aspect-ratio: 1 / 1;

  overflow: hidden;

  background: var(--color-surface-soft);
  border-radius: var(--radius-lg);
}

.product-main-image {
  width: 100%;
  height: 100%;

  object-fit: contain;
}

.product-image-placeholder {
  display: flex;

  width: 100%;
  height: 100%;

  align-items: center;
  justify-content: center;

  color: var(--color-text-subtle);
  background: var(--color-surface-soft);
}

.thumbnail-list {
  display: flex;

  gap: var(--space-3);

  margin-top: var(--space-4);

  overflow-x: auto;
}

.thumbnail-button {
  width: 72px;
  height: 72px;

  flex-shrink: 0;

  padding: var(--space-1);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  cursor: pointer;
}

.thumbnail-button:hover {
  border-color: var(--color-primary);
}

.thumbnail-button.active {
  border: 2px solid var(--color-primary);
}

.thumbnail-button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.thumbnail-image {
  width: 100%;
  height: 100%;

  object-fit: contain;

  border-radius: var(--radius-sm);
}

.quantity-area {
  display: flex;

  align-items: center;

  gap: var(--space-3);
  margin-top: var(--space-4);
}

.quantity-area button {
  width: 36px;
  height: 36px;

  color: var(--color-text);
  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  cursor: pointer;
}

.quantity-area button:hover:not(:disabled) {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.quantity-area button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.quantity-area button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.error-message {
  color: var(--color-danger);
}

.cart-action {
  margin-top: var(--space-5);
}

.add-cart-button {
  width: 100%;
  padding: var(--space-3) var(--space-5);

  color: #fff;
  background: var(--color-primary);

  border: none;
  border-radius: var(--radius-md);

  font-weight: 600;
  cursor: pointer;
}

.add-cart-button:hover:not(:disabled) {
  opacity: 0.9;
}

.add-cart-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.cart-message {
  margin-top: var(--space-3);
  color: var(--color-primary);
}

.purchase-actions {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-5);
}

.add-cart-button,
.checkout-button {
  flex: 1;
  padding: var(--space-3) var(--space-5);

  border-radius: var(--radius-md);

  font-weight: 600;
  cursor: pointer;
}

/* 加入購物車 */
.add-cart-button {
  color: #fff;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
}

.add-cart-button:hover:not(:disabled) {
  opacity: 0.9;
}

/* 立即結帳 */
.checkout-button {
  color: var(--color-primary);
  background: var(--color-surface);
  border: 1px solid var(--color-primary);
}

.checkout-button:hover:not(:disabled) {
  background: var(--color-primary-soft);
}

.add-cart-button:disabled,
.checkout-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.cart-message {
  margin-top: var(--space-3);
  color: var(--color-primary);
}
.favorite-action {
  margin-top: var(--space-5);
}

.favorite-button {
  padding: var(--space-2) var(--space-4);

  color: var(--color-text);
  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  cursor: pointer;
}

.favorite-button:hover:not(:disabled) {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.favorite-button.active {
  color: var(--color-primary);
  background: var(--color-primary-soft);
  border-color: var(--color-primary);
}

.favorite-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.favorite-message {
  margin-top: var(--space-3);
  color: var(--color-primary);
}
</style>
