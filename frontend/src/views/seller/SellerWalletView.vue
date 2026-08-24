<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getSellerWallet, getSellerWalletTransactions } from '@/api/sellerWalletApi'

const wallet = ref({
  availableBalance: 0,
  pendingBalance: 0,
  withdrawnBalance: 0,
  bankName: null,
  bankAccountLast4: null,
  bankVerified: false,
})

const transactions = ref([])
const isLoading = ref(false)
const errorMessage = ref('')

const filters = reactive({
  range: '2026/08/01 - 2026/08/31',
  direction: 'all',
  keyword: '',
  types: ['ORDER_INCOME', 'ADJUSTMENT', 'WALLET_PAY', 'REFUND', 'WITHDRAW'],
})

const transactionTypes = [
  { label: '訂單收入', value: 'ORDER_INCOME' },
  { label: '帳款調整', value: 'ADJUSTMENT' },
  { label: '錢包支付', value: 'WALLET_PAY' },
  { label: '未購買成功的退款', value: 'REFUND' },
  { label: '已提領金額', value: 'WITHDRAW' },
]

const filteredTransactions = computed(() => {
  return transactions.value.filter((item) => {
    const matchesDirection = filters.direction === 'all' || item.direction === filters.direction
    const matchesType = filters.types.includes(item.typeValue)
    const keyword = filters.keyword.trim().toLowerCase()
    const matchesKeyword =
      !keyword ||
      item.orderNo.toLowerCase().includes(keyword) ||
      item.id.toLowerCase().includes(keyword)

    return matchesDirection && matchesType && matchesKeyword
  })
})

const hasTransactions = computed(() => filteredTransactions.value.length > 0)

const bankInitial = computed(() => {
  return wallet.value.bankName?.slice(0, 1) || '銀'
})

function formatCurrency(value) {
  const amount = Number(value || 0)
  const prefix = amount > 0 ? '+' : amount < 0 ? '-' : ''
  return `${prefix}NT$${Math.abs(amount).toLocaleString('zh-TW')}`
}

function formatAmount(value) {
  return Number(value || 0).toLocaleString('zh-TW')
}

function formatDate(value) {
  if (!value) {
    return '-'
  }

  return value.slice(0, 10).replaceAll('-', '/')
}

function mapTransactionStatus(status) {
  const statusMap = {
    AVAILABLE: '已入帳',
    PENDING: '待入帳',
  }

  return statusMap[status] || status
}

function mapTransactionType(type) {
  const typeMap = {
    ORDER_INCOME: '訂單收入',
  }

  return typeMap[type] || type
}

function resetFilters() {
  filters.direction = 'all'
  filters.keyword = ''
  filters.types = transactionTypes.map((item) => item.value)
}

async function loadWallet() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const walletResponse = await getSellerWallet()
    wallet.value = walletResponse.data
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      error.response?.data ||
      '目前無法載入錢包總覽，請確認後端已重新啟動並使用賣家帳號登入。'
    isLoading.value = false
    return
  }

  try {
    const transactionResponse = await getSellerWalletTransactions()
    transactions.value = transactionResponse.data.map((item) => ({
      id: `ORDER-${item.orderId}`,
      orderNo: item.orderNo,
      date: formatDate(item.occurredAt),
      type: mapTransactionType(item.transactionType),
      typeValue: item.transactionType,
      direction: item.direction,
      amount: item.amount,
      status: mapTransactionStatus(item.status),
    }))
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      error.response?.data ||
      '目前無法載入交易紀錄，請稍後再試。'
  } finally {
    isLoading.value = false
  }
}

onMounted(loadWallet)
</script>

