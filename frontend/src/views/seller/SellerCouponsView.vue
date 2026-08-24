<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  activateSellerCoupon,
  createSellerCoupon,
  disableSellerCoupon,
  getSellerCoupons,
  updateSellerCoupon,
} from '@/api/sellerCouponApi'
import { getCurrentSellerId } from '@/utils/seller-session'

const sellerId = getCurrentSellerId()

const coupons = ref([])
const isLoading = ref(false)
const isSubmitting = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const editingCouponId = ref(null)
const isFormOpen = ref(false)
const selectedStatus = ref('ALL')
const searchKeyword = ref('')
const sellerRequiredMessage = '尚未取得賣家身分，請重新登入賣家帳號後再操作。'

const emptyForm = () => ({
  couponCode: '',
  couponName: '',
  discountType: 'AMOUNT',
  discountValue: '',
  minPurchaseAmount: '',
  startAt: '',
  endAt: '',
  limitCount: '',
  scopeType: 'STORE',
  categoryId: '',
  productId: '',
})

const form = reactive(emptyForm())

const activeCoupons = computed(() => coupons.value.filter((coupon) => coupon.status === 'ACTIVE'))
const draftCoupons = computed(() => coupons.value.filter((coupon) => coupon.status === 'DRAFT'))
const disabledCoupons = computed(() =>
  coupons.value.filter((coupon) => coupon.status === 'DISABLED'),
)
const expiredCoupons = computed(() => coupons.value.filter((coupon) => coupon.status === 'EXPIRED'))
const usedCouponCount = computed(() =>
  coupons.value.reduce((sum, coupon) => sum + Number(coupon.usedCount ?? 0), 0),
)

const statItems = computed(() => [
  {
    label: '啟用中',
    value: activeCoupons.value.length,
    note: '顧客目前可使用',
    icon: 'bi-lightning-charge',
  },
  {
    label: '草稿',
    value: draftCoupons.value.length,
    note: '尚未公開',
    icon: 'bi-pencil-square',
  },
  {
    label: '已使用',
    value: usedCouponCount.value,
    note: '累計核銷張數',
    icon: 'bi-receipt',
  },
  {
    label: '已停用/過期',
    value: disabledCoupons.value.length + expiredCoupons.value.length,
    note: '需檢查或重開',
    icon: 'bi-pause-circle',
  },
])

const statusTabs = computed(() => [
  { label: '全部', value: 'ALL', count: coupons.value.length },
  { label: '草稿', value: 'DRAFT', count: draftCoupons.value.length },
  { label: '啟用中', value: 'ACTIVE', count: activeCoupons.value.length },
  { label: '已停用', value: 'DISABLED', count: disabledCoupons.value.length },
  { label: '已過期', value: 'EXPIRED', count: expiredCoupons.value.length },
])

const filteredCoupons = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()

  return coupons.value.filter((coupon) => {
    const matchesStatus = selectedStatus.value === 'ALL' || coupon.status === selectedStatus.value
    const matchesKeyword =
      !keyword ||
      coupon.couponCode?.toLowerCase().includes(keyword) ||
      coupon.couponName?.toLowerCase().includes(keyword)

    return matchesStatus && matchesKeyword
  })
})

const isEditMode = computed(() => editingCouponId.value !== null)
const drawerTitle = computed(() => (isEditMode.value ? '編輯優惠券' : '建立優惠券'))
const submitText = computed(() => (isEditMode.value ? '儲存優惠券' : '建立優惠券'))

const formatCurrency = (value) => `NT$${Number(value ?? 0).toLocaleString()}`

const discountText = (coupon) => {
  if (coupon.discountType === 'PERCENT') {
    return `${Number(coupon.discountValue)}% 折扣`
  }

  return `折 ${formatCurrency(coupon.discountValue)}`
}

const scopeText = (coupon) => {
  if (coupon.scopeType === 'CATEGORY') {
    return `指定分類 #${coupon.categoryId}`
  }
  if (coupon.scopeType === 'PRODUCT') {
    return `指定商品 #${coupon.productId}`
  }
  if (coupon.scopeType === 'ALL') {
    return '全平台'
  }
  return '全店商品'
}

const statusText = (status) => {
  if (status === 'ACTIVE') {
    return '啟用中'
  }
  if (status === 'DISABLED') {
    return '已停用'
  }
  if (status === 'EXPIRED') {
    return '已過期'
  }
  return '草稿'
}

