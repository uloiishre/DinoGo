<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AutomaticEmailTemplate from '../components/AutomaticEmailTemplate.vue'
import { getMemberOrderMessages, getSellerCancelledMessages, getSellerNewOrderMessages } from '../api/sysmsgEmail'
import type { BackendInboxMessage, EmailTemplateModel, RecipientKind } from '../types/email'

const kind = ref<RecipientKind>('MEMBER')
const email = ref('preview@example.com')
const messages = ref<BackendInboxMessage[]>([])
const selectedId = ref<number | null>(null)
const loading = ref(false)
const error = ref('')

const selected = computed(() => messages.value.find(item => item.recordId === selectedId.value) ?? messages.value[0])
const model = computed<EmailTemplateModel | null>(() => selected.value ? {
  ...selected.value, recipientKind: kind.value, recipientEmail: email.value,
} : null)

async function load() {
  loading.value = true; error.value = ''
  try {
    messages.value = kind.value === 'MEMBER'
      ? await getMemberOrderMessages()
      : [...await getSellerNewOrderMessages(), ...await getSellerCancelledMessages()]
    selectedId.value = messages.value[0]?.recordId ?? null
  } catch (e) { error.value = e instanceof Error ? e.message : '載入失敗' }
  finally { loading.value = false }
}

onMounted(load)
</script>

<template>
  <section class="preview-page">
    <aside class="controls">
      <p class="kicker">SYSMSG · EMAIL</p><h2>自動郵件預覽</h2>
      <label>收件匣角色<select v-model="kind" @change="load"><option value="MEMBER">會員</option><option value="SELLER">商家</option></select></label>
      <label>預覽訊息<select v-model="selectedId"><option v-for="item in messages" :key="item.recordId" :value="item.recordId">{{ item.msgLabel }} · {{ item.sendTitle }}</option></select></label>
      <label>收件 Email<input v-model="email" type="email"></label>
      <button type="button" :disabled="loading" @click="load">{{ loading ? '讀取中…' : '重新讀取後端資料' }}</button>
      <p v-if="error" class="error">{{ error }}</p>
      <p class="security">SMTP 帳密只存在 Spring Boot 環境變數，不會傳送到瀏覽器。</p>
    </aside>
    <div class="canvas"><AutomaticEmailTemplate v-if="model" :model="model"/><p v-else-if="!loading">目前沒有可預覽的自動訊息。</p></div>
  </section>
</template>

<style scoped>
.preview-page{min-height:100vh;display:grid;grid-template-columns:310px 1fr;background:#e9f0ed;font-family:Arial,"Noto Sans TC",sans-serif}.controls{padding:36px 28px;background:#183a32;color:#fff}.kicker{color:#8dd6bd;font-size:11px;letter-spacing:.18em}.controls h2{margin:8px 0 30px;font-size:26px}.controls label{display:grid;gap:8px;margin:18px 0;color:#c9dbd5;font-size:13px}.controls select,.controls input{width:100%;box-sizing:border-box;padding:11px 12px;border:1px solid #547068;border-radius:8px;background:#fff;color:#24332f}.controls button{width:100%;margin-top:10px;padding:12px;border:0;border-radius:8px;background:#e3b341;color:#1d342e;font-weight:800;cursor:pointer}.controls button:disabled{opacity:.6}.security{margin-top:26px;color:#a9c2ba;font-size:12px;line-height:1.6}.error{color:#ffb4ab}.canvas{display:grid;place-items:center;padding:48px}@media(max-width:800px){.preview-page{grid-template-columns:1fr}.canvas{padding:24px}.controls{padding:24px}}
</style>