<template>
  <section class="seller-page wallet-page">
    <header class="page-header">
      <div>
        <p class="section-kicker">Seller Wallet</p>
        <h1>賣家錢包</h1>
        <p class="page-description">管理提領金額、銀行帳號與近期交易紀錄。</p>
      </div>
    </header>

    <section class="wallet-block">
      <h2>金額總覽</h2>

      <div class="balance-card">
        <div class="balance-main">
          <span>可提領金額</span>
          <div class="balance-value">
            <strong>NT${{ formatAmount(wallet.availableBalance) }}</strong>
            <button class="withdraw-button" type="button" disabled>提款</button>
          </div>
          <p>待入帳金額 NT${{ formatAmount(wallet.pendingBalance) }}</p>
        </div>

        <div class="bank-panel">
          <div class="bank-heading">
            <h3>我的銀行帳號</h3>
            <button class="link-button" type="button">
              更多 <i class="bi bi-chevron-right"></i>
            </button>
          </div>
          <div v-if="wallet.bankName" class="bank-account">
            <div class="bank-logo">{{ bankInitial }}</div>
            <div>
              <strong>{{ wallet.bankName }}</strong>
              <p>
                <span v-if="wallet.bankVerified">已驗證</span>
                <b>預設</b>
                **** {{ wallet.bankAccountLast4 }}
              </p>
            </div>
          </div>
          <div v-else class="bank-empty">
            <i class="bi bi-bank" aria-hidden="true"></i>
            <span>尚未設定提領銀行帳號</span>
          </div>
        </div>
      </div>
    </section>

    <section class="wallet-block transaction-block">
      <h2>最近的交易</h2>

      <div class="filter-panel">
        <label class="filter-field date-filter">
          <span>交易日期</span>
          <select v-model="filters.range">
            <option value="2026/08/01 - 2026/08/24">在本月：2026/08/01 - 2026/08/24</option>
            <option value="2026/07/01 - 2026/07/31">上個月：2026/07/01 - 2026/07/31</option>
          </select>
        </label>

        <div class="filter-field segmented-row" aria-label="進帳與支出">
          <span>進帳/支出</span>
          <div class="segmented-control">
            <button
              type="button"
              :class="{ active: filters.direction === 'all' }"
              @click="filters.direction = 'all'"
            >
              全部
            </button>
            <button
              type="button"
              :class="{ active: filters.direction === 'income' }"
              @click="filters.direction = 'income'"
            >
              進帳
            </button>
            <button
              type="button"
              :class="{ active: filters.direction === 'expense' }"
              @click="filters.direction = 'expense'"
            >
              支出
            </button>
          </div>
        </div>

        <div class="filter-field type-filter">
          <span>交易類型</span>
          <div class="check-list">
            <label v-for="item in transactionTypes" :key="item.value">
              <input v-model="filters.types" type="checkbox" :value="item.value" />
              {{ item.label }}
            </label>
          </div>
        </div>

        <div class="filter-actions">
          <button class="secondary-button" type="button" @click="resetFilters">重置</button>
          <button class="outline-button" type="button">搜尋</button>
        </div>
      </div>

      <div class="transaction-tools">
        <label class="search-box">
          <i class="bi bi-search" aria-hidden="true"></i>
          <input v-model="filters.keyword" type="search" placeholder="搜尋訂單編號" />
        </label>
        <button class="secondary-button" type="button">匯出報表</button>
        <button class="icon-button" type="button" aria-label="切換列表檢視">
          <i class="bi bi-list-ul" aria-hidden="true"></i>
        </button>
      </div>

      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

      <div v-if="isLoading" class="empty-state">
        <i class="bi bi-arrow-clockwise" aria-hidden="true"></i>
        <p>錢包資料載入中</p>
      </div>

      <div v-else-if="hasTransactions" class="transaction-table">
        <div class="table-head">
          <span>日期</span>
          <span>訂單/交易編號</span>
          <span>交易類型</span>
          <span>狀態</span>
          <span>金額</span>
        </div>

        <article v-for="item in filteredTransactions" :key="item.id" class="table-row">
          <span>{{ item.date }}</span>
          <strong>{{ item.orderNo }}</strong>
          <span>{{ item.type }}</span>
          <span>{{ item.status }}</span>
          <strong :class="item.direction === 'income' ? 'income' : 'expense'">
            {{ formatCurrency(item.amount) }}
          </strong>
        </article>
      </div>

      <div v-else class="empty-state">
        <i class="bi bi-file-earmark-bar-graph" aria-hidden="true"></i>
        <p>無交易紀錄</p>
      </div>
    </section>
  </section>
</template>

