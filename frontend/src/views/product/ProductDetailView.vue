<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api/axios'
import { logSafeError } from '@/utils/safeError'
import { getImageUrl } from '@/utils/imageUrl'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
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
const authStore = useAuthStore()
const cartStore = useCartStore()
const seller = ref(null)
const sellerLoading = ref(false)
const sellerCoupons = ref([])

// Review 檢視版：使用本地展示資料，不呼叫尚未整合的 Review 後端。
const activeDetailTab = ref('description')
const reviews = ref([])
const reviewsLoading = ref(false)
const reviewsLoaded = ref(false)
const selectedReview = ref(null)
const reviewPage = ref(1)
const reviewTotalPages = ref(1)
const reviewFilter = ref({ rating: null, content: 'ALL' })
const reviewSummary = computed(() => {
  const pool = previewReviewPool()
  const rated = pool.filter((review) => review.fiveStar)
  const average = rated.length
    ? Math.floor((rated.reduce((sum, review) => sum + review.fiveStar, 0) / rated.length) * 10) / 10
    : null
  return {
    averageFiveStar: average,
    totalCount: rated.length,
    fiveStarCount: rated.filter((review) => review.fiveStar === 5).length,
    fourStarCount: rated.filter((review) => review.fiveStar === 4).length,
    threeStarCount: rated.filter((review) => review.fiveStar === 3).length,
    twoStarCount: rated.filter((review) => review.fiveStar === 2).length,
    oneStarCount: rated.filter((review) => review.fiveStar === 1).length,
    withFeedbackCount: rated.filter((review) => review.feedback?.trim()).length,
    withImageCount: rated.filter((review) => reviewImages(review).length).length,
  }
})

/**
 * 取得商品詳情
 */
