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
const categories = ref([])
const brands = ref([])

const minPrice = ref('')
const maxPrice = ref('')
const minRating = ref('')

const subcategories = ref([])

const selectedCategoryId = ref(route.query.categoryId || '')
const selectedSubcategoryId = ref(route.query.subcategoryId || '')
const selectedBrandId = ref(route.query.brandId || '')
const fetchCategories = async () => {
  try {
    const response = await api.get('/categories')
    categories.value = response.data
  } catch (error) {
    console.error('取得分類失敗：', error)
  }
}
const fetchSubcategories = async () => {
  try {
    // 沒選大分類時，不顯示任何子分類
    if (!selectedCategoryId.value) {
      subcategories.value = []
      return
    }

    const response = await api.get('/subcategories', {
      params: {
        categoryId: selectedCategoryId.value,
      },
    })

    subcategories.value = response.data
  } catch (error) {
    console.error('取得子分類失敗：', error)
    subcategories.value = []
  }
}
const fetchBrands = async () => {
  try {
    const response = await api.get('/brands')
    brands.value = response.data
  } catch (error) {
    console.error('取得品牌失敗：', error)
  }
}

const applyFilters = () => {
  currentPage.value = 0

  router.push({
    query: {
      ...route.query,

      categoryId: selectedCategoryId.value || undefined,
      subcategoryId: selectedSubcategoryId.value || undefined,
      brandId: selectedBrandId.value || undefined,

      minPrice: minPrice.value || undefined,
      maxPrice: maxPrice.value || undefined,
      minRating: minRating.value || undefined,
    },
  })
}

