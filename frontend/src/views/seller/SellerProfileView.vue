<script setup>
import { reactive, ref } from 'vue'

// TODO: 等 E 模組 Seller profile API 完成後，改為從後端載入與儲存店鋪資料。
const isSaving = ref(false)
const savedMessage = ref('')
const logoFileInput = ref(null)
const logoPreviewUrl = ref('')
const selectedLogoName = ref('')

const form = reactive({
  storeName: '森日選物',
  status: 'ACTIVE',
  description: '提供耐用、安靜且適合日常使用的生活選物。',
  email: 'hello@morihibi.example',
  phone: '02-2345-6789',
  city: '台北市',
  district: '中山區',
  address: '南京東路三段 100 號',
  serviceHours: '週一至週五 10:00 - 18:00',
  announcement: '出貨時間約 1-2 個工作天，遇假日順延。',
})

const openLogoPicker = () => {
  logoFileInput.value?.click()
}

const handleLogoSelect = (event) => {
  const file = event.target.files?.[0]

  if (!file) {
    return
  }

  selectedLogoName.value = file.name
  logoPreviewUrl.value = URL.createObjectURL(file)
}

const statusOptions = [
  { label: '營運中', value: 'ACTIVE' },
  { label: '暫停接單', value: 'PAUSED' },
  { label: '審核中', value: 'REVIEWING' },
]

const statusText = {
  ACTIVE: '營運中',
  PAUSED: '暫停接單',
  REVIEWING: '審核中',
}

const handleSave = () => {
  isSaving.value = true
  savedMessage.value = ''

  window.setTimeout(() => {
    isSaving.value = false
    savedMessage.value = '店鋪資料已暫存於前端展示版本。'
  }, 500)
}
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">店鋪資料</p>
        <h1>店鋪資料</h1>
        <p class="page-description">管理商家館公開資訊與營業設定。</p>
      </div>
    </header>

    <section class="status-panel" :class="`status-${form.status.toLowerCase()}`">
      <div class="status-copy">
        <span class="status-dot" aria-hidden="true"></span>
        <div>
          <p class="section-label">店鋪目前狀態</p>
          <strong>{{ statusText[form.status] }}</strong>
          <span>{{ form.status === 'ACTIVE' ? '顧客可以瀏覽並購買你的商品。' : '目前不會接收新的訂單。' }}</span>
        </div>
      </div>

      <button class="secondary-button" type="button">查看商家館</button>
    </section>

    <form class="profile-grid" @submit.prevent="handleSave">
      <section class="profile-card">
        <div class="card-heading">
          <div>
            <p class="section-kicker">公開資料</p>
            <h2>商家館資訊</h2>
          </div>
          <span class="visibility-note">顧客可見</span>
        </div>

        <label class="form-field full-width">
          店鋪名稱
          <input v-model="form.storeName" type="text" />
        </label>

        <section class="logo-section full-width" aria-labelledby="store-logo-title">
          <h3 id="store-logo-title">店鋪 Logo</h3>

          <button class="logo-upload-button" type="button" @click="openLogoPicker">
            <img
              v-if="logoPreviewUrl"
              class="logo-preview"
              :src="logoPreviewUrl"
              :alt="selectedLogoName || '店鋪 Logo 預覽'"
            />
            <div v-else class="logo-placeholder">
              <i class="bi bi-image" aria-hidden="true"></i>
              <span>點選上傳</span>
              <small>店鋪 Logo placeholder</small>
            </div>
          </button>

          <input
            ref="logoFileInput"
            class="logo-file-input"
            type="file"
            accept="image/*"
            @change="handleLogoSelect"
          />
        </section>

        <label class="form-field full-width">
          店鋪介紹
          <textarea v-model="form.description"></textarea>
        </label>

        <label class="form-field">
          聯絡 Email
          <input v-model="form.email" type="email" />
        </label>

        <label class="form-field">
          客服電話
          <input v-model="form.phone" type="tel" />
        </label>

        <div class="section-divider full-width">
          <span>店鋪位置</span>
          <small>選填</small>
        </div>

        <label class="form-field">
          縣市
          <input v-model="form.city" type="text" placeholder="選填" />
        </label>

        <label class="form-field">
          區域
          <input v-model="form.district" type="text" placeholder="選填" />
        </label>

        <label class="form-field full-width">
          詳細地址
          <input v-model="form.address" type="text" placeholder="選填" />
        </label>
      </section>

      <aside class="profile-side">
        <section class="preview-card">
          <div class="card-heading compact-heading">
            <div>
              <p class="section-kicker">即時預覽</p>
              <h2>商家館顯示</h2>
            </div>
          </div>

          <div class="preview-brand">
            <img
              v-if="logoPreviewUrl"
              class="store-avatar-image"
              :src="logoPreviewUrl"
              :alt="selectedLogoName || `${form.storeName} Logo`"
            />
            <span v-else class="store-avatar">{{ form.storeName.slice(0, 1) }}</span>
            <div>
              <strong>{{ form.storeName }}</strong>
              <span>生活選物</span>
            </div>
          </div>
          <p class="preview-description">{{ form.description }}</p>
          <div class="preview-meta">
            <span>{{ form.city || form.district ? `${form.city}${form.district}` : '未提供地址' }}</span>
            <span>{{ form.serviceHours }}</span>
          </div>
        </section>

        <section class="settings-card">
          <div class="card-heading compact-heading">
            <div>
              <p class="section-kicker">營運設定</p>
              <h2>營業狀態</h2>
            </div>
          </div>

          <label class="form-field">
            店鋪狀態
            <select v-model="form.status">
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>

          <label class="form-field">
            客服時間
            <input v-model="form.serviceHours" type="text" />
          </label>

          <label class="form-field">
            店鋪公告
            <textarea v-model="form.announcement"></textarea>
          </label>
        </section>
      </aside>

      <p v-if="savedMessage" class="saved-message">{{ savedMessage }}</p>

      <div class="form-actions">
        <button class="secondary-button" type="button">取消</button>
        <button class="primary-button" type="submit" :disabled="isSaving">
          {{ isSaving ? '儲存中...' : '儲存變更' }}
        </button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.seller-page {
  display: grid;
  gap: var(--space-5);
  max-width: 1160px;
}

