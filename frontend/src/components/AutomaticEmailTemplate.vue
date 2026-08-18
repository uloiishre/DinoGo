<script setup lang="ts">
import { computed } from 'vue'
import type { EmailTemplateModel } from '../types/email'

const props = defineProps<{ model: EmailTemplateModel }>()

const statusText: Record<string, string> = {
  PAID: '付款完成', SHIPPED: '商品已出貨', DELIVERED: '商品已送達',
  COMPLETED: '訂單已完成', CANCELLED: '訂單已取消',
}
const accent = computed(() => props.model.orderStatus === 'CANCELLED' ? '#b42318' : '#176b5b')
const statusLabel = computed(() => statusText[props.model.orderStatus ?? ''] ?? 'DinoGo 訊息通知')
const recipient = computed(() => props.model.recipientName?.trim() || (props.model.recipientKind === 'SELLER' ? '合作商家' : '親愛的會員'))
const orderNumber = computed(() => props.model.msgLabel || (props.model.orderId ? `#${props.model.orderId}` : '—'))
</script>

<template>
  <article class="email-shell" :style="{ '--accent': accent }">
    <header class="brand-bar">
      <div class="brand-mark">D</div>
      <div><strong>DinoGo</strong><span>購物訊息中心</span></div>
    </header>

    <main class="email-body">
      <p class="eyebrow">{{ statusLabel }}</p>
      <h1>{{ model.sendTitle }}</h1>
      <p class="greeting">{{ recipient }}，您好：</p>
      <p class="content">{{ model.sendContent }}</p>

      <dl v-if="model.orderId" class="order-card">
        <div><dt>訂單編號</dt><dd>{{ orderNumber }}</dd></div>
        <div><dt>訂單狀態</dt><dd>{{ statusLabel }}</dd></div>
        <div><dt>訊息分類</dt><dd>{{ model.msgFunction.slice(0, 2) }}</dd></div>
      </dl>

      <p class="notice">本郵件由 DinoGo 訊息系統自動寄出，請勿直接回覆。</p>
    </main>

    <footer>
      <span>© {{ new Date().getFullYear() }} DinoGo</span>
      <span>Record #{{ model.recordId }} · Send #{{ model.sendId }}</span>
    </footer>
  </article>
</template>

<style scoped>
.email-shell{--accent:#176b5b;width:min(680px,100%);margin:auto;background:#fff;color:#24332f;border:1px solid #dce7e3;border-radius:18px;overflow:hidden;box-shadow:0 18px 50px rgba(24,56,48,.12);font-family:Arial,"Noto Sans TC",sans-serif}.brand-bar{display:flex;align-items:center;gap:12px;padding:22px 28px;background:#f3f8f6;border-bottom:4px solid var(--accent)}.brand-mark{display:grid;place-items:center;width:42px;height:42px;border-radius:12px;background:var(--accent);color:#fff;font-size:24px;font-weight:800}.brand-bar strong,.brand-bar span{display:block}.brand-bar strong{font-size:20px}.brand-bar span{margin-top:2px;color:#60736d;font-size:12px;letter-spacing:.12em}.email-body{padding:38px 34px}.eyebrow{margin:0 0 10px;color:var(--accent);font-size:13px;font-weight:800;letter-spacing:.12em}.email-body h1{margin:0 0 28px;font-size:30px;line-height:1.25}.greeting,.content{font-size:16px;line-height:1.8}.content{white-space:pre-line}.order-card{margin:30px 0;padding:8px 22px;background:#f7faf9;border-left:4px solid var(--accent);border-radius:8px}.order-card div{display:flex;justify-content:space-between;gap:20px;padding:14px 0;border-bottom:1px solid #dde8e4}.order-card div:last-child{border-bottom:0}.order-card dt{color:#667a73}.order-card dd{margin:0;text-align:right;font-weight:700}.notice{margin:28px 0 0;color:#7a8b85;font-size:12px}.email-shell footer{display:flex;justify-content:space-between;gap:16px;padding:18px 28px;background:#263c36;color:#d8e4e0;font-size:11px}@media(max-width:560px){.email-body{padding:28px 22px}.email-body h1{font-size:25px}.email-shell footer{flex-direction:column}.order-card div{align-items:flex-start;flex-direction:column;gap:5px}.order-card dd{text-align:left}}
</style>

