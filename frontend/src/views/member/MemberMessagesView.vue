<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  announceMemberUnreadChanged,
  deleteMemberMessage,
  getMemberInbox,
  getMemberMessage,
  markMemberMessageRead,
} from '@/api/memberMessageApi.js'

const tabs = [
  { key: 'SYSTEM_INBOX', label: '系統通知' }, //msg-系統通知//
  { key: 'ORDER_INBOX', label: '訂單通知' }, //msg-訂單通知//
  { key: 'SELLER_INBOX', label: '賣家通知' }, //msg-賣家通知//
]
const route = useRoute()
const inboxes = reactive(Object.fromEntries(tabs.map((tab) => [tab.key, []])))
const loadedTabs = reactive(Object.fromEntries(tabs.map((tab) => [tab.key, false])))
const activeTab = ref('SYSTEM_INBOX')
const statusFilter = ref('ALL')
const pageSize = 12
const currentPage = ref(1)
const selectedIds = ref(new Set())
const selectedMessage = ref(null)
const loading = ref(false)
const actionPending = ref(false)
const errorMessage = ref('')
const filteredItems = computed(() => inboxes[activeTab.value].filter((message) => statusFilter.value === 'ALL' || message.recordStatus === statusFilter.value))
const totalPages = computed(() => Math.max(1, Math.ceil(filteredItems.value.length / pageSize)))
const visibleMessages = computed(() => filteredItems.value.slice((currentPage.value - 1) * pageSize, currentPage.value * pageSize))
const pageButtons = computed(() => [1, 2].filter((page) => page <= totalPages.value))
const allVisibleSelected = computed(() => visibleMessages.value.length > 0 && visibleMessages.value.every((message) => selectedIds.value.has(message.recordId)))

function sortNewestFirst(items) {
  return items.sort((left, right) => {
    const timeDifference = Date.parse(right.recordCreatedAt) - Date.parse(left.recordCreatedAt)
    return timeDifference || right.recordId - left.recordId
  })
}

async function loadInbox(category, force = false) {
  if (loadedTabs[category] && !force) return
  loading.value = true
  errorMessage.value = ''
  try {
    const firstResponse = await getMemberInbox(category, 0)
    const firstPage = firstResponse.data
    const remainingResponses = await Promise.all(
      Array.from({ length: Math.max(0, firstPage.totalPages - 1) }, (_, index) =>
        getMemberInbox(category, index + 1),
      ),
    )
    inboxes[category] = sortNewestFirst([
      ...(firstPage.items ?? []),
      ...remainingResponses.flatMap((response) => response.data.items ?? []),
    ])
    loadedTabs[category] = true
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '會員收件匣載入失敗，請稍後再試。'
  } finally {
    loading.value = false
  }
}

