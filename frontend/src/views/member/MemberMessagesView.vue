<script setup>
//sysmsg-start，總共3次修改，第1次//
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { deleteMemberMessage, getMemberInbox, getMemberMessageDetail, getMemberUnreadCount, markMemberMessageRead } from '@/api/memberMessageApi'

const tabs = [
  { key: 'SYSTEM_INBOX', label: '系統通知' },
  { key: 'ORDER_INBOX', label: '訂單通知' },
  { key: 'SELLER_INBOX', label: '賣家通知' },
]
const activeTab = ref(tabs[0].key)
const statusFilter = ref('ALL')
const pageSize = ref(10)
const selectedIds = ref(new Set())
const selectedMessage = ref(null)
const unreadCount = ref(0)
const deleting = ref(false)
const pageError = ref('')
const inboxes = reactive(Object.fromEntries(tabs.map((tab) => [tab.key, {
  items: [], cursor: null, hasNext: false, loading: false, loaded: false, page: 1,
}])))

const activeInbox = computed(() => inboxes[activeTab.value])
const filteredInboxItems = computed(() => activeInbox.value.items.filter((item) => {
  if (statusFilter.value === 'UNREAD') return item.recordStatus === 'UNREAD'
  if (statusFilter.value === 'READ') return item.recordStatus === 'READ'
  return true
}))
const currentPageItems = computed(() => {
  const start = (activeInbox.value.page - 1) * pageSize.value
  return filteredInboxItems.value.slice(start, start + pageSize.value)
})
const visibleMessages = computed(() => currentPageItems.value)
const activeSelectedCount = computed(() => visibleMessages.value.filter((item) => selectedIds.value.has(item.recordId)).length)
const allVisibleSelected = computed(() => visibleMessages.value.length > 0 && activeSelectedCount.value === visibleMessages.value.length)
const loadedPageCount = computed(() => Math.max(1, Math.ceil(filteredInboxItems.value.length / pageSize.value)))
const pageButtons = computed(() => {
  const lastKnown = loadedPageCount.value + (activeInbox.value.hasNext ? 1 : 0)
  const values = [1, 2, 3, activeInbox.value.page].filter((page) => page <= lastKnown)
  return [...new Set(values)].sort((left, right) => left - right)
})

function tabHasUnread(category) {
  return inboxes[category].items.some((item) => item.recordStatus === 'UNREAD')
}

async function resolveTabUnread(category) {
  const state = inboxes[category]
  // 標籤只在確實存在未讀時顯示綠點；若前頁全已讀，延遲沿 Cursor 查到未讀或分類結尾。
  while (!tabHasUnread(category) && state.hasNext) await loadInbox(category, { append: true })
}

async function refreshUnreadCount() {
  try {
    unreadCount.value = Number((await getMemberUnreadCount()).data?.unreadCount ?? 0)
  } catch {
    // 列表仍可使用；badge 暫時保留上次成功取得的值。
  }
}

async function loadInbox(category, { append = false } = {}) {
  const state = inboxes[category]
  if (state.loading || (append && !state.hasNext)) return
  state.loading = true
  pageError.value = ''
  try {
    const response = await getMemberInbox(category, {
      cursor: append ? state.cursor : null,
      size: pageSize.value,
      sort: 'NEWEST',
    })
    const page = response.data ?? {}
    const items = Array.isArray(page.items) ? page.items : []
    state.items = append ? [...state.items, ...items] : items
    state.cursor = page.nextCursor ?? null
    state.hasNext = Boolean(page.hasNext)
    state.loaded = true
  } catch (error) {
    pageError.value = error.response?.data?.message ?? '會員收件匣載入失敗，請稍後再試。'
  } finally {
    state.loading = false
  }
}

async function selectTab(category) {
  activeTab.value = category
  selectedIds.value = new Set()
  if (!inboxes[category].loaded) await loadInbox(category)
}

async function goToPage(page) {
  const state = activeInbox.value
  const target = Math.max(1, page)
  while (state.items.length < target * pageSize.value && state.hasNext) {
    await loadInbox(activeTab.value, { append: true })
  }
  const lastAvailable = Math.max(1, Math.ceil(state.items.length / pageSize.value))
  state.page = Math.min(target, lastAvailable)
  selectedIds.value = new Set()
}

async function goToLastPage() {
  const state = activeInbox.value
  while (state.hasNext) await loadInbox(activeTab.value, { append: true })
  state.page = Math.max(1, Math.ceil(state.items.length / pageSize.value))
  selectedIds.value = new Set()
}

