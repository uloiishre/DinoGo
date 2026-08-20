<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getSellerMessages,
  markAllSellerMessagesRead,
  markSellerMessageRead,
} from '@/api/sellerMessageApi'

const router = useRouter()
const categories = [
  { value: 'ALL', label: '全部訊息' },
  { value: 'ORDER', label: '訂單通知' },
  { value: 'PLATFORM', label: '平台公告' },
  { value: 'PROMOTION', label: '優惠通知' },
]

const activeCategory = ref('ALL')
const messages = ref([])
const totalUnreadCount = ref(0)
const isLoading = ref(false)
const isMarkingAll = ref(false)
const errorMessage = ref('')

const getPayload = (response) => {
  const payload = response?.data ?? {}
  return Array.isArray(payload) ? payload : payload.content ?? []
}

const loadMessages = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const response = await getSellerMessages({ category: activeCategory.value })
    messages.value = getPayload(response)
    totalUnreadCount.value = response?.data?.unreadCount ?? messages.value.filter((message) => !message.read).length
  } catch (error) {
    messages.value = []
    errorMessage.value = error.response?.data?.message || '訊息暫時無法載入，請稍後再試。'
  } finally {
    isLoading.value = false
  }
}

const selectCategory = (category) => {
  if (activeCategory.value === category) return
  activeCategory.value = category
  loadMessages()
}

const openMessage = async (message) => {
  if (!message.read) {
    try {
      await markSellerMessageRead(message.id)
      message.read = true
      totalUnreadCount.value = Math.max(0, totalUnreadCount.value - 1)
    } catch (error) {
      errorMessage.value = error.response?.data?.message || '無法更新訊息狀態。'
      return
    }
  }

  if (message.targetUrl?.startsWith('/seller/')) {
    router.push(message.targetUrl)
  }
}

const markAllRead = async () => {
  if (!totalUnreadCount.value || isMarkingAll.value) return
  isMarkingAll.value = true
  errorMessage.value = ''
  try {
    await markAllSellerMessagesRead()
    messages.value.forEach((message) => {
      message.read = true
    })
    totalUnreadCount.value = 0
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '無法將全部訊息設為已讀。'
  } finally {
    isMarkingAll.value = false
  }
}

const formatTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-TW', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

onMounted(loadMessages)
</script>

<template>
  <section class="seller-page" aria-labelledby="message-page-title">
    <header class="page-header">
      <div>
        <p class="eyebrow">訊息管理</p>
        <h1 id="message-page-title">訊息中心</h1>
        <p>查看訂單動態、平台公告與優惠通知。</p>
      </div>
      <button
        class="mark-all-button"
        type="button"
        :disabled="!totalUnreadCount || isLoading || isMarkingAll"
        @click="markAllRead"
      >
        {{ isMarkingAll ? '處理中…' : '全部設為已讀' }}
      </button>
    </header>

    <div class="message-layout">
      <nav class="category-panel" aria-label="訊息分類">
        <button
          v-for="category in categories"
          :key="category.value"
          type="button"
          :class="{ active: activeCategory === category.value }"
          :aria-current="activeCategory === category.value ? 'page' : undefined"
          @click="selectCategory(category.value)"
        >
          <span>{{ category.label }}</span>
          <span v-if="category.value === 'ALL' && totalUnreadCount" class="unread-badge">
            {{ totalUnreadCount }}
          </span>
        </button>
      </nav>

      <div class="message-panel" aria-live="polite" :aria-busy="isLoading">
        <div v-if="errorMessage" class="state-panel error-state" role="alert">
          <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
          <p>{{ errorMessage }}</p>
          <button type="button" @click="loadMessages">重新載入</button>
        </div>

        <div v-else-if="isLoading" class="state-panel">
          <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
          <p>正在載入訊息…</p>
        </div>

        <div v-else-if="!messages.length" class="state-panel">
          <i class="bi bi-inbox" aria-hidden="true"></i>
          <p>目前沒有這個分類的訊息。</p>
        </div>

        <ul v-else class="message-list">
          <li v-for="message in messages" :key="message.id">
            <button
              type="button"
              class="message-row"
              :class="{ unread: !message.read }"
              @click="openMessage(message)"
            >
              <span class="status-dot" :aria-label="message.read ? '已讀' : '未讀'"></span>
              <span class="message-copy">
                <strong>{{ message.title }}</strong>
                <span>{{ message.content }}</span>
              </span>
              <time :datetime="message.createdAt">{{ formatTime(message.createdAt) }}</time>
              <i v-if="message.targetUrl" class="bi bi-chevron-right" aria-hidden="true"></i>
            </button>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<style scoped>