const resetFilters = () => {
  selectedCategoryId.value = ''
  selectedSubcategoryId.value = ''
  selectedBrandId.value = ''

  subcategories.value = []

  minPrice.value = ''
  maxPrice.value = ''
  minRating.value = ''

  router.push({
    query: {
      keyword: route.query.keyword || undefined,
      sellerId: route.query.sellerId || undefined,
      sort: route.query.sort || undefined,
    },
  })
}

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

    if (route.query.minPrice) {
      params.minPrice = route.query.minPrice
    }

    if (route.query.maxPrice) {
      params.maxPrice = route.query.maxPrice
    }

    if (route.query.sort) {
      params.sort = route.query.sort
    }

    if (route.query.sellerId) {
      params.sellerId = route.query.sellerId
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
    route.query.minPrice,
    route.query.maxPrice,
    route.query.sort,
  ],
  () => {
    currentPage.value = 0
    sort.value = route.query.sort || ''
    fetchProducts()
  },
)
watch(selectedCategoryId, async () => {
  // 大分類改變時，原本選的子分類失效
  selectedSubcategoryId.value = ''

  await fetchSubcategories()
})

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
onMounted(async () => {
  loadStoreProfile()
  await fetchCategories()
  await fetchBrands()

  if (selectedCategoryId.value) {
    await fetchSubcategories()
  }

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
      <!-- 賣家資訊 -->
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
          <strong>
            {{ storeProfile.status === 'ACTIVE' ? '營運中' : '暫停接單' }}
          </strong>

          <span v-if="storeProfile.serviceStartTime && storeProfile.serviceEndTime">
            營業時間
            {{ formatStoreTime(storeProfile.serviceStartTime) }}
            -
            {{ formatStoreTime(storeProfile.serviceEndTime) }}
          </span>

          <span v-else> 商品持續更新 </span>
        </div>
      </section>

      <!-- =========================
           左側篩選 + 右側商品
           ========================= -->
      <div class="product-list-layout">
        <!-- 左側篩選 -->
        <aside class="filter-sidebar">
          <div class="filter-sidebar-header">
            <h2>篩選條件</h2>

            <button type="button" class="filter-reset-text" @click="resetFilters">清除</button>
          </div>

          <!-- 分類 -->
          <section class="filter-section">
            <h3>分類</h3>

            <select v-model="selectedCategoryId" class="filter-select">
              <option value="">全部分類</option>

              <option
                v-for="category in categories"
                :key="category.categoryId"
                :value="category.categoryId"
              >
                {{ category.categoryName }}
              </option>
            </select>
          </section>

          <!-- 子分類 -->
          <section class="filter-section">
            <h3>子分類</h3>

            <select
              v-model="selectedSubcategoryId"
              class="filter-select"
              :disabled="!selectedCategoryId"
            >
              <option value="">
                {{ selectedCategoryId ? '全部子分類' : '請先選擇分類' }}
              </option>

              <option
                v-for="subcategory in subcategories"
                :key="subcategory.subcategoryId"
                :value="subcategory.subcategoryId"
              >
                {{ subcategory.subcategoryName }}
              </option>
            </select>
          </section>

          <!-- 品牌 -->
          <section class="filter-section">
            <h3>品牌</h3>

            <select v-model="selectedBrandId" class="filter-select">
              <option value="">全部品牌</option>

              <option v-for="brand in brands" :key="brand.brandId" :value="brand.brandId">
                {{ brand.brandName }}
              </option>
            </select>
          </section>

          <!-- 價格 -->
          <section class="filter-section">
            <h3>價格區間</h3>

            <div class="price-range">
              <input v-model="minPrice" type="number" class="price-input" placeholder="最低價" />

              <span class="price-separator">－</span>

              <input v-model="maxPrice" type="number" class="price-input" placeholder="最高價" />
            </div>
          </section>

          <!-- 評價 -->
          <section class="filter-section">
            <h3>商品評價</h3>

            <label v-for="rating in [5, 4, 3, 2, 1]" :key="rating" class="rating-option">
              <input v-model.number="minRating" type="radio" name="rating-filter" :value="rating" />

              <span class="filter-stars">
                <i
                  v-for="star in 5"
                  :key="star"
                  class="bi"
                  :class="star <= rating ? 'bi-star-fill' : 'bi-star'"
                ></i>
              </span>

              <span> {{ rating }} 星以上 </span>
            </label>
          </section>

          <button type="button" class="apply-filter-button" @click="applyFilters">套用篩選</button>
        </aside>

        <!-- 右側商品 -->
        <section class="product-results">
          <!-- 標題 + 排序 -->
          <div class="product-results-header">
            <div>
              <h1 class="page-title">商品列表</h1>

              <div class="product-count">共 {{ totalElements }} 件商品</div>
            </div>

            <div class="toolbar-actions">
              <div class="sort-selector">
                <label for="sort"> 排序： </label>

                <select id="sort" v-model="sort" @change="changeSort">
                  <option value="">預設排序</option>

                  <option value="newest">最新上架</option>

                  <option value="priceAsc">價格：低到高</option>

                  <option value="priceDesc">價格：高到低</option>

                  <option value="salesDesc">銷量最高</option>
                </select>
              </div>

              <div class="page-size-selector">
                <label for="page-size"> 每頁顯示： </label>

                <select id="page-size" v-model.number="pageSize" @change="changePageSize">
                  <option :value="12">12</option>
                  <option :value="24">24</option>
                </select>
              </div>
            </div>
          </div>

          <!-- Loading -->
          <div v-if="loading" class="text-center py-5">商品載入中...</div>

          <!-- Error -->
          <div v-else-if="errorMessage" class="error-message text-center py-5">
            {{ errorMessage }}
          </div>

          <!-- 無商品 -->
          <div v-else-if="products.length === 0" class="empty-message text-center py-5">
            目前沒有符合條件的商品
          </div>

          <!-- 商品 -->
          <div v-else class="row g-4">
            <div
              v-for="product in products"
              :key="product.productId"
              class="col-6 col-md-4 col-xl-3"
            >
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
              :class="{
                active: currentPage === page - 1,
              }"
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
        </section>
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

.product-list-layout {
  display: grid;

  grid-template-columns:
    220px
    minmax(0, 1fr);

  gap: 28px;

  align-items: start;
}

/* =========================
   左側篩選
   ========================= */

.filter-sidebar {
  padding: 20px;

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.filter-sidebar-header {
  display: flex;

  align-items: center;
  justify-content: space-between;

  margin-bottom: 20px;
}

.filter-sidebar-header h2 {
  margin: 0;

  color: var(--color-text);

  font-size: 18px;
  font-weight: 700;
}

.filter-reset-text {
  padding: 0;

  color: var(--color-text-muted);

  font-size: 13px;

  background: transparent;

  border: 0;

  cursor: pointer;
}

.filter-reset-text:hover {
  color: var(--color-primary);
}

/* =========================
   每個篩選區塊
   ========================= */

.filter-section {
  margin-bottom: 20px;
  padding-bottom: 20px;

  border-bottom: 1px solid var(--color-border);
}

.filter-section h3 {
  margin: 0 0 12px;

  color: var(--color-text);

  font-size: 15px;
  font-weight: 600;
}

/* =========================
   Select
   ========================= */

.filter-select {
  width: 100%;
  height: 40px;

  padding: 0 10px;

  color: var(--color-text);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.filter-select:hover,
.filter-select:focus {
  border-color: var(--color-primary);

  outline: none;
}

/* =========================
   價格
   ========================= */

.price-filter {
  display: grid;

  grid-template-columns:
    minmax(0, 1fr)
    auto
    minmax(0, 1fr);

  align-items: center;

  gap: 6px;
}

.price-filter input {
  width: 100%;
  min-width: 0;
  height: 38px;

  padding: 0 8px;

  color: var(--color-text);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.price-filter input:focus {
  outline: none;

  border-color: var(--color-primary);
}

.price-filter span {
  color: var(--color-text-muted);
}

/* =========================
   評價
   ========================= */

.rating-option {
  display: flex;

  align-items: center;

  gap: 7px;

  margin-bottom: 10px;

  color: var(--color-text);

  font-size: 13px;

  cursor: pointer;
}

.rating-option input {
  accent-color: var(--color-primary);
}

.filter-stars {
  display: inline-flex;

  gap: 1px;

  color: var(--color-warning);

  font-size: 12px;
}

/* =========================
   套用
   ========================= */

.apply-filter-button {
  width: 100%;

  padding: 10px 12px;

  color: #fff;

  font-size: 14px;
  font-weight: 600;

  background: var(--color-primary);

  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);

  cursor: pointer;
}

.apply-filter-button:hover {
  opacity: 0.9;
}

/* =========================
   右側
   ========================= */

.product-results {
  min-width: 0;
}

.product-results-header {
  display: flex;

  align-items: flex-end;
  justify-content: space-between;

  gap: 24px;

  margin-bottom: 24px;
}

.page-title {
  margin: 0 0 8px;

  color: var(--color-text);

  font-family: var(--font-heading);

  font-size: 26px;
  font-weight: 700;
}

.product-count {
  color: var(--color-text-muted);

  font-size: 14px;
}

/* =========================
   排序 / 每頁
   ========================= */

.toolbar-actions {
  display: flex;

  align-items: center;

  gap: 18px;
}

.sort-selector,
.page-size-selector {
  display: flex;

  align-items: center;

  gap: 8px;
}

.sort-selector label,
.page-size-selector label {
  color: var(--color-text);

  font-size: 14px;
}

.sort-selector select,
.page-size-selector select {
  height: 40px;

  padding: 0 10px;

  color: var(--color-text);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.sort-selector select:hover,
.sort-selector select:focus,
.page-size-selector select:hover,
.page-size-selector select:focus {
  outline: none;

  border-color: var(--color-primary);
}

/* =========================
   分頁
   ========================= */

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

  color: var(--color-text);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  cursor: pointer;
}

.page-button:hover:not(:disabled) {
  color: var(--color-primary);

  background: var(--color-primary-soft);

  border-color: var(--color-primary);
}

.page-button.active {
  color: #fff;

  background: var(--color-primary);

  border-color: var(--color-primary);
}

.page-button:disabled {
  cursor: not-allowed;

  opacity: 0.5;
}
.filter-select:disabled {
  color: var(--color-text-muted);
  background: var(--color-surface-soft);
  cursor: not-allowed;
  opacity: 0.7;
}
/* =========================
   RWD
   ========================= */

@media (max-width: 991.98px) {
  .product-list-layout {
    grid-template-columns: 1fr;
  }

  .filter-sidebar {
    width: 100%;
  }

  .product-results-header {
    align-items: stretch;

    flex-direction: column;
  }
}

@media (max-width: 575.98px) {
  .toolbar-actions {
    align-items: stretch;

    flex-direction: column;
  }

  .sort-selector,
  .page-size-selector {
    justify-content: space-between;
  }

  .sort-selector select,
  .page-size-selector select {
    flex: 1;
  }
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
.price-range {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 6px;
  width: 100%;
}

.price-input {
  width: 100%;
  min-width: 0;
  height: 40px;
  padding: 8px;
  font-size: 14px;
  box-sizing: border-box;
}
</style>
