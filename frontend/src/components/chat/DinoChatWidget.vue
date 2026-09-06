<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import api from '@/api/axios'
import { getMemberOrders } from '@/api/order'
import logoUrl from '@/assets/images/dinogo-logo-s.png'
import mascotUrl from '@/assets/images/dinogo-mascot.png'
import { useAuthStore } from '@/stores/auth'
import { getOrderDisplayStatus } from '@/utils/orderDisplayStatus'
import { getImageUrl } from '@/utils/imageUrl'

const router = useRouter()
const authStore = useAuthStore()
const isOpen = ref(false)
const activeTab = ref('assistant')
const inputMessage = ref('')
const isLoading = ref(false)
const chatBodyRef = ref(null)
const chatInputRef = ref(null)
const helperMode = ref('general')
const showMoreHelperActions = ref(false)

const helperMessages = ref([
  {
    id: 1,
    sender: 'agent',
    text: '您好，我是 DINO-GO 小幫手！\n想找商品、查看訂單或優惠券，\n都可以直接問我，也可以使用下方快捷功能。',
  },
])
const chatTemplates = [
  { label: '找商品', action: 'product', placeholder: '輸入商品名稱或關鍵字' },
  { label: '查訂單', action: 'order', placeholder: '輸入訂單編號或想查詢的訂單問題' },
  { label: '優惠券', action: 'coupon', placeholder: '輸入優惠券或折扣問題' },
  { label: '購物車', action: 'cart', placeholder: '輸入購物車相關問題' },
]
const moreHelperActions = [
  { label: '商品分類', action: 'category', placeholder: '輸入想看的商品分類' },
  { label: '我的收藏', action: 'favorite', placeholder: '輸入收藏相關問題' },
  { label: '配送資訊', action: 'shipping', placeholder: '輸入配送相關問題' },
  { label: '付款方式', action: 'payment', placeholder: '輸入付款相關問題' },
  { label: '店鋪搜尋', action: 'store', placeholder: '輸入店鋪名稱或關鍵字' },
  { label: '常見問題', action: 'faq', placeholder: '輸入想詢問的問題' },
]

const conversations = ref([])
const activeConversation = ref(null)
const chatMessages = ref([])
const pendingContext = ref(null)
const totalUnread = ref(0)
const chatLoading = ref(false)
const chatError = ref('')
const chatSending = ref(false)
let socket = null
let reconnectTimer = null
let pendingSocketPayloads = []

const activeChatUnread = computed(() =>
  conversations.value.reduce((total, conversation) => total + Number(conversation.unreadCount || 0), 0),
)
const helperPlaceholder = computed(() => {
  if (activeTab.value !== 'assistant') return '輸入想詢問的內容'
  const action = [...chatTemplates, ...moreHelperActions].find((item) => item.action === helperMode.value)
  return action?.placeholder || '輸入想詢問的內容...'
})

function toggleChat() {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    if (activeTab.value === 'chat') void initializeChat()
    scrollToLatest()
    focusComposer()
  }
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'chat') void initializeChat()
  scrollToLatest()
  focusComposer()
}

async function openFromExternal(event) {
  const detail = event.detail || {}
  isOpen.value = true
  activeTab.value = 'chat'
  await initializeChat()
  if (detail.contextType === 'product') {
    await openProductContext(detail)
  } else if (detail.contextType === 'order') {
    await openOrderContext(detail)
  }
  focusComposer()
}

async function initializeChat() {
  if (!authStore.isAuthenticated) {
    chatError.value = '登入後即可使用聊聊。'
    return
  }
  connectSocket()
  await Promise.all([loadConversations(), loadUnreadCount()])
}

function connectSocket() {
  if (!authStore.token || socket?.readyState === WebSocket.OPEN || socket?.readyState === WebSocket.CONNECTING) return
  const base = (import.meta.env?.VITE_API_URL || 'http://localhost:8080/api').replace(/\/api\/?$/, '')
  const wsBase = base.replace(/^http/, 'ws')
  socket = new WebSocket(`${wsBase}/ws/dino-chat?token=${encodeURIComponent(authStore.token)}`)
  socket.onopen = flushPendingSocketMessages
  socket.onmessage = handleSocketMessage
  socket.onclose = () => {
    socket = null
    if (chatSending.value) {
      chatError.value = 'WebSocket 連線中斷，訊息尚未送出。'
      chatSending.value = false
    }
    if (authStore.isAuthenticated) reconnectTimer = window.setTimeout(connectSocket, 2500)
  }
}

