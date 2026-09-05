<script setup>
import { nextTick, ref } from 'vue'
import api from '@/api/axios'
import { getMemberOrders } from '@/api/order'
import logoUrl from '@/assets/images/dinogo-logo-s.png'
import mascotUrl from '@/assets/images/dinogo-mascot.png'
import { useAuthStore } from '@/stores/auth'
import { getOrderDisplayStatus } from '@/utils/orderDisplayStatus'

const isOpen = ref(false)
const inputMessage = ref('')
const isLoading = ref(false)
const chatBodyRef = ref(null)
const chatInputRef = ref(null)
const authStore = useAuthStore()

const chatTemplates = [
  {
    label: '找商品',
    action: 'product',
  },
  {
    label: '查訂單',
    action: 'order',
  },
  {
    label: '優惠券',
    action: 'coupon',
  },
  {
    label: '購物車',
    action: 'cart',
  },
]

const messages = ref([
  {
    id: 1,
    sender: 'agent',
    text: '歡迎來到 DINO-GO，需要找商品、查訂單、看優惠券或購物車，都可以先從範本開始。',
  },
])

function toggleChat() {
  isOpen.value = !isOpen.value
  scrollToLatest()
  focusComposer()
}

function addMessage(message) {
  messages.value.push({
    id: Date.now() + Math.random(),
    ...message,
  })
  scrollToLatest()
}

async function scrollToLatest() {
  await nextTick()
  if (chatBodyRef.value) {
    chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  }
}

async function focusComposer() {
  await nextTick()
  if (isOpen.value && chatInputRef.value) {
    chatInputRef.value.focus()
  }
}

async function selectTemplate(template) {
  if (isLoading.value) return

  const displayText =
    template.action === 'product' && inputMessage.value.trim()
      ? `找商品：${inputMessage.value.trim()}`
      : template.label

  addMessage({ sender: 'user', text: displayText })
  await runAction(template.action, inputMessage.value.trim())
}

async function handleSubmit() {
  if (isLoading.value) return

  const text = inputMessage.value.trim()
  if (!text) {
    addMessage({
      sender: 'agent',
      text: '請輸入想找的商品名稱，例如：巧克力、耳機。',
    })
    return
  }

  addMessage({ sender: 'user', text })
  inputMessage.value = ''
  await runAction(resolveAction(text), text)
}

function resolveAction(text) {
  if (['訂單', '物流', '出貨'].some((word) => text.includes(word))) {
    return 'order'
  }

  if (['優惠券', '優惠', '折扣'].some((word) => text.includes(word))) {
    return 'coupon'
  }

  if (['購物車', '我的購物車', '查看購物車'].some((word) => text.includes(word))) {
    return 'cart'
  }

  return 'product'
}

async function runAction(action, keyword = '') {
  isLoading.value = true

  try {
    if (action === 'product') {
      await searchProducts(keyword)
    } else if (action === 'order') {
      await showLatestOrder()
    } else if (action === 'coupon') {
      await showCoupons()
    } else if (action === 'cart') {
      await showCart()
    }
  } catch (error) {
    addMessage({
      sender: 'agent',
      text: '目前查詢時遇到狀況，請稍後再試一次。',
    })
  } finally {
    isLoading.value = false
    scrollToLatest()
  }
}

async function searchProducts(keyword) {
  if (!keyword) {
    addMessage({
      sender: 'agent',
      text: '請輸入想找的商品名稱，例如：巧克力、耳機。',
    })
    return
  }

  const { data } = await api.get('/products', {
    params: {
      keyword,
      page: 0,
      size: 3,
    },
  })
  const products = normalizeList(data)

  if (!products.length) {
    addMessage({
      sender: 'agent',
      text: `目前找不到「${keyword}」相關商品，可以換個關鍵字試試看。`,
    })
    return
  }

  addMessage({
    sender: 'agent',
    text: `找到 ${products.length} 筆「${keyword}」相關商品：`,
    items: products.map((product) => ({
      title: product.productName || '未命名商品',
      meta: formatProductPrice(product),
    })),
    link: {
      label: '查看更多商品',
      to: {
        path: '/products',
        query: { keyword },
      },
    },
  })
}

