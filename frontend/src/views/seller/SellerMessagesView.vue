<script setup>
//sysmsg-start，總共3次修改，第1次//
import { computed, reactive, ref } from 'vue'
import { deleteSellerInboxMessage, getSellerInbox, markSellerInboxRead } from '@/api/sellerInboxApi'

const inboxTabs = [
  { key: 'ALL', label: '全部訊息' },
  { key: 'SYSTEM_NOTICE', label: '平台公告' },
  { key: 'NEW_ORDER', label: '訂單進度' },
  { key: 'CANCELLED_ORDER', label: '取消訂單' },
]
const tabs = [...inboxTabs, { key: 'TEMPLATES', label: '範本管理' }, { key: 'CREATE', label: '新增訊息' }]
const categoryKeys = inboxTabs.slice(1).map((tab) => tab.key)
const activeTab = ref('ALL')
const statusFilter = ref('ALL')
const pageSize = ref(10)
const currentPage = ref(1)
const selectedIds = ref(new Set())
const deleting = ref(false)
const errorMessage = ref('')
const states = reactive(Object.fromEntries(categoryKeys.map((key) => [key, {
  items: [], cursor: null, hasNext: true, loading: false, loaded: false,
}])))

const isInboxTab = computed(() => inboxTabs.some((tab) => tab.key === activeTab.value))
const canFilterStatus = computed(() => categoryKeys.includes(activeTab.value))
const loading = computed(() => categoryKeys.some((key) => states[key].loading))
const sourceMessages = computed(() => {
  const items = activeTab.value === 'ALL'
    ? categoryKeys.flatMap((key) => states[key].items)
    : (states[activeTab.value]?.items ?? [])
  const unique = new Map(items.map((item) => [item.recordId, item]))
  return [...unique.values()].sort((left, right) => {
    const timeDifference = new Date(right.recordCreatedAt).getTime() - new Date(left.recordCreatedAt).getTime()
    return timeDifference || right.recordId - left.recordId
  })
})
const filteredMessages = computed(() => {
  if (!canFilterStatus.value || statusFilter.value === 'ALL') return sourceMessages.value
  return sourceMessages.value.filter((item) => item.recordStatus === statusFilter.value)
})
const pageCount = computed(() => Math.max(1, Math.ceil(filteredMessages.value.length / pageSize.value)))
const visibleMessages = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredMessages.value.slice(start, start + pageSize.value)
})
const pageButtons = computed(() => {
  const values = [1, 2, 3, currentPage.value, pageCount.value].filter((page) => page <= pageCount.value)
  return [...new Set(values)].sort((left, right) => left - right)
})
const selectedVisibleCount = computed(() => visibleMessages.value.filter((item) => selectedIds.value.has(item.recordId)).length)
const allVisibleSelected = computed(() => visibleMessages.value.length > 0 && selectedVisibleCount.value === visibleMessages.value.length)

function categoryHasUnread(category) {
  return states[category]?.items.some((item) => item.recordStatus === 'UNREAD') ?? false
}

async function loadCategory(category, { restart = false } = {}) {
  const state = states[category]
  if (!state || state.loading || (!restart && state.loaded)) return
  if (restart) Object.assign(state, { items: [], cursor: null, hasNext: true, loaded: false })
  state.loading = true
  errorMessage.value = ''
  try {
    while (state.hasNext) {
      const response = await getSellerInbox(category, { cursor: state.cursor, size: pageSize.value })
      const page = response.data ?? {}
      state.items.push(...(Array.isArray(page.items) ? page.items : []))
      state.cursor = page.nextCursor ?? null
      state.hasNext = Boolean(page.hasNext)
    }
    state.loaded = true
  } catch (error) {
    errorMessage.value = error.response?.data?.message ?? '商家訊息載入失敗，請稍後再試。'
  } finally {
    state.loading = false
  }
}