.page-header {
  display: flex;
  align-items: flex-start;
}

.eyebrow,
.page-description,
.section-label,
.section-kicker,
.preview-meta,
.preview-brand span,
.status-panel span {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.eyebrow,
.section-kicker,
.section-label {
  margin: 0 0 var(--space-1);
}

.page-description {
  margin: var(--space-1) 0 0;
}

h1,
h2,
p {
  margin-top: 0;
}

h1 {
  margin-bottom: 0;
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}

h2 {
  margin-bottom: 0;
  color: var(--color-text-800);
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
}

h3 {
  margin: 0;
  color: var(--color-text-800);
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
}

.status-panel,
.profile-card,
.preview-card,
.settings-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.status-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
  border-left: 4px solid var(--color-success);
}

.status-paused {
  border-left-color: var(--color-warning);
}

.status-reviewing {
  border-left-color: var(--color-text-muted);
}

.status-copy {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.status-dot {
  width: 10px;
  height: 10px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--color-success);
}

.status-paused .status-dot {
  background: var(--color-warning);
}

.status-reviewing .status-dot {
  background: var(--color-text-muted);
}

.status-copy div {
  display: grid;
  gap: 2px;
}

.status-copy strong {
  color: var(--color-text-900);
  font-size: var(--font-size-lg);
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  align-items: start;
  gap: var(--space-5);
}

.profile-card,
.preview-card,
.settings-card {
  padding: var(--space-5);
}

.profile-card {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

.card-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
  grid-column: 1 / -1;
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.compact-heading {
  padding-bottom: var(--space-3);
}

.visibility-note {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.profile-side {
  display: grid;
  align-content: start;
  gap: var(--space-4);
}

.preview-card {
  border-top: 3px solid var(--color-primary);
}

.preview-brand {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.store-avatar {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-family: var(--font-heading);
  font-size: var(--font-size-lg);
  font-weight: 700;
}

.store-avatar-image {
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  border-radius: var(--radius-md);
  object-fit: cover;
}

.preview-brand div {
  display: grid;
  gap: 2px;
}

.preview-brand strong {
  color: var(--color-text-900);
}

.preview-description {
  min-height: 48px;
  margin: var(--space-4) 0;
  color: var(--color-text-700);
  line-height: 1.6;
}

.preview-meta {
  display: grid;
  gap: var(--space-2);
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);
}

.settings-card {
  display: grid;
  gap: var(--space-4);
}

.form-field {
  display: grid;
  gap: var(--space-2);
  color: var(--color-text-700);
  font-weight: 700;
}

.logo-section {
  display: grid;
  gap: var(--space-3);
}

.logo-upload-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: min(100%, 360px);
  min-height: 230px;
  overflow: hidden;
  border: 0;
  border-radius: var(--radius-md);
  padding: 0;
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
  cursor: pointer;
  font: inherit;
  text-align: center;
}

.logo-upload-button:hover {
  background: var(--color-disabled-bg);
}

.logo-upload-button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.logo-preview {
  width: 100%;
  height: 230px;
  object-fit: cover;
}

.logo-placeholder {
  display: grid;
  min-height: 100%;
  align-content: center;
  justify-items: center;
  gap: var(--space-2);
  font-family: var(--font-body);
}

.logo-placeholder i {
  color: var(--color-primary);
  font-size: 32px;
}

.logo-placeholder span {
  color: var(--color-text-700);
  font-weight: 700;
}

.logo-placeholder small {
  font-size: var(--font-size-sm);
}

.logo-file-input {
  display: none;
}

.full-width {
  grid-column: 1 / -1;
}

.section-divider {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-2);
  color: var(--color-text-800);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.section-divider small {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.section-divider::after {
  height: 1px;
  flex: 1;
  background: var(--color-border);
  content: '';
}

input,
select,
textarea {
  width: 100%;
  box-sizing: border-box;
  min-height: 40px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
  font-weight: 400;
}

input:focus,
select:focus,
textarea:focus {
  outline: 2px solid var(--color-primary-soft);
  border-color: var(--color-primary);
}

textarea {
  min-height: 112px;
  padding: var(--space-3);
  resize: vertical;
}

.settings-card textarea {
  min-height: 88px;
}

.secondary-button,
.primary-button {
  min-height: 40px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.secondary-button {
  background: var(--color-surface);
  color: var(--color-text-700);
}

.primary-button {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: var(--color-surface);
}

.primary-button:disabled {
  cursor: wait;
  opacity: 0.72;
}

.saved-message {
  grid-column: 1 / -1;
  margin: 0;
  color: var(--color-success);
  font-weight: 700;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  grid-column: 1 / -1;
  gap: var(--space-3);
}

@media (max-width: 1000px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .profile-side {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .status-panel,
  .profile-side {
    grid-template-columns: 1fr;
  }

  .status-panel {
    align-items: flex-start;
    flex-direction: column;
  }

  .profile-card {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column-reverse;
  }

  .form-actions button,
  .secondary-button {
    width: 100%;
  }
}
</style>
