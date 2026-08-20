<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createSellerProduct,
  getProductDetail,
  updateSellerProduct,
  updateSellerProductSku,
  createSellerProductSkus,
  disableSellerProductSku,
} from '@/api/sellerProductApi'
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

// 規格設定
const spec1Name = ref('')
const spec1Values = ref([''])

const hasSpec2 = ref(false)
const spec2Name = ref('')
const spec2Values = ref([''])

// 新增規格一的值
const addSpec1Value = () => {
  spec1Values.value.push('')
}

// 刪除規格一的值
const removeSpec1Value = (index) => {
  if (spec1Values.value.length <= 1) {
    return
  }

  spec1Values.value.splice(index, 1)
  generateSkuList()
}

// 新增第二規格
const addSpec2 = () => {
  hasSpec2.value = true
  spec2Values.value = ['']
  generateSkuList()
}

// 移除第二規格
const removeSpec2 = () => {
  hasSpec2.value = false
  spec2Name.value = ''
  spec2Values.value = ['']
  generateSkuList()
}

// 新增規格二的值
const addSpec2Value = () => {
  spec2Values.value.push('')
}

// 刪除規格二的值
const removeSpec2Value = (index) => {
  if (spec2Values.value.length <= 1) {
    return
  }

  spec2Values.value.splice(index, 1)
  generateSkuList()
}

