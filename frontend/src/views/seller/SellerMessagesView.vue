<script setup>
import { computed, reactive, ref } from 'vue'
const inboxTabs = [
  { key: 'ALL', label: '全部訊息' },
  { key: 'SYSTEM_NOTICE', label: '平台公告' },
  { key: 'NEW_ORDER', label: '訂單進度' },
  { key: 'CANCELLED_ORDER', label: '取消訂單' },
]
const outboxTabs = [
  { key: 'TEMPLATES', label: '範本管理' },
  { key: 'CREATE', label: '新增訊息' },
  { key: 'OUTBOX', label: '寄件備份' },
]
const sentBackup = reactive(
  Array.from({ length: 8 }, (_, index) => ({
    sendId: index + 1,
    sendTitle: ['訂單出貨通知', '商品補貨回覆', '售後服務回覆'][index % 3],
    sendContent: `這是第 ${index + 1} 則已寄出的商家訊息內容。`,
    msgLabel: ['出貨提醒', '補貨回覆', '售後服務'][index % 3],
    orderNo: `DG2026${String(index + 1).padStart(4, '0')}`,
    sendUpdAt: new Date(Date.now() - index * 7200000).toISOString(),
  })),
)
const categoryTabs = inboxTabs.slice(1)
const titleMap = {
  SYSTEM_NOTICE: ['平台維護公告', '商家政策更新', '服務功能通知'],
  NEW_ORDER: ['收到新訂單', '訂單付款完成', '訂單等待出貨'],
  CANCELLED_ORDER: ['買家取消訂單', '訂單取消完成', '退款處理通知'],
}
const messages = reactive(
  categoryTabs.flatMap((category, categoryIndex) =>
    Array.from({ length: 36 }, (_, index) => ({
      recordId: categoryIndex * 100 + index + 1,
      category: category.key,
      recordStatus: index % 4 === 0 ? 'READ' : 'UNREAD',
      sendTitle: titleMap[category.key][index % 3],
      sendContent: `這是第 ${index + 1} 則${category.label}內容，點擊可標示為已讀。`,
      recordCreatedAt: new Date(Date.now() - (categoryIndex * 36 + index) * 3600000).toISOString(),
    })),
  ),
)
const activeTab = ref('ALL'),
  statusFilter = ref('ALL'),
  pageSize = 20,
  currentPage = ref(1),
  selectedIds = ref(new Set())
const createForm = reactive({
  orderId: '',
  templateId: '',
  msgLabel: '',
  sendTitle: '',
  sendContent: '',
  sendRemark: '',
  images: [],
})
const createOrders = [
  { orderId: 101, orderNo: 'DG20260001', status: 'PAID' },
  { orderId: 102, orderNo: 'DG20260002', status: 'PROCESSING' },
  { orderId: 103, orderNo: 'DG20260003', status: 'SHIPPED' },
]
const createTemplates = reactive([
  {
    sendId: 1,
    msgLabel: '出貨提醒',
    sendTitle: '商品已出貨',
    sendContent: '您的商品已完成出貨，請留意物流進度。',
  },
  {
    sendId: 2,
    msgLabel: '訂單確認',
    sendTitle: '訂單內容確認',
    sendContent: '感謝您的訂購，我們正在處理您的訂單。',
  },
])
const selectedTemplateIds = ref(new Set()),
  templateDetail = ref(null),
  templateEditor = reactive({
    open: false,
    sendId: null,
    msgLabel: '',
    sendTitle: '',
    sendContent: '',
  })
const selectedOutboxIds = ref(new Set()),
  outboxDetail = ref(null)
