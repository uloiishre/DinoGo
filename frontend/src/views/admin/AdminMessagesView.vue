<script setup>
import { computed, nextTick, reactive, ref } from 'vue'
import {
  createSystemTemplate,
  deleteSystemTemplate,
  getSystemTemplates,
  updateSystemTemplate,
} from '../../api/adminMessageApi.js'

const PAGE_SIZE = 20
const tabs = [
  { key: 'CREATE', label: '新增訊息' },
  { key: 'TEMPLATES', label: '範本管理' },
  { key: 'OVERVIEW', label: '訊息總覽' },
  { key: 'SEND_HISTORY', label: '發送紀錄' },
]
const templateFilters = ['ALL', 'OA', 'OS', 'OC']
const messageFilters = ['ALL', 'OA', 'OS', 'OC', 'AS', 'AC', 'SC']
const activeTab = ref('CREATE')
const templateFilter = ref('ALL')
const templateSort = reactive({ key: 'sendUpdAt', direction: 'desc' })
const messageFilter = ref('ALL')
const overviewFilters = reactive({ msgPrefix: '', msgNumber: '', senderType: 'ALL', senderId: '', sentDate: '' })
const templatePage = ref(1)
const messagePage = ref(1)
const templatePanel = ref(null)
const messagePanel = ref(null)
const notice = ref('')
const templateLoading = ref(false)
const templateError = ref('')
const templatesLoaded = ref(false)
const form = reactive({ msgType: 'OA', sendTitle: '', sendContent: '' })

const templateItems = ref([])
const messageItems = Array.from({ length: 73 }, (_, index) => ({
  sendId: 3000 + index,
  msgfromSellerId: index % 7 === 6 ? 200 + index : 1,
  systemAdminMemberId: 9001,
  msgFunction: `${['OA', 'OS', 'OC', 'AS', 'AC', 'SC'][index % 6]}-${String(index + 1).padStart(3, '0')}`,
  msgLabel: `訊息標籤 ${index + 1}`,
  sendTitle: `平台訊息 ${index + 1}`,
  sendContent: '訊息總覽內容會固定壓縮成單行，不影響每列 65px 的高度。',
  sendStatus: 'SEND',
  sendUpdAt: new Date(Date.now() - index * 2700000).toISOString(),
}))

const prefixOf = (item) => item.msgFunction?.slice(0, 2)
const filteredTemplates = computed(() => templateItems.value
  .filter((item) => item.sendStatus === 'SAVE' && (templateFilter.value === 'ALL' || prefixOf(item) === templateFilter.value))
  .sort((left, right) => {
    const comparison = templateSort.key === 'msgFunction'
      ? String(left.msgFunction || '').localeCompare(String(right.msgFunction || ''), 'zh-TW', { numeric: true })
      : new Date(left.sendUpdAt).getTime() - new Date(right.sendUpdAt).getTime()
    return templateSort.direction === 'asc' ? comparison : -comparison
  }))
const isSendHistory = computed(() => activeTab.value === 'SEND_HISTORY')
const activeMessageFilters = computed(() => isSendHistory.value
  ? messageFilters.filter((filter) => ['ALL', 'OA', 'OS', 'OC'].includes(filter))
  : messageFilters)
const messageSenderType = (item) => prefixOf(item) === 'SC' ? 'SELLER' : 'ADMIN'
const messageSenderId = (item) => messageSenderType(item) === 'SELLER'
  ? item.msgfromSellerId
  : (item.systemAdminMemberId ?? item.memberId ?? item.msgfromSellerId)
const messageDate = (item) => item.sendUpdAt?.slice(0, 10) || ''
const suggestionValues = (selector) => [...new Set(messageItems.map(selector).filter((value) => value !== null && value !== undefined && value !== ''))]
  .map(String)
  .sort((left, right) => left.localeCompare(right, 'zh-TW', { numeric: true }))
const msgFunctionSuggestions = computed(() => suggestionValues((item) =>
  !overviewFilters.msgPrefix || prefixOf(item) === overviewFilters.msgPrefix
    ? item.msgFunction?.split('-')[1]
    : null))
const senderIdSuggestions = computed(() => suggestionValues((item) =>
  overviewFilters.senderType === 'ALL' || messageSenderType(item) === overviewFilters.senderType ? messageSenderId(item) : null))