async function showLatestOrder() {
  if (!authStore.isAuthenticated) {
    addMessage({
      sender: 'agent',
      text: '登入後即可查詢訂單。',
      link: {
        label: '前往登入',
        to: '/login',
      },
    })
    return
  }

  const { data } = await getMemberOrders()
  const latestOrder = Array.isArray(data) ? data[0] : null

  if (!latestOrder) {
    addMessage({
      sender: 'agent',
      text: '目前沒有訂單紀錄。',
    })
    return
  }

  addMessage({
    sender: 'agent',
    text: '最近訂單',
    items: [
      { title: '訂單編號', meta: latestOrder.orderNo || '-' },
      { title: '日期', meta: formatDate(latestOrder.createdAt) },
      { title: '狀態', meta: getOrderDisplayStatus(latestOrder).label },
      { title: '總金額', meta: formatCurrency(latestOrder.totalAmount) },
    ],
    link: {
      label: '查看所有訂單',
      to: '/member/orders',
    },
  })
}

async function showCoupons() {
  const endpoint = authStore.isAuthenticated ? '/member/coupons' : '/coupons/available'
  const { data } = await api.get(endpoint)
  const coupons = normalizeList(data)
  const visibleCoupons = authStore.isAuthenticated
    ? coupons.filter((coupon) => coupon.status === 'AVAILABLE').slice(0, 3)
    : coupons.slice(0, 3)

  if (!visibleCoupons.length) {
    addMessage({
      sender: 'agent',
      text: authStore.isAuthenticated
        ? '目前沒有可使用優惠券。'
        : '目前沒有公開可領優惠券。',
    })
    return
  }

  addMessage({
    sender: 'agent',
    text: authStore.isAuthenticated ? '目前可使用優惠券：' : '目前公開可領優惠券：',
    items: visibleCoupons.map((coupon) => ({
      title: coupon.couponName || '未命名優惠券',
      meta: `${formatCouponDiscount(coupon)} · ${formatCouponExpire(coupon)} 到期`,
    })),
    link: authStore.isAuthenticated
      ? {
          label: '查看我的優惠券',
          to: '/member/coupons',
        }
      : {
          label: '查看優惠券',
          to: '/coupons',
        },
  })
}

async function showCart() {
  if (!authStore.isAuthenticated) {
    addMessage({
      sender: 'agent',
      text: '登入後即可查看購物車。',
      link: {
        label: '前往登入',
        to: '/login',
      },
    })
    return
  }

  const { data } = await api.get('/cart')
  const items = Array.isArray(data?.items) ? data.items : []

  if (!items.length) {
    addMessage({
      sender: 'agent',
      text: '購物車目前沒有商品。',
    })
    return
  }

  const totalAmount = items.reduce(
    (total, item) => total + Number(item.price || 0) * Number(item.quantity || 0),
    0,
  )

  addMessage({
    sender: 'agent',
    text: `購物車共 ${items.length} 種商品`,
    items: items.slice(0, 3).map((item) => ({
      title: item.productName || '未命名商品',
      meta: `數量 ${Number(item.quantity || 0).toLocaleString('zh-TW')} · 單價 ${formatCurrency(item.price)}`,
    })),
    footer: `總金額 ${formatCurrency(totalAmount)}`,
    link: {
      label: '前往購物車',
      to: '/cart',
    },
  })
}

function normalizeList(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.content)) return data.content
  return []
}

function formatProductPrice(product) {
  const minPrice = Number(product.minPrice ?? product.basePrice ?? 0)
  const maxPrice = Number(product.maxPrice ?? product.basePrice ?? minPrice)

  if (maxPrice && maxPrice !== minPrice) {
    return `${formatCurrency(minPrice)} ~ ${formatCurrency(maxPrice)}`
  }

  return formatCurrency(minPrice)
}

function formatCouponDiscount(coupon) {
  if (coupon.discountType === 'PERCENT') {
    return `${Number(coupon.discountValue || 0).toLocaleString('zh-TW')}% 折扣`
  }

  return `折 NT$ ${Number(coupon.discountValue || 0).toLocaleString('zh-TW')}`
}

function formatCouponExpire(coupon) {
  const endAt = coupon.endAt || coupon.expireDate || coupon.endTime
  if (!endAt) return '未設定期限'

  return new Date(endAt).toLocaleDateString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
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

function formatCurrency(value) {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'TWD',
    maximumFractionDigits: 0,
  }).format(Number(value || 0))
}
</script>

