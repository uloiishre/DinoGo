<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'

const route = useRoute()

const product = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const selectedImage = ref('')
const selectedSpec1 = ref('')
const selectedSpec2 = ref('')
const quantity = ref(1)

const fetchProductDetail = async () => {
  try {
    const productId = route.params.id

    const response = await api.get(`/products/${productId}`)

    product.value = response.data

    // 如果商品有圖片，預設選第一張當主圖
    if (product.value.images?.length) {
      selectedImage.value = product.value.images[0].imageUrl
    }

    // 預設第一個 SKU
    if (product.value.skus?.length) {
      selectedSpec1.value = product.value.skus[0].spec1Value || ''
      selectedSpec2.value = product.value.skus[0].spec2Value || ''
    }
  } catch (error) {
    console.error('取得商品詳情失敗：', error)
    errorMessage.value = '商品資料載入失敗'
  } finally {
    loading.value = false
  }
}

const spec1Values = computed(() => {
  if (!product.value?.skus) {
    return []
  }

  return [...new Set(product.value.skus.map((sku) => sku.spec1Value).filter(Boolean))]
})
const spec2Values = computed(() => {
  if (!product.value?.skus) {
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

//使用此方法過濾只有spec1的資料，也就是spec2為null的
const spec2Name = computed(() => {
  if (!product.value?.skus) {
    return ''
  }

  const sku = product.value.skus.find(
    (sku) => sku.spec1Value === selectedSpec1.value && sku.spec2Name,
  )

  return sku?.spec2Name || ''
})

const selectedSku = computed(() => {
  if (!product.value?.skus) {
    return null
  }

  return product.value.skus.find((sku) => {
    const spec1Match = sku.spec1Value === selectedSpec1.value

    const spec2Match = !sku.spec2Value || sku.spec2Value === selectedSpec2.value

    return spec1Match && spec2Match
  })
})

const selectSpec1 = (value) => {
  selectedSpec1.value = value

  const matchingSku = product.value.skus.find((sku) => sku.spec1Value === value)

  selectedSpec2.value = matchingSku?.spec2Value || ''
}

watch(selectedSku, () => {
  quantity.value = 1
})

onMounted(() => {
  fetchProductDetail()
})
</script>
<template>
  <main class="product-detail-page">
    <div class="container py-5">
      <div v-if="loading" class="text-center py-5">商品載入中...</div>

      <div v-else-if="errorMessage" class="text-center py-5">
        {{ errorMessage }}
      </div>

      <div v-else-if="product">
        <div class="row g-5">
          <!-- 商品圖片 -->
          <div class="col-md-6">
            <!-- 主圖 -->
            <div class="main-image-wrapper">
              <img
                v-if="selectedImage"
                :src="selectedImage"
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
                :class="{ active: selectedImage === image.imageUrl }"
                @click="selectedImage = image.imageUrl"
              >
                <img :src="image.imageUrl" :alt="product.productName" class="thumbnail-image" />
              </button>
            </div>
          </div>

          <!-- 商品資訊 -->
          <div class="col-md-6">
            <div class="product-category mb-2">
              {{ product.categoryName }}
              /
              {{ product.subcategoryName }}
            </div>

            <h1 class="product-title">
              {{ product.productName }}
            </h1>

            <div class="product-brand mb-3">品牌：{{ product.brandName }}</div>

            <div class="product-price mb-4">
              NT$ {{ selectedSku ? selectedSku.price : product.basePrice }}
            </div>

            <div class="product-description mb-4">
              {{ product.description }}
            </div>

            <!-- 商品規格 -->
            <div v-if="product.skus?.length" class="product-specs">
              <!-- 規格一 -->
              <div v-if="spec1Values.length" class="spec-group">
                <div class="spec-title">
                  {{ product.skus[0].spec1Name }}
                </div>

                <div class="spec-options">
                  <button
                    v-for="value in spec1Values"
                    :key="value"
                    type="button"
                    class="spec-button"
                    :class="{ active: selectedSpec1 === value }"
                    @click="selectedSpec1 = value"
                  >
                    {{ value }}
                  </button>
                </div>
              </div>

              <!-- 規格二 -->
              <div v-if="spec2Values.length" class="spec-group">
                <div class="spec-title">
                  {{ spec2Name }}
                </div>

                <div class="spec-options">
                  <button
                    v-for="value in spec2Values"
                    :key="value"
                    type="button"
                    class="spec-button"
                    :class="{ active: selectedSpec1 === value }"
                    @click="selectedSpec1 = value"
                  >
                    {{ value }}
                  </button>
                </div>
              </div>

              <!-- 選中的 SKU -->
              <div v-if="selectedSku" class="selected-sku-info">
                <div class="sku-stock">庫存：{{ selectedSku.stock }}</div>
                <!-- 數量選擇 -->
                <div class="quantity-area">
                  <span>數量</span>

                  <button type="button" @click="quantity--" :disabled="quantity <= 1">-</button>

                  <span>{{ quantity }}</span>

                  <button
                    type="button"
                    @click="quantity++"
                    :disabled="quantity >= selectedSku.stock"
                  >
                    +
                  </button>

                  <span>剩餘 {{ selectedSku.stock }} 件</span>
                </div>
              </div>
            </div>

            <div v-else class="text-muted">此商品目前沒有規格資料</div>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>
<style scoped>
.product-category {
  color: #777;
  font-size: 14px;
}

.product-title {
  font-size: 28px;
  font-weight: 600;
}

.product-brand {
  color: #666;
}

.product-price {
  font-size: 28px;
  font-weight: 700;
}

.product-description {
  line-height: 1.8;
  white-space: pre-line;
}

.product-specs {
  margin-top: 24px;
}

.spec-group {
  margin-bottom: 20px;
}

.spec-title {
  margin-bottom: 10px;
  font-weight: 600;
}

.spec-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.spec-button {
  min-width: 80px;
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: white;
  cursor: pointer;
}

.spec-button:hover {
  border-color: #198754;
}

.spec-button.active {
  border-color: #198754;
  background: #eaf5ee;
  color: #198754;
}

.selected-sku-info {
  margin-top: 24px;
}

.sku-price {
  font-size: 24px;
  font-weight: 700;
}

.sku-stock {
  margin-top: 6px;
  color: #666;
}

.main-image-wrapper {
  width: 100%;
  aspect-ratio: 1 / 1;
}

.product-main-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 12px;
}

.product-image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 12px;
  color: #999;
}

/* 縮圖區 */
.thumbnail-list {
  display: flex;
  gap: 10px;
  margin-top: 16px;
  overflow-x: auto;
}

/* 每一個縮圖按鈕 */
.thumbnail-button {
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  padding: 4px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
  cursor: pointer;
}

/* 目前選中的縮圖 */
.thumbnail-button.active {
  border: 2px solid #198754;
}

/* 縮圖本身 */
.thumbnail-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 4px;
}

.quantity-area {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
}

.quantity-area button {
  width: 36px;
  height: 36px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 6px;
  cursor: pointer;
}

.quantity-area button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
</style>