// 自動產生 SKU
// 自動產生 SKU 候選組合
const generateSkuList = () => {
  const values1 = spec1Values.value.map((value) => value.trim()).filter((value) => value !== '')

  const values2 = spec2Values.value.map((value) => value.trim()).filter((value) => value !== '')

  const oldSkus = [...form.skus]
  const newSkus = []

  // 只有第一規格
  if (!hasSpec2.value) {
    for (const value1 of values1) {
      const oldSku = oldSkus.find((sku) => sku.spec1Value === value1 && !sku.spec2Value)

      newSkus.push({
        skuId: oldSku?.skuId,

        spec1Name: spec1Name.value.trim(),
        spec1Value: value1,

        spec2Name: null,
        spec2Value: null,

        // 新增商品：預設勾選
        // 編輯商品：資料庫原本存在才勾選
        enabled: oldSku?.enabled ?? (oldSku?.skuId ? true : !isEditMode.value),

        price: oldSku?.price ?? '',
        stock: oldSku?.stock ?? '',
        status: oldSku?.status ?? 1,
      })
    }
  } else {
    // 兩種規格
    for (const value1 of values1) {
      for (const value2 of values2) {
        const oldSku = oldSkus.find((sku) => sku.spec1Value === value1 && sku.spec2Value === value2)

        newSkus.push({
          skuId: oldSku?.skuId,

          spec1Name: spec1Name.value.trim(),
          spec1Value: value1,

          spec2Name: spec2Name.value.trim(),
          spec2Value: value2,

          // 新增商品：預設勾選
          // 編輯商品：既有 SKU 勾選，不存在的組合不勾
          enabled: oldSku?.enabled ?? (oldSku?.skuId ? true : !isEditMode.value),

          price: oldSku?.price ?? '',
          stock: oldSku?.stock ?? '',
          status: oldSku?.status ?? 1,
        })
      }
    }
  }

  form.skus = newSkus
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

// 商品基本資料
const buildProductPayload = () => ({
  sellerId,
  subcategoryId: Number(form.subcategoryId),
  brandId: Number(form.brandId),
  productName: form.productName.trim(),
  description: form.description.trim(),
  basePrice: Number(form.basePrice),
  status: form.status === 'ACTIVE' ? 1 : 2,
})

// 建立商品使用
const buildCreatePayload = () => ({
  ...buildProductPayload(),

  skus: form.skus
    .filter((sku) => sku.enabled)
    .map((sku) => ({
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

  const skus = product.skus ?? []

  form.skus = skus.length
    ? skus.map((sku) => ({
        skuId: sku.skuId,
        spec1Name: sku.spec1Name ?? '',
        spec1Value: sku.spec1Value ?? '',
        spec2Name: sku.spec2Name ?? null,
        spec2Value: sku.spec2Value ?? null,

        enabled: (sku.status ?? 1) === 1,

        price: sku.price ?? '',
        stock: sku.stock ?? '',
        status: sku.status ?? 1,
      }))
    : []

  if (skus.length === 0) {
    spec1Name.value = ''
    spec1Values.value = ['']
    hasSpec2.value = false
    spec2Name.value = ''
    spec2Values.value = ['']
    return
  }

  // 還原規格一
  spec1Name.value = skus[0].spec1Name ?? ''

  spec1Values.value = [...new Set(skus.map((sku) => sku.spec1Value).filter((value) => value))]

  // 判斷是否有第二規格
  hasSpec2.value = skus.some((sku) => sku.spec2Name && sku.spec2Value)

  if (hasSpec2.value) {
    spec2Name.value = skus.find((sku) => sku.spec2Name)?.spec2Name ?? ''

    spec2Values.value = [...new Set(skus.map((sku) => sku.spec2Value).filter((value) => value))]
  } else {
    spec2Name.value = ''
    spec2Values.value = ['']
  }

  // 依照既有規格值產生所有候選 SKU
  generateSkuList()
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

// 儲存商品 SKU
const saveProductSkus = async () => {
  // 既有而且目前有販售的 SKU
  const existingSkus = form.skus.filter((sku) => sku.skuId && sku.enabled)

  // 沒有 skuId，代表這次新產生的 SKU
  const newSkus = form.skus.filter((sku) => !sku.skuId && sku.enabled)

  // 原本存在，但現在取消販售
  const disabledSkus = form.skus.filter((sku) => sku.skuId && !sku.enabled && sku.status !== 0)

  // ① 修改既有 SKU
  for (const sku of existingSkus) {
    await updateSellerProductSku(productId.value, sku.skuId, {
      spec1Name: sku.spec1Name,
      spec1Value: sku.spec1Value,
      spec2Name: sku.spec2Name || null,
      spec2Value: sku.spec2Value || null,
      price: Number(sku.price),
      stock: Number(sku.stock),
      status: 1,
    })
  }

  // ② 批次新增新的 SKU
  if (newSkus.length > 0) {
    await createSellerProductSkus(
      productId.value,
      newSkus.map((sku) => ({
        spec1Name: sku.spec1Name,
        spec1Value: sku.spec1Value,
        spec2Name: sku.spec2Name || null,
        spec2Value: sku.spec2Value || null,
        price: Number(sku.price),
        stock: Number(sku.stock),
      })),
    )
  }

  // ③ 停用取消販售的 SKU
  for (const sku of disabledSkus) {
    await disableSellerProductSku(productId.value, sku.skuId)
  }
}

// 儲存商品
const handleSubmit = async () => {
  // 商品名稱驗證
  if (!form.productName.trim()) {
    errorMessage.value = '請輸入商品名稱。'
    return
  }

  // 分類、品牌驗證
  if (!form.subcategoryId || !form.brandId) {
    errorMessage.value = '請選擇商品分類與品牌。'
    return
  }

  // 商品基本價格驗證
  if (Number(form.basePrice) < 1) {
    errorMessage.value = '商品價格不可小於 1 元。'
    return
  }

  // 只驗證目前有勾選販售的 SKU
  const activeSkus = form.skus.filter((sku) => sku.enabled)

  if (!isEditMode.value && activeSkus.length === 0) {
    errorMessage.value = '新增商品至少需要一個販售規格。'
    return
  }

  if (activeSkus.some((sku) => Number(sku.price) < 1 || Number(sku.stock) < 0)) {
    errorMessage.value = 'SKU 價格不可小於 1 元，庫存不可小於 0。'
    return
  }

  errorMessage.value = ''

  try {
    isSubmitting.value = true

    if (isEditMode.value) {
      // 編輯商品基本資料
      await updateSellerProduct(productId.value, buildProductPayload())

      // 修改 / 新增 / 停用 SKU
      await saveProductSkus()
    } else {
      // 新增商品時 Product + SKU 一次建立
      await createSellerProduct(buildCreatePayload())
    }

    router.push('/seller/products')
  } catch (error) {
    logSafeError('Save seller product failed:', error)
    errorMessage.value = isEditMode.value
      ? '編輯商品失敗，請確認商品資料與 SKU 是否正確。'
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
          <input v-model="form.basePrice" type="number" min="1" placeholder="1" />
        </label>

        <label class="form-field full-width">
          商品描述
          <textarea v-model="form.description" placeholder="請輸入商品描述"></textarea>
        </label>

        <section class="sku-section full-width">
          <div class="section-header">
            <h2>商品規格 SKU</h2>
          </div>

          <!-- 規格一 -->
          <div class="spec-block">
            <div class="spec-title">
              <strong>規格一</strong>
            </div>

            <label class="form-field">
              規格名稱
              <input v-model="spec1Name" placeholder="例如：顏色" @input="generateSkuList" />
            </label>

            <div class="spec-values">
              <label>規格值</label>

              <div
                v-for="(value, index) in spec1Values"
                :key="`spec1-${index}`"
                class="spec-value-row"
              >
                <input
                  v-model="spec1Values[index]"
                  placeholder="例如：白色"
                  @input="generateSkuList"
                />

                <button
                  v-if="spec1Values.length > 1"
                  type="button"
                  @click="removeSpec1Value(index)"
                >
                  刪除
                </button>
              </div>

              <button type="button" @click="addSpec1Value">＋ 新增規格值</button>
            </div>
          </div>

          <!-- 新增第二規格 -->
          <button v-if="!hasSpec2" type="button" class="add-spec-button" @click="addSpec2">
            ＋ 新增第二規格
          </button>

          <!-- 規格二 -->
          <div v-if="hasSpec2" class="spec-block">
            <div class="spec-title">
              <strong>規格二</strong>

              <button type="button" @click="removeSpec2">移除規格二</button>
            </div>

            <label class="form-field">
              規格名稱
              <input v-model="spec2Name" placeholder="例如：容量" @input="generateSkuList" />
            </label>

            <div class="spec-values">
              <label>規格值</label>

              <div
                v-for="(value, index) in spec2Values"
                :key="`spec2-${index}`"
                class="spec-value-row"
              >
                <input
                  v-model="spec2Values[index]"
                  placeholder="例如：256GB"
                  @input="generateSkuList"
                />

                <button
                  v-if="spec2Values.length > 1"
                  type="button"
                  @click="removeSpec2Value(index)"
                >
                  刪除
                </button>
              </div>

              <button type="button" @click="addSpec2Value">＋ 新增規格值</button>
            </div>
          </div>

          <!-- 自動產生 SKU 組合 -->
          <div v-if="form.skus.length > 0" class="sku-combination-section">
            <h3>規格組合</h3>

            <div class="sku-table-wrapper">
              <table class="sku-table">
                <thead>
                  <tr>
                    <th>{{ spec1Name || '規格一' }}</th>

                    <th v-if="hasSpec2">
                      {{ spec2Name || '規格二' }}
                    </th>
                    <th class="checkbox-cell">販售</th>
                    <th class="checkbox-cell">價格</th>
                    <th class="checkbox-cell">庫存</th>
                  </tr>
                </thead>

                <tbody>
                  <tr
                    v-for="(sku, index) in form.skus"
                    :key="`${sku.spec1Value}-${sku.spec2Value || ''}`"
                  >
                    <td>{{ sku.spec1Value }}</td>

                    <td v-if="hasSpec2">
                      {{ sku.spec2Value }}
                    </td>
                    <td class="checkbox-cell">
                      <input v-model="form.skus[index].enabled" type="checkbox" />
                    </td>
                    <td>
                      <input
                        v-model="form.skus[index].price"
                        type="number"
                        min="1"
                        placeholder="價格"
                        :disabled="!sku.enabled"
                      />
                    </td>

                    <td>
                      <input
                        v-model="form.skus[index].stock"
                        type="number"
                        min="0"
                        placeholder="庫存"
                        :disabled="!sku.enabled"
                      />
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
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

.spec-block {
  display: grid;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.spec-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.spec-values {
  display: grid;
  gap: var(--space-2);
}

.spec-value-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--space-2);
}

.add-spec-button {
  justify-self: start;
}

.sku-combination-section {
  display: grid;
  gap: var(--space-3);
}

.sku-table-wrapper {
  width: 100%;
  overflow-x: auto;
}

.sku-table {
  width: 100%;
  border-collapse: collapse;
}

.sku-table th,
.sku-table td {
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  text-align: left;
}

.sku-table th {
  background: var(--color-bg-muted);
}

.sku-table input {
  min-width: 120px;
}

.checkbox-cell {
  text-align: center !important;
}

.sku-table input[type='checkbox'] {
  width: 14px;
  height: 14px;
  min-width: 14px;
  margin: 0;
  cursor: pointer;
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