const sentDateSuggestions = computed(() => suggestionValues(messageDate).reverse())
const filteredMessages = computed(() => messageItems.filter((item) => item.sendStatus === 'SEND'
  && (!isSendHistory.value || ['OA', 'OS', 'OC'].includes(prefixOf(item)))
  && (messageFilter.value === 'ALL' || prefixOf(item) === messageFilter.value)
  && (isSendHistory.value || !overviewFilters.msgPrefix || prefixOf(item) === overviewFilters.msgPrefix)
  && (isSendHistory.value || !overviewFilters.msgNumber.trim() || item.msgFunction?.split('-')[1]?.includes(overviewFilters.msgNumber.trim()))
  && (isSendHistory.value || overviewFilters.senderType === 'ALL' || messageSenderType(item) === overviewFilters.senderType)
  && (isSendHistory.value || !overviewFilters.senderId.trim() || String(messageSenderId(item) ?? '').includes(overviewFilters.senderId.trim()))
  && (isSendHistory.value || !overviewFilters.sentDate || messageDate(item) === overviewFilters.sentDate)))
const pageCount = (items) => Math.max(1, Math.ceil(items.length / PAGE_SIZE))
const templatePageCount = computed(() => pageCount(filteredTemplates.value))
const messagePageCount = computed(() => pageCount(filteredMessages.value))
const visibleTemplates = computed(() => filteredTemplates.value.slice((templatePage.value - 1) * PAGE_SIZE, templatePage.value * PAGE_SIZE))
const visibleMessages = computed(() => filteredMessages.value.slice((messagePage.value - 1) * PAGE_SIZE, messagePage.value * PAGE_SIZE))
const firstPages = (total) => Array.from({ length: Math.min(2, total) }, (_, index) => index + 1)
const formatTime = (value) => new Intl.DateTimeFormat('zh-TW', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
const senderLabel = (item) => prefixOf(item) === 'SC'
  ? `商家 ${item.msgfromSellerId ?? '—'}`
  : `系統管理員 ${item.systemAdminMemberId ?? item.memberId ?? item.msgfromSellerId ?? '—'}`
const templateEditorLabel = (item) => `系統管理員 ${item.lastModifiedMemberId ?? item.memberId ?? item.msgfromSellerId ?? '—'}`
const selectedTemplateIds = ref(new Set())
const allVisibleTemplatesSelected = computed(() => visibleTemplates.value.length > 0 && visibleTemplates.value.every((item) => selectedTemplateIds.value.has(item.sendId)))

function toggleTemplate(sendId) { const next = new Set(selectedTemplateIds.value); next.has(sendId) ? next.delete(sendId) : next.add(sendId); selectedTemplateIds.value = next }
function toggleAllVisibleTemplates() { const next = new Set(selectedTemplateIds.value); visibleTemplates.value.forEach((item) => allVisibleTemplatesSelected.value ? next.delete(item.sendId) : next.add(item.sendId)); selectedTemplateIds.value = next }
function toggleTemplateSort(key) {
  if (templateSort.key === key) templateSort.direction = templateSort.direction === 'asc' ? 'desc' : 'asc'
  else {
    templateSort.key = key
    templateSort.direction = 'desc'
  }
  templatePage.value = 1
}
const templateSortLabel = (key) => templateSort.key === key
  ? (templateSort.direction === 'asc' ? '正序' : '倒序')
  : '倒序'
const templateSortIcon = (key) => templateSort.key === key
  ? (templateSort.direction === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down')
  : 'bi-arrow-down'
const apiErrorMessage = (error, fallback) => error?.response?.data?.message || error?.message || fallback

async function loadTemplates() {
  templateLoading.value = true
  templateError.value = ''
  try {
    const first = (await getSystemTemplates(0)).data
    const remaining = first.totalPages > 1
      ? await Promise.all(Array.from({ length: first.totalPages - 1 }, (_, index) => getSystemTemplates(index + 1)))
      : []
    templateItems.value = [
      ...(first.items || []),
      ...remaining.flatMap((response) => response.data?.items || []),
    ].filter((item) => item.sendStatus === 'SAVE')
      .sort((left, right) => new Date(right.sendUpdAt) - new Date(left.sendUpdAt) || right.sendId - left.sendId)
    selectedTemplateIds.value = new Set()
    templatesLoaded.value = true
    templatePage.value = Math.min(templatePage.value, pageCount(filteredTemplates.value))
  } catch (error) {
    templateError.value = apiErrorMessage(error, '系統範本載入失敗')
  } finally {
    templateLoading.value = false
  }
}

async function addTemplate() {
  const msgType = templateFilter.value === 'ALL' ? 'OA' : templateFilter.value
  const title = window.prompt(`新增 ${msgType} 範本標題`)
  if (!title?.trim()) return
  const content = window.prompt('新增範本內容')
  if (!content?.trim()) return
  const label = window.prompt('自訂範本名稱', title.trim())
  if (label === null) return
  templateError.value = ''
  try {
    await createSystemTemplate({ msgType, msgLabel: label.trim() || title.trim(), sendTitle: title.trim(), sendContent: content.trim() })
    await loadTemplates()
  } catch (error) {
    templateError.value = apiErrorMessage(error, '新增範本失敗')
  }
}

async function editTemplate(item) {
  const title = window.prompt('修改範本標題', item.sendTitle)
  if (!title?.trim()) return
  const content = window.prompt('修改範本內容', item.sendContent)
  if (!content?.trim()) return
  const label = window.prompt('自訂範本名稱', item.msgLabel || title.trim())
  if (label === null) return
  templateError.value = ''
  try {
    await updateSystemTemplate(item.sendId, { msgLabel: label.trim() || title.trim(), sendTitle: title.trim(), sendContent: content.trim() })
    await loadTemplates()
  } catch (error) {
    templateError.value = apiErrorMessage(error, '修改範本失敗')
  }
}

async function deleteTemplate(item) {
  if (!window.confirm(`是否確認刪除${item.msgLabel}範本`)) return
  templateError.value = ''
  try {
    await deleteSystemTemplate(item.sendId)
    await loadTemplates()
  } catch (error) {
    templateError.value = apiErrorMessage(error, '刪除範本失敗')
  }
}

async function deleteSelectedTemplates() {
  const targets = templateItems.value.filter((item) => selectedTemplateIds.value.has(item.sendId))
  if (targets.length === 0 || !window.confirm(`是否確認刪除已選取的 ${targets.length} 筆範本`)) return
  templateError.value = ''
  try {
    await Promise.all(targets.map((item) => deleteSystemTemplate(item.sendId)))
    await loadTemplates()
  } catch (error) {
    const message = apiErrorMessage(error, '批次刪除範本失敗，已重新載入目前資料')
    await loadTemplates()
    templateError.value = message
  }
}

async function changePage(target, page, total) {
  const next = Math.min(Math.max(1, page), total)
  if (target === 'template') templatePage.value = next
  else messagePage.value = next
  await nextTick()
  ;(target === 'template' ? templatePanel.value : messagePanel.value)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
function changeFilter(target) {
  if (target === 'template') templatePage.value = 1
  else messagePage.value = 1
}
function activateOverviewFilter(group) {
  if (group !== 'function') {
    overviewFilters.msgPrefix = ''
    overviewFilters.msgNumber = ''
  }
  if (group !== 'sender') {
    overviewFilters.senderType = 'ALL'
    overviewFilters.senderId = ''
  }
  if (group !== 'date') overviewFilters.sentDate = ''
  changeFilter('message')
}
function changeOverviewPrefix() {
  const normalized = overviewFilters.msgPrefix.trim().toUpperCase()
  overviewFilters.msgPrefix = ['', 'OA', 'OS', 'OC', 'AS', 'AC', 'SC'].includes(normalized)
    ? normalized
    : overviewFilters.msgPrefix
  overviewFilters.msgNumber = ''
  activateOverviewFilter('function')
}
function changeOverviewSenderType() {
  overviewFilters.senderId = ''
  activateOverviewFilter('sender')
}
function selectOverviewSuggestion(field, event) {
  const value = event.target.value
  if (!value) return
  overviewFilters[field] = value
  activateOverviewFilter(field === 'senderId' ? 'sender' : 'date')
  event.target.selectedIndex = 0
}
function openOverviewOptions(event) {
  const select = event.currentTarget.previousElementSibling
  if (typeof select?.showPicker === 'function') select.showPicker()
  else select?.click()
}
async function selectTab(key) {
  activeTab.value = key
  if (key === 'TEMPLATES' && !templatesLoaded.value) await loadTemplates()
  if (key === 'OVERVIEW' || key === 'SEND_HISTORY') {
    messageFilter.value = 'ALL'
    messagePage.value = 1
  }
}
function submitMessage() {
  notice.value = '檢視版：訊息已模擬送出。'
  form.sendTitle = ''
  form.sendContent = ''
}
</script>

<template>
  <section class="admin-message-page">
    <header class="page-heading"><p>管理後台</p><h1>訊息中心</h1></header>
    <nav class="section-tabs" aria-label="訊息中心功能">
      <button v-for="tab in tabs" :key="tab.key" :class="{ active: activeTab === tab.key }" @click="selectTab(tab.key)">{{ tab.label }}</button>
    </nav>

    <form v-if="activeTab === 'CREATE'" class="content-panel create-form" @submit.prevent="submitMessage">
      <header><h2>新增訊息</h2><p>建立並送出管理端系統訊息。</p></header>
      <p v-if="notice" class="notice" role="status">{{ notice }}</p>
      <label>訊息類型<select v-model="form.msgType"><option value="OA">OA</option><option value="OS">OS</option><option value="OC">OC</option></select></label>
      <label>標題<input v-model="form.sendTitle" maxlength="100" required /></label>
      <label>內容<textarea v-model="form.sendContent" maxlength="1000" rows="8" required></textarea></label>
      <button class="primary-action" type="submit">送出訊息</button>
    </form>

    <section v-else-if="activeTab === 'TEMPLATES'" ref="templatePanel" class="content-panel paged-panel">
      <header class="panel-toolbar"><div><h2>範本管理</h2><p>只顯示 send_status = SAVE。</p></div><div class="admin-template-toolbar"><label><span class="visually-hidden">篩選範本</span><select v-model="templateFilter" aria-label="篩選範本" :disabled="templateLoading" @change="changeFilter('template')"><option v-for="filter in templateFilters" :key="filter" :value="filter">{{ filter === 'ALL' ? '全部範本' : filter }}</option></select></label><button class="template-add-action" :disabled="templateLoading" @click="addTemplate"><i class="bi bi-plus-lg" aria-hidden="true"></i>新增範本</button><button class="template-batch-delete" :disabled="templateLoading || selectedTemplateIds.size === 0" @click="deleteSelectedTemplates">批次刪除（{{ selectedTemplateIds.size }}）</button></div></header>
      <p v-if="templateError" class="template-feedback error" role="alert">{{ templateError }}</p><p v-else-if="templateLoading" class="template-feedback" role="status">範本載入中…</p><div v-else class="template-list"><div class="template-columns"><label><input type="checkbox" :checked="allVisibleTemplatesSelected" @change="toggleAllVisibleTemplates" />全選</label><span>send_id</span><button class="column-sort" :class="{ active: templateSort.key === 'msgFunction' }" :aria-label="`msg_function ${templateSortLabel('msgFunction')}`" @click="toggleTemplateSort('msgFunction')"><span>msg_function</span><i class="bi" :class="templateSortIcon('msgFunction')" aria-hidden="true"></i><span class="visually-hidden">{{ templateSortLabel('msgFunction') }}</span></button><span>自訂範本名稱</span><span>訊息標題 / 訊息內容</span><button class="column-sort" :class="{ active: templateSort.key === 'sendUpdAt' }" :aria-label="`最新修改 ${templateSortLabel('sendUpdAt')}`" @click="toggleTemplateSort('sendUpdAt')"><span>最新修改</span><i class="bi" :class="templateSortIcon('sendUpdAt')" aria-hidden="true"></i><span class="visually-hidden">{{ templateSortLabel('sendUpdAt') }}</span></button><span aria-hidden="true"></span></div><article v-for="item in visibleTemplates" :key="item.sendId" class="template-row"><label class="template-check"><input type="checkbox" :checked="selectedTemplateIds.has(item.sendId)" @change="toggleTemplate(item.sendId)" /></label><span>#{{ item.sendId }}</span><span>{{ item.msgFunction }}</span><span class="template-label"><strong>{{ item.msgLabel }}</strong></span><span class="template-copy"><strong>{{ item.sendTitle }}</strong><small>{{ item.sendContent }}</small></span><span class="template-modified"><time>{{ formatTime(item.sendUpdAt) }}</time><small>{{ templateEditorLabel(item) }}</small></span><span class="template-row-actions"><button class="template-edit-action" @click="editTemplate(item)"><i class="bi bi-pencil" aria-hidden="true"></i>修改</button><button class="template-delete-action" @click="deleteTemplate(item)"><span aria-hidden="true">×</span>刪除</button></span></article><p v-if="visibleTemplates.length === 0" class="template-feedback">目前沒有符合條件的範本。</p></div>
      <nav class="pagination" aria-label="範本頁籤"><button :disabled="templatePage === 1" @click="changePage('template', 1, templatePageCount)">&lt;&lt;</button><button :disabled="templatePage === 1" @click="changePage('template', templatePage - 1, templatePageCount)">&lt;</button><button v-for="page in firstPages(templatePageCount)" :key="page" :class="{ active: templatePage === page }" @click="changePage('template', page, templatePageCount)">{{ page }}</button><span v-if="templatePageCount > 2">…</span><button v-if="templatePageCount > 2" :class="{ active: templatePage === templatePageCount }" @click="changePage('template', templatePageCount, templatePageCount)">{{ templatePageCount }}</button><button :disabled="templatePage === templatePageCount" @click="changePage('template', templatePage + 1, templatePageCount)">&gt;</button><button :disabled="templatePage === templatePageCount" @click="changePage('template', templatePageCount, templatePageCount)">&gt;&gt;</button></nav>
    </section>

    <section v-else ref="messagePanel" class="content-panel paged-panel">
      <header class="panel-toolbar"><div><h2>{{ isSendHistory ? '發送紀錄' : '訊息總覽' }}</h2><p>{{ isSendHistory ? '顯示管理端 OA／OS／OC 發送紀錄。' : '只顯示 send_status = SEND。' }}</p></div><label v-if="isSendHistory">訊息類型<select v-model="messageFilter" @change="changeFilter('message')"><option v-for="filter in activeMessageFilters" :key="filter" :value="filter">{{ filter === 'ALL' ? '全部訊息' : filter }}</option></select></label></header>
      <div v-if="isSendHistory" class="message-list"><div class="message-columns"><span>send_id</span><span>msg_function</span><span>msg_label</span><span>訊息內容</span><span>寄件時間</span></div><article v-for="item in visibleMessages" :key="item.sendId" class="message-row"><span>#{{ item.sendId }}</span><span>{{ item.msgFunction }}</span><span class="message-label"><strong>{{ item.msgLabel }}</strong><small>{{ senderLabel(item) }}</small></span><span class="message-copy"><strong>{{ item.sendTitle }}</strong><small>{{ item.sendContent }}</small></span><time>{{ formatTime(item.sendUpdAt) }}</time></article></div>
      <div v-else class="overview-list">
        <div class="overview-filters">
          <span aria-hidden="true"></span>
          <div class="function-filter-pair">
            <label><span class="visually-hidden">篩選訊息類型</span><select v-model="overviewFilters.msgPrefix" @change="changeOverviewPrefix"><option value="">—</option><option value="OA">OA</option><option value="OS">OS</option><option value="OC">OC</option><option value="AS">AS</option><option value="AC">AC</option><option value="SC">SC</option></select><i class="bi bi-chevron-down" aria-hidden="true"></i></label>
            <b aria-hidden="true">-</b>
            <label><span class="visually-hidden">篩選三位流水號</span><select v-model="overviewFilters.msgNumber" @change="activateOverviewFilter('function')"><option value="">—</option><option v-for="value in msgFunctionSuggestions" :key="value" :value="value">{{ value }}</option></select><i class="bi bi-chevron-down" aria-hidden="true"></i></label>
          </div>
          <span aria-hidden="true"></span>
          <label><span class="visually-hidden">篩選寄件來源</span><select v-model="overviewFilters.senderType" @change="changeOverviewSenderType"><option value="ALL">—</option><option value="SELLER">商家</option><option value="ADMIN">系統訊息</option></select><i class="bi bi-chevron-down" aria-hidden="true"></i></label>
          <label class="filter-combobox"><span class="visually-hidden">篩選寄件人 ID</span><input v-model="overviewFilters.senderId" placeholder="手動輸入" @input="activateOverviewFilter('sender')" /><select class="combobox-menu" aria-label="選擇寄件人 ID" tabindex="-1" @change="selectOverviewSuggestion('senderId', $event)"><option value="" hidden></option><option v-for="value in senderIdSuggestions" :key="value" :value="value">{{ value }}</option></select><button type="button" class="combobox-trigger" aria-label="開啟寄件人 ID 選單" @click="openOverviewOptions"><i class="bi bi-chevron-down" aria-hidden="true"></i></button></label>
          <span aria-hidden="true"></span>
          <label class="filter-combobox"><span class="visually-hidden">篩選寄件時間</span><input v-model="overviewFilters.sentDate" placeholder="手動輸入" @input="activateOverviewFilter('date')" /><select class="combobox-menu" aria-label="選擇寄件時間" tabindex="-1" @change="selectOverviewSuggestion('sentDate', $event)"><option value="" hidden></option><option v-for="value in sentDateSuggestions" :key="value" :value="value">{{ value }}</option></select><button type="button" class="combobox-trigger" aria-label="開啟寄件時間選單" @click="openOverviewOptions"><i class="bi bi-chevron-down" aria-hidden="true"></i></button></label>
        </div>
        <div class="overview-columns"><span>send_id</span><span>msg_function</span><span>自訂範本名稱</span><span>from</span><span>ID</span><span>訊息標題 / 訊息內容</span><span>寄件時間</span></div>
        <article v-for="item in visibleMessages" :key="item.sendId" class="overview-row"><span>#{{ item.sendId }}</span><span>{{ item.msgFunction }}</span><span>{{ item.msgLabel }}</span><span>{{ messageSenderType(item) === 'SELLER' ? '商家' : '系統管理員' }}</span><span>{{ messageSenderId(item) ?? '—' }}</span><span class="message-copy"><strong>{{ item.sendTitle }}</strong><small>{{ item.sendContent }}</small></span><time>{{ formatTime(item.sendUpdAt) }}</time></article>
        <p v-if="visibleMessages.length === 0" class="template-feedback">目前沒有符合篩選條件的訊息。</p>
      </div>
      <nav class="pagination" :aria-label="`${isSendHistory ? '發送紀錄' : '訊息總覽'}頁籤`"><button :disabled="messagePage === 1" @click="changePage('message', 1, messagePageCount)">&lt;&lt;</button><button :disabled="messagePage === 1" @click="changePage('message', messagePage - 1, messagePageCount)">&lt;</button><button v-for="page in firstPages(messagePageCount)" :key="page" :class="{ active: messagePage === page }" @click="changePage('message', page, messagePageCount)">{{ page }}</button><span v-if="messagePageCount > 2">…</span><button v-if="messagePageCount > 2" :class="{ active: messagePage === messagePageCount }" @click="changePage('message', messagePageCount, messagePageCount)">{{ messagePageCount }}</button><button :disabled="messagePage === messagePageCount" @click="changePage('message', messagePage + 1, messagePageCount)">&gt;</button><button :disabled="messagePage === messagePageCount" @click="changePage('message', messagePageCount, messagePageCount)">&gt;&gt;</button></nav>
    </section>
  </section>
</template>

<style scoped>
.admin-message-page{display:grid;gap:var(--space-4);color:var(--color-text)}.page-heading p,.page-heading h1,.content-panel h2,.content-panel header p{margin:0}.page-heading p,.content-panel header p{color:var(--color-text-muted);font-size:var(--font-size-sm)}.page-heading h1{font-family:var(--font-heading);font-size:var(--font-size-xl)}.section-tabs{display:flex;border-bottom:1px solid var(--color-border)}.section-tabs button{min-height:48px;padding-inline:var(--space-5);font:inherit;font-weight:600;color:var(--color-text-muted);background:transparent;border:0;border-bottom:4px solid transparent}.section-tabs button.active{color:var(--color-primary-active);border-bottom-color:var(--color-primary)}.content-panel{overflow:hidden;background:var(--color-surface);border:1px solid var(--color-border);border-radius:var(--radius-lg);scroll-margin-top:var(--space-5)}.create-form{display:grid;gap:var(--space-4);padding:var(--space-5)}.create-form label{display:grid;gap:var(--space-2);color:var(--color-text-muted);font-size:var(--font-size-sm)}.create-form input,.create-form textarea,.create-form select,.panel-toolbar select{padding:var(--space-3);font:inherit;background:var(--color-surface);border:1px solid var(--color-border-strong);border-radius:var(--radius-md)}.primary-action{justify-self:end;min-height:40px;padding-inline:var(--space-4);color:var(--color-surface);font-weight:600;background:var(--color-primary);border:1px solid var(--color-primary);border-radius:var(--radius-md)}.notice{padding:var(--space-3);color:var(--color-primary-active);background:var(--color-primary-soft);border-radius:var(--radius-md)}.paged-panel{padding-bottom:195px}.panel-toolbar{display:flex;align-items:center;justify-content:space-between;gap:var(--space-4);padding:var(--space-4);background:var(--color-surface-soft);border-bottom:1px solid var(--color-border)}.panel-toolbar label{display:flex;align-items:center;gap:var(--space-2);color:var(--color-text-muted);font-size:var(--font-size-sm)}.template-row,.message-row{display:grid;height:65px;align-items:center;gap:var(--space-3);padding-inline:var(--space-4);border-bottom:1px solid var(--color-border)}.template-row{grid-template-columns:1fr 1.5fr 90px 150px}.message-row{grid-template-columns:80px 120px minmax(0,1fr) 100px 150px}.template-row span,.template-row strong,.template-row small,.template-row time,.message-row>*{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.message-copy{display:grid;min-width:0}.message-copy strong,.message-copy small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.template-row small,.template-row time,.message-row,.message-copy small{color:var(--color-text-muted);font-size:var(--font-size-sm)}.pagination{display:flex;align-items:center;justify-content:center;gap:var(--space-2);padding:var(--space-3);border-top:1px solid var(--color-border)}.pagination button{min-width:36px;min-height:36px;color:var(--color-text-muted);background:var(--color-surface);border:1px solid var(--color-border);border-radius:var(--radius-md)}.pagination button.active{color:var(--color-surface);background:var(--color-primary);border-color:var(--color-primary)}button:focus-visible,select:focus-visible,input:focus-visible,textarea:focus-visible{outline:none;box-shadow:var(--shadow-focus)}@media(max-width:900px){.message-row{grid-template-columns:70px 90px minmax(180px,1fr) 80px 130px}.content-panel{overflow-x:auto}.template-row,.message-row,.panel-toolbar,.pagination{min-width:760px}}
.message-row,.message-columns{grid-template-columns:80px 110px minmax(150px,.9fr) minmax(240px,1.8fr) 150px}.message-columns{display:grid;min-height:44px;align-items:center;gap:var(--space-3);padding-inline:var(--space-4);color:var(--color-text-muted);font-size:var(--font-size-xs);font-weight:700;background:var(--color-bg-muted);border-bottom:1px solid var(--color-border)}.message-label{display:grid;min-width:0}.message-label>*{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.message-label small{color:var(--color-text-muted);font-size:var(--font-size-xs)}@media(max-width:900px){.message-columns{min-width:760px}}
.template-row,.template-columns{grid-template-columns:80px 110px minmax(150px,.9fr) minmax(240px,1.8fr) 150px}.template-columns{display:grid;min-height:44px;align-items:center;gap:var(--space-3);padding-inline:var(--space-4);color:var(--color-text-muted);font-size:var(--font-size-xs);font-weight:700;background:var(--color-bg-muted);border-bottom:1px solid var(--color-border)}.template-label,.template-copy{display:grid;min-width:0}.template-label>*,.template-copy>*{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.template-label small,.template-copy small{color:var(--color-text-muted);font-size:var(--font-size-xs)}@media(max-width:900px){.template-columns{min-width:760px}}
.template-actions-bar{display:flex;min-height:60px;align-items:center;justify-content:space-between;gap:var(--space-3);padding:var(--space-3) var(--space-4);background:var(--color-surface-soft);border-bottom:1px solid var(--color-border)}.template-actions-bar>label,.template-actions-bar>div,.template-row-actions{display:flex;align-items:center;gap:var(--space-2)}.template-actions-bar button,.template-row-actions button{display:inline-flex;min-height:36px;align-items:center;justify-content:center;gap:var(--space-1);padding-inline:var(--space-3);font-weight:600;border-radius:var(--radius-md)}.template-actions-bar button{min-width:132px}.template-add-action,.template-edit-action{color:var(--color-surface);background:var(--color-primary);border:1px solid var(--color-primary)}.template-batch-delete,.template-delete-action{color:var(--color-danger);background:var(--color-surface);border:1px solid var(--color-danger)}.template-batch-delete:disabled{color:var(--color-text-subtle);background:var(--color-disabled-bg);border-color:var(--color-disabled)}.template-row,.template-columns{grid-template-columns:42px 74px 105px minmax(145px,.8fr) minmax(210px,1.4fr) 145px 176px}.template-check{display:grid;height:65px;place-items:center}.template-row-actions{overflow:visible}.template-row-actions button{padding-inline:var(--space-2)}@media(max-width:1100px){.template-row,.template-columns,.template-actions-bar{min-width:1000px}}
.admin-template-toolbar{display:flex;align-items:center;justify-content:flex-end;gap:var(--space-2)}.admin-template-toolbar label{display:block}.admin-template-toolbar select,.admin-template-toolbar button{width:112px;height:32px;min-height:32px;padding:0 var(--space-2);font-size:var(--font-size-sm);border-radius:var(--radius-md)}.admin-template-toolbar select{background:var(--color-surface);border:1px solid var(--color-border-strong)}.admin-template-toolbar button{display:inline-flex;align-items:center;justify-content:center;gap:var(--space-1);font-weight:600}.template-row,.template-columns{grid-template-columns:80px 80px 110px 160px minmax(240px,1fr) 170px 176px}.template-columns>label{display:inline-flex;align-items:center;gap:var(--space-2);white-space:nowrap}.template-modified{display:grid;min-width:0;gap:0}.template-modified time,.template-modified small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.template-modified small{color:var(--color-text-muted);font-size:var(--font-size-xs)}.template-row-actions button{width:80px;height:28px;min-height:28px;padding:0 var(--space-2)}@media(max-width:1100px){.template-row,.template-columns,.panel-toolbar{min-width:1050px}}
.template-feedback{margin:0;padding:var(--space-5);color:var(--color-text-muted);text-align:center}.template-feedback.error{color:var(--color-danger)}
.column-sort{display:flex;align-items:center;justify-content:flex-start;gap:var(--space-1);padding:0;color:inherit;font:inherit;text-align:left;background:transparent;border:0}.column-sort i{color:var(--color-text-subtle);font-size:14px;line-height:1}.column-sort.active{color:var(--color-primary-active)}.column-sort.active i{color:var(--color-primary)}
.overview-filters,.overview-columns,.overview-row{display:grid;grid-template-columns:80px 150px 150px 130px 110px minmax(260px,1fr) 170px;align-items:center;gap:var(--space-3);padding-inline:var(--space-4)}.overview-filters{min-height:82px;background:var(--color-bg-muted)}.overview-columns{min-height:36px;color:var(--color-text-muted);font-size:var(--font-size-xs);font-weight:700;background:var(--color-bg-muted);border-bottom:1px solid var(--color-border)}.overview-columns>span:not(:nth-child(6)),.overview-row>span:not(:nth-child(6)),.overview-row>time{text-align:center}.overview-filters label{display:block;position:relative;min-width:0}.function-filter-stack{display:grid;gap:5px}.overview-filters input,.overview-filters select{width:100%;height:30px;min-width:0;padding:2px 24px 2px 7px;color:var(--color-text);font-size:var(--font-size-xs);background:var(--color-surface);border:1px solid var(--color-border-strong);border-radius:var(--radius-sm)}.overview-filters select{appearance:none}.overview-filters input:hover,.overview-filters select:hover{border-color:var(--color-primary)}.overview-filters input:focus-visible,.overview-filters select:focus-visible{outline:0;border-color:var(--color-primary);box-shadow:var(--shadow-focus)}.overview-filters label>i{position:absolute;top:50%;right:7px;color:var(--color-text-muted);font-size:11px;pointer-events:none;transform:translateY(-50%)}.overview-row{height:65px;border-bottom:1px solid var(--color-border)}.overview-row>span,.overview-row time,.overview-row strong,.overview-row small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.overview-row time,.overview-row small{color:var(--color-text-muted);font-size:var(--font-size-sm)}@media(max-width:1200px){.overview-filters,.overview-columns,.overview-row{min-width:1150px}}
.overview-filters{min-height:50px}.function-filter-pair{display:grid;grid-template-columns:minmax(0,1fr) auto minmax(0,1fr);align-items:center;gap:4px}.function-filter-pair>b{color:var(--color-text-muted);font-size:var(--font-size-sm);text-align:center}
.filter-combobox>.combobox-menu{position:absolute;z-index:-1;inset:0;width:100%;height:30px;padding:0;pointer-events:none;opacity:0}.filter-combobox>.combobox-trigger{position:absolute;z-index:2;top:0;right:0;width:30px;height:30px;padding:0;color:var(--color-text-muted);background:transparent;border:0;border-radius:0 var(--radius-sm) var(--radius-sm) 0}.filter-combobox>.combobox-trigger i{font-size:11px}.filter-combobox>.combobox-trigger:hover{color:var(--color-primary-active);background:var(--color-primary-soft)}.filter-combobox>.combobox-trigger:focus-visible{outline:0;box-shadow:var(--shadow-focus)}.filter-combobox:focus-within>input{border-color:var(--color-primary);box-shadow:var(--shadow-focus)}
</style>