async function selectTab(key) {
  activeTab.value = key
  currentPage.value = 1
  selectedIds.value = new Set()
  await loadInbox(key)
}
function toggleMessage(id) { const next = new Set(selectedIds.value); next.has(id) ? next.delete(id) : next.add(id); selectedIds.value = next }
function toggleAllVisible() { const next = new Set(selectedIds.value); visibleMessages.value.forEach((message) => allVisibleSelected.value ? next.delete(message.recordId) : next.add(message.recordId)); selectedIds.value = next }
async function deleteSelected() {
  const ids = [...selectedIds.value]
  if (!ids.length || actionPending.value) return
  actionPending.value = true
  errorMessage.value = ''
  const results = await Promise.allSettled(ids.map((id) => deleteMemberMessage(id)))
  const deletedIds = new Set(ids.filter((id, index) => results[index].status === 'fulfilled'))
  inboxes[activeTab.value] = inboxes[activeTab.value].filter((message) => !deletedIds.has(message.recordId))
  selectedIds.value = new Set(ids.filter((id) => !deletedIds.has(id)))
  currentPage.value = Math.min(currentPage.value, totalPages.value)
  if (deletedIds.size !== ids.length) errorMessage.value = '部分訊息刪除失敗，請稍後再試。'
  if (deletedIds.size > 0) announceMemberUnreadChanged()
  actionPending.value = false
}
async function markAllFilteredRead() {
  const unreadMessages = filteredItems.value.filter((message) => message.recordStatus === 'UNREAD')
  if (!unreadMessages.length || actionPending.value) return
  actionPending.value = true
  errorMessage.value = ''
  const results = await Promise.allSettled(unreadMessages.map((message) => markMemberMessageRead(message.recordId)))
  results.forEach((result, index) => {
    if (result.status === 'fulfilled') unreadMessages[index].recordStatus = 'READ'
  })
  if (results.some((result) => result.status === 'rejected')) errorMessage.value = '部分訊息設為已讀失敗，請稍後再試。'
  if (results.some((result) => result.status === 'fulfilled')) announceMemberUnreadChanged()
  currentPage.value = 1
  actionPending.value = false
}
async function openMessage(message) {
  selectedMessage.value = message
  document.body.style.overflow = 'hidden'
  try {
    const wasUnread = message.recordStatus === 'UNREAD'
    const response = wasUnread
      ? await markMemberMessageRead(message.recordId)
      : await getMemberMessage(message.recordId)
    message.recordStatus = 'READ'
    selectedMessage.value = { ...message, ...response.data }
    if (wasUnread) announceMemberUnreadChanged()
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '訊息內容載入失敗，請稍後再試。'
  }
}
function closeMessage() { selectedMessage.value = null; document.body.style.overflow = '' }
function senderLabel(message) { return message.msgfromSellerId != null ? `seller_id：${message.msgfromSellerId}` : '系統自動訊息' }
function goToPage(page) { currentPage.value = Math.min(Math.max(1, page), totalPages.value); selectedIds.value = new Set(); requestAnimationFrame(() => document.querySelector('.inbox-card')?.scrollIntoView({ behavior: 'smooth', block: 'start' })) }
function formatDate(value) { return new Intl.DateTimeFormat('zh-TW', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(value)) }
watch(statusFilter, () => { currentPage.value = 1; selectedIds.value = new Set() })
watch(
  () => [route.query.recordId, route.query.category],
  async ([recordIdQuery, categoryQuery]) => {
    const category = tabs.some((tab) => tab.key === categoryQuery)
      ? categoryQuery
      : activeTab.value
    activeTab.value = category
    currentPage.value = 1
    selectedIds.value = new Set()
    await loadInbox(category)

    const recordId = Number(recordIdQuery)
    if (!Number.isInteger(recordId) || recordId <= 0) return
    const message = inboxes[category].find((item) => item.recordId === recordId)
    if (message) await openMessage(message)
  },
  { immediate: true },
)
onBeforeUnmount(() => { document.body.style.overflow = '' })
</script>

<template>
  <main class="member-inbox-page container">
    <header class="inbox-heading"><p>會員中心 · 訊息</p><h1>會員收件匣</h1></header>
    <nav class="inbox-tabs" role="tablist" aria-label="收件匣分類">
      <button v-for="tab in tabs" :key="tab.key" type="button" role="tab" :aria-selected="activeTab === tab.key" :class="{ active: activeTab === tab.key }" @click="selectTab(tab.key)">{{ tab.label }}</button>
    </nav>
    <section class="inbox-card">
      <div class="inbox-toolbar">
        <label><input type="checkbox" :checked="allVisibleSelected" :disabled="loading || !visibleMessages.length" @change="toggleAllVisible" />全選目前結果</label>
        <label class="status-filter"><span class="visually-hidden">訊息讀取狀態</span><select v-model="statusFilter"><option value="ALL">全部訊息</option><option value="UNREAD">未讀取</option><option value="READ">已讀取</option></select></label>
        <button type="button" class="read-all-button" :disabled="actionPending || !filteredItems.some((message) => message.recordStatus === 'UNREAD')" @click="markAllFilteredRead">全部設為已讀</button>
        <button type="button" class="delete-button" :disabled="actionPending || selectedIds.size === 0" @click="deleteSelected">刪除已選（{{ selectedIds.size }}）</button>
      </div>
      <p v-if="errorMessage" class="inbox-error" role="alert">{{ errorMessage }}</p>
      <div class="message-list">
        <div v-if="loading" class="inbox-state">訊息載入中…</div>
        <div v-else-if="visibleMessages.length === 0" class="inbox-state">目前沒有符合篩選條件的訊息。</div>
        <article v-for="message in visibleMessages" :key="message.recordId" class="message-row" :class="{ 'message-row--read': message.recordStatus === 'READ' }">
          <label class="message-check"><input type="checkbox" :checked="selectedIds.has(message.recordId)" :aria-label="`選取 ${message.sendTitle}`" @change="toggleMessage(message.recordId)" /></label>
          <button type="button" class="message-open" @click="openMessage(message)"><span class="message-dot" :class="{ read: message.recordStatus === 'READ' }"></span><span class="message-copy"><strong>{{ message.sendTitle }}</strong><small>{{ message.sendContent }}</small></span><time :datetime="message.recordCreatedAt">{{ formatDate(message.recordCreatedAt) }}</time></button>
        </article>
      </div>
      <nav v-if="!loading" class="inbox-pagination" aria-label="會員收件匣頁籤">
        <button type="button" :disabled="currentPage === 1" @click="goToPage(1)">&lt;&lt;</button><button type="button" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">&lt;</button>
        <button v-for="page in pageButtons" :key="page" type="button" :class="{ active: currentPage === page }" @click="goToPage(page)">{{ page }}</button><span v-if="totalPages > 2" class="pagination-ellipsis">…</span><button v-if="totalPages > 2" type="button" :class="{ active: currentPage === totalPages }" @click="goToPage(totalPages)">{{ totalPages }}</button>
        <button type="button" :disabled="currentPage === totalPages" @click="goToPage(currentPage + 1)">&gt;</button><button type="button" :disabled="currentPage === totalPages" @click="goToPage(totalPages)">&gt;&gt;</button>
      </nav>
    </section>
    <div v-if="selectedMessage" class="message-overlay" @click.self="closeMessage">
      <article class="message-dialog" role="dialog" aria-modal="true" aria-labelledby="preview-message-detail-title">
        <button type="button" class="message-dialog__close" aria-label="關閉詳細訊息" @click="closeMessage">×</button>
        <header><div><p>from：{{ senderLabel(selectedMessage) }}</p><h2 id="preview-message-detail-title">{{ selectedMessage.sendTitle }}</h2><time :datetime="selectedMessage.recordCreatedAt">{{ formatDate(selectedMessage.recordCreatedAt) }}</time></div></header>
        <div class="message-dialog__content">{{ selectedMessage.sendContent }}</div>
      </article>
    </div>
  </main>