async function selectTab(tab) {
  activeTab.value = tab
  statusFilter.value = 'ALL'
  currentPage.value = 1
  selectedIds.value = new Set()
  if (tab === 'ALL') await Promise.all(categoryKeys.map((key) => loadCategory(key)))
  else if (categoryKeys.includes(tab)) await loadCategory(tab)
}

async function changePageSize() {
  currentPage.value = 1
  selectedIds.value = new Set()
  const targets = activeTab.value === 'ALL' ? categoryKeys : [activeTab.value]
  await Promise.all(targets.filter((key) => states[key]).map((key) => loadCategory(key, { restart: true })))
}

function changeFilter() {
  currentPage.value = 1
  selectedIds.value = new Set()
}

function goToPage(page) {
  currentPage.value = Math.min(Math.max(1, page), pageCount.value)
  selectedIds.value = new Set()
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
  if (message.recordStatus !== 'UNREAD') return
  try {
    const updated = (await markSellerInboxRead(message.recordId)).data
    Object.values(states).forEach((state) => {
      const index = state.items.findIndex((item) => item.recordId === message.recordId)
      if (index >= 0) state.items[index] = { ...state.items[index], recordStatus: updated.recordStatus }
    })
  } catch (error) {
    errorMessage.value = error.response?.data?.message ?? '無法更新訊息讀取狀態。'
  }
}

async function deleteSelected() {
  const ids = [...selectedIds.value]
  if (!ids.length || deleting.value) return
  deleting.value = true
  const results = await Promise.allSettled(ids.map(deleteSellerInboxMessage))
  const deletedIds = new Set(ids.filter((_, index) => results[index].status === 'fulfilled'))
  Object.values(states).forEach((state) => {
    state.items = state.items.filter((item) => !deletedIds.has(item.recordId))
  })
  selectedIds.value = new Set(ids.filter((id) => !deletedIds.has(id)))
  if (deletedIds.size !== ids.length) errorMessage.value = '部分訊息刪除失敗，請稍後再試。'
  currentPage.value = Math.min(currentPage.value, pageCount.value)
  deleting.value = false
}

function formatTime(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('zh-TW', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false,
  }).format(new Date(value))
}

void selectTab('ALL')
//sysmsg-end，總共3次修改，第1次//
</script>

