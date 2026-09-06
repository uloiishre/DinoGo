<script setup>
import { ref } from 'vue'
import api from '@/api/axios'
import ProductCard from './ProductCard.vue'

const message = ref('')
const loading = ref(false)
const errorMessage = ref('')
const result = ref(null)

async function askAdvisor() {
  if (!message.value.trim()) {
    errorMessage.value = '請先描述想找的商品。'
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await api.post('/ai-shopping-advisor', { message: message.value.trim() })
    result.value = data
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '顧問暫時無法提供建議。'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="advisor-page">
    <section class="hero">
      <p>DinoGo AI Shopping Advisor</p>
      <h1>告訴我你的需求，我幫你挑。</h1>
      <form @submit.prevent="askAdvisor">
        <textarea v-model="message" maxlength="500" :disabled="loading" placeholder="例如：想買送給喜歡露營的朋友，預算 2,000 元內，耐用又好收納" />
        <button type="submit" :disabled="loading">{{ loading ? '正在挑選…' : '問 AI 幫我挑' }}</button>
      </form>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </section>
    <section v-if="result" class="results">
      <h2>{{ result.summary }}</h2>
      <p class="note">推薦只使用 DinoGo 目前已上架商品；價格與庫存以商品頁為準。</p>
      <div v-if="result.comparison" class="comparison">
        <strong>快速比較</strong>
        <span>最便宜：{{ result.comparison.cheapestProductName }}</span>
        <span>庫存最多：{{ result.comparison.highestStockProductName }}</span>
        <span>銷量最高：{{ result.comparison.topSellingProductName }}</span>
        <small>{{ result.comparison.tradeOff }}</small>
      </div>
      <p v-if="!result.recommendations?.length">目前沒有符合的商品，請換個關鍵字或放寬預算。</p>
      <div v-else class="grid">
        <article v-for="item in result.recommendations" :key="item.product.productId" class="recommendation">
          <ProductCard :product="item.product" />
          <ul><li v-for="reason in item.reasons" :key="reason">{{ reason }}</li></ul>
        </article>
      </div>
    </section>
  </main>
</template>

<style scoped>
.advisor-page { max-width: 1200px; margin: 0 auto; padding: 48px 24px 72px; }
.hero { padding: 42px; color: white; border-radius: 24px; background: linear-gradient(135deg, #175b49, #1b8466); }
.hero p { font-weight: 700; letter-spacing: .04em; }.hero h1 { margin: 8px 0 24px; font-size: clamp(2rem, 5vw, 3.2rem); }
form { display: grid; gap: 12px; max-width: 760px; } textarea { min-height: 110px; padding: 14px; border: 0; border-radius: 12px; font: inherit; resize: vertical; }
button { width: fit-content; padding: 12px 18px; border: 0; border-radius: 10px; color: #175b49; background: white; font: inherit; font-weight: 700; cursor: pointer; }.error { color: #ffe2e2; }
.results { padding-top: 42px; }.results h2 { margin-bottom: 8px; }.note { color: #51615b; }.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 20px; margin-top: 24px; }
.comparison { display: grid; gap: 5px; margin-top: 18px; padding: 16px; border-radius: 12px; color: #315344; background: #eaf7f1; }.comparison small { margin-top: 4px; color: #51615b; }
.recommendation { overflow: hidden; border: 1px solid #e2e2e2; border-radius: 16px; }.recommendation ul { display: grid; gap: 6px; padding: 14px 16px 14px 34px; margin: 0; color: #315344; font-size: .92rem; }
@media (max-width: 640px) { .advisor-page { padding: 24px 16px; }.hero { padding: 26px 20px; } }
</style>