.seller-page { display: grid; gap: var(--space-5); max-width: 1160px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--space-5); }
.eyebrow { margin: 0 0 var(--space-1); color: var(--color-text-muted); font-size: var(--font-size-sm); }
h1 { margin: 0; font-family: var(--font-heading); font-size: var(--font-size-xl); color: var(--color-text); }
.page-header p:last-child { margin: var(--space-1) 0 0; color: var(--color-text-muted); font-size: var(--font-size-sm); }
.mark-all-button, .state-panel button { min-height: 40px; padding: 0 var(--space-4); border: 1px solid var(--color-border-strong); border-radius: var(--radius-md); background: var(--color-surface); color: var(--color-primary-active); font-weight: 600; }
.mark-all-button:disabled { border-color: var(--color-disabled); background: var(--color-disabled-bg); color: var(--color-text-subtle); cursor: not-allowed; }
.message-layout { display: grid; grid-template-columns: 210px minmax(0, 1fr); gap: var(--space-5); min-height: 460px; }
.category-panel,
.message-panel { border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: var(--color-surface); }
.category-panel { align-self: start; display: grid; gap: var(--space-1); padding: var(--space-3); }
.category-panel button { min-height: 44px; display: flex; justify-content: space-between; align-items: center; border: 0; border-radius: var(--radius-sm); padding: 0 var(--space-3); background: transparent; color: var(--color-text-700); text-align: left; }
.category-panel button:hover { background: var(--color-surface-soft); }
.category-panel button.active { background: var(--color-primary-soft); color: var(--color-primary-active); font-weight: 700; }
.unread-badge { min-width: 24px; padding: 2px var(--space-2); border-radius: var(--radius-pill); background: var(--color-primary-active); color: var(--color-surface); font-size: var(--font-size-xs); text-align: center; }
.message-panel { min-width: 0; overflow: hidden; }
.message-list { margin: 0; padding: 0; list-style: none; }
.message-list li:not(:last-child) { border-bottom: 1px solid var(--color-border); }
.message-row { width: 100%; min-height: 92px; display: grid; grid-template-columns: 8px minmax(0, 1fr) auto 20px; align-items: center; gap: var(--space-4); padding: var(--space-4) var(--space-5); border: 0; background: var(--color-surface-soft); color: var(--color-text); text-align: left; }
.message-row.unread { background: var(--color-surface); }
.message-row:hover { background: var(--color-primary-50); }
.status-dot { width: 8px; height: 8px; border-radius: var(--radius-pill); background: var(--color-disabled); }
.message-row.unread .status-dot { background: var(--color-primary-active); }
.message-copy { min-width: 0; display: grid; gap: var(--space-1); }
.message-copy strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: var(--font-size-sm); font-weight: 500; }
.message-row.unread strong { font-weight: 700; }
.message-copy > span { overflow: hidden; color: var(--color-text-muted); font-size: var(--font-size-xs); text-overflow: ellipsis; white-space: nowrap; }
.message-row time { color: var(--color-text-muted); font-size: var(--font-size-xs); }
.message-row i { color: var(--color-text-muted); }
.state-panel { min-height: 320px; display: grid; place-content: center; justify-items: center; gap: var(--space-3); padding: var(--space-6); color: var(--color-text-muted); text-align: center; }
.state-panel > i { font-size: 44px; color: var(--color-text-muted); }
.state-panel p { margin: 0; }
.error-state { color: var(--color-danger); background: var(--color-danger-soft); }
.error-state > i { color: var(--color-danger); }
button:focus-visible { outline: none; box-shadow: var(--shadow-focus); position: relative; z-index: 1; }
@media (max-width: 760px) { .page-header { align-items: flex-start; flex-direction: column; } .message-layout { grid-template-columns: 1fr; } .category-panel { grid-template-columns: repeat(4, minmax(max-content, 1fr)); overflow-x: auto; } .message-row { grid-template-columns: 8px minmax(0, 1fr) 20px; } .message-row time { grid-column: 2; } }
@media (max-width: 480px) { .category-panel { display: flex; } .category-panel button { flex: 0 0 auto; } .message-row { padding: var(--space-4); } }
</style>