<template>
  <!-- //sysmsg-start，總共3次修改，第2次// -->
  <section class="seller-page" aria-labelledby="message-page-title">
    <header class="page-header"><div><p class="eyebrow">訊息管理</p><h1 id="message-page-title">訊息中心</h1><p>查看平台公告與訂單動態，管理訊息範本。</p></div></header>
    <div class="message-layout">
      <nav class="category-panel" aria-label="訊息分類">
        <button v-for="tab in tabs" :key="tab.key" type="button" :class="{ active: activeTab === tab.key }" @click="selectTab(tab.key)">
          <span>{{ tab.label }}</span>
          <span v-if="categoryKeys.includes(tab.key) && categoryHasUnread(tab.key)" class="category-unread-dot" aria-label="有未讀訊息"></span>
        </button>
      </nav>

      <div class="message-panel">
        <div v-if="activeTab === 'TEMPLATES'" class="feature-state"><i class="bi bi-file-earmark-text" aria-hidden="true"></i><h2>範本管理</h2><p>管理商家訊息範本與套用內容。</p></div>
        <div v-else-if="activeTab === 'CREATE'" class="feature-state"><i class="bi bi-pencil-square" aria-hidden="true"></i><h2>新增訊息</h2><p>建立新的商家訊息。</p></div>
        <template v-else>
          <div class="message-toolbar">
            <label class="select-all"><input type="checkbox" :checked="allVisibleSelected" @change="toggleAllVisible" />全選目前頁面</label>
            <label v-if="canFilterStatus" class="status-filter"><span class="visually-hidden">讀取狀態</span><select v-model="statusFilter" @change="changeFilter"><option value="ALL">全部訊息</option><option value="UNREAD">未讀取</option><option value="READ">已讀取</option></select></label>
            <label class="page-size-filter"><span>每頁</span><select v-model.number="pageSize" @change="changePageSize"><option :value="10">10筆</option><option :value="50">50筆</option></select></label>
            <button type="button" class="delete-button" :disabled="selectedVisibleCount === 0 || deleting" @click="deleteSelected">{{ deleting ? '刪除中...' : `刪除已選（${selectedVisibleCount}）` }}</button>
          </div>
          <p v-if="errorMessage" class="error-message" role="alert">{{ errorMessage }}</p>
          <div v-if="loading && !sourceMessages.length" class="state-panel"><span class="spinner-border spinner-border-sm" aria-hidden="true"></span><p>正在延遲載入訊息...</p></div>
          <div v-else-if="!visibleMessages.length" class="state-panel"><i class="bi bi-inbox" aria-hidden="true"></i><p>目前沒有符合條件的訊息。</p></div>
          <div v-else class="message-list">
            <article v-for="message in visibleMessages" :key="message.recordId" class="message-item" :class="{ read: message.recordStatus === 'READ' }">
              <label class="message-check" @click.stop><input type="checkbox" :checked="selectedIds.has(message.recordId)" :aria-label="`選取 ${message.sendTitle}`" @change="toggleMessage(message.recordId)" /></label>
              <button type="button" class="message-row" @click="openMessage(message)">
                <span class="status-dot" :class="{ read: message.recordStatus === 'READ' }" :aria-label="message.recordStatus === 'READ' ? '已讀' : '未讀'"></span>
                <span class="message-copy"><strong>{{ message.sendTitle }}</strong><span>{{ message.sendContent }}</span></span>
                <time :datetime="message.recordCreatedAt">{{ formatTime(message.recordCreatedAt) }}</time>
              </button>
            </article>
          </div>
          <nav class="pagination" aria-label="商家訊息頁籤">
            <button type="button" :disabled="currentPage === 1" @click="goToPage(1)">&lt;&lt;</button><button type="button" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">&lt;</button>
            <button v-for="page in pageButtons" :key="page" type="button" :class="{ active: currentPage === page }" @click="goToPage(page)">{{ page }}</button><span v-if="pageCount > 3">…</span>
            <button type="button" :disabled="currentPage === pageCount" @click="goToPage(currentPage + 1)">&gt;</button><button type="button" :disabled="currentPage === pageCount" @click="goToPage(pageCount)">&gt;&gt;</button>
          </nav>
        </template>
      </div>
    </div>
  </section>
  <!-- //sysmsg-end，總共3次修改，第2次// -->
</template>

