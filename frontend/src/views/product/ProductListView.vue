<script setup>
import { onMounted, ref } from 'vue'
import api from '@/api/axios'

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
      <!-- 頁面標題 -->
      <div class="mb-4">
        <h1 class="page-title">商品列表</h1>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="text-center py-5">商品載入中...</div>

      <!-- Error -->
      <div v-else-if="errorMessage" class="error-message text-center py-5">
        {{ errorMessage }}
      </div>

      <!-- 沒有商品 -->
      <div v-else-if="products.length === 0" class="empty-message text-center py-5">
        目前沒有商品
      </div>

      <!-- 商品列表 -->
      <div v-else class="row g-4">
        <div v-for="product in products" :key="product.productId" class="col-6 col-md-4 col-lg-3">
          <RouterLink
            :to="{
              name: 'ProductDetail',
              params: { id: product.productId },
            }"
            class="product-card d-block h-100 text-decoration-none"
          >
            <div class="product-image-wrapper">
              <img
                v-if="product.imageUrl"
                :src="product.imageUrl"
                :alt="product.productName"
                class="product-image"
              />

              <div v-else class="product-image-placeholder">暫無圖片</div>
            </div>

            <div class="product-info">
              <h2 class="product-name">
                {{ product.productName }}
              </h2>

              <p class="product-price mb-0">NT$ {{ product.basePrice }}</p>
            </div>
          </RouterLink>
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

.product-card {
  overflow: hidden;
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.product-card:hover {
  color: var(--color-text);
  transform: translateY(-2px);
}

.product-card:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.product-image-wrapper {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: var(--color-surface-soft);
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-image-placeholder {
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;

  color: var(--color-text-subtle);
  font-size: var(--font-size-sm);
}

.product-info {
  padding: var(--space-4);
}

.product-name {
  margin-bottom: var(--space-2);

  color: var(--color-text);
  font-family: var(--font-body);
  font-size: var(--font-size-base);
  font-weight: 500;
}

.product-price {
  color: var(--color-primary);
  font-size: var(--font-size-md);
  font-weight: 700;
}

.error-message {
  color: var(--color-danger);
}

.empty-message {
  color: var(--color-text-muted);
}
</style>