async function changePageSize() {
  Object.values(inboxes).forEach((state) => {
    state.items = []
    state.cursor = null
    state.hasNext = false
    state.loaded = false
    state.page = 1
  })
  selectedIds.value = new Set()
  await Promise.all(tabs.map((tab) => loadInbox(tab.key)))
}

async function changeStatusFilter() {
  selectedIds.value = new Set()
  activeInbox.value.page = 1
  // 後端維持原 Cursor API；狀態篩選時延遲取完此分類，避免漏掉後續頁的未讀或已讀訊息。
  if (statusFilter.value !== 'ALL') {
    while (activeInbox.value.hasNext) await loadInbox(activeTab.value, { append: true })
  }
}

function toggleMessage(recordId) {
  const next = new Set(selectedIds.value)
  next.has(recordId) ? next.delete(recordId) : next.add(recordId)
  selectedIds.value = next
}

function toggleAllVisible() {
  const next = new Set(selectedIds.value)
  if (allVisibleSelected.value) visibleMessages.value.forEach((item) => next.delete(item.recordId))
  else visibleMessages.value.forEach((item) => next.add(item.recordId))
  selectedIds.value = next
}

async function openMessage(message) {
  selectedMessage.value = { ...message, detailLoading: true }
  document.body.style.overflow = 'hidden'
  try {
    const detail = (await getMemberMessageDetail(message.recordId)).data
    let updated = detail
    if (detail.recordStatus === 'UNREAD') updated = (await markMemberMessageRead(message.recordId)).data
    Object.values(inboxes).forEach((state) => {
      const index = state.items.findIndex((item) => item.recordId === message.recordId)
      if (index >= 0) state.items[index] = { ...state.items[index], recordStatus: updated.recordStatus }
    })
    selectedMessage.value = updated
    await refreshUnreadCount()
  } catch (error) {
    selectedMessage.value = { ...message, detailLoading: false }
    pageError.value = error.response?.data?.message ?? '詳細訊息載入或已讀狀態更新失敗。'
  }
}

function closeMessage() {
  selectedMessage.value = null
  document.body.style.overflow = ''
}

async function deleteSelected() {
  const recordIds = [...selectedIds.value]
  if (!recordIds.length || deleting.value) return
  deleting.value = true
  pageError.value = ''
  const results = await Promise.allSettled(recordIds.map(deleteMemberMessage))
  const deletedIds = new Set(recordIds.filter((_, index) => results[index].status === 'fulfilled'))
  Object.values(inboxes).forEach((state) => {
    state.items = state.items.filter((item) => !deletedIds.has(item.recordId))
  })
  selectedIds.value = new Set(recordIds.filter((recordId) => !deletedIds.has(recordId)))
  if (deletedIds.size !== recordIds.length) pageError.value = '部分訊息刪除失敗，請稍後再試。'
  deleting.value = false
  await refreshUnreadCount()
}

function formatDate(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('zh-TW', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false,
  }).format(new Date(value))
}

function senderLabel(message) {
  return message.msgFunction?.startsWith('SC-')
    ? `seller_id：${message.msgfromSellerId}`
    : '系統自動訊息'
}

function handleEscape(event) {
  if (event.key === 'Escape' && selectedMessage.value) closeMessage()
}

onMounted(async () => {
  window.addEventListener('keydown', handleEscape)
  await Promise.all([loadInbox(activeTab.value), refreshUnreadCount()])
  // 預載另外兩個分類，讓三個標籤的未讀圓點可立即反映目前資料。
  await Promise.all(tabs.slice(1).map((tab) => loadInbox(tab.key)))
  void Promise.all(tabs.map((tab) => resolveTabUnread(tab.key)))
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleEscape)
  document.body.style.overflow = ''
})
//sysmsg-end，總共3次修改，第1次//
</script>