<style scoped>
/* //sysmsg-start，總共3次修改，第3次// */
.seller-page { display: grid; gap: var(--space-5); max-width: 1160px; }.page-header { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--space-5); }.eyebrow { margin: 0 0 var(--space-1); color: var(--color-text-muted); font-size: var(--font-size-sm); }h1 { margin: 0; color: var(--color-text); font-family: var(--font-heading); font-size: var(--font-size-xl); }.page-header p:last-child { margin: var(--space-1) 0 0; color: var(--color-text-muted); font-size: var(--font-size-sm); }
.message-layout { display: grid; min-height: 460px; grid-template-columns: 210px minmax(0, 1fr); gap: var(--space-5); }.category-panel, .message-panel { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-lg); }.category-panel { display: grid; align-self: start; gap: var(--space-1); padding: var(--space-3); }.category-panel button { display: flex; min-height: calc(var(--space-6) + var(--space-3)); align-items: center; justify-content: space-between; padding-inline: var(--space-3); color: var(--color-text-700); text-align: left; background: transparent; border: 0; border-radius: var(--radius-sm); }.category-panel button:hover { background: var(--color-surface-soft); }.category-panel button.active { color: var(--color-primary-active); font-weight: 700; background: var(--color-primary-soft); }.category-unread-dot, .status-dot { width: var(--space-2); height: var(--space-2); flex: 0 0 var(--space-2); background: var(--color-primary); border-radius: var(--radius-pill); }.status-dot.read { background: var(--color-disabled); }
.message-panel { min-width: 0; overflow: hidden; }.message-toolbar { display: flex; align-items: center; gap: var(--space-3); padding: var(--space-3); background: var(--color-surface-soft); border-bottom: 1px solid var(--color-border); }.select-all, .page-size-filter { display: inline-flex; align-items: center; gap: var(--space-2); color: var(--color-text-muted); font-size: var(--font-size-sm); }.status-filter { margin-left: auto; }.status-filter select, .page-size-filter select, .delete-button { min-height: calc(var(--space-6) + var(--space-1)); padding-inline: var(--space-3); font: inherit; font-size: var(--font-size-sm); border-radius: var(--radius-md); }.status-filter select, .page-size-filter select { color: var(--color-text); background: var(--color-surface); border: 1px solid var(--color-border-strong); }.delete-button { color: var(--color-danger); background: var(--color-surface); border: 1px solid var(--color-danger); }.delete-button:disabled { color: var(--color-text-subtle); background: var(--color-disabled-bg); border-color: var(--color-disabled); cursor: not-allowed; }
.message-item { display: grid; grid-template-columns: var(--space-7) minmax(0, 1fr); border-bottom: 1px solid var(--color-border); }.message-check { display: grid; place-items: center; }.message-row { display: grid; min-height: calc(var(--space-8) + var(--space-5)); grid-template-columns: var(--space-3) minmax(0, 1fr) auto; align-items: center; gap: var(--space-4); width: 100%; padding: var(--space-4); color: var(--color-text); text-align: left; background: var(--color-surface); border: 0; }.message-item:hover { background: var(--color-primary-soft); }.message-copy { display: grid; min-width: 0; gap: var(--space-1); }.message-copy strong, .message-copy span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.message-copy span, .message-row time { color: var(--color-text-muted); font-size: var(--font-size-xs); }.message-item.read .message-copy strong, .message-item.read .message-copy span, .message-item.read time { color: var(--color-text-subtle); }
.pagination { display: flex; min-height: var(--space-8); align-items: center; justify-content: center; gap: var(--space-2); padding: var(--space-3); }.pagination button { min-width: calc(var(--space-6) + var(--space-1)); min-height: calc(var(--space-6) + var(--space-1)); color: var(--color-text-muted); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-md); }.pagination button.active { color: var(--color-surface); background: var(--color-primary); border-color: var(--color-primary); }.pagination button:disabled { color: var(--color-text-subtle); background: var(--color-disabled-bg); }.state-panel, .feature-state { display: grid; min-height: 320px; place-content: center; justify-items: center; gap: var(--space-3); padding: var(--space-6); color: var(--color-text-muted); text-align: center; }.feature-state h2, .feature-state p, .state-panel p { margin: 0; }.feature-state i, .state-panel i { color: var(--color-primary); font-size: var(--font-size-xl); }.error-message { margin: var(--space-3); padding: var(--space-3); color: var(--color-danger); background: var(--color-danger-soft); border-radius: var(--radius-md); }button:focus-visible, select:focus-visible, input:focus-visible { position: relative; z-index: 1; outline: none; box-shadow: var(--shadow-focus); }
@media (max-width: 767.98px) { .message-layout { grid-template-columns: 1fr; }.category-panel { display: flex; overflow-x: auto; }.category-panel button { flex: 0 0 auto; }.message-toolbar { align-items: stretch; flex-wrap: wrap; }.status-filter { margin-left: 0; }.message-row { grid-template-columns: var(--space-3) minmax(0, 1fr); }.message-row time { grid-column: 2; }.pagination { justify-content: flex-start; overflow-x: auto; } }
/* //sysmsg-end，總共3次修改，第3次// */
</style>