<template>
  <aside class="dino-chat" aria-label="DINO-GO 小幫手">
    <section v-if="isOpen" class="dino-chat__panel" aria-live="polite">
      <header class="dino-chat__header">
        <div class="dino-chat__title">
          <p class="dino-chat__eyebrow">DINO-GO CHAT</p>
          <h2>DINO-GO 小幫手</h2>
        </div>
        <div class="dino-chat__header-scene" aria-hidden="true">
          <img :src="logoUrl" alt="" />
        </div>
        <button
          class="dino-chat__icon-button"
          type="button"
          aria-label="收合聊天室"
          @click="toggleChat"
        >
          <i class="bi bi-x-lg" aria-hidden="true"></i>
        </button>
      </header>

      <div ref="chatBodyRef" class="dino-chat__body">
        <article
          v-for="message in messages"
          :key="message.id"
          class="dino-chat__bubble"
          :class="`dino-chat__bubble--${message.sender}`"
        >
          <p>{{ message.text }}</p>
          <ul v-if="message.items?.length" class="dino-chat__result-list">
            <li v-for="item in message.items" :key="`${message.id}-${item.title}-${item.meta}`">
              <strong>{{ item.title }}</strong>
              <span>{{ item.meta }}</span>
            </li>
          </ul>
          <p v-if="message.footer" class="dino-chat__result-footer">{{ message.footer }}</p>
          <RouterLink v-if="message.link" class="dino-chat__link" :to="message.link.to">
            {{ message.link.label }}
          </RouterLink>
        </article>
        <p v-if="isLoading" class="dino-chat__bubble dino-chat__bubble--agent">
          查詢中...
        </p>
      </div>

      <div class="dino-chat__templates" aria-label="常用問題範本">
        <button
          v-for="template in chatTemplates"
          :key="template.label"
          class="dino-chat__template"
          type="button"
          :disabled="isLoading"
          @click="selectTemplate(template)"
        >
          {{ template.label }}
        </button>
      </div>

      <form class="dino-chat__composer" @submit.prevent="handleSubmit">
        <label class="visually-hidden" for="dino-chat-input">輸入訊息</label>
        <input
          id="dino-chat-input"
          ref="chatInputRef"
          v-model="inputMessage"
          type="text"
          placeholder="輸入想詢問的內容"
          :disabled="isLoading"
        />
        <button type="submit" aria-label="送出訊息" :disabled="isLoading">
          <i class="bi bi-send" aria-hidden="true"></i>
        </button>
      </form>
    </section>

    <button
      v-if="!isOpen"
      class="dino-chat__launcher"
      type="button"
      aria-label="開啟 DINO-GO 小幫手"
      @click="toggleChat"
    >
      <span class="dino-chat__hint">需要幫忙嗎？</span>
      <img :src="mascotUrl" alt="" aria-hidden="true" />
    </button>
  </aside>
</template>

<style scoped>
.dino-chat {
  position: fixed;
  right: clamp(var(--space-5), 2.5vw, 40px);
  bottom: clamp(var(--space-5), 5vh, 80px);
  z-index: 1080;
  display: grid;
  justify-items: end;
  gap: var(--space-3);
  pointer-events: none;
}

.dino-chat__panel,
.dino-chat__launcher {
  pointer-events: auto;
}

.dino-chat__panel {
  display: flex;
  flex-direction: column;
  min-height: min(260px, calc(100vh - 160px));
  min-height: min(260px, calc(100dvh - 160px));
  max-height: min(540px, calc(100vh - 160px));
  max-height: min(540px, calc(100dvh - 160px));
  width: min(340px, calc(100vw - 32px));
  overflow: hidden;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: 0 16px 36px rgba(26, 31, 46, 0.16);
}

.dino-chat__header {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4);
  min-height: 74px;
  overflow: hidden;
  background: var(--color-primary-700);
  color: var(--color-surface);
}

.dino-chat__title,
.dino-chat__icon-button {
  position: relative;
  z-index: 1;
}

.dino-chat__eyebrow {
  margin: 0 0 var(--space-1);
  font-size: var(--font-size-xs);
  font-weight: 700;
}

.dino-chat__header h2 {
  margin: 0;
  font-size: var(--font-size-md);
  font-weight: 700;
  letter-spacing: 0;
}

.dino-chat__header-scene {
  position: absolute;
  top: 50%;
  right: 58px;
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  pointer-events: none;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 50%;
  box-shadow: 0 6px 14px rgba(26, 31, 46, 0.12);
  transform: translateY(-50%);
}

