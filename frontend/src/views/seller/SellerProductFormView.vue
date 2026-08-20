<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createSellerProduct, getProductDetail, updateSellerProduct } from '@/api/sellerProductApi'
import { getCurrentSellerId } from '@/utils/seller-session'
import { logSafeError } from '@/utils/safeError'

const sellerId = getCurrentSellerId()
const route = useRoute()
const router = useRouter()

const productId = computed(() => Number(route.params.id))
const isEditMode = computed(() => route.name === 'SellerProductEdit')
const isSubmitting = ref(false)
const isLoading = ref(false)
const errorMessage = ref('')
const imageFileInput = ref(null)
const imagePreviewUrl = ref('')
const selectedImageName = ref('')

const pageTitle = computed(() => (isEditMode.value ? '編輯商品' : '新增商品'))
const pageDescription = computed(() =>
  isEditMode.value ? '編輯既有商品資料與庫存' : '建立新的商品假資料',
)
const submitText = computed(() => (isEditMode.value ? '儲存變更' : '送出商品'))

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

const openImagePicker = () => {
  imageFileInput.value?.click()
}

const handleImageSelect = (event) => {
  const file = event.target.files?.[0]

  if (!file) {
    return
  }

  selectedImageName.value = file.name
  imagePreviewUrl.value = URL.createObjectURL(file)
}

const toFormStatus = (status) => {
  if (status === 1 || status === 'ACTIVE') {
    return 'ACTIVE'
  }
  return 'INACTIVE'
}

const buildPayload = () => ({
  sellerId,
  subcategoryId: Number(form.subcategoryId),
  brandId: Number(form.brandId),
  productName: form.productName.trim(),
  description: form.description.trim(),
  basePrice: Number(form.basePrice),
  status: form.status === 'ACTIVE' ? 1 : 2,
  skus: form.skus.map((sku) => ({
    spec1Name: sku.spec1Name.trim(),
    spec1Value: sku.spec1Value.trim(),
    spec2Name: sku.spec2Name?.trim() || null,
    spec2Value: sku.spec2Value?.trim() || null,
    price: Number(sku.price),
    stock: Number(sku.stock),
  })),
  images: form.imageUrl
    ? [
        {
          imageUrl: form.imageUrl.trim(),
          sortOrder: 1,
        },
      ]
    : [],
})

const fillProductForm = (product) => {
  form.productName = product.productName ?? ''
  form.subcategoryId = product.subcategoryId ? String(product.subcategoryId) : ''
  form.brandId = product.brandId ? String(product.brandId) : ''
  form.basePrice = product.basePrice ?? ''
  form.description = product.description ?? ''
  form.status = toFormStatus(product.status)
  form.imageUrl = product.images?.[0]?.imageUrl ?? product.imageUrl ?? ''
  imagePreviewUrl.value = ''
  form.skus = product.skus?.length
    ? product.skus.map((sku) => ({
        spec1Name: sku.spec1Name ?? '',
        spec1Value: sku.spec1Value ?? '',
        spec2Name: sku.spec2Name ?? '',
        spec2Value: sku.spec2Value ?? '',
        price: sku.price ?? '',
        stock: sku.stock ?? '',
        status: sku.status ?? 1,
      }))
    : [createEmptySku()]
}

const loadProduct = async () => {
  if (!isEditMode.value) {
    return
  }

  try {
    isLoading.value = true
    errorMessage.value = ''
    const response = await getProductDetail(productId.value)
    fillProductForm(response.data)
  } catch (error) {
    logSafeError('Load seller product failed:', error)
    errorMessage.value = '商品資料載入失敗，請確認商品是否存在。'
  } finally {
    isLoading.value = false
  }
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

  const payload = buildPayload()

  try {
    isSubmitting.value = true
    if (isEditMode.value) {
      await updateSellerProduct(productId.value, payload)
    } else {
      await createSellerProduct(payload)
    }
    router.push('/seller/products')
  } catch (error) {
    logSafeError('Save seller product failed:', error)
    errorMessage.value = isEditMode.value
      ? '編輯商品失敗，請確認 B 模組是否已提供商品修改 API。'
      : '新增商品失敗，請確認欄位是否正常。'
  } finally {
    isSubmitting.value = false
  }
}

