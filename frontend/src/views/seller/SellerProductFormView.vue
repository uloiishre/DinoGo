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
  updateSellerProductMainImage,
  updateSellerProductImageSort,
  deleteSellerProductImage,
  uploadSellerProductImages,
} from '@/api/sellerProductApi'
import { getCurrentSellerId } from '@/utils/seller-session'
import { logSafeError } from '@/utils/safeError'
import { getImageUrl } from '@/utils/imageUrl'

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
const selectedMainImageId = ref(null)
const selectedImageIndex = ref(null)
const selectedImageId = ref(null)
const selectedImageFiles = ref([])
const newImagePreviews = ref([])
const selectedNewMainIndex = ref(0)
const selectedNewImageIndex = ref(0)

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

  // 商品既有圖片
  images: [],
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
  const files = Array.from(event.target.files || [])

  if (files.length === 0) {
    return
  }

  // 清除之前建立的預覽 URL
  newImagePreviews.value.forEach((preview) => {
    URL.revokeObjectURL(preview.url)
  })

  selectedImageFiles.value = files

  newImagePreviews.value = files.map((file) => ({
    file,
    url: URL.createObjectURL(file),
  }))

  // 上方大圖先預覽第一張
  imagePreviewUrl.value = newImagePreviews.value[0]?.url ?? ''

  selectedImageName.value = files.length === 1 ? files[0].name : `已選擇 ${files.length} 張圖片`
}

const moveNewImage = (index, direction) => {
  if (index === null) {
    return
  }

  const newIndex = index + direction

  if (newIndex < 0 || newIndex >= newImagePreviews.value.length) {
    return
  }

  const previews = [...newImagePreviews.value]

  const temp = previews[index]
  previews[index] = previews[newIndex]
  previews[newIndex] = temp

  newImagePreviews.value = previews

  // 上傳檔案順序同步
  selectedImageFiles.value = previews.map((preview) => preview.file)

  // 如果移動的是主圖，主圖 index 跟著圖片移動
  if (selectedNewMainIndex.value === index) {
    selectedNewMainIndex.value = newIndex
  } else if (selectedNewMainIndex.value === newIndex) {
    selectedNewMainIndex.value = index
  }

  // 目前選取圖片也跟著移動
  selectedNewImageIndex.value = newIndex
}
const setNewMainImage = () => {
  if (selectedNewImageIndex.value === null) {
    return
  }

  selectedNewMainIndex.value = selectedNewImageIndex.value
}

const changeMainImage = async (imageId) => {
  console.log('changeMainImage 開始：', imageId)
  try {
    await updateSellerProductMainImage(productId.value, imageId)

    selectedMainImageId.value = imageId

    // 同步前端目前主圖狀態
    form.images.forEach((image) => {
      image.isMain = image.imageId === imageId
    })

    // 更新上方主圖預覽
    const mainImage = form.images.find((image) => image.imageId === imageId)

    if (mainImage) {
      form.imageUrl = mainImage.imageUrl
    }
  } catch (error) {
    logSafeError('Update main image failed:', error)
    errorMessage.value = '設定主圖失敗。'
  }
}

const selectNewImage = (index) => {
  selectedNewImageIndex.value = index

  // 上方大圖顯示目前選取的待上傳圖片
  imagePreviewUrl.value = newImagePreviews.value[index]?.url ?? ''
}

const setSelectedImageAsMain = async () => {
  if (selectedImageIndex.value === null) {
    return
  }

  const image = form.images[selectedImageIndex.value]

  if (!image) {
    return
  }

  // 已經是主圖就不用再送 API
  if (image.imageId === selectedMainImageId.value) {
    return
  }

  await changeMainImage(image.imageId)
}

const selectImage = (index) => {
  selectedImageIndex.value = index

  const image = form.images[index]

  selectedImageId.value = image.imageId

  // 上方大圖預覽切成目前選取圖片
  form.imageUrl = image.imageUrl
  imagePreviewUrl.value = ''
}

// 調整商品圖片順序
const moveImage = (index, direction) => {
  if (index === null) {
    return
  }

  const newIndex = index + direction

  if (newIndex < 0 || newIndex >= form.images.length) {
    return
  }

  const images = [...form.images]

  const temp = images[index]
  images[index] = images[newIndex]
  images[newIndex] = temp

  // sortOrder 統一從 1 開始
  images.forEach((image, imageIndex) => {
    image.sortOrder = imageIndex + 1
  })

  form.images = images

  // 移動後，選取狀態跟著圖片移動
  selectedImageIndex.value = newIndex
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

  images: [],
})

