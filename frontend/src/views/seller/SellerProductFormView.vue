<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
// TODO: 等 B 模組 Product create API 完成後恢復 createSellerProduct 呼叫。
// import { createSellerProduct } from '@/api/sellerProductApi'

// const router = useRouter()

const isSubmitting = ref(false)
const errorMessage = ref('')

const createEmptySku = () => ({
  spec1Name: '',
  spec1Value: '',
  spec2Name: '',
  spec2Value: '',
  price: '',
  stock: '',
  status: 1,
})

const form = reactive({
  productName: '',
  subcategoryId: '',
  brandId: '',
  basePrice: '',
  description: '',
  skus: [createEmptySku()],
  status: 'ACTIVE',
  imageUrl: '',
})

const addSku = () => {
  form.skus.push(createEmptySku())
}

const handleSubmit = async () => {
  if (!form.productName.trim()) {
    errorMessage.value = '請輸入商品名稱。'
    return
  }

  if (!form.subcategoryId || !form.brandId) {
    errorMessage.value = '請選擇商品分類與品牌。'
    return
  }

  if (Number(form.basePrice) < 0) {
    errorMessage.value = '商品價格不可小於 0。'
    return
  }

  if (form.skus.some((sku) => Number(sku.price) < 0 || Number(sku.stock) < 0)) {
    errorMessage.value = 'SKU 價格與庫存不可小於 0。'
    return
  }
  errorMessage.value = ''

  const payload = {
    sellerId: 1,
    subcategoryId: Number(form.subcategoryId),
    brandId: Number(form.brandId),
    productName: form.productName.trim(),
    description: form.description.trim(),
    basePrice: Number(form.basePrice),
    status: form.status === 'ACTIVE' ? 1 : 0,
    skus: [
      {
        spec1Name: '規格',
        spec1Value: form.skuName.trim(),
        spec2Name: null,
        spec2Value: null,
        price: Number(form.basePrice),
        stock: Number(form.stock),
      },
    ],
    images: [
      {
        imageUrl: form.imageUrl.trim(),
        sortOrder: 0,
      },
    ],
  }

  console.log('等待 B 模組 API，預計送出的 payload:', payload)
  errorMessage.value = '等待 B 模組新增商品 API 完成後再開放送出。'
  return
  //   try {
  //     isSubmitting.value = true
  //     await createSellerProduct(payload)
  //     router.push('/seller/products')
  //   } catch (error) {
  //     console.error('Create seller product failed:', error)
  //     errorMessage.value = '新增商品失敗，請確認欄位是否正常。'

  //   } finally {
  //     isSubmitting.value = false
  //   }
}
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">商品管理</p>
        <h1>新增商品</h1>
      </div>
    </header>

    <form class="product-form" @submit.prevent="handleSubmit">
      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

      <label class="form-field">
        商品名稱
        <input v-model="form.productName" type="text" placeholder="請輸入商品名稱" />
      </label>

      <label class="form-field">
        商品分類
        <select v-model="form.subcategoryId">
          <option value="">請選擇分類</option>
          <option value="1">手機</option>
          <option value="2">筆電</option>
          <option value="3">周邊配件</option>
        </select>
      </label>

      <label class="form-field">
        品牌
        <select v-model="form.brandId">
          <option value="">請選擇品牌</option>
          <option value="1">Apple</option>
          <option value="2">Samsung</option>
          <option value="3">ASUS</option>
        </select>
      </label>

      <label class="form-field">
        基本售價
        <input v-model="form.basePrice" type="number" min="0" placeholder="0" />
      </label>

      <label class="form-field full-width">
        商品描述
        <textarea v-model="form.description" placeholder="請輸入商品描述"></textarea>
      </label>

      <section class="sku-section full-width">
        <div class="section-header">
          <h2>商品規格 SKU</h2>
          <button type="button" @click="addSku">新增 SKU</button>
        </div>

        <div v-for="(sku, index) in form.skus" :key="index" class="sku-row">
          <label class="form-field">
            規格 1 名稱
            <input v-model="sku.spec1Name" placeholder="例如：顏色、尺寸" />
          </label>

          <label class="form-field">
            規格 1 值
            <input v-model="sku.spec1Value" placeholder="例如：白、L" />
          </label>

          <label class="form-field">
            規格 2 名稱
            <input v-model="sku.spec2Name" placeholder="例如：容量、版本" />
          </label>

          <label class="form-field">
            規格 2 值
            <input v-model="sku.spec2Value" placeholder="例如：256GB、Pro" />
          </label>

          <label class="form-field">
            SKU 價格
            <input v-model="sku.price" type="number" min="0" />
          </label>

          <label class="form-field">
            SKU 庫存
            <input v-model="sku.stock" type="number" min="0" />
          </label>
        </div>
      </section>

      <label class="form-field">
        商品圖片
        <input v-model="form.imageUrl" type="text" placeholder="請輸入圖片 URL" />
      </label>

      <label class="form-field">
        狀態
        <select v-model="form.status">
          <option value="ACTIVE">上架</option>
          <option value="INACTIVE">下架</option>
        </select>
      </label>

      <div class="form-actions">
        <button type="button">儲存草稿</button>
        <button class="primary-button" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '送出中...' : '送出商品' }}
        </button>
      </div>
    </form>
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
}

.eyebrow {
  margin: 0 0 var(--space-1);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

h1,
h2 {
  margin: 0;
  font-family: var(--font-heading);
}

h1 {
  font-size: var(--font-size-xl);
}

h2 {
  font-size: var(--font-size-lg);
}

.product-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
  max-width: 860px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
}

.form-field {
  display: grid;
  gap: var(--space-2);
  color: var(--color-text-700);
  font-weight: 600;
}

.full-width {
  grid-column: 1 / -1;
}

.sku-section {
  display: grid;
  gap: var(--space-4);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.sku-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-4);
}

input,
select,
textarea {
  width: 100%;
  min-height: 40px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  color: var(--color-text);
  background: var(--color-surface);
}

textarea {
  min-height: 96px;
  padding: var(--space-3);
  resize: vertical;
}

.form-actions {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

button {
  min-height: 40px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  background: var(--color-surface);
  color: var(--color-text-700);
  font-weight: 600;
}

.primary-button {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: var(--color-surface);
}

.primary-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error-message {
  grid-column: 1 / -1;
  margin: 0;
  color: #b42318;
  font-weight: 600;
}

@media (max-width: 680px) {
  .product-form,
  .sku-row {
    grid-template-columns: 1fr;
  }
}
</style>