<template>
  <!-- //sysmsg-start，總共3次修改，第2次// -->
  <main class="member-inbox-page container">
    <header class="inbox-heading">
      <div><p>會員中心 · 訊息</p><h1>會員收件匣</h1></div>
      <div class="inbox-bell" aria-label="會員全部未讀訊息">
        <i class="bi bi-bell" aria-hidden="true"></i>
        <span v-if="unreadCount > 0" class="inbox-badge">
          <span :class="{ compact: unreadCount > 9, dense: unreadCount > 99 }">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </span>
      </div>
    </header>

    <nav class="inbox-tabs" role="tablist" aria-label="收件匣分類">
      <button v-for="tab in tabs" :key="tab.key" type="button" role="tab" :aria-selected="activeTab === tab.key" :class="{ active: activeTab === tab.key }" @click="selectTab(tab.key)">
        <span v-if="tabHasUnread(tab.key)" class="tab-dot" aria-label="有未讀訊息"></span>{{ tab.label }}
      </button>
    </nav>

    <section class="inbox-card">
      <div class="inbox-toolbar">
        <label class="select-all"><input type="checkbox" :checked="allVisibleSelected" @change="toggleAllVisible" /><span>全選目前結果</span></label>
        <label class="page-size-filter"><span>每頁</span><select v-model.number="pageSize" @change="changePageSize"><option :value="10">10筆</option><option :value="50">50筆</option></select></label>
        <label class="status-filter"><span class="visually-hidden">訊息讀取狀態</span><select v-model="statusFilter" @change="changeStatusFilter"><option value="ALL">全部訊息</option><option value="UNREAD">未讀取</option><option value="READ">已讀取</option></select></label>
        <button type="button" class="delete-button" :disabled="activeSelectedCount === 0 || deleting" @click="deleteSelected">{{ deleting ? '刪除中...' : `刪除已選（${activeSelectedCount}）` }}</button>
      </div>

      <p v-if="pageError" class="inbox-error" role="alert">{{ pageError }}</p>
      <div class="message-list" tabindex="0">
        <div v-if="activeInbox.loading && !activeInbox.loaded" class="inbox-state">正在載入收件匣...</div>
        <div v-else-if="visibleMessages.length === 0" class="inbox-state">目前沒有符合篩選條件的訊息。</div>
        <article v-for="message in visibleMessages" :key="message.recordId" class="message-row" :class="{ 'message-row--read': message.recordStatus === 'READ' }">
          <label class="message-check" @click.stop><input type="checkbox" :checked="selectedIds.has(message.recordId)" :aria-label="`選取 ${message.sendTitle}`" @change="toggleMessage(message.recordId)" /></label>
          <button type="button" class="message-open" @click="openMessage(message)">
            <span class="message-dot" :class="{ read: message.recordStatus === 'READ' }" :aria-label="message.recordStatus === 'READ' ? '已讀' : '未讀'"></span>
            <span class="message-copy"><strong>{{ message.sendTitle }}</strong><small>{{ message.sendContent }}</small></span>
            <time :datetime="message.recordCreatedAt">{{ formatDate(message.recordCreatedAt) }}</time>
          </button>
        </article>
        <div v-if="activeInbox.loading && activeInbox.loaded" class="inbox-state inbox-state--compact">正在延遲載入頁面...</div>
      </div>
      <nav class="inbox-pagination" aria-label="會員收件匣頁籤">
        <button type="button" :disabled="activeInbox.page === 1 || activeInbox.loading" aria-label="第一頁" @click="goToPage(1)">&lt;&lt;</button>
        <button type="button" :disabled="activeInbox.page === 1 || activeInbox.loading" aria-label="上一頁" @click="goToPage(activeInbox.page - 1)">&lt;</button>
        <button v-for="page in pageButtons" :key="page" type="button" :class="{ active: activeInbox.page === page }" :aria-current="activeInbox.page === page ? 'page' : undefined" :disabled="activeInbox.loading" @click="goToPage(page)">{{ page }}</button>
        <span v-if="activeInbox.hasNext || loadedPageCount > 3" class="pagination-ellipsis" aria-hidden="true">…</span>
        <button type="button" :disabled="(!activeInbox.hasNext && activeInbox.page >= loadedPageCount) || activeInbox.loading" aria-label="下一頁" @click="goToPage(activeInbox.page + 1)">&gt;</button>
        <button type="button" :disabled="(!activeInbox.hasNext && activeInbox.page >= loadedPageCount) || activeInbox.loading" aria-label="最後一頁" @click="goToLastPage">&gt;&gt;</button>
      </nav>
    </section>

    <div v-if="selectedMessage" class="message-overlay" @click.self="closeMessage">
      <article class="message-dialog" role="dialog" aria-modal="true" aria-labelledby="message-detail-title">
        <button type="button" class="message-dialog__close" aria-label="關閉詳細訊息" @click="closeMessage">×</button>
        <div v-if="selectedMessage.detailLoading" class="inbox-state">正在載入詳細訊息...</div>
        <template v-else>
          <header><span class="message-dot" :class="{ read: selectedMessage.recordStatus === 'READ' }" aria-hidden="true"></span><div><p>{{ senderLabel(selectedMessage) }}</p><h2 id="message-detail-title">{{ selectedMessage.sendTitle }}</h2><time :datetime="selectedMessage.recordCreatedAt">{{ formatDate(selectedMessage.recordCreatedAt) }}</time></div></header>
          <div class="message-dialog__content">{{ selectedMessage.sendContent }}</div>
        </template>
      </article>
    </div>
  </main>
  <!-- //sysmsg-end，總共3次修改，第2次// -->