function handleSocketMessage(event) {
  const payload = JSON.parse(event.data)
  if (payload.type === 'CONNECTED') {
    flushPendingSocketMessages()
    return
  }
  if (payload.type === 'ERROR') {
    chatError.value = payload.message || '訊息傳送失敗。'
    chatSending.value = false
    return
  }
  if (payload.type !== 'MESSAGE') return
  const message = payload.message
  if (activeConversation.value?.conversationId === message.conversationId) {
    upsertMessage(message)
    if (message.senderMemberId === authStore.member?.memberId) {
      inputMessage.value = ''
      pendingContext.value = null
      chatSending.value = false
    }
    void api.post(`/chat/conversations/${message.conversationId}/open`).then(loadConversations)
  } else {
    void loadConversations()
  }
  void loadUnreadCount()
}

function sendSocketMessage(payload) {
  connectSocket()
  if (socket?.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(payload))
    return
  }
  pendingSocketPayloads.push(payload)
}

function flushPendingSocketMessages() {
  if (socket?.readyState !== WebSocket.OPEN || !pendingSocketPayloads.length) return
  const payloads = pendingSocketPayloads
  pendingSocketPayloads = []
  payloads.forEach((payload) => socket.send(JSON.stringify(payload)))
}

async function loadConversations() {
  if (!authStore.isAuthenticated) return
  try {
    const { data } = await api.get('/chat/conversations')
    conversations.value = Array.isArray(data) ? data : []
  } catch (error) {
    chatError.value = error.response?.data?.message || '目前無法載入聊聊列表。'
  }
}

async function loadUnreadCount() {
  if (!authStore.isAuthenticated) {
    totalUnread.value = 0
    return
  }
  try {
    const { data } = await api.get('/chat/unread-count')
    totalUnread.value = Number(data?.totalUnreadCount || 0)
  } catch {
    totalUnread.value = 0
  }
}

async function openConversation(conversation) {
  activeConversation.value = conversation
  pendingContext.value = null
  chatLoading.value = true
  chatError.value = ''
  try {
    await api.post(`/chat/conversations/${conversation.conversationId}/open`)
    const { data } = await api.get(`/chat/conversations/${conversation.conversationId}/messages`)
    chatMessages.value = Array.isArray(data) ? data : []
    await loadConversations()
    await loadUnreadCount()
    scrollToLatest()
  } catch (error) {
    chatError.value = error.response?.data?.message || '目前無法載入聊天室。'
  } finally {
    chatLoading.value = false
  }
}

async function openProductContext(detail) {
  const conversation = await getOrCreateConversation({
    sellerId: detail.sellerId,
    productId: detail.productId,
    skuId: detail.skuId,
  })
  await openConversation(conversation)
  pendingContext.value = {
    type: 'product',
    productId: detail.productId,
    skuId: detail.skuId,
    productName: detail.productName,
    skuText: detail.skuText,
    price: detail.price,
    imageUrl: detail.imageUrl,
  }
}

async function openOrderContext(detail) {
  const conversation = await getOrCreateConversation({ orderId: detail.orderId })
  await openConversation(conversation)
  pendingContext.value = {
    type: 'order',
    orderId: detail.orderId,
    orderNo: detail.orderNo,
    statusLabel: detail.statusLabel,
    totalAmount: detail.totalAmount,
  }
}

async function getOrCreateConversation(payload) {
  const { data } = await api.post('/chat/conversations', payload)
  return data
}

async function handleSubmit() {
  if (activeTab.value === 'assistant') await submitHelper()
  else submitChat()
}

function submitChat() {
  if (!authStore.isAuthenticated) {
    router.push({ name: 'Login' })
    return
  }
  const text = inputMessage.value.trim()
  if (!text || !activeConversation.value || chatSending.value) return
  const context = pendingContext.value
  const payload = {
    conversationId: activeConversation.value.conversationId,
    messageType: context?.type === 'order' ? 'ORDER' : context?.type === 'product' ? 'PRODUCT' : 'TEXT',
    content: text,
    productId: context?.type === 'product' ? context.productId : null,
    skuId: context?.type === 'product' ? context.skuId : null,
    orderId: context?.type === 'order' ? context.orderId : null,
  }
  chatError.value = ''
  chatSending.value = true
  sendSocketMessage(payload)
}

function upsertMessage(message) {
  const exists = chatMessages.value.some((item) => item.messageId === message.messageId)
  if (!exists) chatMessages.value.push(message)
  scrollToLatest()
}

function addHelperMessage(message) {
  helperMessages.value.push({ id: Date.now() + Math.random(), ...message })
  scrollToLatest()
}

async function selectTemplate(template) {
  if (isLoading.value) return
  helperMode.value = template.action
  const keyword = inputMessage.value.trim()
  const displayText = template.action === 'product' && keyword ? `找商品：${keyword}` : template.label
  addHelperMessage({ sender: 'user', text: displayText })
  inputMessage.value = ''
  await runAction(template.action, keyword, { fromQuickAction: true })
}