const statusClass = (status) => {
  if (status === 'ACTIVE') {
    return 'is-active'
  }
  if (status === 'DISABLED') {
    return 'is-disabled'
  }
  if (status === 'EXPIRED') {
    return 'is-expired'
  }
  return 'is-draft'
}

const formatDateTime = (value) => {
  if (!value) {
    return '未設定'
  }

  return new Date(value).toLocaleString('zh-TW', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const toInputDateTime = (value) => {
  if (!value) {
    return ''
  }

  return value.slice(0, 16)
}

const normalizePayload = () => ({
  couponCode: form.couponCode.trim(),
  couponName: form.couponName.trim(),
  discountType: form.discountType,
  discountValue: Number(form.discountValue),
  minPurchaseAmount: form.minPurchaseAmount === '' ? null : Number(form.minPurchaseAmount),
  startAt: form.startAt,
  endAt: form.endAt,
  limitCount: form.limitCount === '' ? null : Number(form.limitCount),
  scopeType: form.scopeType,
  categoryId: form.scopeType === 'CATEGORY' ? Number(form.categoryId) : null,
  productId: form.scopeType === 'PRODUCT' ? Number(form.productId) : null,
})

const validateForm = () => {
  if (!isEditMode.value && !form.couponCode.trim()) {
    return '請輸入優惠券代碼。'
  }
  if (!form.couponName.trim()) {
    return '請輸入優惠券名稱。'
  }
  if (Number(form.discountValue) <= 0) {
    return '折扣數值必須大於 0。'
  }
  if (!form.startAt || !form.endAt) {
    return '請設定活動開始與結束時間。'
  }
  if (new Date(form.endAt) <= new Date(form.startAt)) {
    return '結束時間必須晚於開始時間。'
  }
  if (form.limitCount !== '' && Number(form.limitCount) <= 0) {
    return '發放上限必須大於 0。'
  }
  if (form.scopeType === 'CATEGORY' && !form.categoryId) {
    return '請選擇或填寫指定分類。'
  }
  if (form.scopeType === 'PRODUCT' && !form.productId) {
    return '請選擇或填寫指定商品。'
  }
  return ''
}

const resetForm = () => {
  Object.assign(form, emptyForm())
  editingCouponId.value = null
  successMessage.value = ''
  errorMessage.value = ''
}

const openCreateForm = () => {
  if (!sellerId) {
    errorMessage.value = sellerRequiredMessage
    successMessage.value = ''
    return
  }

  resetForm()
  isFormOpen.value = true
}

const closeForm = () => {
  isFormOpen.value = false
  resetForm()
}

const loadCoupons = async () => {
  errorMessage.value = ''

  if (!sellerId) {
    coupons.value = []
    errorMessage.value = sellerRequiredMessage
    return
  }

  isLoading.value = true

  try {
    const response = await getSellerCoupons(sellerId)
    coupons.value = response.data
  } catch (error) {
    errorMessage.value = '優惠券資料載入失敗，請確認後端 API 是否已啟動。'
  } finally {
    isLoading.value = false
  }
}

const editCoupon = (coupon) => {
  if (!sellerId) {
    errorMessage.value = sellerRequiredMessage
    successMessage.value = ''
    return
  }

  editingCouponId.value = coupon.couponId
  successMessage.value = ''
  errorMessage.value = ''
  Object.assign(form, {
    couponCode: coupon.couponCode ?? '',
    couponName: coupon.couponName ?? '',
    discountType: coupon.discountType ?? 'AMOUNT',
    discountValue: coupon.discountValue ?? '',
    minPurchaseAmount: coupon.minPurchaseAmount ?? '',
    startAt: toInputDateTime(coupon.startAt),
    endAt: toInputDateTime(coupon.endAt),
    limitCount: coupon.limitCount ?? '',
    scopeType: coupon.scopeType ?? 'STORE',
    categoryId: coupon.categoryId ?? '',
    productId: coupon.productId ?? '',
  })
  isFormOpen.value = true
}

const submitCoupon = async () => {
  if (!sellerId) {
    errorMessage.value = sellerRequiredMessage
    successMessage.value = ''
    return
  }

  const validationMessage = validateForm()
  if (validationMessage) {
    errorMessage.value = validationMessage
    successMessage.value = ''
    return
  }

  const payload = normalizePayload()
  const updatePayload = { ...payload }
  delete updatePayload.couponCode

  try {
    isSubmitting.value = true
    errorMessage.value = ''
    if (isEditMode.value) {
      await updateSellerCoupon(sellerId, editingCouponId.value, updatePayload)
      successMessage.value = '優惠券已更新。'
    } else {
      await createSellerCoupon(sellerId, payload)
      successMessage.value = '優惠券已建立為草稿。'
    }
    await loadCoupons()
    closeForm()
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message || '優惠券儲存失敗，請確認欄位與代碼是否正確。'
  } finally {
    isSubmitting.value = false
  }
}

const setCouponActive = async (coupon) => {
  if (!sellerId) {
    errorMessage.value = sellerRequiredMessage
    successMessage.value = ''
    return
  }

  try {
    errorMessage.value = ''
    await activateSellerCoupon(sellerId, coupon.couponId)
    successMessage.value = `${coupon.couponName} 已啟用。`
    await loadCoupons()
  } catch (error) {
    errorMessage.value = '啟用優惠券失敗，請稍後再試。'
  }
}

const setCouponDisabled = async (coupon) => {
  if (!sellerId) {
    errorMessage.value = sellerRequiredMessage
    successMessage.value = ''
    return
  }

  try {
    errorMessage.value = ''
    await disableSellerCoupon(sellerId, coupon.couponId)
    successMessage.value = `${coupon.couponName} 已停用。`
    await loadCoupons()
  } catch (error) {
    errorMessage.value = '停用優惠券失敗，請稍後再試。'
  }
}

onMounted(loadCoupons)
</script>

<template>
  <section class="seller-page coupon-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Marketing Center</p>
        <h1>優惠券管理</h1>
        <p class="page-description">建立、啟用與追蹤店鋪優惠活動。</p>
      </div>

      <div class="header-actions">
        <button class="secondary-button" type="button" @click="loadCoupons">
          <i class="bi bi-arrow-clockwise" aria-hidden="true"></i>
          重新整理
        </button>
        <button class="primary-button" type="button" @click="openCreateForm">
          <i class="bi bi-plus-lg" aria-hidden="true"></i>
          建立優惠券
        </button>
      </div>
    </header>

    <div class="summary-grid">
      <article v-for="item in statItems" :key="item.label" class="summary-card">
        <i class="bi" :class="item.icon" aria-hidden="true"></i>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.note }}</em>
        </div>
      </article>
    </div>

    <section class="coupon-panel">
      <div class="panel-toolbar">
        <div class="status-tabs" aria-label="優惠券狀態篩選">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            type="button"
            :class="{ active: selectedStatus === tab.value }"
            @click="selectedStatus = tab.value"
          >
            <span>{{ tab.label }}</span>
            <strong>{{ tab.count }}</strong>
          </button>
        </div>

        <label class="search-field">
          <i class="bi bi-search" aria-hidden="true"></i>
          <input v-model="searchKeyword" type="search" placeholder="搜尋優惠券名稱或代碼" />
        </label>
      </div>

      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
      <p v-if="successMessage" class="success-message">{{ successMessage }}</p>
      <p v-if="isLoading" class="state-message">優惠券資料載入中...</p>
      <div v-else-if="coupons.length === 0" class="empty-state">
        <i class="bi bi-ticket-perforated" aria-hidden="true"></i>
        <strong>尚未建立優惠券</strong>
        <span>建立第一張店鋪優惠券，讓買家在結帳時更容易完成下單。</span>
        <button class="primary-button" type="button" @click="openCreateForm">建立優惠券</button>
      </div>
      <div v-else-if="filteredCoupons.length === 0" class="empty-state">
        <i class="bi bi-search" aria-hidden="true"></i>
        <strong>沒有符合條件的優惠券</strong>
        <span>調整狀態篩選或搜尋關鍵字後再試一次。</span>
      </div>

      <div v-else class="coupon-table" role="table" aria-label="店鋪優惠券列表">
        <div class="coupon-table-head" role="row">
          <span>優惠券</span>
          <span>折扣與門檻</span>
          <span>期間</span>
          <span>使用狀態</span>
          <span>操作</span>
        </div>

        <article
          v-for="coupon in filteredCoupons"
          :key="coupon.couponId"
          class="coupon-row"
          role="row"
        >
          <div class="coupon-main">
            <span class="coupon-code">{{ coupon.couponCode }}</span>
            <h2>{{ coupon.couponName }}</h2>
            <p>{{ scopeText(coupon) }}</p>
          </div>

          <div class="coupon-cell">
            <strong>{{ discountText(coupon) }}</strong>
            <span>滿 {{ formatCurrency(coupon.minPurchaseAmount) }} 可用</span>
          </div>

          <div class="coupon-cell">
            <strong>{{ formatDateTime(coupon.startAt) }}</strong>
            <span>至 {{ formatDateTime(coupon.endAt) }}</span>
          </div>

          <div class="coupon-cell">
            <span class="status-badge" :class="statusClass(coupon.status)">
              {{ statusText(coupon.status) }}
            </span>
            <span>已使用 {{ coupon.usedCount ?? 0 }} / {{ coupon.limitCount ?? '不限' }}</span>
          </div>

          <div class="coupon-actions">
            <button type="button" @click="editCoupon(coupon)">編輯</button>
            <button
              v-if="coupon.status !== 'ACTIVE'"
              class="primary-button"
              type="button"
              @click="setCouponActive(coupon)"
            >
              <i class="bi bi-play-fill" aria-hidden="true"></i>
              啟用
            </button>
            <button v-else class="danger-button" type="button" @click="setCouponDisabled(coupon)">
              <i class="bi bi-pause-fill" aria-hidden="true"></i>
              停用
            </button>
          </div>
        </article>
      </div>
    </section>

    <div v-if="isFormOpen" class="drawer-backdrop" @click.self="closeForm">
      <aside class="coupon-drawer" aria-label="優惠券表單">
        <div class="drawer-header">
          <div>
            <p class="eyebrow">{{ isEditMode ? 'Edit Coupon' : 'New Coupon' }}</p>
            <h2>{{ drawerTitle }}</h2>
          </div>
          <button class="icon-button" type="button" aria-label="關閉優惠券表單" @click="closeForm">
            <i class="bi bi-x-lg" aria-hidden="true"></i>
          </button>
        </div>

        <form class="coupon-form" @submit.prevent="submitCoupon">
          <section class="form-section">
            <h3>基本資訊</h3>
            <label class="form-field">
              優惠券代碼
              <input
                v-model="form.couponCode"
                type="text"
                :disabled="isEditMode"
                placeholder="WELCOME300"
              />
            </label>

            <label class="form-field">
              優惠券名稱
              <input v-model="form.couponName" type="text" placeholder="新會員滿額折抵" />
            </label>
          </section>

          <section class="form-section">
            <h3>折扣規則</h3>
            <div class="split-fields">
              <label class="form-field">
                折扣類型
                <select v-model="form.discountType">
                  <option value="AMOUNT">固定金額</option>
                  <option value="PERCENT">百分比</option>
                </select>
              </label>

              <label class="form-field">
                折扣數值
                <input
                  v-model="form.discountValue"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="300"
                />
              </label>
            </div>

            <label class="form-field">
              最低消費
              <input
                v-model="form.minPurchaseAmount"
                type="number"
                min="0"
                step="1"
                placeholder="2000"
              />
            </label>
          </section>

          <section class="form-section">
            <h3>適用範圍</h3>
            <div class="split-fields">
              <label class="form-field">
                適用範圍
                <select v-model="form.scopeType">
                  <option value="STORE">全店商品</option>
                  <option value="ALL">全平台</option>
                  <option value="CATEGORY">指定分類</option>
                  <option value="PRODUCT">指定商品</option>
                </select>
              </label>

              <label class="form-field">
                發放上限
                <input v-model="form.limitCount" type="number" min="1" placeholder="不限" />
              </label>
            </div>

            <label v-if="form.scopeType === 'CATEGORY'" class="form-field">
              指定分類
              <input
                v-model="form.categoryId"
                type="number"
                min="1"
                placeholder="暫以 Category ID 測試"
              />
            </label>

            <label v-if="form.scopeType === 'PRODUCT'" class="form-field">
              指定商品
              <input
                v-model="form.productId"
                type="number"
                min="1"
                placeholder="暫以 Product ID 測試"
              />
            </label>
          </section>

          <section class="form-section">
            <h3>活動期間</h3>
            <label class="form-field">
              開始時間
              <input v-model="form.startAt" type="datetime-local" />
            </label>

            <label class="form-field">
              結束時間
              <input v-model="form.endAt" type="datetime-local" />
            </label>
          </section>

          <section class="coupon-preview">
            <span>買家看到的優惠</span>
            <strong>{{ form.couponName || '優惠券名稱' }}</strong>
            <p>
              {{
                form.discountType === 'PERCENT'
                  ? `${form.discountValue || 0}% 折扣`
                  : `折 NT$${form.discountValue || 0}`
              }}
              · 滿 {{ formatCurrency(form.minPurchaseAmount || 0) }} 可用
            </p>
          </section>

          <div class="form-actions">
            <button type="button" @click="resetForm">清空</button>
            <button class="primary-button" type="submit" :disabled="isSubmitting">
              {{ isSubmitting ? '儲存中...' : submitText }}
            </button>
          </div>
        </form>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.coupon-page {
  display: grid;
  width: 100%;
  max-width: 1280px;
  gap: var(--space-5);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.eyebrow {
  margin: 0 0 var(--space-1);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

h1,
h2,
h3,
p {
  margin-top: 0;
}

h1 {
  margin-bottom: 0;
  color: var(--color-text-900);
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}

h2,
h3 {
  margin-bottom: 0;
  color: var(--color-text-900);
  font-family: var(--font-heading);
}

h2 {
  font-size: var(--font-size-lg);
}

h3 {
  font-size: var(--font-size-base);
}

.page-description {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-4);
}

.summary-card {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-height: 104px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  background: var(--color-surface);
}

.summary-card > i {
  display: inline-flex;
  flex: 0 0 40px;
  width: 40px;
  height: 40px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: var(--color-primary-soft);
  color: var(--color-primary-active);
  font-size: 20px;
}

.summary-card div {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.summary-card span {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.summary-card strong {
  color: var(--color-text-900);
  font-size: 28px;
  line-height: 1;
}

.summary-card em {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-style: normal;
}

.header-actions,
.coupon-actions,
.form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-3);
}

.header-actions button,
.coupon-actions button,
.empty-state button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
}

