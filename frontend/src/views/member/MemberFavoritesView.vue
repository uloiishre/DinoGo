<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'

// ================================
// 收藏商品
// ================================

const favorites = ref([])

const loading = ref(false)
const errorMessage = ref('')
const removingId = ref(null)
const addingCartId = ref(null)

// ================================
// 取得收藏
// ================================

const loadFavorites = async () => {
  try {
    loading.value = true
    errorMessage.value = ''

    const response = await api.get('/favorites')

    favorites.value = response.data
  } catch (error) {
    console.error('取得收藏失敗：', error)

    errorMessage.value = error.response?.data?.message || '無法取得收藏商品'
  } finally {
    loading.value = false
  }
}

// ================================
// 移除收藏
// ================================

const removeFavorite = async (productId) => {
  try {
    removingId.value = productId

    await api.delete(`/favorites/${productId}`)

    favorites.value = favorites.value.filter((item) => item.productId !== productId)
  } catch (error) {
    console.error('移除收藏失敗：', error)

    alert(error.response?.data?.message || '移除收藏失敗，請稍後再試')
  } finally {
    removingId.value = null
  }
}

// ================================
// 加入購物車
// ================================

const addToCart = async (favorite) => {
  try {
    addingCartId.value = favorite.productId

    if (!favorite.skuId) {
      alert('此商品缺少 SKU 資訊，無法加入購物車')
      return
    }

    await api.post('/cart/items', {
      skuId: favorite.skuId,
      quantity: 1,
    })

    alert('已加入購物車')
  } catch (error) {
    console.error('加入購物車失敗：', error)

    if (error.response?.status === 401) {
      alert('請先登入會員')
      return
    }

    alert(error.response?.data?.message || '加入購物車失敗，請稍後再試')
  } finally {
    addingCartId.value = null
  }
}

// ================================
// 金額格式
// ================================

const formatPrice = (price) => {
  return Number(price || 0).toLocaleString('zh-TW')
}

// ================================
// 初始化
// ================================

onMounted(() => {
  loadFavorites()
})
</script>

<template>
  <main class="favorite-page">
    <div class="favorite-container">
      <!-- ================================
           Page Header
      ================================= -->

      <header class="favorite-header">
        <div>
          <h1 class="favorite-title">我的收藏</h1>

          <p class="favorite-description">管理收藏商品並快速加入購物車</p>
        </div>

        <span class="favorite-count"> {{ favorites.length }} 件商品 </span>
      </header>

      <!-- ================================
           Loading
      ================================= -->

      <div v-if="loading" class="state-message" role="status">
        <i class="bi bi-arrow-repeat loading-icon"></i>
        <span>正在載入收藏...</span>
      </div>

      <!-- ================================
           Error
      ================================= -->

      <div v-else-if="errorMessage" class="state-message state-message-error" role="alert">
        <i class="bi bi-exclamation-circle"></i>

        <span>
          {{ errorMessage }}
        </span>
      </div>

      <!-- ================================
           收藏內容
      ================================= -->

      <div v-else-if="favorites.length > 0" class="favorite-grid">
        <!-- 收藏商品 -->

        <article v-for="favorite in favorites" :key="favorite.favoriteId" class="favorite-card">
          <!-- 商品圖片 -->
          <!-- 點圖片進入商品詳細頁 -->

          <RouterLink
            :to="{
              name: 'ProductDetail',
              params: {
                id: favorite.productId,
              },
            }"
            class="product-link"
          >
            <div class="product-image">
              <img v-if="favorite.imageUrl" :src="favorite.imageUrl" :alt="favorite.productName" />

              <div v-else class="image-placeholder" aria-label="沒有商品圖片">
                <i class="bi bi-image"></i>
              </div>
            </div>

            <!-- 商品資訊 -->

            <div class="product-info">
              <h2 class="product-name">
                {{ favorite.productName }}
              </h2>

              <p class="product-price">NT$ {{ formatPrice(favorite.basePrice) }}</p>
            </div>
          </RouterLink>

          <!-- 商品操作 -->

          <div class="product-actions">
            <!-- 收藏愛心 -->

            <button
              type="button"
              class="favorite-button"
              :disabled="removingId === favorite.productId"
              title="移除收藏"
              aria-label="移除收藏"
              @click="removeFavorite(favorite.productId)"
            >
              <i
                class="bi"
                :class="removingId === favorite.productId ? 'bi-arrow-repeat' : 'bi-heart-fill'"
              ></i>
            </button>

            <!-- 加入購物車 -->

            <button
              type="button"
              class="cart-button"
              :disabled="addingCartId === favorite.productId"
              @click="addToCart(favorite)"
            >
              <i
                class="bi"
                :class="addingCartId === favorite.productId ? 'bi-arrow-repeat' : 'bi-cart-plus'"
              ></i>

              <span>
                {{ addingCartId === favorite.productId ? '加入中...' : '加入購物車' }}
              </span>
            </button>
          </div>
        </article>
      </div>

      <!-- ================================
           沒有收藏商品
      ================================= -->

      <div v-else class="empty-favorite">
        <div class="empty-icon">
          <i class="bi bi-heart"></i>
        </div>

        <h2>收藏商品會顯示在這裡</h2>

        <p>立刻去逛逛，找到喜歡的商品吧</p>
      </div>
    </div>
  </main>
</template>

<style scoped>
/* ========================================
   Favorite Page
======================================== */

