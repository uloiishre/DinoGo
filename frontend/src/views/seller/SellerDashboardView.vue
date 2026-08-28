<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import flatpickr from 'flatpickr'
import { MandarinTraditional } from 'flatpickr/dist/l10n/zh-tw'
import 'flatpickr/dist/flatpickr.css'
import '@/assets/styles/seller-flatpickr.css'
import { Line } from 'vue-chartjs'
import {
  CategoryScale,
  Chart as ChartJS,
  Filler,
  Legend,
  LinearScale,
  LineElement,
  PointElement,
  Tooltip,
} from 'chart.js'
import { getSellerOrders } from '@/api/sellerOrderApi'
import { getSellerProducts } from '@/api/sellerProductApi'
import { getSellerWalletTransactions } from '@/api/sellerWalletApi'
import { getCurrentSellerId } from '@/utils/seller-session'

ChartJS.register(CategoryScale, Filler, Legend, LinearScale, LineElement, PointElement, Tooltip)

const walletTransactions = ref([])
const sellerOrders = ref([])
const sellerProducts = ref([])
const dashboardLoading = ref(false)
const dashboardError = ref('')
const dateInput = ref(null)
const selectedDate = ref(toDateKey(new Date()))
const selectedCategory = ref('ALL')
const selectedTrendKeys = ref(['sales', 'orders'])
const chartPalette = ref({
  sales: '#657a6d',
  orders: '#9a7b42',
  cancelled: '#c73e3a',
  quantity: '#64748b',
  text: '#38423d',
  muted: '#66706a',
  border: '#e8e6e1',
  surface: '#ffffff',
})
let dashboardRefreshTimer = null
let dashboardDatePicker = null

async function loadDashboardData() {
  try {
    dashboardLoading.value = true
    dashboardError.value = ''

    const sellerId = getCurrentSellerId()
    const [walletResponse, orderResponse, productResponse] = await Promise.all([
      getSellerWalletTransactions(),
      getSellerOrders(),
      sellerId ? getSellerProducts(sellerId) : Promise.resolve({ data: [] }),
    ])

    walletTransactions.value = walletResponse.data || []
    sellerOrders.value = Array.isArray(orderResponse.data) ? orderResponse.data : []
    sellerProducts.value = Array.isArray(productResponse.data) ? productResponse.data : []
  } catch {
    dashboardError.value = '無法載入營運資料，請稍後再試。'
    walletTransactions.value = []
    sellerOrders.value = []
    sellerProducts.value = []
  } finally {
    dashboardLoading.value = false
  }
}

const incomeTransactions = computed(() =>
  walletTransactions.value.filter(
    (item) => item.status === 'AVAILABLE' && item.direction === 'income',
  ),
)

const selectedDateRevenue = computed(() => getRevenueByDate(selectedDate.value))
const sevenDayRange = computed(() => getDateRange(selectedDate.value, 6, 0))
const previousSevenDayRange = computed(() => getDateRange(selectedDate.value, 13, 7))
const sevenDayRevenue = computed(() =>
  getRevenueBetween(sevenDayRange.value.startKey, sevenDayRange.value.endKey),
)
const selectedDateOrders = computed(() => getOrdersByDate(selectedDate.value))
const yesterdayKey = computed(() => {
  const date = new Date(`${selectedDate.value}T00:00:00`)
  date.setDate(date.getDate() - 1)
  return toDateKey(date)
})
const yesterdayOrders = computed(() => getOrdersByDate(yesterdayKey.value))
const yesterdayRevenue = computed(() => getRevenueByDate(yesterdayKey.value))
const orderCount = computed(() => selectedDateOrders.value.length)
const yesterdayOrderCount = computed(() => yesterdayOrders.value.length)
const pendingShipmentCount = computed(
  () =>
    selectedDateOrders.value.filter((order) => ['PAID', 'PROCESSING'].includes(order.status))
      .length,
)
const yesterdayPendingShipmentCount = computed(
  () =>
    yesterdayOrders.value.filter((order) => ['PAID', 'PROCESSING'].includes(order.status)).length,
)
const selectedSoldQuantity = computed(() => getSoldQuantity(selectedDateOrders.value))
const yesterdaySoldQuantity = computed(() => getSoldQuantity(yesterdayOrders.value))
const averageOrderValue = computed(() =>
  orderCount.value ? Math.round(selectedDateRevenue.value / orderCount.value) : 0,
)
const yesterdayAverageOrderValue = computed(() =>
  yesterdayOrderCount.value ? Math.round(yesterdayRevenue.value / yesterdayOrderCount.value) : 0,
)
const previousSevenDayRevenue = computed(() =>
  getRevenueBetween(previousSevenDayRange.value.startKey, previousSevenDayRange.value.endKey),
)