const createNotice = ref('')
const canFilter = computed(() => categoryTabs.some((tab) => tab.key === activeTab.value))
const sourceMessages = computed(() =>
  messages
    .filter((item) => activeTab.value === 'ALL' || item.category === activeTab.value)
    .sort((a, b) => new Date(b.recordCreatedAt) - new Date(a.recordCreatedAt)),
)
const filteredMessages = computed(() =>
  !canFilter.value || statusFilter.value === 'ALL'
    ? sourceMessages.value
    : sourceMessages.value.filter((item) => item.recordStatus === statusFilter.value),
)
const pageCount = computed(() => Math.max(1, Math.ceil(filteredMessages.value.length / pageSize)))
const visibleMessages = computed(() =>
  filteredMessages.value.slice((currentPage.value - 1) * pageSize, currentPage.value * pageSize),
)
const pageButtons = computed(() => [1, 2].filter((page) => page <= pageCount.value))
const allSelected = computed(
  () =>
    visibleMessages.value.length > 0 &&
    visibleMessages.value.every((item) => selectedIds.value.has(item.recordId)),
)
const allTemplatesSelected = computed(
  () =>
    createTemplates.length > 0 &&
    createTemplates.every((item) => selectedTemplateIds.value.has(item.sendId)),
)
const unreadFilteredMessages = computed(() =>
  filteredMessages.value.filter((item) => item.recordStatus === 'UNREAD'),
)
function hasUnread(category) {
  return messages.some(
    (item) => (category === 'ALL' || item.category === category) && item.recordStatus === 'UNREAD',
  )
}
function selectTab(key) {
  activeTab.value = key
  statusFilter.value = 'ALL'
  resetPage()
}
function resetPage() {
  currentPage.value = 1
  selectedIds.value = new Set()
}
function goToPage(page) {
  currentPage.value = Math.min(Math.max(1, page), pageCount.value)
  selectedIds.value = new Set()
  requestAnimationFrame(() =>
    document
      .querySelector('.message-panel')
      ?.scrollIntoView({ behavior: 'smooth', block: 'start' }),
  )
}
function toggleOne(id) {
  const next = new Set(selectedIds.value)
  next.has(id) ? next.delete(id) : next.add(id)
  selectedIds.value = next
}
function toggleAll() {
  const next = new Set(selectedIds.value)
  visibleMessages.value.forEach((item) =>
    allSelected.value ? next.delete(item.recordId) : next.add(item.recordId),
  )
  selectedIds.value = next
}
function deleteSelected() {
  for (let index = messages.length - 1; index >= 0; index -= 1)
    if (selectedIds.value.has(messages[index].recordId)) messages.splice(index, 1)
  resetPage()
}
function markAllFilteredRead() {
  unreadFilteredMessages.value.forEach((item) => {
    item.recordStatus = 'READ'
  })
}
function applySelectedTemplate() {
  const template = createTemplates.find((item) => item.sendId === Number(createForm.templateId))
  if (template)
    Object.assign(createForm, {
      msgLabel: template.msgLabel,
      sendTitle: template.sendTitle,
      sendContent: template.sendContent,
    })
}
async function selectImages(event) {
  const files = [...(event.target.files ?? [])].slice(0, 3)
  createForm.images = files.map((file) => ({ name: file.name }))
  event.target.value = ''
}
function submitNewMessage(mode) {
  if (!createForm.sendTitle.trim() || !createForm.sendContent.trim() || !createForm.orderId) return
  createNotice.value = mode === 'SEND_ONLY' ? '訊息已寄出。' : '訊息已寄出，並已儲存為範本。'
  Object.assign(createForm, {
    orderId: '',
    templateId: '',
    msgLabel: '',
    sendTitle: '',
    sendContent: '',
    sendRemark: '',
    images: [],
  })
}
function toggleTemplate(id) {
  const next = new Set(selectedTemplateIds.value)
  next.has(id) ? next.delete(id) : next.add(id)
  selectedTemplateIds.value = next
}
function toggleAllTemplates() {
  selectedTemplateIds.value = allTemplatesSelected.value
    ? new Set()
    : new Set(createTemplates.map((item) => item.sendId))
}
function openTemplateEditor(item = null) {
  Object.assign(templateEditor, {
    open: true,
    sendId: item?.sendId ?? null,
    msgLabel: item?.msgLabel ?? '',
    sendTitle: item?.sendTitle ?? '',
    sendContent: item?.sendContent ?? '',
  })
}
function editSelectedTemplate() {
  openTemplateEditor(createTemplates.find((item) => selectedTemplateIds.value.has(item.sendId)))
}
function deleteSelectedTemplates() {
  for (let i = createTemplates.length - 1; i >= 0; i--) {
    const item = createTemplates[i]
    if (
      selectedTemplateIds.value.has(item.sendId) &&
      window.confirm(`是否確認刪除${item.msgLabel}範本`)
    )
      createTemplates.splice(i, 1)
  }
  selectedTemplateIds.value = new Set()
}
function editTemplate(item) {
  openTemplateEditor(item)
}
function deleteTemplate(item) {
  if (!window.confirm(`是否確認刪除${item.msgLabel}範本`)) return
  const index = createTemplates.findIndex((value) => value.sendId === item.sendId)
  if (index >= 0) createTemplates.splice(index, 1)
  const next = new Set(selectedTemplateIds.value)
  next.delete(item.sendId)
  selectedTemplateIds.value = next
}
function editTemplateFromDetail() {
  const item = templateDetail.value
  templateDetail.value = null
  if (item) openTemplateEditor(item)
}
function deleteTemplateFromDetail() {
  const item = templateDetail.value
  if (!item || !window.confirm(`是否確認刪除${item.msgLabel}範本`)) return
  const index = createTemplates.findIndex((value) => value.sendId === item.sendId)
  if (index >= 0) createTemplates.splice(index, 1)
  const next = new Set(selectedTemplateIds.value)
  next.delete(item.sendId)
  selectedTemplateIds.value = next
  templateDetail.value = null
}
function saveTemplateEditor() {
  const item = createTemplates.find((value) => value.sendId === templateEditor.sendId)
  if (item) Object.assign(item, templateEditor)
  else
    createTemplates.push({
      sendId: Date.now(),
      msgLabel: templateEditor.msgLabel,
      sendTitle: templateEditor.sendTitle,
      sendContent: templateEditor.sendContent,
    })
  templateEditor.open = false
}
function toggleOutbox(id) {
  const next = new Set(selectedOutboxIds.value)
  next.has(id) ? next.delete(id) : next.add(id)
  selectedOutboxIds.value = next
}
function deleteSelectedOutbox() {
  for (let i = sentBackup.length - 1; i >= 0; i--)
    if (selectedOutboxIds.value.has(sentBackup[i].sendId)) sentBackup.splice(i, 1)
  selectedOutboxIds.value = new Set()
}
function formatTime(value) {
  return new Intl.DateTimeFormat('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}
</script>
<template>
  <section class="seller-page">
    <header>
      <p class="eyebrow">訊息管理</p>
      <h1>訊息中心</h1>
      <p>查看平台公告與訂單動態，管理寄件範本。</p>
    </header>
    <div class="message-layout">
      <nav class="category-panel">
        <p class="category-heading">收件匣</p>
        <button
          v-for="tab in inboxTabs"
          :key="tab.key"
          :class="{ active: activeTab === tab.key }"
          @click="selectTab(tab.key)"
        >
          <span>{{ tab.label }}</span
          ><span v-if="hasUnread(tab.key)" class="category-unread-dot"></span>
        </button>
        <p class="category-heading outbox-heading">寄件匣</p>
        <button
          v-for="tab in outboxTabs"
          :key="tab.key"
          :class="{ active: activeTab === tab.key }"
          @click="selectTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </nav>
      <div
        class="message-panel"
        :class="{ 'message-panel--inbox': !['TEMPLATES', 'CREATE', 'OUTBOX'].includes(activeTab) }"
      >
        <div v-if="activeTab === 'TEMPLATES'" class="feature-state">
          <h2>範本管理</h2>
          <p>管理商家訊息範本與套用內容。</p>
        </div>
        <form v-else-if="activeTab === 'CREATE'" class="create-message-form" @submit.prevent>
          <header>
            <h2>新增訊息</h2>
            <p>編寫訊息並選擇寄出或儲存方式。</p>
          </header>
          <p v-if="createNotice" class="create-notice">{{ createNotice }}</p>
          <label
            >選擇訂單（寄出時必填）<select v-model="createForm.orderId">
              <option value="">請選擇未完成且未取消的訂單</option>
              <option v-for="order in createOrders" :key="order.orderId" :value="order.orderId">
                {{ order.orderNo }} · {{ order.status }}
              </option>
            </select></label
          ><label
            >套用現有範本<select v-model="createForm.templateId" @change="applySelectedTemplate">
              <option value="">不套用範本</option>
              <option
                v-for="template in createTemplates"
                :key="template.sendId"
                :value="template.sendId"
              >
                {{ template.msgLabel }}
              </option>
            </select></label
          ><label
            >範本名稱<input
              v-model="createForm.msgLabel"
              maxlength="50"
              placeholder="未填時使用訊息標題" /></label
          ><label>訊息標題<input v-model="createForm.sendTitle" maxlength="100" required /></label
          ><label class="textarea-field"
            >訊息內容<textarea
              v-model="createForm.sendContent"
              maxlength="1000"
              rows="8"
              required
            ></textarea
            ><small class="field-counter">{{ createForm.sendContent.length }}/1000</small></label
          ><label class="textarea-field"
            >備註<textarea v-model="createForm.sendRemark" maxlength="1000" rows="3"></textarea
            ><small class="field-counter">{{ createForm.sendRemark.length }}/1000</small></label
          ><label class="image-upload"
            >上傳圖片（至多三張）<input
              type="file"
              accept="image/*"
              multiple
              @change="selectImages"
            /><small>{{ createForm.images.length }}/3</small></label
          >
          <ul v-if="createForm.images.length" class="image-file-list">
            <li v-for="image in createForm.images" :key="image.name">{{ image.name }}</li>
          </ul>
          <div class="create-actions">
            <button type="button" class="send-only-button" @click="submitNewMessage('SEND_ONLY')">
              只寄出訊息</button
            ><button
              type="button"
              class="save-template-button"
              @click="submitNewMessage('SAVE_ONLY')"
            >
              只儲存範本</button
            ><button
              type="button"
              class="send-template-button"
              @click="submitNewMessage('SEND_AND_SAVE')"
            >
              寄出並儲存範本
            </button>
          </div>
        </form>
        <div v-else-if="activeTab === 'OUTBOX'" class="sent-backup">
          <header>
            <div>
              <h2>寄件備份</h2>
              <p>保留商家已實際寄出的訊息紀錄。</p>
            </div>
            <button :disabled="!selectedOutboxIds.size" @click="deleteSelectedOutbox">
              刪除已選（{{ selectedOutboxIds.size }}）
            </button>
          </header>
          <article v-for="message in sentBackup" :key="message.sendId" class="sent-backup-row">
            <input
              type="checkbox"
              :checked="selectedOutboxIds.has(message.sendId)"
              @change="toggleOutbox(message.sendId)"
            />
            <button @click="outboxDetail = message">
              <strong>{{ message.sendTitle }}</strong
              ><small>{{ message.sendContent }}</small>
            </button>
            <span class="outbox-field">{{ message.msgLabel }}</span>
            <span class="outbox-field">{{ message.orderNo }}</span
            ><time>{{ formatTime(message.sendUpdAt) }}</time>
          </article>
        </div>
        <template v-else>
          <div class="message-toolbar">
            <label><input type="checkbox" :checked="allSelected" @change="toggleAll" />全選</label
            ><label v-if="canFilter" class="status-filter"
              ><select v-model="statusFilter" @change="resetPage">
                <option value="ALL">全部訊息</option>
                <option value="UNREAD">未讀取</option>
                <option value="READ">已讀取</option>
              </select></label
            ><button
              class="read-all-button"
              :disabled="!unreadFilteredMessages.length"
              @click="markAllFilteredRead"
            >
              全部設為已讀</button
            ><button class="delete-button" :disabled="!selectedIds.size" @click="deleteSelected">
              刪除已選（{{ selectedIds.size }}）
            </button>
          </div>
          <div v-if="!visibleMessages.length" class="feature-state">目前沒有符合條件的訊息。</div>
          <div v-else>
            <article
              v-for="message in visibleMessages"
              :key="message.recordId"
              class="message-item"
              :class="{ read: message.recordStatus === 'READ' }"
            >
              <label class="message-check"
                ><input
                  type="checkbox"
                  :checked="selectedIds.has(message.recordId)"
                  @change="toggleOne(message.recordId)" /></label
              ><button class="message-row" @click="message.recordStatus = 'READ'">
                <span class="status-dot" :class="{ read: message.recordStatus === 'READ' }"></span
                ><span class="message-copy"
                  ><strong>{{ message.sendTitle }}</strong
                  ><span>{{ message.sendContent }}</span></span
                ><time>{{ formatTime(message.recordCreatedAt) }}</time>
              </button>
            </article>
          </div>
          <nav class="pagination">
            <button :disabled="currentPage === 1" @click="goToPage(1)">&lt;&lt;</button
            ><button :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">&lt;</button
            ><button
              v-for="page in pageButtons"
              :key="page"
              :class="{ active: currentPage === page }"
              @click="goToPage(page)"
            >
              {{ page }}</button
            ><span v-if="pageCount > 2">…</span
            ><button
              v-if="pageCount > 2"
              :class="{ active: currentPage === pageCount }"
              @click="goToPage(pageCount)"
            >
              {{ pageCount }}</button
            ><button :disabled="currentPage === pageCount" @click="goToPage(currentPage + 1)">
              &gt;</button
            ><button :disabled="currentPage === pageCount" @click="goToPage(pageCount)">
              &gt;&gt;
            </button>
          </nav></template
        >
      </div>
    </div>
    <section v-if="activeTab === 'TEMPLATES'" class="template-preview-manager">
      <header>
        <h2>範本管理</h2>
      </header>
      <div class="template-toolbar">
        <label
          ><input
            type="checkbox"
            :checked="allTemplatesSelected"
            @change="toggleAllTemplates"
          />全選</label
        >
        <button type="button" @click="openTemplateEditor()">新增範本</button>
        <button :disabled="selectedTemplateIds.size !== 1" @click="editSelectedTemplate">
          修改範本</button
        ><button :disabled="!selectedTemplateIds.size" @click="deleteSelectedTemplates">
          刪除範本
        </button>
      </div>
      <article v-for="item in createTemplates" :key="item.sendId">
        <label class="template-check" @click.stop
          ><input
            type="checkbox"
            :checked="selectedTemplateIds.has(item.sendId)"
            @change="toggleTemplate(item.sendId)" /></label
        ><button @click="templateDetail = item">
          <strong>{{ item.msgLabel }}</strong
          ><small>{{ item.sendContent }}</small></button
        ><button
          class="template-row-action"
          type="button"
          aria-label="修改範本"
          @click="editTemplate(item)"
        >
          <i class="bi bi-pencil" aria-hidden="true"></i></button
        ><button
          class="template-row-action template-row-delete"
          type="button"
          aria-label="刪除範本"
          @click="deleteTemplate(item)"
        >
          ×
        </button>
      </article>
    </section>
    <div v-if="outboxDetail" class="template-overlay" @click.self="outboxDetail = null">
      <article class="template-dialog outbox-dialog" role="dialog" aria-modal="true">
        <button class="outbox-dialog__close" aria-label="關閉寄件詳情" @click="outboxDetail = null">
          ×
        </button>
        <header>
          <p>{{ outboxDetail.msgLabel }}</p>
          <h2>{{ outboxDetail.sendTitle }}</h2>
          <p>訂單 {{ outboxDetail.orderNo }}</p>
          <time>{{ formatTime(outboxDetail.sendUpdAt) }}</time>
        </header>
        <div class="outbox-dialog__content">{{ outboxDetail.sendContent }}</div>
      </article>
    </div>
    <div v-if="templateDetail" class="template-overlay" @click.self="templateDetail = null">
      <article class="template-dialog template-detail-dialog" role="dialog" aria-modal="true">
        <button
          class="template-detail-dialog__close"
          aria-label="關閉範本詳情"
          @click="templateDetail = null"
        >
          ×
        </button>
        <header>
          <p>{{ templateDetail.msgLabel }}</p>
          <h2>{{ templateDetail.sendTitle }}</h2>
        </header>
        <div class="template-detail-dialog__actions">
          <button type="button" @click="editTemplateFromDetail">修改</button
          ><button type="button" class="template-detail-delete" @click="deleteTemplateFromDetail">
            刪除
          </button>
        </div>
        <div class="template-detail-dialog__content">{{ templateDetail.sendContent }}</div>
      </article>
    </div>
    <div
      v-if="templateEditor.open"
      class="template-overlay"
      @click.self="templateEditor.open = false"
    >
      <form class="template-dialog" @submit.prevent="saveTemplateEditor">
        <input v-model="templateEditor.msgLabel" placeholder="範本名稱" /><input
          v-model="templateEditor.sendTitle"
          placeholder="標題"
        /><textarea v-model="templateEditor.sendContent" rows="8"></textarea
        ><button type="submit">儲存範本</button>
      </form>
    </div>
  </section>
</template>
<style scoped>
.seller-page {
  display: grid;
  gap: var(--space-5);
  max-width: 1160px;
}
header p,
h1 {
  margin: 0;
}
.eyebrow,
header p:last-child {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
h1 {
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}
.message-layout {
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr);
  gap: var(--space-5);
  min-height: 460px;
}
.category-panel,
.message-panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.category-panel {
  display: grid;
  align-self: start;
  gap: var(--space-1);
  padding: var(--space-3);
}
.category-heading {
  margin: var(--space-1) var(--space-3);
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  font-weight: 700;
}
.outbox-heading {
  margin-top: var(--space-4);
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);
}
.category-panel button {
  position: relative;
  min-height: 44px;
  padding: 0 var(--space-3);
  text-align: left;
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
}
.category-panel button.active {
  color: var(--color-primary-active);
  font-weight: 700;
  background: var(--color-primary-soft);
}
.category-unread-dot {
  position: absolute;
  top: var(--space-2);
  right: var(--space-2);
  width: var(--space-2);
  height: var(--space-2);
  background: var(--color-primary);
  border-radius: 50%;
}
.message-panel {
  min-width: 0;
  overflow: hidden;
}
.message-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  background: var(--color-surface-soft);
  border-bottom: 1px solid var(--color-border);
}
.message-toolbar label {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
}
.status-filter {
  margin-left: auto;
}
.message-toolbar select,
.delete-button {
  min-height: 36px;
  padding-inline: var(--space-3);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
}
.delete-button {
  color: var(--color-danger);
  background: var(--color-surface);
  border-color: var(--color-danger);
}
.message-item {
  display: grid;
  grid-template-columns: var(--space-7) minmax(0, 1fr);
  border-bottom: 1px solid var(--color-border);
}
.message-check {
  display: grid;
  place-items: center;
}
.message-row {
  display: grid;
  width: 100%;
  min-height: 68px;
  grid-template-columns: var(--space-3) minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  text-align: left;
  background: var(--color-surface);
  border: 0;
}
.status-dot {
  width: var(--space-2);
  height: var(--space-2);
  background: var(--color-primary);
  border-radius: 50%;
}
.status-dot.read {
  background: var(--color-disabled);
}
.message-copy {
  display: grid;
  min-width: 0;
}
.message-copy strong,
.message-copy span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.message-copy span,
time {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}
.message-item.read strong,
.message-item.read .message-copy span,
.message-item.read time {
  color: var(--color-text-subtle);
}
.pagination {
  display: flex;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-3);
}
.pagination button {
  min-width: 36px;
  min-height: 36px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.pagination button.active {
  color: white;
  background: var(--color-primary);
}
.feature-state {
  display: grid;
  min-height: 320px;
  place-content: center;
  justify-items: center;
  color: var(--color-text-muted);
}
@media (max-width: 767px) {
  .message-layout {
    grid-template-columns: 1fr;
  }
  .category-panel {
    display: flex;
    overflow-x: auto;
  }
  .category-heading {
    display: none;
  }
  .message-toolbar {
    flex-wrap: wrap;
  }
  .status-filter {
    margin-left: 0;
  }
  .message-row {
    grid-template-columns: var(--space-3) minmax(0, 1fr);
  }
  .message-row time {
    grid-column: 2;
  }
  .pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
.message-toolbar {
  min-height: 48px;
  padding-block: var(--space-2);
}
.status-filter {
  margin-left: auto;
}
.message-item {
  height: var(--seller-message-row-height);
  min-height: 0;
  overflow: hidden;
  grid-template-columns: 42px minmax(0, 1fr);
  background: var(--color-surface);
}
.message-row {
  height: 100%;
  min-height: 0;
  overflow: hidden;
  grid-template-columns: var(--space-2) minmax(0, 1fr) auto;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-4) var(--space-2) 0;
  background: transparent;
}
.message-copy {
  gap: 0;
}
.message-copy strong,
.message-copy span,
.message-row time {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.message-panel--inbox {
  --seller-message-row-height: 65px;
  padding-bottom: calc(3 * var(--seller-message-row-height));
  scroll-margin-top: var(--space-5);
}
@media (max-width: 767px) {
  .message-row {
    grid-template-columns: var(--space-2) minmax(0, 1fr);
  }
  .message-row time {
    grid-column: 2;
  }
}
.read-all-button {
  min-height: 36px;
  margin-left: auto;
  padding-inline: var(--space-3);
  color: var(--color-primary-active);
  background: var(--color-surface);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
}
.status-filter + .read-all-button {
  margin-left: 0;
}
.read-all-button:disabled {
  color: var(--color-text-subtle);
  background: var(--color-disabled-bg);
  border-color: var(--color-disabled);
}
.create-message-form {
  display: grid;
  gap: var(--space-4);
  padding: var(--space-5);
}
.create-message-form header h2,
.create-message-form header p {
  margin: 0;
}
.create-message-form header p {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
.create-message-form label {
  display: grid;
  gap: var(--space-2);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
.create-message-form input,
.create-message-form textarea {
  width: 100%;
  padding: var(--space-3);
  font: inherit;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
}
.create-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}
.create-actions button {
  min-height: 40px;
  padding-inline: var(--space-4);
  border-radius: var(--radius-md);
}
.save-template-button {
  color: var(--color-primary-active);
  background: var(--color-surface);
  border: 1px solid var(--color-primary);
}
.send-template-button {
  color: var(--color-surface);
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
}
.create-notice {
  margin: 0;
  padding: var(--space-3);
  color: var(--color-primary-active);
  background: var(--color-primary-soft);
  border-radius: var(--radius-md);
}
.sent-backup {
  display: grid;
}
.sent-backup > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-5);
  border-bottom: 1px solid var(--color-border);
}
.sent-backup > header h2,
.sent-backup > header p {
  margin: 0;
}
.sent-backup-row {
  display: grid;
  min-height: 65px;
  grid-template-columns: 20px minmax(0, 1fr) minmax(90px, 0.35fr) minmax(120px, 0.45fr) auto;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-2) var(--space-4);
  border-bottom: 1px solid var(--color-border);
}
.sent-backup-row > button {
  display: grid;
  min-width: 0;
  padding: 0;
  text-align: left;
  background: transparent;
  border: 0;
}
.sent-backup-row strong,
.sent-backup-row small,
.sent-backup-row .outbox-field {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sent-backup-row small,
.sent-backup-row .outbox-field,
.sent-backup-row span,
.sent-backup-row time {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}
.outbox-dialog__close {
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  width: var(--space-7);
  height: var(--space-7);
  color: var(--color-text-muted);
  font-size: var(--font-size-xl);
  background: transparent;
  border: 0;
}
.outbox-dialog header {
  padding-right: var(--space-7);
}
.outbox-dialog header p,
.outbox-dialog h2 {
  margin: 0;
}
.outbox-dialog h2 {
  margin-block: var(--space-1);
}
.outbox-dialog time {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}
.outbox-dialog__content {
  margin-top: var(--space-5);
  padding-top: var(--space-5);
  white-space: pre-wrap;
  border-top: 1px solid var(--color-border);
}
.textarea-field {
  position: relative;
}
.textarea-field textarea {
  padding-bottom: var(--space-6);
}
.field-counter {
  position: absolute;
  right: var(--space-3);
  bottom: var(--space-2);
  color: var(--color-text-subtle);
  font-size: var(--font-size-xs);
  line-height: 1;
  pointer-events: none;
}
.create-message-form select {
  width: 100%;
  min-height: 42px;
  padding-inline: var(--space-3);
  font: inherit;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
}
.send-only-button {
  color: var(--color-primary-active);
  background: var(--color-primary-soft);
  border: 1px solid var(--color-primary);
}
.image-upload {
  position: relative;
}
.image-upload small {
  position: absolute;
  top: 0;
  right: 0;
}
.image-file-list {
  margin: calc(-1 * var(--space-2)) 0 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}
.create-actions .save-template-button {
  display: none;
}
.template-preview-manager {
  position: absolute;
  top: 120px;
  left: 230px;
  right: 0;
  z-index: 2;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.seller-page {
  position: relative;
}
.template-preview-manager > header,
.template-preview-manager > div {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
  padding: var(--space-3);
}
.template-toolbar label {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  margin-right: auto;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
.template-preview-manager > header {
  justify-content: space-between;
}
.template-add {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  color: var(--color-surface);
  background: var(--color-primary);
  border: 0;
  border-radius: var(--radius-pill);
}
.template-preview-manager article {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) 40px 40px;
  border-top: 1px solid var(--color-border);
}
.template-check {
  display: grid;
  min-height: 65px;
  place-items: center;
}
.template-preview-manager article > button:not(.template-row-action) {
  display: grid;
  padding: var(--space-3);
  text-align: left;
  background: transparent;
  border: 0;
}
.template-row-action {
  align-self: center;
  width: 36px;
  height: 36px;
  padding: 0;
  color: var(--color-primary-active);
  background: transparent;
  border: 0;
  border-radius: var(--radius-md);
}
.template-row-action:hover {
  background: var(--color-primary-soft);
}
.template-row-delete {
  color: var(--color-danger);
  font-size: var(--font-size-lg);
}
.template-overlay {
  position: fixed;
  z-index: 1060;
  inset: 0;
  display: grid;
  place-items: center;
  padding: var(--space-5);
  background: #0008;
}
.template-dialog {
  position: relative;
  display: grid;
  gap: var(--space-3);
  width: min(100%, 680px);
  max-height: calc(100vh - 40px);
  overflow-y: auto;
  padding: var(--space-6);
  background: white;
  border-radius: var(--radius-lg);
}
.template-detail-dialog__close {
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  width: var(--space-7);
  height: var(--space-7);
  color: var(--color-text-muted);
  font-size: var(--font-size-xl);
  background: transparent;
  border: 0;
}
.template-detail-dialog header {
  padding-right: var(--space-7);
}
.template-detail-dialog header p,
.template-detail-dialog h2 {
  margin: 0;
}
.template-detail-dialog h2 {
  margin-top: var(--space-1);
}
.template-detail-dialog__content {
  margin-top: var(--space-5);
  padding-top: var(--space-5);
  white-space: pre-wrap;
  border-top: 1px solid var(--color-border);
}
.template-detail-dialog__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  margin-top: var(--space-3);
}
.template-detail-dialog__actions button {
  min-height: 38px;
  padding-inline: var(--space-4);
  font: inherit;
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
}
.template-detail-dialog__actions button:first-child {
  color: var(--color-primary-active);
  background: var(--color-primary-soft);
}
.template-detail-dialog__actions .template-detail-delete {
  color: var(--color-danger);
  background: var(--color-surface);
  border-color: var(--color-danger);
}
</style>