async function submitHelper() {
  if (isLoading.value) return
  const text = inputMessage.value.trim()
  if (!text) {
    addHelperMessage({ sender: 'agent', text: '請輸入想找的商品名稱，例如：巧克力、耳機。' })
    return
  }
  addHelperMessage({ sender: 'user', text })
  inputMessage.value = ''
  await runAction(resolveAction(text), text)
}

function resolveAction(text) {
  if (['分類', '商品分類'].some((word) => text.includes(word))) return 'category'
  if (['收藏', '我的收藏'].some((word) => text.includes(word))) return 'favorite'
  if (['配送', '運費', '物流'].some((word) => text.includes(word))) return 'shipping'
  if (['付款', '信用卡', '貨到付款'].some((word) => text.includes(word))) return 'payment'
  if (['店鋪', '賣場', '商店'].some((word) => text.includes(word))) return 'store'
  if (['常見問題', 'FAQ', '客服'].some((word) => text.includes(word))) return 'faq'
  if (['訂單', '物流', '出貨'].some((word) => text.includes(word))) return 'order'
  if (['優惠券', '優惠', '折扣'].some((word) => text.includes(word))) return 'coupon'
  if (['購物車', '我的購物車', '查看購物車'].some((word) => text.includes(word))) return 'cart'
  return 'product'
}

async function runAction(action, keyword = '', options = {}) {
  helperMode.value = action
  isLoading.value = true
  try {
    if (action === 'product') await searchProducts(keyword, options)
    else if (action === 'order') await showLatestOrder(options)
    else if (action === 'coupon') await showCoupons(options)
    else if (action === 'cart') await showCart(options)
    else if (action === 'category') showCategoryGuide()
    else if (action === 'favorite') showFavoriteGuide()
    else if (action === 'shipping') showShippingGuide()
    else if (action === 'payment') showPaymentGuide()
    else if (action === 'store') showStoreGuide()
    else if (action === 'faq') showFaqGuide()
  } catch {
    addHelperMessage({ sender: 'agent', text: '目前查詢時遇到狀況，請稍後再試一次。' })
  } finally {
    isLoading.value = false
    scrollToLatest()
  }
}

async function searchProducts(keyword, options = {}) {
  if (!keyword) {
    addHelperMessage({ sender: 'agent', text: options.fromQuickAction ? '想找什麼商品呢？可以直接輸入商品名稱或關鍵字。' : '請輸入想找的商品名稱，例如：巧克力、耳機。' })
    return
  }
  const { data } = await api.get('/products', { params: { keyword, page: 0, size: 3 } })
  const products = normalizeList(data)
  if (!products.length) {
    addHelperMessage({ sender: 'agent', text: `目前找不到「${keyword}」相關商品，可以換個關鍵字試試看。` })
    return
  }
  addHelperMessage({
    sender: 'agent',
    text: `找到 ${products.length} 筆「${keyword}」相關商品：`,
    items: products.map((product) => ({ title: product.productName || '未命名商品', meta: formatProductPrice(product) })),
    link: { label: '查看更多商品', to: { path: '/products', query: { keyword } } },
  })
}

async function showLatestOrder(options = {}) {
  if (options.fromQuickAction) {
    addHelperMessage({ sender: 'agent', text: '可以查看您的近期訂單，或輸入訂單編號查詢。' })
  }
  if (!authStore.isAuthenticated) {
    addHelperMessage({ sender: 'agent', text: '登入後即可查詢訂單。', link: { label: '前往登入', to: '/login' } })
    return
  }
  const { data } = await getMemberOrders()
  const latestOrder = Array.isArray(data) ? data[0] : null
  if (!latestOrder) {
    addHelperMessage({ sender: 'agent', text: '目前沒有訂單紀錄。' })
    return
  }
  addHelperMessage({
    sender: 'agent',
    text: '最近訂單',
    items: [
      { title: '訂單編號', meta: latestOrder.orderNo || '-' },
      { title: '日期', meta: formatDate(latestOrder.createdAt) },
      { title: '狀態', meta: getOrderDisplayStatus(latestOrder).label },
      { title: '總金額', meta: formatCurrency(latestOrder.totalAmount) },
    ],
    link: { label: '查看所有訂單', to: '/member/orders' },
  })
}

