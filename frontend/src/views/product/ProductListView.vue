<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import api from '@/api/axios'
import { logSafeError } from '@/utils/safeError'
import ProductCard from '@/views/product/ProductCard.vue'
import { getPublicStore, resolveSellerLogoUrl } from '@/api/sellerProfileApi'
const route = useRoute()
const router = useRouter()

const sort = ref(route.query.sort || '')
const products = ref([])
const loading = ref(false)
const errorMessage = ref('')
const currentPage = ref(0)
const pageSize = ref(12)
const storeProfile = ref(null)

const loadStoreProfile = async () => {
  if (!route.query.sellerId) {
    storeProfile.value = null
    return
  }

  try {
    const response = await getPublicStore(route.query.sellerId)
    storeProfile.value = response.data
  } catch (error) {
    console.error('Load public store failed:', error)
    storeProfile.value = null
  }
}
const totalPages = ref(0)
const totalElements = ref(0)

// 取得商品
const fetchProducts = async () => {
  try {
    loading.value = true
    errorMessage.value = ''

    const params = {
      page: currentPage.value,
      size: pageSize.value,
    }

    if (route.query.keyword) {
      params.keyword = route.query.keyword
    }

    if (route.query.categoryId) {
      params.categoryId = route.query.categoryId
    }

    if (route.query.subcategoryId) {
      params.subcategoryId = route.query.subcategoryId
    }

    if (route.query.brandId) {
      params.brandId = route.query.brandId
    }

    if (route.query.sort) {
      params.sort = route.query.sort
    }

    const response = await api.get('/products', { params })

    products.value = response.data.content

    totalPages.value = response.data.totalPages
    totalElements.value = response.data.totalElements
    currentPage.value = response.data.number
  } catch (error) {
    logSafeError('取得商品失敗：', error)
    errorMessage.value = '商品資料載入失敗'
  } finally {
    loading.value = false
  }
}
//新增換頁
const goToPage = (page) => {
  if (page < 0 || page >= totalPages.value) {
    return
  }

  currentPage.value = page
  fetchProducts()
}

//新增每頁 12 / 24 切換
const changePageSize = () => {
  currentPage.value = 0
  fetchProducts()
}
//當搜尋或分類條件改變時，回第一頁
watch(
  () => [
    route.query.keyword,
    route.query.categoryId,
    route.query.subcategoryId,
    route.query.brandId,
    route.query.sort,
  ],
  () => {
    currentPage.value = 0
    sort.value = route.query.sort || ''
    fetchProducts()
  },
)

// 切換排序
const changeSort = () => {
  currentPage.value = 0

  router.push({
    query: {
      ...route.query,
      sort: sort.value || undefined,
    },
  })
}

// 第一次進入頁面時取得商品
onMounted(() => {
  loadStoreProfile()
  fetchProducts()
})
const formatStoreTime = (time) => {
  if (!time) {
    return ''
  }

  return time.slice(0, 5)
}
</script>