.favorite-page {
  width: 100%;
  min-height: 500px;

  background: var(--color-bg);
  color: var(--color-text);
}

.favorite-container {
  width: 100%;
  max-width: 1440px;

  margin: 0 auto;

  padding: var(--space-7) var(--space-6);

  box-sizing: border-box;
}

/* ========================================
   Page Header
======================================== */

.favorite-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;

  gap: var(--space-5);

  margin-bottom: var(--space-6);
}

.favorite-title {
  margin: 0 0 var(--space-2);

  color: var(--color-text);

  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
  font-weight: 700;

  line-height: 1.3;
}

.favorite-description {
  margin: 0;

  color: var(--color-text-muted);

  font-family: var(--font-body);
  font-size: var(--font-size-sm);

  line-height: 1.5;
}

.favorite-count {
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
   Favorite Grid
======================================== */

.favorite-grid {
  display: grid;

  grid-template-columns: repeat(4, minmax(0, 1fr));

  gap: var(--space-5);
}

/* ========================================
   Product Card
======================================== */

.favorite-card {
  min-width: 0;

  display: flex;
  flex-direction: column;

  padding: var(--space-3);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);

  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.favorite-card:hover {
  transform: translateY(-2px);

  box-shadow: var(--shadow-sm);
}

/* ========================================
   Product Image
======================================== */

.product-image {
  width: 100%;
  aspect-ratio: 1 / 1;

  display: flex;

  align-items: center;
  justify-content: center;

  overflow: hidden;

  background: var(--color-surface-soft);

  border-radius: var(--radius-md);
}

.product-image img {
  width: 100%;
  height: 100%;

  display: block;

  object-fit: contain;
}

.image-placeholder {
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

.product-info {
  padding: var(--space-4) var(--space-1) var(--space-3);
}

.product-name {
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

.product-price {
  margin: 0;

  color: var(--color-text);

  font-family: var(--font-body);
  font-size: var(--font-size-md);
  font-weight: 700;

  line-height: 1.4;
}

/* ========================================
   Product Actions
======================================== */

.product-actions {
  display: flex;
  align-items: center;

  gap: var(--space-2);

  margin-top: auto;
}

/* ========================================
   Favorite Button
======================================== */

.favorite-button {
  width: 40px;
  min-width: 40px;
  height: 40px;

  display: inline-flex;

  align-items: center;
  justify-content: center;

  padding: 0;

  color: var(--color-primary);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  font-size: 18px;

  cursor: pointer;

  transition:
    color 0.15s ease,
    background-color 0.15s ease;
}

.favorite-button:hover:not(:disabled) {
  color: var(--color-primary-hover);

  background: var(--color-primary-soft);
}

.favorite-button:active:not(:disabled) {
  color: var(--color-primary-active);
}

.favorite-button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.favorite-button:disabled {
  opacity: 0.55;

  cursor: not-allowed;
}

/* ========================================
   Cart Button
======================================== */

.cart-button {
  flex: 1;

  min-width: 0;
  height: 40px;

  display: inline-flex;

  align-items: center;
  justify-content: center;

  gap: var(--space-2);

  padding: 0 var(--space-3);

  color: var(--color-surface);

  background: var(--color-primary);

  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);

  font-family: var(--font-body);
  font-size: var(--font-size-sm);
  font-weight: 600;

  cursor: pointer;

  transition:
    background-color 0.15s ease,
    border-color 0.15s ease;
}

.cart-button:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.cart-button:active:not(:disabled) {
  background: var(--color-primary-active);
  border-color: var(--color-primary-active);
}

.cart-button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.cart-button:disabled {
  opacity: 0.6;

  cursor: not-allowed;
}

/* ========================================
   Empty
======================================== */

.empty-favorite {
  min-height: 360px;

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

.empty-icon {
  margin-bottom: var(--space-4);

  color: var(--color-primary);

  font-size: 48px;
}

.empty-favorite h2 {
  margin: 0 0 var(--space-2);

  color: var(--color-text);

  font-family: var(--font-heading);
  font-size: var(--font-size-lg);
  font-weight: 700;
}

.empty-favorite p {
  margin: 0;

  color: var(--color-text-muted);

  font-family: var(--font-body);
  font-size: var(--font-size-sm);
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

  background: var(--color-surface);
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
  .favorite-container {
    padding: var(--space-6) var(--space-5);
  }

  .favorite-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

/* ========================================
   768px
======================================== */

@media (max-width: 768px) {
  .favorite-container {
    padding: var(--space-5) var(--space-4);
  }

  .favorite-header {
    align-items: flex-start;

    flex-direction: column;

    margin-bottom: var(--space-5);
  }

  .favorite-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));

    gap: var(--space-4);
  }
}

/* ========================================
   375px
======================================== */

@media (max-width: 480px) {
  .favorite-container {
    padding: var(--space-4) var(--space-3);
  }

  .favorite-title {
    font-size: var(--font-size-lg);
  }

  .favorite-grid {
    grid-template-columns: 1fr;

    gap: var(--space-4);
  }

  .product-image {
    aspect-ratio: 1 / 1;
  }

  .favorite-card {
    padding: var(--space-3);
  }
}
.product-link {
  display: block;

  color: inherit;
  text-decoration: none;
}

.product-link:hover {
  color: inherit;
  text-decoration: none;
}

.product-link .product-name {
  color: var(--color-text);
}

.product-link .product-price {
  color: var(--color-primary);
}
</style>
