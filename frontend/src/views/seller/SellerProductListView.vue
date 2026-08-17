<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { disableSellerProduct, getSellerProducts } from '../../api/sellerProductApi'
import { getCurrentSellerId } from '@/utils/seller-session'

const sellerId = getCurrentSellerId()
// TODO: 等 A/B 模組提供 current seller 商品 API 後，移除 sellerId 假資料。
const products = ref([])

const isLoading = ref(false)
const errorMessage = ref('')

const loadProducts = async () => {
  isLoading.value = true
  errorMessage.value = ''

  try {
    //等 A/B 模組完成後，這兩處應改成：
    // const response = await getSellerProducts()
    // await disableSellerProduct(productId)
    const response = await getSellerProducts(sellerId)

    products.value = response.data
  } catch (error) {
    errorMessage.value = '商品資料載入失敗，請確認後端商品 API 是否已啟動。'
  } finally {
    isLoading.value = false
  }
}

const handleDisable = async (productId) => {
  const confirmed = window.confirm('確定要下架這項商品嗎？')

  if (!confirmed) {
    return
  }

  try {
    await disableSellerProduct(sellerId, productId)
    await loadProducts()
  } catch (error) {
    errorMessage.value = '商品下架失敗，請稍後再試。'
  }
}

const formatCurrency = (price) => `NT$${Number(price ?? 0).toLocaleString()}`

const stockText = (stock) => stock ?? 0

const statusLabel = (status) => {
  if (status === 'ACTIVE') {
    return '上架中'
  }
  if (status === 'INACTIVE') {
    return '已下架'
  }
  return '草稿'
}

const statusClass = (status) => {
  if (status === 'ACTIVE') {
    return 'is-active'
  }
  if (status === 'INACTIVE') {
    return 'is-disabled'
  }
  return 'is-warning'
}

onMounted(loadProducts)
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">商品管理</p>
        <h1>賣家商品列表</h1>
      </div>
    </header>

    <p v-if="errorMessage" class="error-message">
      {{ errorMessage }}
    </p>

    <section class="table-panel">
      <div class="table-header">
        <span>商品名稱</span>
        <span>價格</span>
        <span>庫存</span>
        <span>狀態</span>
        <span>操作</span>
      </div>

      <p v-if="isLoading" class="state-message">商品資料載入中...</p>

      <p v-else-if="products.length === 0" class="state-message">目前沒有商品資料。</p>

      <div v-for="product in products" v-else :key="product.productId" class="product-row">
        <div class="product-name">
          <strong>{{ product.productName }}</strong>
          <span>#SP-{{ String(product.productId).padStart(4, '0') }}</span>
        </div>
        <span>{{ formatCurrency(product.basePrice) }}</span>
        <span>{{ stockText(product.stock) }}</span>
        <span class="status-badge" :class="statusClass(product.status)">
          {{ statusLabel(product.status) }}
        </span>
        <div class="row-actions">
          <RouterLink class="secondary-action" :to="`/seller/products/${product.productId}/edit`">
            編輯
          </RouterLink>
          <button
            class="danger-action"
            type="button"
            :disabled="product.status === 'INACTIVE'"
            @click="handleDisable(product.productId)"
          >
            下架
          </button>
        </div>
      </div>
    </section>
  </section>
</template>

<style scoped>
.seller-page {
  display: grid;
  gap: var(--space-5);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.eyebrow {
  margin: 0 0 var(--space-1);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

h1 {
  margin: 0;
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}

.secondary-action,
.danger-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  font-weight: 600;
  text-decoration: none;
}

.secondary-action {
  color: var(--color-primary-700);
  background: var(--color-primary-soft);
}

.danger-action {
  border: 1px solid var(--color-danger);
  color: var(--color-danger);
  background: var(--color-danger-soft);
}

.danger-action:disabled {
  border-color: var(--color-disabled);
  color: var(--color-text-subtle);
  background: var(--color-disabled-bg);
  cursor: not-allowed;
}

.secondary-action:focus-visible,
.danger-action:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.error-message {
  margin: 0;
  border: 1px solid var(--color-danger);
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.table-panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.table-header,
.product-row {
  display: grid;
  grid-template-columns: minmax(220px, 2fr) 1fr 1fr 1fr 160px;
  gap: var(--space-3);
  align-items: center;
}

.table-header {
  padding: var(--space-4) var(--space-5);
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.product-row {
  min-height: 72px;
  padding: 0 var(--space-5);
  border-top: 1px solid var(--color-border);
  color: var(--color-text-700);
  font-size: var(--font-size-sm);
}

.product-name {
  display: grid;
  gap: var(--space-1);
}

.product-name strong {
  color: var(--color-text);
  font-size: var(--font-size-base);
}

.product-name span,
.state-message {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.state-message {
  margin: 0;
  padding: var(--space-6);
}

.status-badge {
  width: fit-content;
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  border-radius: var(--radius-pill);
  padding: 0 var(--space-3);
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.status-badge.is-active {
  background: var(--color-success-soft);
  color: var(--color-success);
}

.status-badge.is-warning {
  background: var(--color-warning-soft);
  color: var(--color-warning);
}

.status-badge.is-disabled {
  background: var(--color-disabled-bg);
  color: var(--color-text-subtle);
}

.row-actions {
  display: flex;
  gap: var(--space-2);
}

@media (max-width: 900px) {
  .table-header {
    display: none;
  }

  .product-row {
    grid-template-columns: 1fr 1fr;
    gap: var(--space-2);
    padding: var(--space-4) var(--space-5);
  }

  .product-name,
  .row-actions {
    grid-column: 1 / -1;
  }
}

@media (max-width: 680px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