<template>
  <main class="product-list-page">
    <div class="container py-5">
      <section v-if="route.query.sellerId && storeProfile" class="store-banner">
        <img
          v-if="storeProfile.storeLogoUrl"
          class="store-avatar-image"
          :src="resolveSellerLogoUrl(storeProfile.storeLogoUrl)"
          :alt="`${storeProfile.storeName} Logo`"
        />

        <div class="store-copy">
          <span>品牌與商家</span>
          <h1>{{ storeProfile.storeName }}</h1>
          <p>{{ storeProfile.storeDescription }}</p>
        </div>

        <div class="store-meta">
          <strong>{{ storeProfile.status === 'ACTIVE' ? '營運中' : '暫停接單' }}</strong>
          <span v-if="storeProfile.serviceStartTime && storeProfile.serviceEndTime">
            營業時間 {{ formatStoreTime(storeProfile.serviceStartTime) }} -
            {{ formatStoreTime(storeProfile.serviceEndTime) }}
          </span>
          <span v-else>商品持續更新</span>
        </div>
      </section>

      <!-- 標題 -->
      <div class="mb-4">
        <h1 class="page-title">商品列表</h1>
      </div>

      <!-- 每頁顯示 -->
      <div class="product-list-toolbar">
        <div class="product-count">共 {{ totalElements }} 件商品</div>

        <div class="toolbar-actions">
          <!-- 排序 -->
          <div class="sort-selector">
            <label for="sort">排序：</label>

            <select id="sort" v-model="sort" @change="changeSort">
              <option value="">預設排序</option>
              <option value="newest">最新上架</option>
              <option value="priceAsc">價格：低到高</option>
              <option value="priceDesc">價格：高到低</option>
              <option value="salesDesc">銷量最高</option>
            </select>
          </div>

          <!-- 每頁顯示 -->
          <div class="page-size-selector">
            <label for="page-size">每頁顯示：</label>

            <select id="page-size" v-model.number="pageSize" @change="changePageSize">
              <option :value="2">2(測試用)</option>
              <option :value="12">12</option>
              <option :value="24">24</option>
            </select>
          </div>
        </div>
      </div>

      <!-- 載入中 -->
      <div v-if="loading" class="text-center py-5">商品載入中...</div>

      <!-- 錯誤 -->
      <div v-else-if="errorMessage" class="error-message text-center py-5">
        {{ errorMessage }}
      </div>

      <!-- 沒有商品 -->
      <div v-else-if="products.length === 0" class="empty-message text-center py-5">
        目前沒有符合條件的商品
      </div>

      <!-- 商品列表 -->
      <div v-else class="row g-4">
        <div v-for="product in products" :key="product.productId" class="col-6 col-md-4 col-lg-3">
          <ProductCard :product="product" />
        </div>
      </div>

      <!-- 分頁 -->
      <div v-if="totalPages > 1" class="pagination-wrapper">
        <button
          type="button"
          class="page-button"
          :disabled="currentPage === 0"
          @click="goToPage(currentPage - 1)"
        >
          上一頁
        </button>

        <button
          v-for="page in totalPages"
          :key="page"
          type="button"
          class="page-button"
          :class="{ active: currentPage === page - 1 }"
          @click="goToPage(page - 1)"
        >
          {{ page }}
        </button>

        <button
          type="button"
          class="page-button"
          :disabled="currentPage === totalPages - 1"
          @click="goToPage(currentPage + 1)"
        >
          下一頁
        </button>
      </div>
    </div>
  </main>
</template>

<style scoped>
.product-list-page {
  min-height: 100vh;

  background: var(--color-bg);
  color: var(--color-text);
}

.page-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}

.store-banner {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  background: var(--color-surface);
}

.store-avatar {
  width: 64px;
  height: 64px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-surface);
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
  font-weight: 800;
}

.store-copy {
  display: grid;
  gap: var(--space-1);
  min-width: 0;
}

.store-copy span,
.store-copy p,
.store-meta span {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.store-copy h1,
.store-copy p {
  margin: 0;
}

.store-copy h1 {
  color: var(--color-text-900);
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}

.store-meta {
  display: grid;
  gap: 2px;
  margin-left: auto;
  border-left: 1px solid var(--color-border);
  padding-left: var(--space-4);
}

.store-meta strong {
  color: var(--color-success);
}

.error-message {
  color: var(--color-danger);
}

.empty-message {
  color: var(--color-text-muted);
}

.product-list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;

  margin-bottom: 24px;
}

.product-count {
  color: var(--color-text-muted);
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-size-selector select {
  padding: 6px 10px;

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  background: var(--color-surface);
  color: var(--color-text);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  gap: 8px;

  margin-top: 40px;
}

.page-button {
  min-width: 40px;
  height: 40px;

  padding: 0 12px;

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  background: var(--color-surface);
  color: var(--color-text);

  cursor: pointer;
}

.page-button:hover:not(:disabled) {
  background: var(--color-primary-soft);
}

.page-button.active {
  color: white;
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.page-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.sort-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-selector select {
  padding: 6px 10px;

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  background: var(--color-surface);
  color: var(--color-text);
}

@media (max-width: 680px) {
  .store-banner {
    align-items: flex-start;
    flex-direction: column;
  }

  .store-meta {
    margin-left: 0;
    border-left: 0;
    padding-left: 0;
  }
}
.store-avatar-image {
  width: 64px;
  height: 64px;
  flex: 0 0 auto;
  border-radius: var(--radius-md);
  object-fit: cover;
  border: 1px solid var(--color-border);
}
</style>