</template>

<style scoped>
.member-inbox-page { --bs-gutter-x: var(--space-6); max-width: 1232px; padding-block: 40px; }.inbox-heading p, .inbox-heading h1 { margin: 0; }.inbox-heading p { color: var(--color-primary-active); font-size: var(--font-size-sm); font-weight: 700; }.inbox-heading h1 { margin-top: var(--space-1); color: var(--color-text); font-family: var(--font-body); font-size: var(--font-size-xl); font-weight: 700; line-height: var(--line-height-heading); }
.inbox-tabs { display: grid; grid-template-columns: repeat(3, 1fr); margin-top: var(--space-5); }.inbox-tabs button { position: relative; min-height: var(--space-7); padding-inline: var(--space-5); color: var(--color-text-muted); font: inherit; font-weight: 600; background: transparent; border: 0; border-bottom: var(--space-1) solid transparent; border-radius: var(--radius-md) var(--radius-md) 0 0; }.inbox-tabs button + button::before { position: absolute; bottom: 0; left: 0; width: 1px; height: 66.6667%; content: ''; background: var(--color-border-strong); }.inbox-tabs button:hover { color: var(--color-primary-active); background: var(--color-primary-soft); }.inbox-tabs button.active { color: var(--color-primary-active); background: var(--color-primary-soft); border-bottom-color: var(--color-primary); }
.inbox-card { overflow: hidden; margin-top: var(--space-4); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-lg); }.inbox-toolbar { display: flex; align-items: center; gap: var(--space-3); padding: var(--space-3) var(--space-4); background: var(--color-surface-soft); border-bottom: 1px solid var(--color-border); }.inbox-toolbar label { display: inline-flex; align-items: center; gap: var(--space-2); color: var(--color-text-muted); font-size: var(--font-size-sm); }.page-size { margin-left: auto; }.inbox-toolbar select, .delete-button { min-height: calc(var(--space-6) + var(--space-1)); padding-inline: var(--space-3); font: inherit; border-radius: var(--radius-md); }.inbox-toolbar select { background: var(--color-surface); border: 1px solid var(--color-border-strong); }.delete-button { color: var(--color-danger); background: var(--color-surface); border: 1px solid var(--color-danger); }.delete-button:disabled { color: var(--color-text-subtle); background: var(--color-disabled-bg); border-color: var(--color-disabled); }
.message-list { min-height: 420px; }.message-row { display: grid; height: var(--inbox-message-row-height); overflow: hidden; grid-template-columns: var(--space-7) minmax(0, 1fr); border-bottom: 1px solid var(--color-border); }.message-check { display: grid; place-items: center; }.message-open { display: grid; width: 100%; min-width: 0; grid-template-columns: var(--space-3) minmax(0, 1fr); align-items: center; gap: var(--space-2); padding: var(--space-1) var(--space-4); color: var(--color-text); text-align: left; background: transparent; border: 0; }.message-row:hover { background: var(--color-primary-soft); }.message-dot { width: var(--space-2); height: var(--space-2); background: var(--color-primary); border-radius: var(--radius-pill); }.message-dot.read { background: var(--color-disabled); }.message-copy { display: grid; min-width: 0; gap: 0; }.message-copy strong, .message-copy small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.message-copy small, .message-dialog time { color: var(--color-text-muted); font-size: var(--font-size-xs); }.message-row--read .message-open, .message-row--read .message-copy strong, .message-row--read .message-copy small, .message-row--read .message-open time { color: var(--color-text-subtle); }.inbox-state { display: grid; min-height: var(--inbox-message-row-height); place-items: center; color: var(--color-text-muted); }
.message-overlay { position: fixed; z-index: 1050; inset: 0; display: grid; place-items: center; padding: var(--space-5); background: color-mix(in srgb, var(--color-text) 65%, transparent); }.message-dialog { position: relative; width: min(100%, 720px); max-height: calc(100vh - (2 * var(--space-5))); overflow-y: auto; padding: var(--space-6); background: var(--color-surface); border-radius: var(--radius-lg); box-shadow: var(--shadow-card); }.message-dialog__close { position: absolute; top: var(--space-3); right: var(--space-3); width: var(--space-7); height: var(--space-7); color: var(--color-text-muted); font-size: var(--font-size-xl); background: transparent; border: 0; }.message-dialog header { padding-right: var(--space-7); }.message-dialog header p, .message-dialog h2 { margin: 0; }.message-dialog h2 { margin-block: var(--space-1); }.message-dialog__content { margin-top: var(--space-5); padding-top: var(--space-5); white-space: pre-wrap; border-top: 1px solid var(--color-border); }
.inbox-pagination { display: flex; align-items: center; justify-content: center; gap: var(--space-2); padding: var(--space-3); border-top: 1px solid var(--color-border); }.inbox-pagination button { min-width: calc(var(--space-6) + var(--space-1)); min-height: calc(var(--space-6) + var(--space-1)); color: var(--color-text-muted); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-md); }.inbox-pagination button.active { color: var(--color-surface); background: var(--color-primary); border-color: var(--color-primary); }.pagination-ellipsis { color: var(--color-text-muted); }
.inbox-card { --inbox-message-row-height: 65px; min-height: 0; margin-top: 0; padding-bottom: calc(3 * var(--inbox-message-row-height)); border-top: 0; border-radius: 0 0 var(--radius-lg) var(--radius-lg); scroll-margin-top: var(--space-5); }
.message-list { min-height: 0; }
.inbox-error { margin: 0; padding: var(--space-2) var(--space-4); color: var(--color-danger); font-size: var(--font-size-sm); background: var(--color-danger-soft); border-bottom: 1px solid var(--color-danger); }
button:focus-visible, select:focus-visible, input:focus-visible { outline: none; box-shadow: var(--shadow-focus); }
@media (max-width: 767.98px) { .inbox-toolbar { align-items: stretch; flex-wrap: wrap; }.delete-button { margin-left: auto; }.inbox-pagination { justify-content: flex-start; overflow-x: auto; } }
@media (max-width: 575.98px) { .member-inbox-page { padding-block: var(--space-6); } }
.message-open { grid-template-columns: var(--space-3) minmax(0, 1fr) auto; }.message-open time { overflow: hidden; color: var(--color-text-muted); font-size: var(--font-size-xs); text-overflow: ellipsis; white-space: nowrap; }.status-filter { margin-left: auto; }.read-all-button { min-height: calc(var(--space-6) + var(--space-1)); padding-inline: var(--space-3); color: var(--color-primary-active); background: var(--color-surface); border: 1px solid var(--color-primary); border-radius: var(--radius-md); }.read-all-button:disabled { color: var(--color-text-subtle); background: var(--color-disabled-bg); border-color: var(--color-disabled); }
</style>