.dino-chat__header-scene img {
  display: block;
  width: 28px;
  height: auto;
}

.dino-chat__icon-button {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  color: var(--color-surface);
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: var(--radius-sm);
}

.dino-chat__body {
  display: grid;
  flex: 0 1 auto;
  gap: var(--space-3);
  min-height: 0;
  overflow-y: auto;
  padding: var(--space-4);
  background: var(--color-surface-soft);
}

.dino-chat__bubble {
  max-width: 88%;
  margin: 0;
  padding: var(--space-3);
  font-size: var(--font-size-sm);
  line-height: 1.5;
  border-radius: var(--radius-lg);
  overflow-wrap: anywhere;
}

.dino-chat__bubble p {
  margin: 0;
}

.dino-chat__bubble--agent {
  justify-self: start;
  color: var(--color-text-700);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
}

.dino-chat__bubble--user {
  justify-self: end;
  color: var(--color-surface);
  background: var(--color-primary);
}

.dino-chat__result-list {
  display: grid;
  gap: var(--space-2);
  margin: var(--space-2) 0 0;
  padding: 0;
  list-style: none;
}

.dino-chat__result-list li {
  display: grid;
  gap: 2px;
}

.dino-chat__result-list strong {
  color: var(--color-text);
  font-weight: 700;
}

.dino-chat__result-list span,
.dino-chat__result-footer {
  color: var(--color-text-600);
}

.dino-chat__result-footer {
  margin-top: var(--space-2);
  font-weight: 700;
}

.dino-chat__link {
  display: inline-flex;
  margin-top: var(--space-2);
  color: var(--color-primary-active);
  font-weight: 700;
  text-decoration: none;
}

.dino-chat__link:hover,
.dino-chat__link:focus-visible {
  color: var(--color-primary);
  text-decoration: underline;
}

.dino-chat__templates {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-2);
  padding: 0 var(--space-4) var(--space-4);
  background: var(--color-surface-soft);
}

.dino-chat__template {
  min-height: 36px;
  padding: var(--space-2) var(--space-3);
  color: var(--color-primary-active);
  font-size: var(--font-size-sm);
  font-weight: 700;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.dino-chat__template:hover {
  color: var(--color-surface);
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.dino-chat__template:disabled,
.dino-chat__composer input:disabled,
.dino-chat__composer button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.dino-chat__composer {
  display: grid;
  grid-template-columns: 1fr 40px;
  gap: var(--space-2);
  padding: var(--space-4);
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
}

.dino-chat__composer input {
  min-width: 0;
  height: 40px;
  padding: 0 var(--space-3);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
}

.dino-chat__composer button {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  color: var(--color-surface);
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
}

.dino-chat__launcher {
  position: relative;
  display: block;
  width: clamp(96px, 11vw, 160px);
  max-width: 18vw;
  padding: 0;
  background: transparent;
  border: 0;
  transition:
    transform 0.16s ease,
    filter 0.16s ease;
}

.dino-chat__launcher:hover {
  transform: translateY(-3px);
  filter: drop-shadow(0 10px 16px rgba(26, 31, 46, 0.16));
}

.dino-chat__launcher img {
  display: block;
  width: 100%;
  height: auto;
  user-select: none;
}

.dino-chat__hint {
  position: absolute;
  right: 72%;
  bottom: 74%;
  width: max-content;
  max-width: 140px;
  padding: var(--space-2) var(--space-3);
  color: var(--color-primary-active);
  font-size: var(--font-size-sm);
  font-weight: 700;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

@media (max-width: 767.98px) {
  .dino-chat {
    right: var(--space-4);
  }

  .dino-chat__launcher {
    width: 88px;
    max-width: 22vw;
  }

  .dino-chat__hint {
    display: none;
  }
}

@media (max-width: 575.98px) {
  .dino-chat {
    right: var(--space-3);
    bottom: var(--space-4);
  }

  .dino-chat__panel {
    max-height: min(520px, calc(100vh - 96px));
    max-height: min(520px, calc(100dvh - 96px));
    width: calc(100vw - 24px);
  }

  .dino-chat__header-scene {
    right: 50px;
    width: 34px;
    height: 34px;
  }

  .dino-chat__header-scene img {
    width: 25px;
  }

  .dino-chat__launcher {
    width: 76px;
    max-width: 24vw;
  }
}
</style>