.coupon-cell span,
.coupon-main p,
.coupon-preview span,
.coupon-preview p {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.coupon-panel {
  display: grid;
  gap: var(--space-4);
  padding: 0;
  overflow: hidden;
  border-radius: var(--radius-md);
}

.panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}

.status-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 4px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
}

.status-tabs button,
.secondary-button,
.primary-button,
.danger-button,
.coupon-actions button,
.form-actions button,
.icon-button {
  min-height: 38px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  background: var(--color-surface);
  color: var(--color-text-700);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.status-tabs button {
  gap: var(--space-2);
  min-height: 34px;
  border-color: transparent;
  padding: 0 var(--space-3);
  background: transparent;
}

.status-tabs button strong {
  min-width: 22px;
  min-height: 22px;
  display: inline-grid;
  place-items: center;
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}

.status-tabs button.active {
  border-color: var(--color-primary);
  background: var(--color-surface);
  color: var(--color-primary-active);
}

.status-tabs button.active strong {
  background: var(--color-primary-soft);
  color: var(--color-primary-active);
}

.primary-button {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: var(--color-surface);
}

.danger-button {
  border-color: var(--color-danger);
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.search-field {
  position: relative;
  width: min(100%, 320px);
  color: var(--color-text-muted);
}

.search-field i {
  position: absolute;
  top: 50%;
  left: var(--space-3);
  transform: translateY(-50%);
  font-size: var(--font-size-sm);
}

.search-field input {
  padding-left: 36px;
}

.error-message,
.success-message,
.state-message {
  margin: 0 var(--space-4);
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  font-weight: 700;
}

.error-message {
  border: 1px solid var(--color-danger);
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.success-message {
  border: 1px solid var(--color-success-bg);
  background: var(--color-success-soft);
  color: var(--color-success);
}

.state-message {
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
}

.coupon-table {
  overflow: hidden;
  border-top: 1px solid var(--color-border);
  border-radius: 0;
}

.coupon-table-head,
.coupon-row {
  display: grid;
  grid-template-columns:
    minmax(190px, 1.15fr) minmax(150px, 0.8fr) minmax(150px, 0.85fr) minmax(150px, 0.75fr)
    minmax(150px, auto);
  gap: var(--space-4);
  align-items: center;
}

.coupon-table-head {
  min-height: 48px;
  padding: 0 var(--space-4);
  background: #18251f;
  color: var(--color-surface);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.coupon-row {
  min-height: 92px;
  padding: var(--space-4);
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
}

.coupon-row:hover {
  background: var(--color-bg-muted);
}

.coupon-main,
.coupon-cell {
  display: grid;
  gap: var(--space-1);
  min-width: 0;
}

.coupon-main h2,
.coupon-main p {
  margin: 0;
}

.coupon-code {
  width: fit-content;
  border-radius: var(--radius-sm);
  padding: 3px 7px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: 800;
}

.coupon-cell strong {
  color: var(--color-text-900);
  font-size: var(--font-size-sm);
}

.status-badge {
  width: fit-content;
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  border-radius: var(--radius-pill);
  padding: 0 var(--space-3);
  font-size: var(--font-size-xs);
  font-weight: 700;
}

.status-badge.is-active {
  background: var(--color-success-soft);
  color: var(--color-success);
}

.status-badge.is-disabled {
  background: var(--color-disabled-bg);
  color: var(--color-text-subtle);
}

.status-badge.is-expired {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.status-badge.is-draft {
  background: var(--color-warning-soft);
  color: var(--color-warning);
}

.empty-state {
  min-height: 260px;
  display: grid;
  align-content: center;
  justify-items: center;
  gap: var(--space-3);
  margin: 0 var(--space-4) var(--space-4);
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
  text-align: center;
}

.empty-state i {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-primary);
  font-size: 24px;
}

.empty-state strong {
  color: var(--color-text-900);
  font-size: var(--font-size-base);
}

.empty-state span {
  max-width: 360px;
  font-size: var(--font-size-sm);
  line-height: 1.6;
}

.drawer-backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  justify-content: flex-end;
  background: rgba(20, 30, 26, 0.28);
}

.coupon-drawer {
  width: min(100vw, 720px);
  height: 100vh;
  overflow-y: auto;
  background: var(--color-surface);
  box-shadow: -12px 0 28px rgba(14, 22, 18, 0.16);
}

.drawer-header {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-5);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface);
}

.drawer-header::before {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  content: '';
  background: var(--color-primary);
}

.icon-button {
  width: 40px;
  padding: 0;
}

.coupon-form {
  display: grid;
  gap: var(--space-4);
  padding: var(--space-5);
}

.form-section {
  display: grid;
  gap: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  background: var(--color-surface);
}

.split-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(180px, 1fr));
  gap: var(--space-3);
}