const fillProductForm = (product) => {
  form.productName = product.productName ?? ''

  form.subcategoryId = product.subcategoryId ? String(product.subcategoryId) : ''

  form.brandId = product.brandId ? String(product.brandId) : ''

  form.basePrice = product.basePrice ?? ''
  form.description = product.description ?? ''
  form.status = toFormStatus(product.status)

  // 商品圖片
  form.images = [...(product.images ?? [])].sort(
    (a, b) => (a.sortOrder ?? 999) - (b.sortOrder ?? 999),
  )

  const mainImage = form.images.find((image) => image.isMain === true) ?? form.images[0] ?? null

  form.imageUrl = mainImage?.imageUrl ?? product.imageUrl ?? ''

  imagePreviewUrl.value = ''

  selectedMainImageId.value = mainImage?.imageId ?? null

  selectedImageIndex.value = mainImage
    ? form.images.findIndex((image) => image.imageId === mainImage.imageId)
    : form.images.length > 0
      ? 0
      : null
  selectedImageId.value = mainImage?.imageId ?? form.images[0]?.imageId ?? null

  // SKU
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
  // 原本程式繼續...
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
      // ① 編輯商品基本資料
      await updateSellerProduct(productId.value, buildProductPayload())

      // ② 修改 / 新增 / 停用 SKU
      await saveProductSkus()

      // ③ 儲存商品圖片排序
      if (form.images.length > 0) {
        await updateSellerProductImageSort(
          productId.value,
          form.images.map((image, index) => ({
            imageId: image.imageId,
            sortOrder: index + 1,
          })),
        )
      }
      // 上傳新選擇的商品圖片
      if (selectedImageFiles.value.length > 0) {
        await uploadSellerProductImages(productId.value, selectedImageFiles.value)
        newImagePreviews.value.forEach((preview) => {
          URL.revokeObjectURL(preview.url)
        })

        selectedImageFiles.value = []
        newImagePreviews.value = []
        imagePreviewUrl.value = ''
        selectedImageName.value = ''
      }
    } else {
      // ① 先建立商品
      const response = await createSellerProduct(buildCreatePayload())

      const newProductId = response.data.productId

      // ② 商品建立完成後，再上傳剛才選的多張圖片
      if (newProductId && selectedImageFiles.value.length > 0) {
        // 上傳圖片
        const uploadResponse = await uploadSellerProductImages(
          newProductId,
          selectedImageFiles.value,
        )

        // 取得剛剛上傳完成的圖片
        const uploadedImages = uploadResponse.data

        // 找出使用者選擇的主圖
        const mainImage = uploadedImages[selectedNewMainIndex.value]

        // 設定主圖
        if (mainImage) {
          await updateSellerProductMainImage(newProductId, mainImage.imageId)
        }
      }
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
const removeExistingImage = async () => {
  if (selectedImageId.value === null) {
    return
  }

  const selectedImage = form.images.find((image) => image.imageId === selectedImageId.value)

  if (!selectedImage) {
    return
  }

  // 前端先擋主圖
  if (selectedImage.imageId === selectedMainImageId.value) {
    errorMessage.value = '主圖不可直接刪除，請先設定其他圖片為主圖。'
    return
  }

  try {
    await deleteSellerProductImage(productId.value, selectedImage.imageId)

    form.images = form.images.filter((image) => image.imageId !== selectedImage.imageId)

    // 重新整理 sortOrder
    form.images.forEach((image, index) => {
      image.sortOrder = index + 1
    })

    selectedImageIndex.value = null
    selectedImageId.value = null
  } catch (error) {
    logSafeError('Delete product image failed:', error)
    errorMessage.value = '刪除圖片失敗。'
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

    <div class="product-form" @submit.prevent="handleSubmit" @keydown.enter.prevent>
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
              v-if="imagePreviewUrl || form.imageUrl"
              class="image-preview"
              :src="imagePreviewUrl || getImageUrl(form.imageUrl)"
              :alt="selectedImageName || '商品主圖'"
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
            multiple
            @change="handleImageSelect"
          />
          <!-- 新選擇、尚未上傳的圖片 -->
          <div v-if="newImagePreviews.length > 0" class="new-image-preview-section">
            <div class="image-list-header">
              <p class="image-list-title">待上傳圖片（{{ newImagePreviews.length }} 張）</p>

              <small> 點選圖片後可調整順序或設定主圖 </small>
            </div>

            <div class="image-thumbnail-strip">
              <button
                v-for="(preview, index) in newImagePreviews"
                :key="preview.url"
                type="button"
                class="image-thumbnail-button"
                :class="{
                  active: selectedNewImageIndex === index,
                  'is-main': selectedNewMainIndex === index,
                }"
                @click="selectNewImage(index)"
              >
                <img :src="preview.url" alt="待上傳商品圖片" class="existing-image-thumbnail" />

                <span v-if="selectedNewMainIndex === index" class="thumbnail-main-badge">
                  主圖
                </span>

                <span class="thumbnail-order">
                  {{ index + 1 }}
                </span>
              </button>
            </div>

            <div class="image-order-actions">
              <button
                type="button"
                :disabled="selectedNewImageIndex === null || selectedNewImageIndex === 0"
                @click="moveNewImage(selectedNewImageIndex, -1)"
              >
                ← 往左
              </button>

              <button
                type="button"
                :disabled="
                  selectedNewImageIndex === null ||
                  selectedNewImageIndex === newImagePreviews.length - 1
                "
                @click="moveNewImage(selectedNewImageIndex, 1)"
              >
                往右 →
              </button>

              <button
                type="button"
                :disabled="
                  selectedNewImageIndex === null || selectedNewImageIndex === selectedNewMainIndex
                "
                @click="setNewMainImage"
              >
                {{ selectedNewImageIndex === selectedNewMainIndex ? '目前主圖' : '設為主圖' }}
              </button>
            </div>
          </div>
          <!-- 編輯商品：既有商品圖片 -->
          <div v-if="isEditMode && form.images.length > 0" class="existing-image-list">
            <div class="image-list-header">
              <p class="image-list-title">目前商品圖片</p>
              <small>點選圖片後可調整順序或設定主圖</small>
            </div>

            <!-- 橫向縮圖 -->
            <div class="image-thumbnail-strip">
              <button
                v-for="(image, index) in form.images"
                :key="image.imageId"
                type="button"
                class="image-thumbnail-button"
                :class="{
                  active: selectedImageIndex === index,
                  'is-main': selectedMainImageId === image.imageId,
                }"
                @click="selectImage(index)"
              >
                <img
                  :src="getImageUrl(image.imageUrl)"
                  alt="商品圖片"
                  class="existing-image-thumbnail"
                />

                <span v-if="selectedMainImageId === image.imageId" class="thumbnail-main-badge">
                  主圖
                </span>

                <span class="thumbnail-order">
                  {{ index + 1 }}
                </span>
              </button>
            </div>

            <!-- 選取圖片後的統一操作區 -->
            <div v-if="selectedImageIndex !== null" class="image-order-actions">
              <button
                type="button"
                :disabled="selectedImageIndex === 0"
                @click="moveImage(selectedImageIndex, -1)"
              >
                ← 往左
              </button>

              <button
                type="button"
                :disabled="selectedImageIndex === form.images.length - 1"
                @click="moveImage(selectedImageIndex, 1)"
              >
                往右 →
              </button>

              <button
                type="button"
                :disabled="form.images[selectedImageIndex]?.imageId === selectedMainImageId"
                @click="setSelectedImageAsMain"
              >
                {{
                  form.images[selectedImageIndex]?.imageId === selectedMainImageId
                    ? '目前主圖'
                    : '設為主圖'
                }}
              </button>

              <button
                type="button"
                :disabled="selectedImageId === null || selectedImageId === selectedMainImageId"
                @click="removeExistingImage"
              >
                刪除圖片
              </button>
            </div>
          </div>
        </section>

        <div class="form-actions side-actions">
          <button type="button">儲存草稿</button>
          <button
            class="primary-button"
            type="button"
            :disabled="isSubmitting"
            @click="handleSubmit"
          >
            {{ isSubmitting ? '送出中...' : submitText }}
          </button>
        </div>
      </aside>
    </div>
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
  object-fit: contain;
  background: var(--color-bg-muted);
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

.existing-image-list {
  display: grid;
  gap: var(--space-3);
  margin-top: var(--space-2);
}

.image-list-title {
  margin: 0;
  color: var(--color-text-700);
  font-weight: 700;
}

.image-list-header {
  display: grid;
  gap: 4px;
}

.image-list-header small {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

/* 橫向圖片縮圖列 */
.image-thumbnail-strip {
  display: flex;
  gap: var(--space-2);

  width: 100%;
  padding-bottom: var(--space-2);

  overflow-x: auto;
}

/* 每一張縮圖 */
.image-thumbnail-button {
  position: relative;

  flex: 0 0 82px;

  width: 82px;
  height: 82px;
  min-height: 82px;

  padding: 4px;

  border: 2px solid transparent;
  border-radius: var(--radius-md);

  background: var(--color-bg-muted);

  cursor: pointer;
}

.image-thumbnail-button.active {
  border-color: var(--color-primary);
}

.image-thumbnail-button.is-main {
  box-shadow: 0 0 0 1px var(--color-primary);
}

.existing-image-thumbnail {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: var(--color-bg-muted);
}

/* 主圖標記 */
.thumbnail-main-badge {
  position: absolute;
  top: 4px;
  left: 4px;

  padding: 2px 5px;

  border-radius: 4px;

  background: var(--color-primary);
  color: white;

  font-size: 11px;
  font-weight: 700;
}

/* 圖片順序 */
.thumbnail-order {
  position: absolute;
  right: 4px;
  bottom: 4px;

  display: grid;
  place-items: center;

  width: 20px;
  height: 20px;

  border-radius: 50%;

  background: rgba(0, 0, 0, 0.65);
  color: white;

  font-size: 11px;
}

/* 圖片操作按鈕 */
.image-order-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.image-order-actions button {
  min-height: 34px;
  padding: 0 var(--space-3);
}

.image-order-actions button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.new-image-preview-section {
  display: grid;
  gap: var(--space-2);
}

.new-image-preview-item {
  position: relative;
  flex: 0 0 82px;
  width: 82px;
  height: 82px;
  padding: 4px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
}

.new-image-preview-item img {
  width: 100%;
  height: 100%;
  object-fit: contain;
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
