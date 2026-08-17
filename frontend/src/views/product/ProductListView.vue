<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import api from '@/api/axios'
import ProductCard from '@/views/product/ProductCard.vue'

const route = useRoute()

const products = ref([])
const loading = ref(false)
const errorMessage = ref('')

// 取得商品
const fetchProducts = async () => {
  try {
    loading.value = true
    errorMessage.value = ''

    const params = {}

    // 子分類篩選
    if (route.query.subcategoryId) {
      params.subcategoryId = route.query.subcategoryId
    }

    // 品牌篩選
    if (route.query.brandId) {
      params.brandId = route.query.brandId
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

    products.value = response.data
  } catch (error) {
    console.error('取得商品失敗：', error)
    errorMessage.value = '商品資料載入失敗'
  } finally {
    loading.value = false
  }
}

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
</style>
