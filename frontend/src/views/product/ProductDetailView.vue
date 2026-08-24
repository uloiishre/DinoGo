<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api/axios'
import { logSafeError } from '@/utils/safeError'
import { getImageUrl } from '@/utils/imageUrl'
//review-start，總共10次修改，第1次//
import { getProductReviews } from '@/api/review'
//review-end，總共10次修改，第1次//

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

//review-start，總共10次修改，第2次//
const activeDetailTab = ref('description')
const reviews = ref([])
const reviewsLoading = ref(false)
const reviewsLoaded = ref(false)
const reviewsError = ref('')
const reviewsHasNext = ref(false)
const reviewsNextCursor = ref(null)
const selectedReview = ref(null)
//review-end，總共10次修改，第2次//

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

  // 未登入就不查收藏
  const token = localStorage.getItem('token')

  if (!token) {
    isFavorite.value = false
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

//review-start，總共10次修改，第3次//
function maskMemberId(memberId) {
  const value = String(memberId ?? '')
  if (!value) return '會員 ****'
  if (value.length === 1) return `會員 ${value}****`
  return `會員 ${value.slice(0, 1)}****${value.slice(-1)}`
}

function reviewImages(review) {
  return [review?.imgOne, review?.imgTwo, review?.imgThree]
    .filter(Boolean)
    .map((value) => `data:image/*;base64,${value}`)
}

async function loadReviews({ append = false } = {}) {
  if (!product.value || reviewsLoading.value || (append && !reviewsHasNext.value)) return
  reviewsLoading.value = true
  reviewsError.value = ''
  try {
    const response = await getProductReviews(
      product.value.productId,
      append ? reviewsNextCursor.value : null,
    )
    const page = response.data ?? {}
    const content = Array.isArray(page.content) ? page.content : []
    reviews.value = append ? [...reviews.value, ...content] : content
    reviewsHasNext.value = Boolean(page.hasNext)
    reviewsNextCursor.value = page.nextCursor ?? null
    reviewsLoaded.value = true
  } catch (error) {
    reviewsError.value = error.response?.data?.message ?? '用戶評價載入失敗，請稍後再試。'
  } finally {
    reviewsLoading.value = false
  }
}

async function selectDetailTab(tab) {
  activeDetailTab.value = tab
  if (tab === 'reviews' && !reviewsLoaded.value) await loadReviews()
}

function handleReviewScroll(event) {
  const target = event.currentTarget
  const nearBottom = target.scrollTop + target.clientHeight >= target.scrollHeight - 24
  if (nearBottom) void loadReviews({ append: true })
}

function openReview(review) {
  selectedReview.value = review
  document.body.style.overflow = 'hidden'
}

function closeReview() {
  selectedReview.value = null
  document.body.style.overflow = ''
}

function handleReviewEscape(event) {
  if (event.key === 'Escape' && selectedReview.value) closeReview()
}
//review-end，總共10次修改，第3次//
/**
 * SKU 改變時，數量回到 1
 */
watch(selectedSku, () => {
  quantity.value = 1
})

onMounted(async () => {
  await fetchProductDetail()
  await fetchFavoriteStatus()
  //review-start，總共10次修改，第4次//
  window.addEventListener('keydown', handleReviewEscape)
  //review-end，總共10次修改，第4次//
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleReviewEscape)
  document.body.style.overflow = ''
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

            <!-- //review-start，總共10次修改，第5次// -->
            <!-- 商品說明移至主圖下方的可切換明細面板，避免同一內容重複顯示。 -->
            <!-- //review-end，總共10次修改，第5次// -->

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

        <!-- //review-start，總共10次修改，第6次// -->
        <section class="product-detail-tabs" aria-label="產品明細內容">
          <div class="detail-tab-list" role="tablist" aria-label="產品資訊篩選">
            <button
              type="button"
              role="tab"
              :aria-selected="activeDetailTab === 'description'"
              :class="{ active: activeDetailTab === 'description' }"
              @click="selectDetailTab('description')"
            >
              產品說明
            </button>
            <button
              type="button"
              role="tab"
              :aria-selected="activeDetailTab === 'reviews'"
              :class="{ active: activeDetailTab === 'reviews' }"
              @click="selectDetailTab('reviews')"
            >
              用戶評價
            </button>
          </div>

          <div v-if="activeDetailTab === 'description'" class="detail-panel description-panel" role="tabpanel">
            <h2>產品說明</h2>
            <p>{{ product.description || '目前尚無產品說明。' }}</p>
          </div>

          <div
            v-else
            class="detail-panel reviews-panel"
            role="tabpanel"
            tabindex="0"
            aria-label="用戶評價，可向下捲動載入更多"
            @scroll.passive="handleReviewScroll"
          >
            <div v-if="reviewsError && reviews.length === 0" class="review-state review-state--error" role="alert">
              <span>{{ reviewsError }}</span>
              <button type="button" @click="loadReviews()">重新載入</button>
            </div>
            <div v-else-if="reviewsLoaded && reviews.length === 0" class="review-state">
              此商品目前尚無用戶評價。
            </div>
            <div v-else class="review-list">
              <button
                v-for="review in reviews"
                :key="review.starId"
                type="button"
                class="review-card"
                :aria-label="`詳閱 ${maskMemberId(review.memberId)} 的評價`"
                @click="openReview(review)"
              >
                <div class="review-card__heading">
                  <strong>{{ maskMemberId(review.memberId) }}</strong>
                  <span class="review-stars" :aria-label="`${review.fiveStar} 顆星`">
                    <i
                      v-for="value in 5"
                      :key="value"
                      class="bi"
                      :class="value <= review.fiveStar ? 'bi-star-fill' : 'bi-star'"
                      aria-hidden="true"
                    ></i>
                  </span>
                </div>
                <p>{{ review.feedback || '此會員只留下星等評價。' }}</p>
                <div v-if="reviewImages(review).length" class="review-thumbnails" aria-label="評價照片">
                  <img
                    v-for="(image, index) in reviewImages(review)"
                    :key="index"
                    :src="image"
                    :alt="`評價照片 ${index + 1}`"
                  />
                </div>
              </button>
            </div>
            <div v-if="reviewsLoading" class="review-loading" role="status">正在載入評價...</div>
            <div v-else-if="reviewsLoaded && !reviewsHasNext && reviews.length" class="review-end">已顯示全部評價</div>
          </div>
        </section>
        <!-- //review-end，總共10次修改，第6次// -->

        <!-- //review-start，總共10次修改，第7次// -->
        <div v-if="selectedReview" class="review-overlay" role="presentation" @click.self="closeReview">
          <article class="review-dialog" role="dialog" aria-modal="true" aria-labelledby="review-dialog-title">
            <button type="button" class="review-dialog__close" aria-label="關閉評價詳閱" @click="closeReview">×</button>
            <header>
              <p>{{ maskMemberId(selectedReview.memberId) }}</p>
              <h2 id="review-dialog-title">用戶評價</h2>
              <span class="review-stars" :aria-label="`${selectedReview.fiveStar} 顆星`">
                <i
                  v-for="value in 5"
                  :key="value"
                  class="bi"
                  :class="value <= selectedReview.fiveStar ? 'bi-star-fill' : 'bi-star'"
                  aria-hidden="true"
                ></i>
              </span>
            </header>
            <p class="review-dialog__feedback">{{ selectedReview.feedback || '此會員只留下星等評價。' }}</p>
            <div v-if="reviewImages(selectedReview).length" class="review-dialog__images">
              <img
                v-for="(image, index) in reviewImages(selectedReview)"
                :key="index"
                :src="image"
                :alt="`評價放大照片 ${index + 1}`"
              />
            </div>
          </article>
        </div>
        <!-- //review-end，總共10次修改，第7次// -->
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

  /* //review-start，總共10次修改，第8次// */
  color: var(--color-surface);
  /* //review-end，總共10次修改，第8次// */
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
  /* //review-start，總共10次修改，第9次// */
  color: var(--color-surface);
  /* //review-end，總共10次修改，第9次// */
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

/* //review-start，總共10次修改，第10次// */
.product-detail-tabs {
  width: min(100%, calc(100% * 2 / 3));
  margin: var(--space-7) auto 0;
}

.detail-tab-list {
  display: flex;
  border-bottom: 1px solid var(--color-border);
}

.detail-tab-list button {
  position: relative;
  min-height: calc(var(--space-7) + var(--space-1));
  flex: 1;
  color: var(--color-text-muted);
  font-family: var(--font-body);
  font-size: var(--font-size-base);
  font-weight: 600;
  background: var(--color-surface);
  border: 0;
  cursor: pointer;
}

.detail-tab-list button::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: var(--space-1);
  content: '';
  background: transparent;
}

.detail-tab-list button:hover {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.detail-tab-list button.active,
.detail-tab-list button:active {
  color: var(--color-primary-active);
}

.detail-tab-list button.active::after {
  background: var(--color-primary);
}

.detail-tab-list button:focus-visible,
.detail-panel:focus-visible,
.review-card:focus-visible,
.review-dialog button:focus-visible,
.review-state button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.detail-panel {
  min-height: calc((var(--space-8) + var(--space-5)) * 6);
  padding: var(--space-5);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-top: 0;
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
}

.description-panel h2 {
  margin: 0 0 var(--space-4);
  font-family: var(--font-heading);
  font-size: var(--font-size-lg);
}

.description-panel p {
  margin: 0;
  line-height: var(--line-height-base);
  white-space: pre-line;
}

.reviews-panel {
  max-height: calc((var(--space-8) + var(--space-5)) * 6);
  overflow-y: auto;
  scrollbar-gutter: stable;
}

.review-list {
  display: grid;
  gap: var(--space-3);
}

.review-card {
  display: grid;
  min-height: calc(var(--space-8) + var(--space-5));
  gap: var(--space-2);
  width: 100%;
  padding: var(--space-4);
  color: var(--color-text);
  text-align: left;
  background: var(--color-surface-soft);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
}

.review-card:hover {
  background: var(--color-primary-soft);
  border-color: var(--color-primary);
}

.review-card:active {
  border-color: var(--color-primary-active);
}

.review-card__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.review-card__heading strong {
  font-size: var(--font-size-sm);
}

.review-stars {
  display: inline-flex;
  gap: var(--space-1);
  color: var(--color-warning);
}

.review-card p {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.review-thumbnails {
  display: flex;
  gap: var(--space-2);
}

.review-thumbnails img {
  width: var(--space-7);
  height: var(--space-7);
  object-fit: cover;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.review-state,
.review-loading,
.review-end {
  display: flex;
  min-height: calc(var(--space-8) * 2);
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  color: var(--color-text-muted);
  text-align: center;
}

.review-state--error {
  color: var(--color-danger);
}

.review-state button {
  padding: var(--space-2) var(--space-4);
  color: var(--color-primary);
  background: var(--color-surface);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
}

.review-overlay {
  position: fixed;
  z-index: 1050;
  inset: 0;
  display: grid;
  padding: var(--space-5);
  place-items: center;
  background: color-mix(in srgb, var(--color-text) 65%, transparent);
}

.review-dialog {
  position: relative;
  width: min(100%, calc(var(--space-8) * 10));
  max-height: calc(100vh - var(--space-8));
  overflow-y: auto;
  padding: var(--space-6);
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.review-dialog__close {
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  display: grid;
  width: calc(var(--space-5) + var(--space-4));
  height: calc(var(--space-5) + var(--space-4));
  place-items: center;
  color: var(--color-text-muted);
  font-size: var(--font-size-xl);
  background: transparent;
  border: 0;
  border-radius: var(--radius-pill);
  cursor: pointer;
}

.review-dialog__close:hover {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.review-dialog header p,
.review-dialog header h2,
.review-dialog__feedback {
  margin: 0;
}

.review-dialog header p {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.review-dialog header h2 {
  margin: var(--space-1) 0 var(--space-2);
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}

.review-dialog__feedback {
  margin-top: var(--space-5);
  white-space: pre-wrap;
}

.review-dialog__images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-3);
  margin-top: var(--space-5);
}

.review-dialog__images img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: var(--radius-md);
}

@media (max-width: 767.98px) {
  .product-detail-tabs {
    width: 100%;
  }

  .detail-panel {
    padding: var(--space-4);
  }

  .review-dialog__images {
    grid-template-columns: 1fr;
  }
}
/* //review-end，總共10次修改，第10次// */
</style>