const metricCards = computed(() => [
  {
    label: '今日銷售',
    value: formatCurrency(selectedDateRevenue.value),
    help: '選擇日期當天已入帳的訂單收入總和。',
    previous: `昨日 ${formatCurrency(yesterdayRevenue.value)}`,
    change: formatChange(selectedDateRevenue.value, yesterdayRevenue.value),
  },
  {
    label: '近 7 日營收',
    value: formatCurrency(sevenDayRevenue.value),
    help: `以選擇日期為結束日，統計 ${formatDateRange(sevenDayRange.value)} 已入帳交易加總。`,
    previous: `前 7 日 ${formatCurrency(previousSevenDayRevenue.value)}`,
    change: formatChange(sevenDayRevenue.value, previousSevenDayRevenue.value),
  },
  {
    label: '訂單數',
    value: `${orderCount.value} 筆`,
    help: '選擇日期建立的賣家訂單筆數。',
    previous: `昨日 ${yesterdayOrderCount.value} 筆`,
    change: formatChange(orderCount.value, yesterdayOrderCount.value),
  },
  {
    label: '平均客單價',
    value: formatCurrency(averageOrderValue.value),
    help: '今日銷售除以訂單數。',
    previous: `昨日 ${formatCurrency(yesterdayAverageOrderValue.value)}`,
    change: formatChange(averageOrderValue.value, yesterdayAverageOrderValue.value),
  },
  {
    label: '銷售件數',
    value: `${selectedSoldQuantity.value} 件`,
    help: '選擇日期所有訂單明細的商品數量加總。',
    previous: `昨日 ${yesterdaySoldQuantity.value} 件`,
    change: formatChange(selectedSoldQuantity.value, yesterdaySoldQuantity.value),
  },
  {
    label: '待出貨訂單',
    value: `${pendingShipmentCount.value} 筆`,
    help: '狀態為已付款或處理中的訂單，通常代表需要安排出貨。',
    previous: `昨日 ${yesterdayPendingShipmentCount.value} 筆`,
    change: formatChange(
      pendingShipmentCount.value,
      yesterdayPendingShipmentCount.value,
      'lower-is-better',
    ),
  },
])

const productLookup = computed(() => {
  const map = new Map()
  sellerProducts.value.forEach((product) => {
    map.set(product.productId, product)
  })
  return map
})

const categoryOptions = computed(() => {
  const categories = new Set(
    sellerProducts.value.map((product) => getProductCategory(product)).filter(Boolean),
  )

  return ['ALL', ...Array.from(categories)]
})

