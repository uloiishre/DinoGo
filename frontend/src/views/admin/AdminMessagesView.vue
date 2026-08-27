<script setup>
import { computed, nextTick, reactive, ref } from 'vue'

const PAGE_SIZE = 20
const tabs = [
  { key: 'CREATE', label: '新增訊息' },
  { key: 'TEMPLATES', label: '範本管理' },
  { key: 'OVERVIEW', label: '訊息總覽' },
]
const templateFilters = ['ALL', 'OA', 'OS', 'OC']
const messageFilters = ['ALL', 'OA', 'OS', 'OC', 'AS', 'AC', 'SC']
const activeTab = ref('CREATE')
const templateFilter = ref('ALL')
const messageFilter = ref('ALL')
const templatePage = ref(1)
const messagePage = ref(1)
const templatePanel = ref(null)
const messagePanel = ref(null)
const notice = ref('')
const form = reactive({ msgType: 'OA', sendTitle: '', sendContent: '' })

const templateItems = ref(Array.from({ length: 47 }, (_, index) => ({
  sendId: 1000 + index,
  msgFunction: `${['OA', 'OS', 'OC'][index % 3]}-${String(index + 1).padStart(3, '0')}`,
  msgLabel: `系統範本 ${index + 1}`,
  lastModifiedMemberId: 9001 + (index % 3),
  sendTitle: `管理後台常用訊息 ${index + 1}`,
  sendContent: '這是管理後台可重複使用的訊息範本內容。',
  sendStatus: 'SAVE',
  sendUpdAt: new Date(Date.now() - index * 3600000).toISOString(),
})))
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
const filteredTemplates = computed(() => templateItems.value.filter((item) => item.sendStatus === 'SAVE' && (templateFilter.value === 'ALL' || prefixOf(item) === templateFilter.value)))
const filteredMessages = computed(() => messageItems.filter((item) => item.sendStatus === 'SEND' && (messageFilter.value === 'ALL' || prefixOf(item) === messageFilter.value)))
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
function addTemplate() { const title = window.prompt('新增範本標題'); if (!title?.trim()) return; templateItems.value.unshift({ sendId: Date.now(), msgFunction: `${templateFilter.value === 'ALL' ? 'OA' : templateFilter.value}-NEW`, msgLabel: title.trim(), sendTitle: title.trim(), sendContent: '新增範本內容', sendStatus: 'SAVE', lastModifiedMemberId: 9001, sendUpdAt: new Date().toISOString() }) }
function editTemplate(item) { const title = window.prompt('修改範本標題', item.sendTitle); if (!title?.trim()) return; item.sendTitle = title.trim(); item.msgLabel = title.trim(); item.lastModifiedMemberId = 9001; item.sendUpdAt = new Date().toISOString() }
function deleteTemplate(item) { if (!window.confirm(`是否確認刪除${item.msgLabel}範本`)) return; templateItems.value = templateItems.value.filter((value) => value.sendId !== item.sendId); const next = new Set(selectedTemplateIds.value); next.delete(item.sendId); selectedTemplateIds.value = next }
function deleteSelectedTemplates() { for (const item of [...templateItems.value]) if (selectedTemplateIds.value.has(item.sendId) && window.confirm(`是否確認刪除${item.msgLabel}範本`)) templateItems.value = templateItems.value.filter((value) => value.sendId !== item.sendId); selectedTemplateIds.value = new Set() }

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
      <button v-for="tab in tabs" :key="tab.key" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">{{ tab.label }}</button>
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
      <header class="panel-toolbar"><div><h2>範本管理</h2><p>只顯示 send_status = SAVE。</p></div><label>範本類型<select v-model="templateFilter" @change="changeFilter('template')"><option v-for="filter in templateFilters" :key="filter" :value="filter">{{ filter === 'ALL' ? '全部範本' : filter }}</option></select></label></header>
      <div class="template-actions-bar"><label><input type="checkbox" :checked="allVisibleTemplatesSelected" @change="toggleAllVisibleTemplates" />全選</label><div><button class="template-add-action" @click="addTemplate"><i class="bi bi-plus-lg"></i>新增範本</button><button class="template-batch-delete" :disabled="selectedTemplateIds.size === 0" @click="deleteSelectedTemplates">批次刪除（{{ selectedTemplateIds.size }}）</button></div></div>
      <div class="template-list"><div class="template-columns"><span></span><span>send_id</span><span>msg_function</span><span>msg_label</span><span>訊息內容</span><span>最後修改時間</span><span>操作</span></div><article v-for="item in visibleTemplates" :key="item.sendId" class="template-row"><label class="template-check"><input type="checkbox" :checked="selectedTemplateIds.has(item.sendId)" @change="toggleTemplate(item.sendId)" /></label><span>#{{ item.sendId }}</span><span>{{ item.msgFunction }}</span><span class="template-label"><strong>{{ item.msgLabel }}</strong><small>{{ templateEditorLabel(item) }}</small></span><span class="template-copy"><strong>{{ item.sendTitle }}</strong><small>{{ item.sendContent }}</small></span><time>{{ formatTime(item.sendUpdAt) }}</time><span class="template-row-actions"><button class="template-edit-action" @click="editTemplate(item)"><i class="bi bi-pencil"></i>修改</button><button class="template-delete-action" @click="deleteTemplate(item)"><span>×</span>刪除</button></span></article></div>
      <nav class="pagination" aria-label="範本頁籤"><button :disabled="templatePage === 1" @click="changePage('template', 1, templatePageCount)">&lt;&lt;</button><button :disabled="templatePage === 1" @click="changePage('template', templatePage - 1, templatePageCount)">&lt;</button><button v-for="page in firstPages(templatePageCount)" :key="page" :class="{ active: templatePage === page }" @click="changePage('template', page, templatePageCount)">{{ page }}</button><span v-if="templatePageCount > 2">…</span><button v-if="templatePageCount > 2" :class="{ active: templatePage === templatePageCount }" @click="changePage('template', templatePageCount, templatePageCount)">{{ templatePageCount }}</button><button :disabled="templatePage === templatePageCount" @click="changePage('template', templatePage + 1, templatePageCount)">&gt;</button><button :disabled="templatePage === templatePageCount" @click="changePage('template', templatePageCount, templatePageCount)">&gt;&gt;</button></nav>
    </section>

    <section v-else ref="messagePanel" class="content-panel paged-panel">
      <header class="panel-toolbar"><div><h2>訊息總覽</h2><p>只顯示 send_status = SEND。</p></div><label>訊息類型<select v-model="messageFilter" @change="changeFilter('message')"><option v-for="filter in messageFilters" :key="filter" :value="filter">{{ filter === 'ALL' ? '全部訊息' : filter }}</option></select></label></header>
      <div class="message-list"><div class="message-columns"><span>send_id</span><span>msg_function</span><span>msg_label</span><span>訊息內容</span><span>寄件時間</span></div><article v-for="item in visibleMessages" :key="item.sendId" class="message-row"><span>#{{ item.sendId }}</span><span>{{ item.msgFunction }}</span><span class="message-label"><strong>{{ item.msgLabel }}</strong><small>{{ senderLabel(item) }}</small></span><span class="message-copy"><strong>{{ item.sendTitle }}</strong><small>{{ item.sendContent }}</small></span><time>{{ formatTime(item.sendUpdAt) }}</time></article></div>
      <nav class="pagination" aria-label="訊息總覽頁籤"><button :disabled="messagePage === 1" @click="changePage('message', 1, messagePageCount)">&lt;&lt;</button><button :disabled="messagePage === 1" @click="changePage('message', messagePage - 1, messagePageCount)">&lt;</button><button v-for="page in firstPages(messagePageCount)" :key="page" :class="{ active: messagePage === page }" @click="changePage('message', page, messagePageCount)">{{ page }}</button><span v-if="messagePageCount > 2">…</span><button v-if="messagePageCount > 2" :class="{ active: messagePage === messagePageCount }" @click="changePage('message', messagePageCount, messagePageCount)">{{ messagePageCount }}</button><button :disabled="messagePage === messagePageCount" @click="changePage('message', messagePage + 1, messagePageCount)">&gt;</button><button :disabled="messagePage === messagePageCount" @click="changePage('message', messagePageCount, messagePageCount)">&gt;&gt;</button></nav>
    </section>
  </section>