onMounted(loadProduct)
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">商品管理</p>
        <h1>{{ pageTitle }}</h1>
        <p class="page-description">{{ pageDescription }}</p>
      </div>
    </header>

    <form class="product-form" @submit.prevent="handleSubmit">
      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
      <p v-if="isLoading" class="state-message">商品資料載入中...</p>

      <div class="product-main-fields">
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
      </div>

      <aside class="product-side-panel">
        <section class="image-section">
          <h2>商品圖片</h2>

          <button class="image-upload-button" type="button" @click="openImagePicker">
            <img
              v-if="imagePreviewUrl"
              class="image-preview"
              :src="imagePreviewUrl"
              :alt="selectedImageName || '點選上傳商品圖片'"
            />
            <div v-else class="image-placeholder">
              <i class="bi bi-image" aria-hidden="true"></i>
              <span>點選上傳</span>
              <small>商品圖片 placeholder</small>
            </div>
          </button>

          <input
            ref="imageFileInput"
            class="image-file-input"
            type="file"
            accept="image/*"
            @change="handleImageSelect"
          />
        </section>

        <div class="form-actions side-actions">
          <button type="button">儲存草稿</button>
          <button class="primary-button" type="submit" :disabled="isSubmitting">
            {{ isSubmitting ? '送出中...' : submitText }}
          </button>
        </div>
      </aside>
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

.page-description {
  margin: var(--space-1) 0 0;
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
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--space-5);
  width: 100%;
  max-width: 1180px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
}

.product-main-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
  align-content: start;
}

.product-side-panel {
  display: grid;
  align-content: start;
  gap: var(--space-4);
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

.image-section {
  display: grid;
  gap: var(--space-3);
}

.image-upload-button {
  width: 100%;
  min-height: 230px;
  border: 0;
  border-radius: var(--radius-md);
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--color-text-muted);
  background: var(--color-bg-muted);
  cursor: pointer;
  font: inherit;
  overflow: hidden;
}

.image-upload-button:hover {
  background: var(--color-disabled-bg);
}

.image-upload-button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.image-preview {
  width: 100%;
  height: 230px;
  object-fit: cover;
}

.image-placeholder {
  display: grid;
  justify-items: center;
  align-content: center;
  gap: var(--space-2);
  min-height: 100%;
  font-family: var(--font-body);
}

.image-placeholder i {
  color: var(--color-primary);
  font-size: 32px;
}

.image-placeholder span,
.image-placeholder small {
  display: block;
}

.image-placeholder span {
  color: var(--color-text-700);
  font-weight: 700;
}

.image-placeholder small {
  font-size: var(--font-size-sm);
}

.image-file-input {
  display: none;
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
  font: inherit;
  font-weight: 400;
}

textarea {
  min-height: 96px;
  padding: var(--space-3);
  resize: vertical;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

.side-actions {
  justify-content: stretch;
}

.side-actions button {
  flex: 1;
}

button {
  min-height: 40px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  background: var(--color-surface);
  color: var(--color-text-700);
  font: inherit;
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

.error-message,
.state-message {
  grid-column: 1 / -1;
  margin: 0;
}

.error-message {
  color: #b42318;
  font-weight: 600;
}

.state-message {
  color: var(--color-text-muted);
}

@media (max-width: 960px) {
  .product-form {
    grid-template-columns: 1fr;
  }

  .product-side-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .product-main-fields,
  .sku-row {
    grid-template-columns: 1fr;
  }

  .product-form {
    padding: var(--space-4);
  }

  .form-actions {
    flex-direction: column;
  }

  .form-actions button {
    width: 100%;
  }
}
</style>