const productInsightRows = computed(() => {
  const productMap = new Map()

  selectedDateOrders.value.forEach((order) => {
    ;(order.items || []).forEach((item) => {
      const productId = item.productId || item.product?.productId || item.id
      if (!productId) return

      const current = productMap.get(productId) || {
        productId,
        productName: item.productName || item.product?.productName || `商品 #${productId}`,
        category: getProductCategory(productLookup.value.get(productId) || item.product || item),
        revenue: 0,
        orderCount: 0,
        quantity: 0,
        buyerIds: new Set(),
        stock: Number(productLookup.value.get(productId)?.stock || 0),
        status: productLookup.value.get(productId)?.status,
      }

      const quantity = Number(item.quantity || 0)
      const amount = Number(item.subtotal || item.totalAmount || item.unitPrice * quantity || 0)

      current.revenue += amount
      current.quantity += quantity
      current.orderCount += 1
      if (order.buyerId) current.buyerIds.add(order.buyerId)
      productMap.set(productId, current)
    })
  })

  return Array.from(productMap.values())
    .filter((item) => selectedCategory.value === 'ALL' || item.category === selectedCategory.value)
    .sort((a, b) => b.revenue - a.revenue)
    .slice(0, 5)
    .map((item, index) => ({
      ...item,
      rank: index + 1,
      buyerCount: item.buyerIds.size,
    }))
})

const trendMetricOptions = [
  { key: 'sales', label: '銷售額' },
  { key: 'orders', label: '訂單數' },
  { key: 'cancelled', label: '取消訂單' },
  { key: 'quantity', label: '銷售件數' },
]

const hourlyTrendBuckets = computed(() => {
  const buckets = Array.from({ length: 12 }, (_, index) => {
    const startHour = index * 2
    return {
      label: `${String(startHour).padStart(2, '0')}:00`,
      sales: 0,
      orders: 0,
      cancelled: 0,
      quantity: 0,
    }
  })

  incomeTransactions.value
    .filter((item) => item.occurredAt?.slice(0, 10) === selectedDate.value)
    .forEach((item) => {
      const hour = new Date(item.occurredAt).getHours()
      const bucketIndex = Math.min(Math.floor(hour / 2), buckets.length - 1)
      buckets[bucketIndex].sales += Number(item.amount || 0)
    })

  selectedDateOrders.value.forEach((order) => {
    if (!order.createdAt) return
    const hour = new Date(order.createdAt).getHours()
    const bucketIndex = Math.min(Math.floor(hour / 2), buckets.length - 1)
    buckets[bucketIndex].orders += 1
    buckets[bucketIndex].quantity += getSoldQuantity([order])

    if (order.status === 'CANCELLED') {
      buckets[bucketIndex].cancelled += 1
    }
  })

  return buckets
})

const trendChartData = computed(() => {
  const enabledOptions = trendMetricOptions.filter((option) =>
    selectedTrendKeys.value.includes(option.key),
  )

  return {
    labels: hourlyTrendBuckets.value.map((bucket) => bucket.label),
    datasets: enabledOptions.map((option) => ({
      label: option.label,
      data: hourlyTrendBuckets.value.map((bucket) => bucket[option.key]),
      borderColor: chartPalette.value[option.key],
      backgroundColor: toAlpha(chartPalette.value[option.key], 0.12),
      pointBackgroundColor: chartPalette.value.surface,
      pointBorderColor: chartPalette.value[option.key],
      pointHoverBackgroundColor: chartPalette.value[option.key],
      pointHoverBorderColor: chartPalette.value.surface,
      pointRadius: 3,
      pointHoverRadius: 5,
      borderWidth: 2,
      tension: 0.36,
      fill: option.key === 'sales',
    })),
  }
})

const trendChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  interaction: {
    intersect: false,
    mode: 'index',
  },
  plugins: {
    legend: {
      display: false,
    },
    tooltip: {
      backgroundColor: chartPalette.value.text,
      displayColors: true,
      padding: 12,
      callbacks: {
        label(context) {
          const value = Number(context.raw || 0)
          return context.dataset.label === '銷售額'
            ? `${context.dataset.label}: ${formatCurrency(value)}`
            : `${context.dataset.label}: ${value.toLocaleString('zh-TW')}`
        },
      },
    },
  },
  scales: {
    x: {
      grid: {
        display: false,
      },
      ticks: {
        color: chartPalette.value.muted,
        maxRotation: 0,
        autoSkip: true,
        autoSkipPadding: 18,
      },
    },
    y: {
      beginAtZero: true,
      grid: {
        color: chartPalette.value.border,
      },
      ticks: {
        color: chartPalette.value.muted,
        precision: 0,
      },
    },
  },
}))

function toDateKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getDateRange(baseDateKey, startDaysBefore, endDaysBefore) {
  const baseDate = new Date(`${baseDateKey}T00:00:00`)
  const start = new Date(baseDate)
  start.setDate(baseDate.getDate() - startDaysBefore)
  const end = new Date(baseDate)
  end.setDate(baseDate.getDate() - endDaysBefore)

  return {
    startKey: toDateKey(start),
    endKey: toDateKey(end),
  }
}

function getOrdersByDate(dateKey) {
  return sellerOrders.value.filter((order) => order.createdAt?.slice(0, 10) === dateKey)
}

function getRevenueByDate(dateKey) {
  return incomeTransactions.value
    .filter((item) => item.occurredAt?.slice(0, 10) === dateKey)
    .reduce((sum, item) => sum + Number(item.amount || 0), 0)
}

function getRevenueBetween(startKey, endKey) {
  return incomeTransactions.value
    .filter((item) => {
      const dateKey = item.occurredAt?.slice(0, 10)
      return dateKey && dateKey >= startKey && dateKey <= endKey
    })
    .reduce((sum, item) => sum + Number(item.amount || 0), 0)
}

function getSoldQuantity(orders) {
  return orders.reduce(
    (sum, order) =>
      sum + (order.items || []).reduce((itemSum, item) => itemSum + Number(item.quantity || 0), 0),
    0,
  )
}

function formatChange(current, previous, direction = 'higher-is-better') {
  const currentValue = Number(current || 0)
  const previousValue = Number(previous || 0)

  if (previousValue === 0) {
    const tone =
      currentValue === 0 ? 'neutral' : direction === 'lower-is-better' ? 'negative' : 'positive'
    return { text: currentValue === 0 ? '0.00%' : '+100%', tone }
  }

  const rate = ((currentValue - previousValue) / previousValue) * 100
  const isPositive = direction === 'lower-is-better' ? rate < 0 : rate > 0
  const isNegative = direction === 'lower-is-better' ? rate > 0 : rate < 0

  return {
    text: `${rate > 0 ? '+' : ''}${rate.toFixed(2)}%`,
    tone: isPositive ? 'positive' : isNegative ? 'negative' : 'neutral',
  }
}

function toggleTrendKey(key) {
  if (selectedTrendKeys.value.includes(key)) {
    selectedTrendKeys.value = selectedTrendKeys.value.filter((item) => item !== key)
    return
  }

  selectedTrendKeys.value = [...selectedTrendKeys.value, key]
}

function openDatePicker() {
  dashboardDatePicker?.open()
  dateInput.value?.focus()
}