</template>

<style scoped>
.admin-message-page{display:grid;gap:var(--space-4);color:var(--color-text)}.page-heading p,.page-heading h1,.content-panel h2,.content-panel header p{margin:0}.page-heading p,.content-panel header p{color:var(--color-text-muted);font-size:var(--font-size-sm)}.page-heading h1{font-family:var(--font-heading);font-size:var(--font-size-xl)}.section-tabs{display:flex;border-bottom:1px solid var(--color-border)}.section-tabs button{min-height:48px;padding-inline:var(--space-5);font:inherit;font-weight:600;color:var(--color-text-muted);background:transparent;border:0;border-bottom:4px solid transparent}.section-tabs button.active{color:var(--color-primary-active);border-bottom-color:var(--color-primary)}.content-panel{overflow:hidden;background:var(--color-surface);border:1px solid var(--color-border);border-radius:var(--radius-lg);scroll-margin-top:var(--space-5)}.create-form{display:grid;gap:var(--space-4);padding:var(--space-5)}.create-form label{display:grid;gap:var(--space-2);color:var(--color-text-muted);font-size:var(--font-size-sm)}.create-form input,.create-form textarea,.create-form select,.panel-toolbar select{padding:var(--space-3);font:inherit;background:var(--color-surface);border:1px solid var(--color-border-strong);border-radius:var(--radius-md)}.primary-action{justify-self:end;min-height:40px;padding-inline:var(--space-4);color:var(--color-surface);font-weight:600;background:var(--color-primary);border:1px solid var(--color-primary);border-radius:var(--radius-md)}.notice{padding:var(--space-3);color:var(--color-primary-active);background:var(--color-primary-soft);border-radius:var(--radius-md)}.paged-panel{padding-bottom:195px}.panel-toolbar{display:flex;align-items:center;justify-content:space-between;gap:var(--space-4);padding:var(--space-4);background:var(--color-surface-soft);border-bottom:1px solid var(--color-border)}.panel-toolbar label{display:flex;align-items:center;gap:var(--space-2);color:var(--color-text-muted);font-size:var(--font-size-sm)}.template-row,.message-row{display:grid;height:65px;align-items:center;gap:var(--space-3);padding-inline:var(--space-4);border-bottom:1px solid var(--color-border)}.template-row{grid-template-columns:1fr 1.5fr 90px 150px}.message-row{grid-template-columns:80px 120px minmax(0,1fr) 100px 150px}.template-row span,.template-row strong,.template-row small,.template-row time,.message-row>*{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.message-copy{display:grid;min-width:0}.message-copy strong,.message-copy small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.template-row small,.template-row time,.message-row,.message-copy small{color:var(--color-text-muted);font-size:var(--font-size-sm)}.pagination{display:flex;align-items:center;justify-content:center;gap:var(--space-2);padding:var(--space-3);border-top:1px solid var(--color-border)}.pagination button{min-width:36px;min-height:36px;color:var(--color-text-muted);background:var(--color-surface);border:1px solid var(--color-border);border-radius:var(--radius-md)}.pagination button.active{color:var(--color-surface);background:var(--color-primary);border-color:var(--color-primary)}button:focus-visible,select:focus-visible,input:focus-visible,textarea:focus-visible{outline:none;box-shadow:var(--shadow-focus)}@media(max-width:900px){.message-row{grid-template-columns:70px 90px minmax(180px,1fr) 80px 130px}.content-panel{overflow-x:auto}.template-row,.message-row,.panel-toolbar,.pagination{min-width:760px}}
.message-row,.message-columns{grid-template-columns:80px 110px minmax(150px,.9fr) minmax(240px,1.8fr) 150px}.message-columns{display:grid;min-height:44px;align-items:center;gap:var(--space-3);padding-inline:var(--space-4);color:var(--color-text-muted);font-size:var(--font-size-xs);font-weight:700;background:var(--color-bg-muted);border-bottom:1px solid var(--color-border)}.message-label{display:grid;min-width:0}.message-label>*{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.message-label small{color:var(--color-text-muted);font-size:var(--font-size-xs)}@media(max-width:900px){.message-columns{min-width:760px}}
.template-row,.template-columns{grid-template-columns:80px 110px minmax(150px,.9fr) minmax(240px,1.8fr) 150px}.template-columns{display:grid;min-height:44px;align-items:center;gap:var(--space-3);padding-inline:var(--space-4);color:var(--color-text-muted);font-size:var(--font-size-xs);font-weight:700;background:var(--color-bg-muted);border-bottom:1px solid var(--color-border)}.template-label,.template-copy{display:grid;min-width:0}.template-label>*,.template-copy>*{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.template-label small,.template-copy small{color:var(--color-text-muted);font-size:var(--font-size-xs)}@media(max-width:900px){.template-columns{min-width:760px}}
.template-actions-bar{display:flex;min-height:60px;align-items:center;justify-content:space-between;gap:var(--space-3);padding:var(--space-3) var(--space-4);background:var(--color-surface-soft);border-bottom:1px solid var(--color-border)}.template-actions-bar>label,.template-actions-bar>div,.template-row-actions{display:flex;align-items:center;gap:var(--space-2)}.template-actions-bar button,.template-row-actions button{display:inline-flex;min-height:36px;align-items:center;justify-content:center;gap:var(--space-1);padding-inline:var(--space-3);font-weight:600;border-radius:var(--radius-md)}.template-actions-bar button{min-width:132px}.template-add-action,.template-edit-action{color:var(--color-surface);background:var(--color-primary);border:1px solid var(--color-primary)}.template-batch-delete,.template-delete-action{color:var(--color-danger);background:var(--color-surface);border:1px solid var(--color-danger)}.template-batch-delete:disabled{color:var(--color-text-subtle);background:var(--color-disabled-bg);border-color:var(--color-disabled)}.template-row,.template-columns{grid-template-columns:42px 74px 105px minmax(145px,.8fr) minmax(210px,1.4fr) 145px 176px}.template-check{display:grid;height:65px;place-items:center}.template-row-actions{overflow:visible}.template-row-actions button{padding-inline:var(--space-2)}@media(max-width:1100px){.template-row,.template-columns,.template-actions-bar{min-width:1000px}}
</style>