const fetchProductDetail = async () => {
  try {
    const productId = route.params.id

    const response = await api.get(`/products/${productId}`)

    product.value = response.data
    console.log('商品圖片：', product.value.images)

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

    // 同步更新 Header 購物車數量
    await cartStore.fetchCart()

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

    const response = await api.post('/cart/items', {
      skuId: selectedSku.value.skuId,
      quantity: quantity.value,
    })

    // 同步更新 Header
    await cartStore.fetchCart()

    const cartItemId = response.data?.cartItemId

    if (cartItemId) {
      // 帶著要勾選的 cartItemId 前往購物車
      router.push({
        path: '/cart',
        query: {
          selectedCartItemId: cartItemId,
        },
      })
    } else {
      // 後端沒有回傳 cartItemId
      router.push('/cart')
    }
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

  // 訪客不需要查收藏狀態
  if (!authStore.isAuthenticated) {
    isFavorite.value = false
    return
  }

  try {
    const response = await api.get('/favorites')

    isFavorite.value = response.data.some(
      (favorite) => Number(favorite.productId) === Number(product.value.productId),
    )
  } catch (error) {
    console.error('取得收藏狀態失敗：', error)

    if (error.response?.status === 401) {
      isFavorite.value = false
    }
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

function previewReviewPool() {
  const imageUrls = (product.value?.images ?? [])
    .map((image) => getImageUrl(image.imageUrl))
    .filter(Boolean)
  const messages = [
    '包裝完整，商品質感很好，實際使用後符合期待。',
    '尺寸與頁面說明一致，出貨速度也很快。',
    '操作容易，細節做工不錯，會推薦給其他買家。',
    '顏色接近實品照片，整體使用體驗很滿意。',
    '功能符合需求，客服回覆清楚，值得再次購買。',
  ]
  return Array.from({ length: 20 }, (_, index) => {
    const displayType = index % 4
    return {
      starId: index + 1,
      memberId: 12031 + index * 17,
      fiveStar: 5 - (index % 3),
      feedback: displayType === 2 || displayType === 3 ? '' : messages[index % messages.length],
      images:
        imageUrls.length && (displayType === 0 || displayType === 2)
          ? Array.from(
              { length: Math.min(3, imageUrls.length) },
              (_, imageIndex) => imageUrls[(index + imageIndex) % imageUrls.length],
            )
          : [],
      starUpdAt: new Date(Date.now() - index * 60000).toISOString(),
    }
  }).sort(compareReviewDisplayOrder)
}

// 產品明細檢視版比照後端：內容＋圖片為 2、內容或圖片為 1、僅星等為 0。
function reviewDisplayPriority(review) {
  const hasContent = Boolean(review?.feedback?.trim())
  const hasImage = reviewImages(review).length > 0
  return hasContent && hasImage ? 2 : hasContent || hasImage ? 1 : 0
}

function compareReviewDisplayOrder(left, right) {
  return (
    reviewDisplayPriority(right) - reviewDisplayPriority(left) ||
    new Date(right.starUpdAt).getTime() - new Date(left.starUpdAt).getTime() ||
    right.starId - left.starId
  )
}

function maskMemberId(memberId) {
  const value = String(memberId ?? '')
  if (!value) return '會員 *****'
  if (value.length === 1) return `會員 ${value}*****`
  return `會員 ${value.slice(0, 1)}*****${value.slice(-1)}`
}

function reviewImages(review) {
  return Array.isArray(review?.images) ? review.images : []
}

async function loadReviews() {
  if (reviewsLoading.value) return
  reviewsLoading.value = true
  await Promise.resolve()
  const pool = previewReviewPool().filter((review) => {
    if (reviewFilter.value.rating && review.fiveStar !== reviewFilter.value.rating) return false
    if (reviewFilter.value.content === 'FEEDBACK' && !review.feedback?.trim()) return false
    if (reviewFilter.value.content === 'IMAGE' && !reviewImages(review).length) return false
    return true
  })
  const totalPages = Math.max(1, Math.ceil(pool.length / 10))
  reviewPage.value = Math.min(reviewPage.value, totalPages)
  reviewTotalPages.value = totalPages
  const start = (reviewPage.value - 1) * 10
  const batch = pool.slice(start, start + 10)
  reviews.value = batch
  reviewsLoaded.value = true
  reviewsLoading.value = false
}

async function selectReviewFilter({ rating = null, content = 'ALL' }) {
  reviewFilter.value = { rating, content }
  reviews.value = []
  reviewsLoaded.value = false
  reviewPage.value = 1
  await loadReviews()
}

function isReviewFilterActive({ rating = null, content = 'ALL' }) {
  return reviewFilter.value.rating === rating && reviewFilter.value.content === content
}

function ratingCount(value) {
  return reviewSummary.value[`${['', 'one', 'two', 'three', 'four', 'five'][value]}StarCount`] ?? 0
}

function formatAverage(value) {
  const average = Number(value)
  return Number.isFinite(average) ? average.toFixed(1) : '—'
}

const averageInteger = computed(() => formatAverage(reviewSummary.value.averageFiveStar).split('.')[0])
const averageDecimal = computed(() => {
  const formatted = formatAverage(reviewSummary.value.averageFiveStar)
  return formatted.includes('.') ? `.${formatted.split('.')[1]}` : ''
})

function formatReviewTime(value) {
  if (!value) return '評價時間未提供'
  return new Intl.DateTimeFormat('zh-TW', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
  }).format(new Date(value))
}

const reviewPageItems = computed(() => {
  const total = reviewTotalPages.value
  const current = reviewPage.value
  if (total <= 7) return Array.from({ length: total }, (_, index) => index + 1)
  const values = [1]
  if (current > 4) values.push('left-ellipsis')
  for (let page = Math.max(2, current - 1); page <= Math.min(total - 1, current + 1); page += 1) values.push(page)
  if (current < total - 3) values.push('right-ellipsis')
  values.push(total)
  return values
})

async function selectReviewPage(page) {
  const target = Math.min(Math.max(Number(page), 1), reviewTotalPages.value)
  if (target === reviewPage.value || reviewsLoading.value) return
  reviewPage.value = target
  await loadReviews()
}

async function selectDetailTab(tab) {
  activeDetailTab.value = tab
  if (tab === 'reviews' && !reviewsLoaded.value) await loadReviews()
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

const fetchSeller = async () => {
  if (!product.value?.sellerId) return

  try {
    sellerLoading.value = true

    const response = await api.get(`/stores/${product.value.sellerId}`)

    seller.value = response.data
  } catch (error) {
    console.error('取得賣家資料失敗：', error)
  } finally {
    sellerLoading.value = false
  }
}

const fetchSellerCoupons = async () => {
  if (!product.value?.sellerId) return

  try {
    const response = await api.get('/coupons/available', {
      params: {
        sellerId: product.value.sellerId,
      },
    })

    sellerCoupons.value = response.data
  } catch (error) {
    console.error('取得賣家優惠券失敗：', error)
  }
}
const getStoreLogoUrl = (url) => {
  if (!url) return ''

  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }

  return `http://localhost:8080${url}`
}

const goToStore = (sellerId) => {
  router.push({
    path: '/products',
    query: {
      sellerId,
    },
  })
}
const claimCoupon = async (couponId) => {
  try {
    await api.post(`/member/coupons/${couponId}/claim`)

    alert('優惠券領取成功')
  } catch (error) {
    console.error('領取優惠券失敗：', error)

    if (error.response?.status === 401) {
      router.push({
        name: 'Login',
        query: {
          redirect: route.fullPath,
        },
      })
    }
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
  await loadReviews()
  await fetchSeller()
  await fetchSellerCoupons()
  await fetchFavoriteStatus()

  window.addEventListener('keydown', handleReviewEscape)
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
            <div class="product-heading-row">
              <div class="product-heading-copy">
                <div class="product-meta">
                  分類：{{ product.categoryName }} / {{ product.subcategoryName }}
                </div>

                <h1 class="product-name">
                  {{ product.productName }}
                </h1>

                <div class="product-meta">品牌：{{ product.brandName }}</div>
              </div>
              <div class="product-heading-rating" aria-label="商品平均評分">
                <span class="product-heading-rating__label">商品評價</span>
                <strong>{{ averageInteger }}<small>{{ averageDecimal }}</small></strong>
                <span class="review-stars">
                  <i v-for="value in 5" :key="value" class="bi" :class="value <= Math.floor(reviewSummary.averageFiveStar || 0) ? 'bi-star-fill' : 'bi-star'" aria-hidden="true"></i>
                </span>
              </div>
            </div>

            <div class="product-price-area mb-4">
              <div class="product-price">
                NT$ {{ selectedSku ? selectedSku.price : product.basePrice }}
              </div>

              <div class="product-sold-count">已售出 {{ product.soldCount ?? 0 }} 件</div>
            </div>

            <!-- 商品說明改由主圖下方標籤面板顯示。 -->

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

        <section class="product-detail-tabs" aria-label="產品明細內容">
          <div class="product-bottom-layout">
            <!-- 左邊：產品說明 / 商品評價 -->
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
                  商品評價
                </button>
              </div>

              <div
                v-if="activeDetailTab === 'description'"
                class="detail-panel description-panel"
                role="tabpanel"
              >
                <h2>產品說明</h2>

                <p>
                  {{ product.description || '目前尚無產品說明。' }}
                </p>
              </div>

              <div
                v-else
                class="detail-panel reviews-panel"
                role="tabpanel"
                aria-label="商品評價"
              >
                <div class="review-overview">
                  <div class="review-average" aria-label="全部評價平均分數">
                    <strong>{{ averageInteger }}<small>{{ averageDecimal }}</small></strong>
                    <span class="review-stars">
                      <i v-for="value in 5" :key="value" class="bi" :class="value <= Math.floor(reviewSummary.averageFiveStar || 0) ? 'bi-star-fill' : 'bi-star'"></i>
                    </span>
                  </div>
                  <div class="review-filters" aria-label="商品評價篩選">
                    <div class="review-filter-row">
                      <button type="button" :class="{ active: isReviewFilterActive({}) }" @click="selectReviewFilter({})">全部（{{ reviewSummary.totalCount }}）</button>
                      <button v-for="value in [5, 4, 3, 2, 1]" :key="value" type="button" :class="{ active: isReviewFilterActive({ rating: value }) }" @click="selectReviewFilter({ rating: value })">{{ value }}★（{{ ratingCount(value) }}）</button>
                    </div>
                    <div class="review-filter-row">
                      <button type="button" :class="{ active: isReviewFilterActive({ content: 'FEEDBACK' }) }" @click="selectReviewFilter({ content: 'FEEDBACK' })">附上評論（{{ reviewSummary.withFeedbackCount }}）</button>
                      <button type="button" :class="{ active: isReviewFilterActive({ content: 'IMAGE' }) }" @click="selectReviewFilter({ content: 'IMAGE' })">附上圖片（{{ reviewSummary.withImageCount }}）</button>
                    </div>
                  </div>
                </div>
                <div v-if="reviewsLoaded && reviews.length === 0" class="review-state">
                  此商品目前尚無商品評價。
                </div>

                <div v-else class="review-list">
                  <button
                    v-for="review in reviews"
                    :key="review.starId"
                    type="button"
                    class="review-card"
                    @click="openReview(review)"
                  >
                    <div class="review-card__copy">
                      <strong>
                        {{ maskMemberId(review.memberId) }}
                      </strong>
                      <time class="review-card__time" :datetime="review.starUpdAt">{{ formatReviewTime(review.starUpdAt) }}</time>
                      <span class="review-stars" :aria-label="`${review.fiveStar} 顆星`">
                        <i v-for="value in 5" :key="value" class="bi" :class="value <= review.fiveStar ? 'bi-star-fill' : 'bi-star'" aria-hidden="true"></i>
                      </span>
                      <p>{{ review.feedback || '此會員只留下星等評價。' }}</p>
                    </div>
                    <div v-if="reviewImages(review).length" class="review-thumbnails">
                      <img v-for="(image, index) in reviewImages(review)" :key="index" :src="image" :alt="`評價照片 ${index + 1}`" />
                    </div>
                  </button>
                </div>

                <div v-if="reviewsLoading" class="review-loading" role="status">
                  正在載入評價...
                </div>

                <nav v-if="reviewsLoaded && reviewTotalPages > 1" class="review-pagination" aria-label="商品評價頁碼">
                  <button type="button" :disabled="reviewPage === 1" aria-label="第一頁" @click="selectReviewPage(1)">&lt;&lt;</button>
                  <button type="button" :disabled="reviewPage === 1" aria-label="上一頁" @click="selectReviewPage(reviewPage - 1)">&lt;</button>
                  <template v-for="item in reviewPageItems" :key="item">
                    <span v-if="typeof item === 'string'">…</span>
                    <button v-else type="button" :class="{ active: item === reviewPage }" :aria-current="item === reviewPage ? 'page' : undefined" @click="selectReviewPage(item)">{{ item }}</button>
                  </template>
                  <button type="button" :disabled="reviewPage === reviewTotalPages" aria-label="下一頁" @click="selectReviewPage(reviewPage + 1)">&gt;</button>
                  <button type="button" :disabled="reviewPage === reviewTotalPages" aria-label="最後一頁" @click="selectReviewPage(reviewTotalPages)">&gt;&gt;</button>
                </nav>
              </div>
            </section>

            <!-- 右邊 -->
            <aside class="product-sidebar">
              <!-- 賣家 -->
              <section v-if="sellerLoading || seller" class="seller-card">
                <div v-if="sellerLoading" class="sidebar-loading">賣家資料載入中...</div>

                <template v-else>
                  <div class="seller-header">
                    <img
                      v-if="seller.storeLogoUrl"
                      :src="getStoreLogoUrl(seller.storeLogoUrl)"
                      :alt="seller.storeName"
                      class="seller-logo"
                    />

                    <div v-else class="seller-logo-placeholder">
                      <i class="bi bi-shop"></i>
                    </div>

                    <div class="seller-info">
                      <h2 class="seller-name">
                        {{ seller.storeName }}
                      </h2>

                      <p class="seller-description">
                        {{ seller.storeDescription || '目前尚無商店介紹' }}
                      </p>
                    </div>
                  </div>

                  <div class="seller-actions">
                    <button type="button" class="seller-chat-button">
                      <i class="bi bi-chat-dots"></i>
                      聊天
                    </button>

                    <button
                      type="button"
                      class="seller-store-button"
                      @click="goToStore(seller.sellerId)"
                    >
                      <i class="bi bi-shop"></i>
                      進入賣家賣場
                    </button>
                  </div>
                </template>
              </section>

              <!-- 優惠券 -->
              <section class="seller-coupon-card">
                <h2 class="sidebar-title">該賣家可使用的優惠券</h2>

                <div v-if="sellerCoupons.length === 0" class="coupon-empty">
                  目前尚無可使用的優惠券
                </div>

                <div v-else class="coupon-list">
                  <div v-for="coupon in sellerCoupons" :key="coupon.couponId" class="coupon-item">
                    <div class="coupon-main">
                      <strong class="coupon-name">
                        {{ coupon.couponName }}
                      </strong>

                      <div class="coupon-discount">
                        <template v-if="coupon.discountType === 'PERCENT'">
                          {{ coupon.discountValue }}% OFF
                        </template>

                        <template v-else> 折 NT$ {{ coupon.discountValue }} </template>
                      </div>

                      <div v-if="coupon.minPurchaseAmount" class="coupon-condition">
                        滿 NT$ {{ coupon.minPurchaseAmount }} 可用
                      </div>
                    </div>

                    <button
                      type="button"
                      class="coupon-claim-button"
                      @click.stop="claimCoupon(coupon.couponId)"
                    >
                      領取
                    </button>
                  </div>
                </div>
              </section>
            </aside>
          </div>
        </section>

        <div
          v-if="selectedReview"
          class="review-overlay"
          role="presentation"
          @click.self="closeReview"
        >
          <article
            class="review-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="review-dialog-title"
          >
            <button
              type="button"
              class="review-dialog__close"
              aria-label="關閉評價詳閱"
              @click="closeReview"
            >
              ×
            </button>
            <header>
              <p>{{ maskMemberId(selectedReview.memberId) }}</p>
              <h2 id="review-dialog-title">商品評價</h2>
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
            <p class="review-dialog__feedback">
              {{ selectedReview.feedback || '此會員只留下星等評價。' }}
            </p>
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
      </div>
    </div>
  </main>
</template>

<style scoped>
/* =========================================
   整體頁面
   ========================================= */

.product-detail-page {
  min-height: 100vh;
  color: var(--color-text);
  background: var(--color-bg);
}

/* =========================================
   商品基本資訊
   ========================================= */

.product-meta {
  margin-bottom: 8px;

  color: var(--color-text-muted);
  font-size: 14px;
  font-weight: 400;
  line-height: 1.5;
}

.product-name {
  margin: 6px 0 12px;

  color: var(--color-text);
  font-family: var(--font-heading);
  font-size: 26px;
  font-weight: 700;
  line-height: 1.35;
}

.product-heading-row {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: var(--space-5);
}

.product-heading-copy { min-width: 0; }

.product-heading-rating {
  display: grid;
  grid-template-rows: 1fr auto 1fr;
  flex: 0 0 auto;
  align-content: stretch;
  justify-items: center;
  min-width: calc(var(--space-8) * 2 + var(--space-5));
  padding: var(--space-2) var(--space-3);
  color: var(--color-surface);
  text-align: center;
  background: var(--color-primary);
  border-radius: var(--radius-lg);
}

.product-heading-rating__label { align-self: start; color: var(--color-surface); font-size: var(--font-size-sm); font-weight: 700; }
.product-heading-rating strong { align-self: center; color: var(--color-surface); font-size: var(--font-size-2xl); line-height: 1; }
.product-heading-rating small { font-size: 0.55em; }
.product-heading-rating .review-stars { align-self: end; color: #ffe44d; font-size: var(--font-size-lg); }

/* =========================================
   主圖
   ========================================= */

.main-image-wrapper {
  width: 100%;
  aspect-ratio: 1 / 1;

  overflow: hidden;

  background: var(--color-surface-soft);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.product-main-image {
  display: block;

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

/* =========================================
   縮圖
   ========================================= */

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
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.thumbnail-image {
  width: 100%;
  height: 100%;

  object-fit: contain;

  border-radius: var(--radius-sm);
}

/* =========================================
   價格區
   ========================================= */

.product-price-area {
  display: flex;

  width: 100%;

  align-items: center;

  gap: 20px;

  margin: 16px 0 24px !important;
  padding: 14px 18px;

  background: #e4ece6;

  border-radius: 6px;
}

.product-price {
  margin: 0;

  color: var(--color-primary);

  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
}

.product-sold-count {
  margin: 0;

  color: var(--color-text-muted);

  font-size: 14px;
  line-height: 1.2;
}

/* =========================================
   商品規格
   ========================================= */

.product-specs {
  margin-top: var(--space-5);
}

.spec-group {
  margin-bottom: var(--space-5);
}

.spec-title {
  margin-bottom: var(--space-3);

  color: var(--color-text);

  font-size: 15px;
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

  font-weight: 600;

  background: var(--color-primary-soft);

  border: 2px solid var(--color-primary);
}

.spec-button:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

/* =========================================
   SKU / 庫存
   ========================================= */

.selected-sku-info {
  margin-top: var(--space-5);

  padding-top: var(--space-4);

  border-top: 1px solid var(--color-border);
}

.sku-stock {
  margin-top: var(--space-2);

  color: var(--color-text-muted);

  font-size: 14px;
}

.sku-unavailable {
  margin-top: var(--space-5);

  color: var(--color-danger);
}

.empty-sku-message {
  color: var(--color-text-muted);
}

/* =========================================
   數量
   ========================================= */

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

  background: var(--color-primary-soft);

  border-color: var(--color-primary);
}

.quantity-area button:disabled {
  cursor: not-allowed;

  opacity: 0.5;
}

/* =========================================
   購買按鈕
   ========================================= */

.purchase-actions {
  display: flex;

  align-items: stretch;

  gap: var(--space-3);

  margin-top: var(--space-5);
}

.favorite-action {
  display: flex;

  margin-top: 0;
}

.favorite-button,
.add-cart-button,
.checkout-button {
  min-height: 44px;

  padding: var(--space-3) var(--space-4);

  border-radius: var(--radius-md);

  font-size: 14px;
  font-weight: 600;

  cursor: pointer;
}

.favorite-button {
  color: var(--color-text);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
}

.favorite-button:hover:not(:disabled) {
  color: var(--color-primary);

  background: var(--color-primary-soft);

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

.add-cart-button,
.checkout-button {
  flex: 1;
}

.add-cart-button {
  color: #fff;

  background: var(--color-primary);

  border: 1px solid var(--color-primary);
}

.add-cart-button:hover:not(:disabled) {
  opacity: 0.9;
}

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

.cart-message,
.favorite-message {
  margin-top: var(--space-3);

  color: var(--color-primary);
}

.error-message {
  color: var(--color-danger);
}

/* =========================================
   下半部左右配置
   左 2 / 右 1
   ========================================= */

.product-bottom-layout {
  display: grid;

  grid-template-columns:
    minmax(0, 2fr)
    minmax(300px, 1fr);

  gap: 28px;

  margin-top: var(--space-7);

  align-items: start;

  padding-bottom: calc(var(--space-8) * 3 + var(--space-7) * 3);
}

.product-detail-tabs {
  width: 100%;

  margin: 0;
}

/* =========================================
   Tab
   ========================================= */

.detail-tab-list {
  display: inline-flex;
  align-items: flex-end;

  width: auto;

  overflow: hidden;

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-bottom: 0;
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
}

.detail-tab-list button {
  position: relative;

  width: 110px;
  height: 48px;

  padding: 0 16px;

  color: var(--color-text-muted);
  font-size: 15px;
  font-weight: 600;

  background: var(--color-surface);

  border: 0;

  cursor: pointer;
}

.detail-tab-list button::after {
  position: absolute;

  right: 0;
  bottom: -1px;
  left: 0;

  height: 3px;

  content: '';

  background: transparent;
}

.detail-tab-list button.active {
  color: var(--color-primary);
}

.detail-tab-list button.active::after {
  background: var(--color-primary);
}

.detail-tab-list button:hover {
  color: var(--color-primary);
}

/* =========================================
   商品說明 / 評價內容
   ========================================= */

.detail-panel {
  width: 100%;

  padding: 24px;

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.description-panel h2 {
  margin: 0 0 var(--space-4);

  font-family: var(--font-heading);
  font-size: var(--font-size-lg);
}

.description-panel p {
  margin: 0;

  line-height: 1.8;

  white-space: pre-line;
}

.reviews-panel {
  min-height: 0;
}

.review-overview {
  display: grid;
  grid-template-columns: minmax(150px, 0.3fr) 1fr;
  gap: var(--space-5);
  margin: -24px -24px var(--space-4);
  padding: var(--space-5);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  min-height: calc(var(--space-8) + var(--space-7));
}

.review-average { display: grid; align-content: center; justify-items: center; gap: var(--space-1); text-align: center; }
.review-average strong { color: var(--color-primary-active); font-size: var(--font-size-2xl); line-height: 1; }
.review-average small { font-size: 0.55em; }
.review-filters { display: grid; align-content: center; gap: var(--space-2); }
.review-filter-row { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.review-filters button { padding: var(--space-1) var(--space-2); color: var(--color-primary-700); font-size: var(--font-size-xs); background: var(--color-surface); border: 1px solid var(--color-primary-300); border-radius: var(--radius-pill); cursor: pointer; }
.review-filters button:hover, .review-filters button.active { color: var(--color-surface); background: var(--color-primary); border-color: var(--color-primary); }

/* =========================================
   右側欄
   ========================================= */

.product-sidebar {
  display: flex;

  flex-direction: column;

  gap: 20px;

  /*
   * 左邊 Tab 高約 48px，
   * 所以右邊往下 48px，
   * 頂端對齊左邊內容框。
   */
  margin-top: 48px;
}

/* =========================================
   賣家 Card
   ========================================= */

.seller-card {
  padding: 22px;

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.seller-header {
  display: flex;

  align-items: center;

  gap: 16px;
}

.seller-logo,
.seller-logo-placeholder {
  width: 72px;
  height: 72px;

  flex: 0 0 72px;

  border-radius: 50%;
}

.seller-logo {
  object-fit: cover;

  border: 1px solid var(--color-border);
}

.seller-logo-placeholder {
  display: flex;

  align-items: center;
  justify-content: center;

  color: var(--color-text-muted);

  font-size: 28px;

  background: var(--color-surface-soft);

  border: 1px solid var(--color-border);
}

.seller-info {
  min-width: 0;

  flex: 1;
}

.seller-name {
  margin: 0;

  color: var(--color-text);

  font-size: 18px;
  font-weight: 700;
}

.seller-description {
  display: -webkit-box;

  overflow: hidden;

  margin: 6px 0 0;

  color: var(--color-text-muted);

  font-size: 13px;
  line-height: 1.5;

  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.sidebar-loading {
  padding: 24px 0;

  color: var(--color-text-muted);

  text-align: center;
}

/* =========================================
   賣家按鈕
   ========================================= */

.seller-actions {
  display: grid;

  grid-template-columns: 1fr 1.6fr;

  gap: 10px;

  margin-top: 20px;
}

.seller-actions button {
  min-height: 40px;

  padding: 8px 10px;

  font-size: 13px;
  font-weight: 600;

  border-radius: var(--radius-md);

  cursor: pointer;
}

.seller-chat-button {
  color: var(--color-primary);

  background: var(--color-surface);

  border: 1px solid var(--color-primary);
}

.seller-chat-button:hover {
  background: var(--color-primary-soft);
}

.seller-store-button {
  color: #fff;

  background: var(--color-primary);

  border: 1px solid var(--color-primary);
}

.seller-store-button:hover {
  opacity: 0.9;
}

/* =========================================
   優惠券
   ========================================= */

.seller-coupon-card {
  padding: 22px;

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.sidebar-title {
  margin: 0 0 18px;

  padding-bottom: 12px;

  color: var(--color-text);

  font-size: 17px;
  font-weight: 700;

  border-bottom: 1px solid var(--color-border);
}

.coupon-list {
  display: flex;

  flex-direction: column;

  gap: 12px;
}

.coupon-item {
  display: flex;

  align-items: center;

  gap: 12px;

  padding: 14px;

  background: var(--color-primary-soft);

  border: 1px solid var(--color-border);
  border-left: 4px solid var(--color-primary);

  border-radius: var(--radius-md);
}

.coupon-main {
  min-width: 0;

  flex: 1;
}

.coupon-name {
  display: block;

  margin-bottom: 4px;

  color: var(--color-text);

  font-size: 14px;
  font-weight: 700;
}

.coupon-discount {
  margin-top: 3px;

  color: var(--color-primary);

  font-size: 16px;
  font-weight: 700;
}

.coupon-condition {
  margin-top: 4px;

  color: var(--color-text-muted);

  font-size: 12px;
}

.coupon-claim-button {
  flex: 0 0 auto;

  padding: 7px 12px;

  color: #fff;

  font-size: 13px;
  font-weight: 600;

  background: var(--color-primary);

  border: 0;
  border-radius: var(--radius-md);

  cursor: pointer;
}

.coupon-claim-button:hover {
  opacity: 0.9;
}

.coupon-empty {
  padding: 30px 10px;

  color: var(--color-text-muted);

  font-size: 14px;
  text-align: center;
}

/* =========================================
   評價列表
   ========================================= */

.review-list {
  display: grid;

  gap: var(--space-3);
}

.review-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  width: 100%;
  min-height: calc(var(--space-8) + var(--space-7));
  align-items: center;
  gap: var(--space-2);

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

.review-card__copy {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.review-card__copy strong {
  font-size: var(--font-size-sm);
}

.review-stars {
  display: inline-flex;

  gap: var(--space-1);

  color: var(--color-primary);
}

.review-card__copy p {
  overflow: hidden;
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-card__time { color: var(--color-text-muted); font-size: var(--font-size-xs); }

.review-thumbnails { display: flex; gap: var(--space-2); }
.review-thumbnails img { width: var(--space-8); height: var(--space-8); object-fit: cover; border: 1px solid var(--color-border); border-radius: var(--radius-sm); }

.review-state,
.review-loading {
  display: flex;

  min-height: 120px;

  align-items: center;
  justify-content: center;

  color: var(--color-text-muted);

  text-align: center;
}

.review-pagination { display: flex; min-height: var(--space-7); align-items: center; justify-content: center; gap: var(--space-1); margin-top: var(--space-3); }
.review-pagination button { min-width: var(--space-5); min-height: var(--space-5); padding: var(--space-1) var(--space-2); color: var(--color-primary-700); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-sm); cursor: pointer; }
.review-pagination button:hover:not(:disabled), .review-pagination button.active { color: var(--color-surface); background: var(--color-primary); border-color: var(--color-primary); }
.review-pagination button:disabled { cursor: default; opacity: 0.4; }

/* =========================================
   評價 Modal
   ========================================= */

.review-overlay {
  position: fixed;

  z-index: 1050;

  inset: 0;

  display: grid;

  padding: var(--space-5);

  place-items: center;

  background: rgb(0 0 0 / 60%);
}

.review-dialog {
  position: relative;

  width: min(100%, 640px);

  max-height: calc(100vh - 64px);

  overflow-y: auto;

  padding: var(--space-6);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.review-dialog__close {
  position: absolute;

  top: var(--space-3);
  right: var(--space-3);

  display: grid;

  width: 36px;
  height: 36px;

  place-items: center;

  color: var(--color-text-muted);

  font-size: var(--font-size-xl);

  background: transparent;

  border: 0;
  border-radius: 50%;

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

/* =========================================
   RWD
   ========================================= */

@media (max-width: 991.98px) {
  .product-bottom-layout {
    grid-template-columns: 1fr;
  }

  .product-sidebar {
    margin-top: 0;
  }
}

@media (max-width: 767.98px) {
  .product-detail-tabs {
    width: 100%;
  }

  .detail-panel {
    padding: var(--space-4);
  }

  .review-overview { grid-template-columns: 1fr; }

  .review-card { grid-template-columns: minmax(0, 1fr) auto; }
  .review-thumbnails img { width: var(--space-7); height: var(--space-7); }

  .purchase-actions {
    flex-wrap: wrap;
  }

  .favorite-action {
    width: 100%;
  }

  .favorite-button {
    width: 100%;
  }

  .review-dialog__images {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 575.98px) {
  .product-name {
    font-size: 23px;
  }

  .product-price {
    font-size: 23px;
  }

  .product-price-area {
    align-items: flex-start;

    flex-direction: column;

    gap: 6px;
  }

  .purchase-actions {
    flex-direction: column;
  }

  .add-cart-button,
  .checkout-button {
    width: 100%;
  }

  .seller-actions {
    grid-template-columns: 1fr;
  }

  .coupon-item {
    align-items: stretch;

    flex-direction: column;
  }

  .coupon-claim-button {
    width: 100%;
  }
}
</style>