function exportDashboardCsv() {
  const metricRows = metricCards.value.map((item) => ({
    type: '關鍵指標',
    name: item.label,
    value: item.value,
    previous: item.previous,
    change: item.change.text,
    product: '',
    category: '',
    revenue: '',
    orderCount: '',
    quantity: '',
    buyerCount: '',
    stock: '',
    status: '',
  }))
  const productRows = productInsightRows.value.map((item) => ({
    type: '商品銷售排名',
    name: `第 ${item.rank} 名`,
    value: '',
    previous: '',
    change: '',
    product: item.productName,
    category: item.category,
    revenue: formatCurrency(item.revenue),
    orderCount: item.orderCount,
    quantity: item.quantity,
    buyerCount: item.buyerCount,
    stock: item.stock,
    status: formatProductStatus(item.status),
  }))
  const headers = [
    '類型',
    '名稱',
    '數值',
    '比較基準',
    '變化',
    '商品',
    '分類',
    '銷售額',
    '訂單數',
    '件數',
    '買家數',
    '庫存',
    '狀態',
  ]
  const rows = [...metricRows, ...productRows].map((row) => [
    row.type,
    row.name,
    row.value,
    row.previous,
    row.change,
    row.product,
    row.category,
    row.revenue,
    row.orderCount,
    row.quantity,
    row.buyerCount,
    row.stock,
    row.status,
  ])
  const csv = [headers, ...rows].map((row) => row.map(escapeCsvCell).join(',')).join('\r\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `DinoGo_營運總覽_${selectedDate.value}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

function formatCurrency(value) {
  return `NT$${Number(value || 0).toLocaleString('zh-TW')}`
}

function formatDateRange(range) {
  return `${range.startKey} 至 ${range.endKey}`
}

function formatProductStatus(status) {
  if (status === 'ACTIVE' || status === 0) return '上架中'
  if (status === 'INACTIVE' || status === 1) return '草稿'
  if (status === 2) return '已下架'
  return '未設定'
}

function getProductCategory(product = {}) {
  return (
    product.categoryName ||
    product.category?.categoryName ||
    product.category?.name ||
    product.subcategoryName ||
    product.subcategory?.subcategoryName ||
    product.subcategory?.name ||
    product.subcategory?.category?.categoryName ||
    product.subcategory?.category?.name ||
    '未分類'
  )
}

function escapeCsvCell(value) {
  const text = String(value ?? '')
  return /[",\r\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text
}

function loadChartPalette() {
  const styles = window.getComputedStyle(document.documentElement)
  chartPalette.value = {
    sales: getCssColor(styles, '--color-primary-600', chartPalette.value.sales),
    orders: getCssColor(styles, '--color-warning', chartPalette.value.orders),
    cancelled: getCssColor(styles, '--color-danger', chartPalette.value.cancelled),
    quantity: getCssColor(styles, '--color-info', chartPalette.value.quantity),
    text: getCssColor(styles, '--color-text-700', chartPalette.value.text),
    muted: getCssColor(styles, '--color-text-muted', chartPalette.value.muted),
    border: getCssColor(styles, '--color-border', chartPalette.value.border),
    surface: getCssColor(styles, '--color-surface', chartPalette.value.surface),
  }
}

function getCssColor(styles, token, fallback) {
  return styles.getPropertyValue(token).trim() || fallback
}

function toAlpha(color, alpha) {
  const normalized = color.trim()
  if (!normalized.startsWith('#') || ![4, 7].includes(normalized.length)) {
    return normalized
  }

  const hex =
    normalized.length === 4
      ? `#${normalized[1]}${normalized[1]}${normalized[2]}${normalized[2]}${normalized[3]}${normalized[3]}`
      : normalized
  const red = Number.parseInt(hex.slice(1, 3), 16)
  const green = Number.parseInt(hex.slice(3, 5), 16)
  const blue = Number.parseInt(hex.slice(5, 7), 16)

  return `rgba(${red}, ${green}, ${blue}, ${alpha})`
}

function initializeDashboardDatePicker() {
  if (!dateInput.value) return

  dashboardDatePicker = flatpickr(dateInput.value, {
    altInput: true,
    altInputClass: 'seller-flatpickr-input seller-dashboard-date-input',
    altFormat: 'Y/m/d',
    allowInput: false,
    dateFormat: 'Y-m-d',
    defaultDate: selectedDate.value,
    disableMobile: true,
    locale: MandarinTraditional,
    monthSelectorType: 'dropdown',
    onChange: (_, dateString) => {
      selectedDate.value = dateString
    },
    onReady: (_, __, instance) => {
      instance.calendarContainer.classList.add(
        'seller-module-flatpickr',
        'seller-dashboard-date-calendar',
      )
    },
  })
}

onMounted(() => {
  loadChartPalette()
  initializeDashboardDatePicker()
  loadDashboardData()
  dashboardRefreshTimer = window.setInterval(loadDashboardData, 5 * 60 * 1000)
})

onUnmounted(() => {
  if (dashboardRefreshTimer) {
    window.clearInterval(dashboardRefreshTimer)
  }
  dashboardDatePicker?.destroy()
  dashboardDatePicker = null
})
</script>

<template>
  <section class="seller-dashboard">
    <header class="page-header">
      <div>
        <p class="eyebrow">賣家中心</p>
        <h1>營運總覽</h1>
        <p class="page-description">追蹤關鍵營運指標、銷售趨勢與商品表現。</p>
      </div>
    </header>

    <section class="filter-bar">
      <label class="date-filter" @click="openDatePicker">
        選擇日期
        <input ref="dateInput" v-model="selectedDate" type="text" aria-label="選擇營運資料日期" />
      </label>
      <button class="export-link" type="button" @click="exportDashboardCsv">
        <i class="bi bi-download" aria-hidden="true"></i>
        匯出資料
      </button>
    </section>

    <p v-if="dashboardError" class="state-message state-message--error">{{ dashboardError }}</p>
    <p v-else-if="dashboardLoading" class="state-message">營運資料載入中...</p>

    <section class="metric-panel">
      <div class="panel-heading">
        <h1>關鍵指標</h1>
        <span>每 5 分鐘自動更新</span>
      </div>

      <div class="metric-grid">
        <article v-for="item in metricCards" :key="item.label" class="metric-card">
          <span class="metric-label">
            {{ item.label }}
            <span class="metric-help" tabindex="0" aria-label="指標說明">
              ?
              <span class="metric-tooltip">{{ item.help }}</span>
            </span>
          </span>
          <strong>{{ item.value }}</strong>
          <div class="metric-compare">
            <span>{{ item.previous }}</span>
            <em :class="`metric-change--${item.change.tone}`">{{ item.change.text }}</em>
          </div>
        </article>
      </div>
    </section>

    <section class="hourly-sales-panel">
      <div class="panel-heading">
        <h2>今日趨勢</h2>
        <span>已選擇 {{ selectedTrendKeys.length }} / {{ trendMetricOptions.length }}</span>
      </div>

      <div class="trend-selector" aria-label="選擇趨勢指標">
        <button
          v-for="option in trendMetricOptions"
          :key="option.key"
          type="button"
          :class="{ active: selectedTrendKeys.includes(option.key) }"
          :style="{ '--series-color': chartPalette[option.key] }"
          @click="toggleTrendKey(option.key)"
        >
          <i></i>
          <span>{{ option.label }}</span>
        </button>
      </div>

      <div class="chart-frame chart-frame--line" aria-label="分時指標趨勢">
        <Line :data="trendChartData" :options="trendChartOptions" />
      </div>
    </section>

    <section class="product-performance-panel">
      <div class="panel-heading">
        <h1>商品銷售排名</h1>
      </div>

      <div class="product-toolbar">
        <label class="category-filter">
          分類
          <select v-model="selectedCategory" aria-label="篩選商品分類">
            <option v-for="category in categoryOptions" :key="category" :value="category">
              {{ category === 'ALL' ? '全部分類' : category }}
            </option>
          </select>
        </label>
      </div>

      <div class="ranking-control">
        排名依照
        <strong>銷售額</strong>
      </div>

      <div class="product-table">
        <div class="product-table-head">
          <span>排名</span>
          <span>商品</span>
          <span>分類</span>
          <span>銷售額</span>
          <span>訂單數</span>
          <span>件數</span>
          <span>買家數</span>
          <span>庫存</span>
          <span>狀態</span>
        </div>

        <div v-if="productInsightRows.length === 0" class="product-empty">
          <i class="bi bi-file-earmark-bar-graph" aria-hidden="true"></i>
          <p>目前尚未收集到足夠的數據進行商品排名，可先完成訂單或上架商品後再查看。</p>
        </div>

        <div
          v-for="item in productInsightRows"
          v-else
          :key="`${selectedCategory}-${item.productId}`"
          class="product-table-row"
        >
          <span>{{ item.rank }}</span>
          <strong>{{ item.productName }}</strong>
          <span>{{ item.category }}</span>
          <span>{{ formatCurrency(item.revenue) }}</span>
          <span>{{ item.orderCount }}</span>
          <span>{{ item.quantity }}</span>
          <span>{{ item.buyerCount }}</span>
          <span>{{ item.stock }}</span>
          <span>{{ formatProductStatus(item.status) }}</span>
        </div>
      </div>
    </section>
  </section>
</template>

<style scoped>
.seller-dashboard {
  display: grid;
  gap: var(--space-5);
  width: min(100%, 1480px);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.eyebrow,
.page-description {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.eyebrow {
  margin: 0 0 var(--space-1);
}

.page-description {
  margin: var(--space-1) 0 0;
}

.page-header h1 {
  margin: 0;
  color: var(--color-text-900);
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
  font-weight: 700;
}

.filter-bar,
.metric-panel,
.hourly-sales-panel,
.product-performance-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-5);
}

.date-filter,
.filter-bar button {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  gap: var(--space-2);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--color-surface);
  color: var(--color-text-700);
  font: inherit;
  text-decoration: none;
}

.date-filter {
  cursor: pointer;
  font-weight: 700;
}

.date-filter input {
  min-height: 30px;
  border: 0;
  background: transparent;
  color: var(--color-text-900);
  cursor: pointer;
  font: inherit;
  font-weight: 800;
}

.date-filter input:focus {
  outline: none;
}

.export-link {
  margin-left: auto;
  cursor: pointer;
  font-weight: 700;
}

.metric-panel,
.hourly-sales-panel,
.product-performance-panel {
  padding: var(--space-5);
}

.panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.panel-heading h1,
.panel-heading h2 {
  margin: 0;
  color: var(--color-text-900);
  font-family: var(--font-heading);
}

.panel-heading h1 {
  font-size: var(--font-size-lg);
}

.panel-heading h2 {
  font-size: var(--font-size-base);
}

.panel-heading span,
.metric-label,
.metric-compare span {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.metric-card {
  position: relative;
  display: grid;
  min-width: 0;
  min-height: 112px;
  grid-template-rows: auto 1fr auto;
  gap: var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-3);
  background: linear-gradient(180deg, var(--color-surface) 0%, var(--color-bg-muted) 100%);
}

.metric-label {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
  white-space: nowrap;
}

.metric-help {
  position: relative;
  display: inline-grid;
  place-items: center;
  width: 15px;
  height: 15px;
  flex: 0 0 auto;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-pill);
  color: var(--color-text-muted);
  font-size: 10px;
  font-style: normal;
  line-height: 1;
  cursor: help;
}

.metric-tooltip {
  position: absolute;
  left: 50%;
  top: calc(100% + 8px);
  z-index: 30;
  display: block;
  width: max-content;
  max-width: 240px;
  transform: translateX(-50%) translateY(-2px);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  padding: 8px 10px;
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
  color: var(--color-text-950);
  font-size: var(--font-size-xs);
  font-weight: 700;
  line-height: 1.5;
  opacity: 0;
  pointer-events: none;
  text-align: left;
  transition:
    opacity 0.15s ease,
    transform 0.15s ease;
  white-space: normal;
}

.metric-tooltip::before {
  position: absolute;
  left: 50%;
  bottom: 100%;
  width: 8px;
  height: 8px;
  border-top: 1px solid var(--color-border-strong);
  border-left: 1px solid var(--color-border-strong);
  background: var(--color-surface);
  content: '';
  transform: translate(-50%, 4px) rotate(45deg);
}

.metric-help:hover .metric-tooltip,
.metric-help:focus .metric-tooltip,
.metric-help:focus-visible .metric-tooltip {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

.metric-card strong {
  min-width: 0;
  overflow: hidden;
  color: var(--color-text-900);
  font-size: 28px;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-compare {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--space-2);
  align-items: end;
  border-top: 1px solid var(--color-border);
  padding-top: var(--space-2);
}

.metric-compare span,
.metric-compare em {
  min-width: 0;
  overflow: hidden;
  font-size: var(--font-size-xs);
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-compare em {
  justify-self: end;
  border-radius: var(--radius-pill);
  padding: 2px 7px;
  font-weight: 800;
}

.metric-change--positive {
  background: var(--color-success-soft);
  color: var(--color-success);
}

.metric-change--negative {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.metric-change--neutral {
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
}

.hourly-sales-panel {
  display: grid;
  gap: var(--space-4);
  align-content: start;
}

.trend-selector {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.trend-selector button {
  display: inline-flex;
  min-height: 28px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 var(--space-2);
  background: var(--color-surface);
  color: var(--color-text-700);
  font: inherit;
  font-size: var(--font-size-xs);
  font-weight: 800;
  cursor: pointer;
}

.trend-selector button > i {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-pill);
  background: var(--series-color);
  opacity: 0.28;
}

.trend-selector button.active {
  border-color: var(--series-color);
  background: color-mix(in srgb, var(--series-color) 8%, var(--color-surface));
}

.trend-selector button.active > i {
  opacity: 1;
}

.trend-selector button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.chart-frame {
  position: relative;
  min-width: 0;
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
  padding: var(--space-4);
}

.chart-frame--line {
  height: 320px;
}

.product-performance-panel {
  display: grid;
  gap: var(--space-3);
}

.product-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: var(--space-3);
  border-bottom: 1px solid var(--color-border);
  padding-bottom: var(--space-3);
}

.category-filter,
.ranking-control {
  display: inline-flex;
  min-height: 36px;
  align-items: center;
  gap: var(--space-2);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--color-surface);
  color: var(--color-text-700);
  font-size: var(--font-size-sm);
  text-decoration: none;
}

.category-filter select,
.ranking-control strong {
  border: 0;
  background: transparent;
  color: var(--color-text-900);
  font: inherit;
  font-weight: 800;
}

.category-filter select:focus {
  outline: none;
}

.ranking-control {
  margin-top: var(--space-3);
}

.product-table {
  margin-top: var(--space-3);
  overflow-x: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.product-table-head,
.product-table-row {
  display: grid;
  grid-template-columns:
    64px minmax(220px, 1.6fr) minmax(112px, 0.85fr) minmax(132px, 1fr)
    minmax(84px, 0.7fr) minmax(84px, 0.7fr) minmax(84px, 0.7fr) minmax(84px, 0.7fr)
    minmax(96px, 0.8fr);
  align-items: center;
  min-width: 900px;
}

.product-table-head {
  min-height: 44px;
  background: var(--color-bg-muted);
  color: var(--color-text-700);
  font-size: var(--font-size-sm);
  font-weight: 800;
}

.product-table-head span,
.product-table-row span,
.product-table-row strong {
  min-width: 0;
  padding: 0 var(--space-3);
  white-space: nowrap;
}

.product-table-row {
  min-height: 52px;
  border-top: 1px solid var(--color-border);
  color: inherit;
  font-size: var(--font-size-sm);
  text-decoration: none;
}

.product-table-row strong {
  color: var(--color-text-900);
}

.product-empty {
  display: grid;
  min-height: 170px;
  place-items: center;
  gap: var(--space-3);
  padding: var(--space-5);
  color: var(--color-text-muted);
  text-align: center;
}

.product-empty i {
  color: var(--color-border-strong);
  font-size: 42px;
}

.product-empty p {
  margin: 0;
  max-width: 520px;
  font-size: var(--font-size-sm);
}

.state-message {
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
  font-weight: 700;
}

.state-message--error {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

@media (max-width: 1180px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .filter-bar {
    overflow-x: auto;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .product-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