.form-field {
  min-width: 0;
}

input,
select {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

@media (max-width: 680px) {
  .split-fields {
    grid-template-columns: 1fr;
  }
}

.form-field {
  display: grid;
  gap: var(--space-2);
  color: var(--color-text-700);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

input,
select {
  width: 100%;
  box-sizing: border-box;
  min-width: 0;
  min-height: 40px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
  font-weight: 400;
}

input:disabled {
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
}

input:focus,
select:focus,
button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.coupon-preview {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-4);
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}

.coupon-preview strong {
  color: var(--color-text-900);
  font-size: var(--font-size-base);
}

.coupon-preview p {
  margin-bottom: 0;
}

.form-actions {
  position: sticky;
  bottom: 0;
  margin: 0 calc(var(--space-5) * -1);
  padding: var(--space-4) var(--space-5) 0;
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
}

.form-actions button {
  flex: 1;
}

.primary-button:disabled {
  cursor: wait;
  opacity: 0.72;
}

@media (max-width: 1120px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .coupon-table-head {
    display: none;
  }

  .coupon-row {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .coupon-actions {
    justify-content: stretch;
  }

  .coupon-actions button {
    flex: 1;
  }
}

@media (max-width: 760px) {
  .page-header,
  .panel-toolbar,
  .header-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .summary-grid,
  .split-fields {
    grid-template-columns: 1fr;
  }

  .search-field,
  .header-actions button,
  .status-tabs button {
    width: 100%;
  }

  .coupon-drawer {
    width: 100vw;
  }
}
</style>