async function showCoupons(options = {}) {
  if (options.fromQuickAction) {
    addHelperMessage({ sender: 'agent', text: '這裡可以查看目前可使用的優惠券。' })
  }
  const endpoint = authStore.isAuthenticated ? '/member/coupons' : '/coupons/available'
  const { data } = await api.get(endpoint)
  const coupons = normalizeList(data)
  const visibleCoupons = authStore.isAuthenticated
    ? coupons.filter((coupon) => coupon.status === 'AVAILABLE').slice(0, 3)
    : coupons.slice(0, 3)
  if (!visibleCoupons.length) {
    addHelperMessage({ sender: 'agent', text: authStore.isAuthenticated ? '目前沒有可使用優惠券。' : '目前沒有公開可領優惠券。' })
    return
  }
  addHelperMessage({
    sender: 'agent',
    text: authStore.isAuthenticated ? '目前可使用優惠券：' : '目前公開可領優惠券：',
    items: visibleCoupons.map((coupon) => ({
      title: coupon.couponName || '未命名優惠券',
      meta: `${formatCouponDiscount(coupon)} · ${formatCouponExpire(coupon)} 到期`,
    })),
    link: authStore.isAuthenticated ? { label: '查看我的優惠券', to: '/member/coupons' } : { label: '查看優惠券', to: '/coupons' },
  })
}

async function showCart(options = {}) {
  if (options.fromQuickAction) {
    addHelperMessage({ sender: 'agent', text: '可以查看目前購物車中的商品。' })
  }
  if (!authStore.isAuthenticated) {
    addHelperMessage({ sender: 'agent', text: '登入後即可查看購物車。', link: { label: '前往登入', to: '/login' } })
    return
  }
  const { data } = await api.get('/cart')
  const items = Array.isArray(data?.items) ? data.items : []
  if (!items.length) {
    addHelperMessage({ sender: 'agent', text: '購物車目前沒有商品。' })
    return
  }
  const totalAmount = items.reduce((total, item) => total + Number(item.price || 0) * Number(item.quantity || 0), 0)
  addHelperMessage({
    sender: 'agent',
    text: `購物車共 ${items.length} 種商品`,
    items: items.slice(0, 3).map((item) => ({
      title: item.productName || '未命名商品',
      meta: `數量 ${Number(item.quantity || 0).toLocaleString('zh-TW')} · 單價 ${formatCurrency(item.price)}`,
    })),
    footer: `總金額 ${formatCurrency(totalAmount)}`,
    link: { label: '前往購物車', to: '/cart' },
  })
}

function toggleMoreHelperActions() {
  showMoreHelperActions.value = !showMoreHelperActions.value
}

function showCategoryGuide() {
  addHelperMessage({
    sender: 'agent',
    text: '可以從商品列表瀏覽分類，也可以直接輸入分類或商品關鍵字讓我幫您查找。',
    link: { label: '前往商品列表', to: '/products' },
  })
}

function showFavoriteGuide() {
  if (!authStore.isAuthenticated) {
    addHelperMessage({ sender: 'agent', text: '登入後即可查看收藏商品。', link: { label: '前往登入', to: '/login' } })
    return
  }
  addHelperMessage({
    sender: 'agent',
    text: '您的收藏商品集中在會員中心，可以從那裡快速回到喜歡的商品。',
    link: { label: '查看我的收藏', to: '/member/favorites' },
  })
}

function showShippingGuide() {
  addHelperMessage({ sender: 'agent', text: '配送進度可在訂單詳情查看；結帳時也會顯示可用配送方式與運費。' })
}

function showPaymentGuide() {
  addHelperMessage({ sender: 'agent', text: '付款方式會在結帳流程中依目前商品與環境顯示，可用方式包含貨到付款或線上付款設定。' })
}

function showStoreGuide() {
  addHelperMessage({
    sender: 'agent',
    text: '想找特定店鋪嗎？可以前往店鋪搜尋，或直接輸入店鋪名稱讓我協助判斷。',
    link: { label: '前往店鋪搜尋', to: '/stores' },
  })
}

function showFaqGuide() {
  addHelperMessage({ sender: 'agent', text: '您可以直接輸入商品、訂單、優惠券、配送或付款問題，我會依目前平台功能協助查詢或導引。' })
}

function removePendingContext() {
  pendingContext.value = null
}

function backToConversationList() {
  activeConversation.value = null
  chatMessages.value = []
  pendingContext.value = null
}

async function scrollToLatest() {
  await nextTick()
  if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
}

async function focusComposer() {
  await nextTick()
  if (isOpen.value && chatInputRef.value) chatInputRef.value.focus()
}

function normalizeList(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.content)) return data.content
  return []
}

function formatProductPrice(product) {
  const minPrice = Number(product.minPrice ?? product.basePrice ?? 0)
  const maxPrice = Number(product.maxPrice ?? product.basePrice ?? minPrice)
  return maxPrice && maxPrice !== minPrice ? `${formatCurrency(minPrice)} ~ ${formatCurrency(maxPrice)}` : formatCurrency(minPrice)
}

