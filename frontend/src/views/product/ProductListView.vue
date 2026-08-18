<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import api from '@/api/axios'
import ProductCard from '@/views/product/ProductCard.vue'

const route = useRoute()

const products = ref([])
const loading = ref(false)
const errorMessage = ref('')
const currentPage = ref(0)
const pageSize = ref(12)

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

    const response = await api.get('/products', {
      params,
    })

    products.value = response.data.content

    totalPages.value = response.data.totalPages
    totalElements.value = response.data.totalElements
    currentPage.value = response.data.number
  } catch (error) {
    console.error('取得商品失敗：', error)
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
  ],
  () => {
    currentPage.value = 0
    fetchProducts()
  },
)

// 監聽網址上的篩選條件
watch(
  () => [route.query.categoryId, route.query.subcategoryId, route.query.brandId],
  () => {
    fetchProducts()
  },
)

// 第一次進入頁面時取得商品
onMounted(() => {
  fetchProducts()
})
</script>

<template>
  <main class="product-list-page">
    <div class="container py-5">
      <!-- 標題 -->
      <div class="mb-4">
        <h1 class="page-title">商品列表</h1>
      </div>

      <!-- 每頁顯示 -->
      <div class="product-list-toolbar">
        <div class="product-count">共 {{ totalElements }} 件商品</div>

        <div class="page-size-selector">
          <label for="page-size">每頁顯示：</label>

          <select id="page-size" v-model.number="pageSize" @change="changePageSize">
            <option :value="2">2(測試用)</option>
            <option :value="12">12</option>
            <option :value="24">24</option>
          </select>
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
</style>