<style scoped>
.wallet-page {
  display: grid;
  gap: var(--space-5);
  width: 100%;
  max-width: none;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.section-kicker,
.page-description,
.balance-main span,
.balance-main p,
.filter-panel span,
.bank-account p,
.table-head,
.table-row span,
.empty-state p {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.section-kicker,
h1,
h2,
h3,
p {
  margin-top: 0;
}

.section-kicker {
  margin-bottom: var(--space-1);
  font-weight: 800;
}

h1,
h2,
h3 {
  color: var(--color-text-900);
  font-family: var(--font-heading);
}

h1 {
  margin-bottom: var(--space-1);
  font-size: var(--font-size-xl);
}

h2 {
  margin-bottom: var(--space-4);
  font-size: var(--font-size-lg);
}

h3 {
  margin-bottom: 0;
  font-size: var(--font-size-md);
}

.page-description,
.balance-main p,
.bank-account p,
.empty-state p {
  margin-bottom: 0;
}

.wallet-block {
  width: 100%;
  padding: var(--space-5);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.balance-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 360px);
  gap: var(--space-5);
  min-height: 128px;
  padding: var(--space-5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.balance-main {
  display: grid;
  align-content: center;
  gap: var(--space-2);
}

.balance-value {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.balance-value strong {
  color: var(--color-text-900);
  font-size: 32px;
  line-height: 1;
}

.withdraw-button {
  min-height: 40px;
  border: 0;
  border-radius: var(--radius-md);
  padding: 0 var(--space-5);
  background: #ef9a91;
  color: var(--color-surface);
  font-weight: 800;
}

.withdraw-button:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.bank-panel {
  display: grid;
  align-content: center;
  gap: var(--space-4);
  min-width: 0;
  padding-left: var(--space-5);
  border-left: 1px solid var(--color-border);
}

.bank-heading,
.bank-account,
.bank-empty,
.filter-actions,
.transaction-tools,
.search-box {
  display: flex;
  align-items: center;
}

.bank-heading {
  justify-content: space-between;
}

.bank-account {
  gap: var(--space-3);
  min-width: 0;
}

.bank-empty {
  min-height: 56px;
  gap: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
}

.bank-empty i {
  color: var(--color-primary-active);
  font-size: 22px;
}

.bank-logo {
  display: grid;
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
  background: #e84c3d;
  color: var(--color-surface);
  font-weight: 900;
}

.bank-account strong {
  display: block;
  margin-bottom: 4px;
  color: var(--color-primary-active);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bank-account b {
  margin: 0 var(--space-2);
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--color-primary-soft);
  color: var(--color-primary-active);
  font-size: var(--font-size-xs);
}

.link-button,
.secondary-button,
.outline-button,
.icon-button,
.segmented-control button {
  min-height: 36px;
  border-radius: var(--radius-sm);
  padding: 0 var(--space-4);
  background: var(--color-surface);
  color: var(--color-text-700);
  font: inherit;
  font-weight: 800;
}

.link-button {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  border: 0;
  color: var(--color-primary-active);
}

.transaction-block {
  display: grid;
  gap: var(--space-4);
}

.filter-panel {
  display: grid;
  grid-template-columns: minmax(280px, 420px) minmax(240px, 1fr);
  gap: var(--space-4) var(--space-5);
  align-items: start;
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
}

.filter-field {
  display: grid;
  gap: var(--space-2);
}

.date-filter select {
  min-height: 38px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  padding: 0 var(--space-3);
  background: var(--color-surface);
  color: var(--color-text);
}

.segmented-control {
  display: flex;
  flex-wrap: wrap;
}

.segmented-control button {
  border: 1px solid #ef604f;
  border-right: 0;
  color: #ef604f;
  white-space: nowrap;
}

.segmented-control button:first-child {
  border-radius: var(--radius-sm) 0 0 var(--radius-sm);
}

.segmented-control button:last-child {
  border-right: 1px solid #ef604f;
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.segmented-control button.active {
  background: #ef604f;
  color: var(--color-surface);
}

.type-filter {
  grid-column: 1 / -1;
}

.check-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.check-list label {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-700);
  font-size: var(--font-size-sm);
}

.check-list input {
  width: 16px;
  height: 16px;
  accent-color: #ef604f;
}

.filter-actions {
  grid-column: 1 / -1;
  justify-content: flex-end;
  gap: var(--space-3);
}

.secondary-button,
.icon-button {
  border: 1px solid var(--color-border-strong);
}

.outline-button {
  border: 1px solid #ef604f;
  color: #ef604f;
}

.transaction-tools {
  justify-content: flex-end;
  gap: var(--space-3);
}

.search-box {
  min-width: 260px;
  min-height: 38px;
  gap: var(--space-2);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  padding: 0 var(--space-3);
  background: var(--color-surface);
}

.search-box input {
  width: 100%;
  border: 0;
  outline: 0;
  color: var(--color-text);
  font: inherit;
}

.icon-button {
  display: grid;
  width: 38px;
  padding: 0;
  place-items: center;
}

.transaction-table {
  overflow-x: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: 140px minmax(180px, 1fr) 160px 120px 140px;
  align-items: center;
  gap: var(--space-3);
  min-width: 760px;
  padding: var(--space-3) var(--space-4);
}

.table-head {
  background: var(--color-bg-muted);
  font-weight: 800;
}

.table-row {
  min-height: 64px;
  border-top: 1px solid var(--color-border);
}

.table-row strong {
  color: var(--color-text-900);
}

.table-row .income {
  color: var(--color-primary-active);
}

.table-row .expense {
  color: #b6473d;
}

.empty-state {
  display: grid;
  min-height: 240px;
  place-items: center;
  align-content: center;
  gap: var(--space-3);
  color: var(--color-text-muted);
}

.error-message {
  margin: 0;
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  background: #fff4f1;
  color: #b6473d;
  font-weight: 800;
}

.empty-state i {
  font-size: 56px;
  opacity: 0.4;
}

@media (max-width: 980px) {
  .balance-card,
  .filter-panel {
    grid-template-columns: 1fr;
  }

  .bank-panel {
    padding-top: var(--space-4);
    padding-left: 0;
    border-top: 1px solid var(--color-border);
    border-left: 0;
  }

  .type-filter {
    grid-column: 1 / -1;
  }

  .transaction-tools {
    align-items: stretch;
    flex-direction: column;
  }

  .search-box {
    min-width: 0;
  }
}

@media (max-width: 640px) {
  .wallet-block,
  .balance-card,
  .filter-panel {
    padding: var(--space-4);
  }

  .balance-value,
  .bank-heading,
  .filter-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .withdraw-button,
  .secondary-button,
  .outline-button {
    width: 100%;
  }

  .segmented-control button {
    flex: 1 1 33.333%;
  }
}
</style>