function formatCouponDiscount(coupon) {
  if (coupon.discountType === 'PERCENT') return `${Number(coupon.discountValue || 0).toLocaleString('zh-TW')}% 折扣`
  return `折 NT$ ${Number(coupon.discountValue || 0).toLocaleString('zh-TW')}`
}

function formatCouponExpire(coupon) {
  const endAt = coupon.endAt || coupon.expireDate || coupon.endTime
  if (!endAt) return '未設定期限'
  return new Date(endAt).toLocaleDateString('zh-TW', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

function formatDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

function formatTime(value) {
  if (!value) return ''
  return new Intl.DateTimeFormat('zh-TW', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(value))
}

function formatCurrency(value) {
  return new Intl.NumberFormat('zh-TW', { style: 'currency', currency: 'TWD', maximumFractionDigits: 0 }).format(Number(value || 0))
}

onMounted(() => {
  window.addEventListener('dinogo-chat:open', openFromExternal)
  if (import.meta.env.MODE !== 'test') void loadUnreadCount()
})

onUnmounted(() => {
  window.removeEventListener('dinogo-chat:open', openFromExternal)
  if (reconnectTimer !== null) window.clearTimeout(reconnectTimer)
  if (socket) socket.close()
})
</script>

<template>
  <aside class="dino-chat" aria-label="DINO-GO CHAT">
    <section v-if="isOpen" class="dino-chat__panel" aria-live="polite">
      <header class="dino-chat__header">
        <div class="dino-chat__title">
          <p class="dino-chat__eyebrow">DINO-GO CHAT</p>
        </div>
        <div class="dino-chat__header-scene" aria-hidden="true"><img :src="logoUrl" alt="" /></div>
        <button class="dino-chat__icon-button" type="button" aria-label="收合聊天室" @click="toggleChat">
          <i class="bi bi-x-lg" aria-hidden="true"></i>
        </button>
      </header>

      <div ref="chatBodyRef" class="dino-chat__body">
        <template v-if="activeTab === 'assistant'">
          <article v-for="message in helperMessages" :key="message.id" class="dino-chat__bubble" :class="`dino-chat__bubble--${message.sender}`">
            <span v-if="message.sender === 'agent'" class="dino-chat__bubble-avatar" aria-hidden="true">
              <img :src="logoUrl" alt="" />
            </span>
            <div class="dino-chat__bubble-content">
              <p>{{ message.text }}</p>
            <ul v-if="message.items?.length" class="dino-chat__result-list">
              <li v-for="item in message.items" :key="`${message.id}-${item.title}-${item.meta}`"><strong>{{ item.title }}</strong><span>{{ item.meta }}</span></li>
            </ul>
            <p v-if="message.footer" class="dino-chat__result-footer">{{ message.footer }}</p>
            <RouterLink v-if="message.link" class="dino-chat__link" :to="message.link.to">{{ message.link.label }}</RouterLink>
            </div>
          </article>
          <p v-if="isLoading" class="dino-chat__bubble dino-chat__bubble--agent">查詢中...</p>
          <section class="dino-chat__assistant-actions" aria-label="小幫手快捷功能">
            <div class="dino-chat__pill-group">
              <button v-for="template in chatTemplates" :key="template.label" class="dino-chat__template" type="button" :disabled="isLoading" @click="selectTemplate(template)">{{ template.label }}</button>
            </div>
            <button class="dino-chat__more-toggle" type="button" @click="toggleMoreHelperActions">
              {{ showMoreHelperActions ? '收合更多功能' : '看更多功能' }}
              <i :class="['bi', showMoreHelperActions ? 'bi-chevron-up' : 'bi-chevron-down']" aria-hidden="true"></i>
            </button>
            <div v-if="showMoreHelperActions" class="dino-chat__pill-group dino-chat__pill-group--more">
              <button v-for="action in moreHelperActions" :key="action.label" class="dino-chat__template dino-chat__template--secondary" type="button" :disabled="isLoading" @click="selectTemplate(action)">{{ action.label }}</button>
            </div>
          </section>
        </template>

        <template v-else>
          <div v-if="!authStore.isAuthenticated" class="dino-chat__empty">
            <strong>登入後即可和店鋪聊聊</strong>
            <RouterLink to="/login">前往登入</RouterLink>
          </div>
          <div v-else-if="!activeConversation" class="dino-chat__conversation-list">
            <p v-if="chatError" class="dino-chat__error">{{ chatError }}</p>
            <button v-for="conversation in conversations" :key="conversation.conversationId" type="button" class="dino-chat__conversation" @click="openConversation(conversation)">
              <img v-if="conversation.sellerLogoUrl" :src="getImageUrl(conversation.sellerLogoUrl)" :alt="conversation.sellerName" />
              <span v-else class="dino-chat__store-logo"><i class="bi bi-shop" aria-hidden="true"></i></span>
              <span class="dino-chat__conversation-copy"><strong>{{ conversation.sellerName }}</strong><small>{{ conversation.latestMessage }}</small></span>
              <span class="dino-chat__conversation-meta"><time>{{ formatTime(conversation.latestMessageAt) }}</time><b v-if="conversation.unreadCount">{{ conversation.unreadCount }}</b></span>
            </button>
            <p v-if="!conversations.length && !chatError" class="dino-chat__empty-text">目前尚無聊天紀錄。</p>
          </div>
          <template v-else>
            <button class="dino-chat__back" type="button" @click="backToConversationList"><i class="bi bi-chevron-left" aria-hidden="true"></i> 返回聊聊</button>
            <p v-if="chatLoading" class="dino-chat__empty-text">載入中...</p>
            <article v-for="message in chatMessages" :key="message.messageId" class="dino-chat__bubble" :class="message.senderMemberId === authStore.member?.memberId ? 'dino-chat__bubble--user' : 'dino-chat__bubble--agent'">
              <RouterLink v-if="message.product" class="dino-chat__context-card" :to="{ name: 'ProductDetail', params: { id: message.product.productId } }">
                <img v-if="message.product.imageUrl" :src="getImageUrl(message.product.imageUrl)" :alt="message.product.productName" />
                <span><strong>{{ message.product.productName }}</strong><small v-if="message.product.skuText">{{ message.product.skuText }}</small><b>{{ formatCurrency(message.product.price) }}</b></span>
              </RouterLink>
              <RouterLink v-if="message.order" class="dino-chat__order-card" :to="{ name: 'MemberOrderDetail', params: { id: message.order.orderId } }">
                <strong>訂單編號 {{ message.order.orderNo }}</strong><span>{{ message.order.status }} · {{ formatCurrency(message.order.totalAmount) }}</span>
              </RouterLink>
              <img v-if="message.imageUrl" class="dino-chat__message-image" :src="getImageUrl(message.imageUrl)" alt="聊天圖片" />
              <p v-if="message.content">{{ message.content }}</p>
              <time>{{ formatTime(message.createdAt) }}</time>
            </article>
          </template>
        </template>
      </div>

      <div v-if="activeTab === 'chat' && pendingContext" class="dino-chat__pending">
        <button type="button" class="dino-chat__pending-remove" aria-label="移除詢問內容" @click="removePendingContext">×</button>
        <p>{{ pendingContext.type === 'product' ? '正在詢問' : '正在詢問訂單' }}</p>
        <div v-if="pendingContext.type === 'product'" class="dino-chat__context-card pending">
          <img v-if="pendingContext.imageUrl" :src="getImageUrl(pendingContext.imageUrl)" :alt="pendingContext.productName" />
          <span><strong>{{ pendingContext.productName }}</strong><small v-if="pendingContext.skuText">SKU：{{ pendingContext.skuText }}</small><b>{{ formatCurrency(pendingContext.price) }}</b></span>
        </div>
        <div v-else class="dino-chat__order-card pending">
          <strong>訂單編號 {{ pendingContext.orderNo }}</strong><span>{{ pendingContext.statusLabel }} · {{ formatCurrency(pendingContext.totalAmount) }}</span>
          <RouterLink :to="{ name: 'MemberOrderDetail', params: { id: pendingContext.orderId } }">查看訂單</RouterLink>
        </div>
      </div>

      <form class="dino-chat__composer" @submit.prevent="handleSubmit">
        <label class="visually-hidden" for="dino-chat-input">輸入訊息</label>
        <input id="dino-chat-input" ref="chatInputRef" v-model="inputMessage" type="text" :placeholder="helperPlaceholder" :disabled="isLoading || (activeTab === 'chat' && (!authStore.isAuthenticated || !activeConversation))" />
        <button type="submit" aria-label="送出訊息" :disabled="isLoading || chatSending || (activeTab === 'chat' && (!authStore.isAuthenticated || !activeConversation))"><i class="bi bi-send" aria-hidden="true"></i></button>
      </form>
    </section>

    <button v-if="!isOpen" class="dino-chat__launcher" type="button" aria-label="開啟 DINO-GO CHAT" @click="toggleChat">
      <span v-if="totalUnread" class="dino-chat__launcher-badge">{{ totalUnread }}</span>
      <span class="dino-chat__hint">需要幫忙嗎？</span>
      <img :src="mascotUrl" alt="" aria-hidden="true" />
    </button>
  </aside>
</template>

<style scoped>
.dino-chat{position:fixed;right:clamp(var(--space-5),2.5vw,40px);bottom:clamp(var(--space-5),5vh,80px);z-index:1080;display:grid;justify-items:end;gap:var(--space-3);pointer-events:none}.dino-chat__panel,.dino-chat__launcher{pointer-events:auto}.dino-chat__panel{display:flex;flex-direction:column;min-height:min(320px,calc(100vh - 160px));max-height:min(600px,calc(100vh - 160px));width:min(380px,calc(100vw - 32px));overflow:hidden;background:var(--color-surface);border:1px solid var(--color-border);border-radius:var(--radius-lg);box-shadow:0 16px 36px rgba(26,31,46,.16)}.dino-chat__header{position:relative;display:flex;align-items:center;justify-content:space-between;gap:var(--space-4);padding:var(--space-3) var(--space-4);min-height:58px;overflow:hidden;background:var(--color-primary-700);color:var(--color-surface)}.dino-chat__title,.dino-chat__icon-button{position:relative;z-index:1}.dino-chat__eyebrow{margin:0;font-size:var(--font-size-sm);font-weight:800}.dino-chat__header-scene{position:absolute;top:50%;right:58px;display:grid;width:34px;height:34px;place-items:center;pointer-events:none;background:rgba(255,255,255,.96);border-radius:50%;transform:translateY(-50%)}.dino-chat__header-scene img{width:24px}.dino-chat__icon-button{display:grid;width:32px;height:32px;place-items:center;color:var(--color-surface);background:rgba(255,255,255,.12);border:1px solid rgba(255,255,255,.22);border-radius:var(--radius-sm)}.dino-chat__launcher-badge,.dino-chat__conversation-meta b{display:inline-grid;min-width:20px;height:20px;place-items:center;padding:0 6px;color:#fff;font-size:12px;background:var(--color-danger);border-radius:var(--radius-pill)}.dino-chat__body{display:grid;flex:1 1 auto;align-content:start;gap:var(--space-3);min-height:0;overflow-y:auto;padding:var(--space-4);background:var(--color-surface-soft)}.dino-chat__bubble{max-width:88%;margin:0;padding:var(--space-3);font-size:var(--font-size-sm);line-height:1.5;border-radius:var(--radius-lg);overflow-wrap:anywhere}.dino-chat__bubble p{margin:0;white-space:pre-line}.dino-chat__bubble time{display:block;margin-top:var(--space-1);font-size:11px;opacity:.72}.dino-chat__bubble--agent{justify-self:start;color:var(--color-text-700);background:var(--color-surface);border:1px solid var(--color-border)}.dino-chat__bubble--user{justify-self:end;color:var(--color-surface);background:var(--color-primary)}.dino-chat__bubble--agent{display:grid;grid-template-columns:28px minmax(0,1fr);gap:var(--space-2);max-width:94%;padding:var(--space-2) var(--space-3)}.dino-chat__bubble-avatar{display:grid;width:28px;height:28px;place-items:center;align-self:start;background:var(--color-primary-soft);border-radius:var(--radius-pill)}.dino-chat__bubble-avatar img{width:20px;height:20px;object-fit:contain}.dino-chat__bubble-content{min-width:0}.dino-chat__result-list{display:grid;gap:var(--space-2);margin:var(--space-2) 0 0;padding:0;list-style:none}.dino-chat__result-list li{display:grid;gap:2px}.dino-chat__result-list strong{color:var(--color-text)}.dino-chat__result-list span,.dino-chat__result-footer{color:var(--color-text-600)}.dino-chat__link,.dino-chat__empty a,.dino-chat__order-card a{color:var(--color-primary-active);font-weight:700;text-decoration:none}.dino-chat__assistant-actions{display:grid;gap:var(--space-2);padding:var(--space-1) 0 var(--space-2)}.dino-chat__pill-group{display:flex;flex-wrap:wrap;gap:var(--space-2)}.dino-chat__template{min-height:34px;padding:0 var(--space-3);color:var(--color-primary-active);font-size:var(--font-size-sm);font-weight:800;background:var(--color-primary-soft);border:1px solid rgba(39,120,85,.22);border-radius:var(--radius-pill);transition:background-color .18s ease,border-color .18s ease,transform .18s ease}.dino-chat__template:hover:not(:disabled){background:var(--color-surface);border-color:var(--color-primary);transform:translateY(-1px)}.dino-chat__template--secondary{background:var(--color-surface);border-color:var(--color-border)}.dino-chat__more-toggle{justify-self:start;display:inline-flex;align-items:center;gap:var(--space-1);min-height:32px;padding:0;color:var(--color-primary-active);font-size:var(--font-size-sm);font-weight:800;background:transparent;border:0}.dino-chat__conversation-list{display:grid;gap:var(--space-2)}.dino-chat__conversation{display:grid;grid-template-columns:44px minmax(0,1fr) auto;align-items:center;gap:var(--space-3);width:100%;padding:var(--space-3);text-align:left;background:var(--color-surface);border:1px solid var(--color-border);border-radius:var(--radius-md)}.dino-chat__conversation img,.dino-chat__store-logo{width:44px;height:44px;border-radius:var(--radius-md);object-fit:cover}.dino-chat__store-logo{display:grid;place-items:center;color:var(--color-primary);background:var(--color-primary-soft)}.dino-chat__conversation-copy{display:grid;gap:2px;min-width:0}.dino-chat__conversation-copy strong,.dino-chat__conversation-copy small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.dino-chat__conversation-copy small,.dino-chat__conversation-meta time,.dino-chat__empty-text{color:var(--color-text-muted);font-size:var(--font-size-xs)}.dino-chat__conversation-meta{display:grid;justify-items:end;gap:var(--space-1)}.dino-chat__back{justify-self:start;padding:0;color:var(--color-primary-active);font-weight:800;background:transparent;border:0}.dino-chat__empty{display:grid;justify-items:center;gap:var(--space-2);padding:var(--space-6);text-align:center;background:var(--color-surface);border:1px solid var(--color-border);border-radius:var(--radius-md)}.dino-chat__error{margin:0;color:var(--color-danger)}.dino-chat__context-card,.dino-chat__order-card{display:grid;grid-template-columns:56px minmax(0,1fr);gap:var(--space-2);margin-bottom:var(--space-2);padding:var(--space-2);color:var(--color-text);text-decoration:none;background:rgba(255,255,255,.9);border:1px solid var(--color-border);border-radius:var(--radius-md)}.dino-chat__context-card img{width:56px;height:56px;object-fit:cover;border-radius:var(--radius-sm)}.dino-chat__context-card span,.dino-chat__order-card{min-width:0}.dino-chat__context-card strong,.dino-chat__context-card small,.dino-chat__order-card strong,.dino-chat__order-card span{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.dino-chat__context-card small,.dino-chat__order-card span{color:var(--color-text-muted);font-size:var(--font-size-xs)}.dino-chat__context-card b{color:var(--color-primary-active)}.dino-chat__order-card{grid-template-columns:1fr}.dino-chat__message-image{display:block;max-width:100%;border-radius:var(--radius-md)}.dino-chat__pending{position:relative;padding:var(--space-3) var(--space-4);background:var(--color-surface);border-top:1px solid var(--color-border)}.dino-chat__pending p{margin:0 0 var(--space-2);color:var(--color-primary-active);font-size:var(--font-size-xs);font-weight:800}.dino-chat__pending-remove{position:absolute;top:var(--space-2);right:var(--space-3);width:28px;height:28px;color:var(--color-text-muted);background:transparent;border:0}.dino-chat__composer{display:grid;grid-template-columns:1fr 40px;gap:var(--space-2);padding:var(--space-4);background:var(--color-surface);border-top:1px solid var(--color-border)}.dino-chat__composer input{min-width:0;height:40px;padding:0 var(--space-3);border:1px solid var(--color-border-strong);border-radius:var(--radius-md)}.dino-chat__composer button{display:grid;width:40px;height:40px;place-items:center;color:var(--color-surface);background:var(--color-primary);border:1px solid var(--color-primary);border-radius:var(--radius-md)}.dino-chat__launcher{position:relative;display:block;width:clamp(96px,11vw,160px);max-width:18vw;padding:0;background:transparent;border:0}.dino-chat__launcher img{display:block;width:100%;height:auto}.dino-chat__launcher-badge{position:absolute;top:4px;right:8px;z-index:1}.dino-chat__hint{position:absolute;right:72%;bottom:74%;width:max-content;max-width:140px;padding:var(--space-2) var(--space-3);color:var(--color-primary-active);font-size:var(--font-size-sm);font-weight:700;background:var(--color-surface);border:1px solid var(--color-border);border-radius:var(--radius-lg);box-shadow:var(--shadow-card)}@media (max-width:575.98px){.dino-chat{right:var(--space-3);bottom:var(--space-4)}.dino-chat__panel{max-height:min(540px,calc(100vh - 96px));width:calc(100vw - 24px)}.dino-chat__hint{display:none}.dino-chat__launcher{width:76px;max-width:24vw}.dino-chat__template{min-height:32px;padding:0 var(--space-2)}}
</style>