</template>

<style scoped>
/* //sysmsg-start，總共3次修改，第3次// */
.member-inbox-page { max-width: 1440px; padding-block: var(--space-5) var(--space-8); }
.inbox-heading { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); }.inbox-heading p, .inbox-heading h1 { margin: 0; }.inbox-heading p { color: var(--color-text-muted); font-size: var(--font-size-sm); }.inbox-heading h1 { margin-top: var(--space-1); font-family: var(--font-heading); font-size: var(--font-size-xl); }
.inbox-bell { position: relative; display: grid; width: var(--space-8); height: var(--space-8); place-items: center; color: var(--color-text-muted); font-size: var(--font-size-lg); }.inbox-badge { position: absolute; top: var(--space-1); right: 0; display: inline-flex; min-width: var(--space-4); height: var(--space-4); align-items: center; justify-content: center; padding-inline: var(--space-1); color: var(--color-surface); font-size: var(--font-size-xs); line-height: 1; background: var(--color-primary); border-radius: var(--radius-pill); }.inbox-badge span { transform-origin: center; }.inbox-badge .compact { transform: scale(.85); }.inbox-badge .dense { transform: scale(.7); }
.inbox-tabs { display: flex; margin-top: var(--space-5); overflow-x: auto; border-bottom: 1px solid var(--color-border); }.inbox-tabs button { display: inline-flex; min-height: var(--space-7); flex: 1 0 auto; align-items: center; justify-content: center; gap: var(--space-2); padding-inline: var(--space-5); color: var(--color-text-muted); font: inherit; font-weight: 600; background: transparent; border: 0; border-bottom: var(--space-1) solid transparent; cursor: pointer; }.inbox-tabs button:hover { color: var(--color-primary); background: var(--color-primary-soft); }.inbox-tabs button.active { color: var(--color-primary-active); border-bottom-color: var(--color-primary); }.tab-dot, .message-dot { width: var(--space-2); height: var(--space-2); flex: 0 0 var(--space-2); background: var(--color-primary); border-radius: var(--radius-pill); }.tab-dot.read, .message-dot.read { background: var(--color-disabled); }
.inbox-card { overflow: hidden; margin-top: var(--space-4); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-lg); }.inbox-toolbar { display: flex; align-items: center; gap: var(--space-3); padding: var(--space-3) var(--space-4); background: var(--color-surface-soft); border-bottom: 1px solid var(--color-border); }.select-all { display: inline-flex; align-items: center; gap: var(--space-2); color: var(--color-text-muted); font-size: var(--font-size-sm); }.status-filter { margin-left: auto; }.status-filter select, .delete-button { min-height: calc(var(--space-6) + var(--space-1)); padding-inline: var(--space-3); font: inherit; font-size: var(--font-size-sm); border-radius: var(--radius-md); }.status-filter select { color: var(--color-text); background: var(--color-surface); border: 1px solid var(--color-border-strong); }.delete-button { color: var(--color-danger); background: var(--color-surface); border: 1px solid var(--color-danger); }.delete-button:hover:not(:disabled) { color: var(--color-surface); background: var(--color-danger); }.delete-button:disabled { color: var(--color-text-subtle); background: var(--color-disabled-bg); border-color: var(--color-disabled); cursor: not-allowed; }
.inbox-error { margin: var(--space-3); padding: var(--space-3); color: var(--color-danger); background: var(--color-danger-soft); border-radius: var(--radius-md); }.message-list { max-height: calc(var(--space-8) + var(--space-8) + var(--space-8) + var(--space-8) + var(--space-8) + var(--space-8)); overflow-y: auto; }.message-row { display: grid; grid-template-columns: var(--space-7) minmax(0, 1fr); border-bottom: 1px solid var(--color-border); }.message-check { display: grid; place-items: center; }.message-open { display: grid; min-width: 0; grid-template-columns: var(--space-3) minmax(0, 1fr) auto; align-items: center; gap: var(--space-3); padding: var(--space-4); color: var(--color-text); text-align: left; background: transparent; border: 0; cursor: pointer; }.message-row:hover { background: var(--color-primary-soft); }.message-copy { display: grid; min-width: 0; gap: var(--space-1); }.message-copy strong, .message-copy small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.message-copy small, .message-open time { color: var(--color-text-muted); font-size: var(--font-size-xs); }.inbox-state { display: grid; min-height: calc(var(--space-8) + var(--space-8)); place-items: center; color: var(--color-text-muted); }.inbox-state--compact { min-height: var(--space-7); font-size: var(--font-size-xs); }
.message-overlay { position: fixed; z-index: 1050; inset: 0; display: grid; padding: var(--space-5); place-items: center; background: color-mix(in srgb, var(--color-text) 65%, transparent); }.message-dialog { position: relative; width: min(100%, 720px); max-height: calc(100vh - var(--space-8)); overflow-y: auto; padding: var(--space-6); background: var(--color-surface); border-radius: var(--radius-lg); box-shadow: var(--shadow-card); }.message-dialog__close { position: absolute; top: var(--space-3); right: var(--space-3); display: grid; width: calc(var(--space-5) + var(--space-4)); height: calc(var(--space-5) + var(--space-4)); place-items: center; color: var(--color-text-muted); font-size: var(--font-size-xl); background: transparent; border: 0; border-radius: var(--radius-pill); cursor: pointer; }.message-dialog header { display: flex; align-items: flex-start; gap: var(--space-3); padding-right: var(--space-7); }.message-dialog header p, .message-dialog header h2 { margin: 0; }.message-dialog header p, .message-dialog time { color: var(--color-text-muted); font-size: var(--font-size-sm); }.message-dialog header h2 { margin-block: var(--space-1); font-family: var(--font-heading); font-size: var(--font-size-lg); }.message-dialog__content { margin-top: var(--space-5); padding-top: var(--space-5); white-space: pre-wrap; border-top: 1px solid var(--color-border); }
button:focus-visible, select:focus-visible, input:focus-visible, .message-list:focus-visible { outline: none; box-shadow: var(--shadow-focus); }
@media (max-width: 767.98px) { .inbox-toolbar { align-items: stretch; flex-wrap: wrap; }.status-filter { margin-left: 0; }.delete-button { margin-left: auto; }.message-open { grid-template-columns: var(--space-3) minmax(0, 1fr); }.message-open time { grid-column: 2; }.message-dialog { width: 100%; padding: var(--space-5); } }
.page-size-filter { display: inline-flex; align-items: center; gap: var(--space-2); margin-left: auto; color: var(--color-text-muted); font-size: var(--font-size-sm); }.page-size-filter select { min-height: calc(var(--space-6) + var(--space-1)); padding-inline: var(--space-2); color: var(--color-text); font: inherit; background: var(--color-surface); border: 1px solid var(--color-border-strong); border-radius: var(--radius-md); }.page-size-filter + .status-filter { margin-left: 0; }
.message-row--read .message-copy strong, .message-row--read .message-copy small, .message-row--read .message-open time { color: var(--color-text-subtle); }
.inbox-pagination { display: flex; min-height: var(--space-8); align-items: center; justify-content: center; gap: var(--space-2); padding: var(--space-3); border-top: 1px solid var(--color-border); }.inbox-pagination button { min-width: calc(var(--space-6) + var(--space-1)); min-height: calc(var(--space-6) + var(--space-1)); padding-inline: var(--space-2); color: var(--color-text-muted); font: inherit; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-md); cursor: pointer; }.inbox-pagination button:hover:not(:disabled) { color: var(--color-primary); background: var(--color-primary-soft); border-color: var(--color-primary); }.inbox-pagination button.active { color: var(--color-surface); background: var(--color-primary); border-color: var(--color-primary); }.inbox-pagination button:disabled { color: var(--color-text-subtle); background: var(--color-disabled-bg); cursor: not-allowed; }.pagination-ellipsis { color: var(--color-text-muted); }
@media (max-width: 767.98px) { .page-size-filter { margin-left: 0; }.inbox-pagination { gap: var(--space-1); overflow-x: auto; justify-content: flex-start; }.inbox-pagination button { flex: 0 0 auto; } }
/* //sysmsg-end，總共3次修改，第3次// */
</style>
