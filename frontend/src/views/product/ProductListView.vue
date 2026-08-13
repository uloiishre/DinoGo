<script setup>
import { onMounted, ref } from 'vue'
import api from '@/api/axios'
import ProductCard from '@/views/product/ProductCard.vue'

const products = ref([])
const loading = ref(true)
const errorMessage = ref('')

const fetchProducts = async () => {
  try {
    const response = await api.get('/products')
    products.value = response.data
  } catch (error) {
    console.error('取得商品列表失敗：', error)
    errorMessage.value = '商品載入失敗，請稍後再試'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchProducts()
})
</script>

<template>
  <main class="product-list-page">
    <div class="container py-5">
      <div class="mb-4">
        <h1 class="page-title">商品列表</h1>
      </div>

      <div v-if="loading" class="text-center py-5">商品載入中...</div>

      <div v-else-if="errorMessage" class="error-message text-center py-5">
        {{ errorMessage }}
      </div>

      <div v-else-if="products.length === 0" class="empty-message text-center py-5">
        目前沒有商品
      </div>

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
