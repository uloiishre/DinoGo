<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import flatpickr from 'flatpickr'
import { MandarinTraditional } from 'flatpickr/dist/l10n/zh-tw'
import 'flatpickr/dist/flatpickr.css'
import '@/assets/styles/seller-flatpickr.css'
import { Line } from 'vue-chartjs'
import { getSellerOrders } from '@/api/sellerOrderApi'
import { getSellerProducts } from '@/api/sellerProductApi'
import { getSellerWalletTransactions } from '@/api/sellerWalletApi'
import { getCurrentSellerId } from '@/utils/seller-session'
import {
  createSellerChartPalette,
  createSellerLineChartOptions,
  createSellerLineDataset,
  formatFullDateLabel,
  registerSellerLineChart,
} from './useSellerLineChart'

registerSellerLineChart()

const walletTransactions = ref([])
const sellerOrders = ref([])
const sellerProducts = ref([])
const trendWalletTransactions = ref([])
const trendSellerOrders = ref([])
const dashboardLoading = ref(false)
const dashboardError = ref('')
const dateInput = ref(null)
const trendStartDate = ref('')
const trendEndDate = ref('')
const trendStartInput = ref(null)
const trendEndInput = ref(null)
const selectedDate = ref(toDateKey(new Date()))
const selectedCategory = ref('ALL')
const selectedTrendKeys = ref(['sales', 'orders'])
const chartPalette = ref(createSellerChartPalette())
let dashboardRefreshTimer = null
let dashboardDatePicker = null
let trendStartPicker = null
let trendEndPicker = null

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

async function loadTrendData() {
  if (!trendStartDate.value || !trendEndDate.value) return

  const params = {
    startDate: trendStartDate.value,
    endDate: trendEndDate.value,
  }

  const [walletResponse, orderResponse] = await Promise.all([
    getSellerWalletTransactions(params),
    getSellerOrders(params),
  ])

  trendWalletTransactions.value = walletResponse.data || []
  trendSellerOrders.value = Array.isArray(orderResponse.data) ? orderResponse.data : []
}

const incomeTransactions = computed(() =>
  walletTransactions.value.filter(
    (item) => item.status === 'AVAILABLE' && item.direction === 'income',
  ),
)
const trendIncomeTransactions = computed(() =>
  trendWalletTransactions.value.filter(
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
  { key: 'averageOrderValue', label: '平均客單價' },
  { key: 'quantity', label: '銷售件數' },
]

const trendDateBuckets = computed(() => {
  const buckets = new Map()
  const start = new Date(`${trendStartDate.value}T00:00:00`)
  const end = new Date(`${trendEndDate.value}T00:00:00`)

  for (const date = new Date(start); date <= end; date.setDate(date.getDate() + 1)) {
    const dateKey = toDateKey(date)
    buckets.set(dateKey, {
      label: dateKey,
      sales: 0,
      orders: 0,
      averageOrderValue: 0,
      quantity: 0,
    })
  }

  trendIncomeTransactions.value.forEach((item) => {
    const dateKey = item.occurredAt?.slice(0, 10)
    if (buckets.has(dateKey)) {
      buckets.get(dateKey).sales += Number(item.amount || 0)
    }
  })

  trendSellerOrders.value.forEach((order) => {
    const dateKey = order.createdAt?.slice(0, 10)
    if (!buckets.has(dateKey)) return

    const bucket = buckets.get(dateKey)
    bucket.orders += 1
    bucket.quantity += getSoldQuantity([order])
  })

  buckets.forEach((bucket) => {
    bucket.averageOrderValue = bucket.orders ? Math.round(bucket.sales / bucket.orders) : 0
  })

  return [...buckets.values()]
})

const trendRangeLabel = computed(() =>
  trendStartDate.value && trendEndDate.value
    ? `${trendStartDate.value.replaceAll('-', '/')} - ${trendEndDate.value.replaceAll('-', '/')}`
    : '',
)

const trendChartData = computed(() => {
  const enabledOptions = trendMetricOptions.filter((option) =>
    selectedTrendKeys.value.includes(option.key),
  )

  return {
    labels: trendDateBuckets.value.map((bucket) => bucket.label),
    datasets: enabledOptions.map((option, index) =>
      createSellerLineDataset(
        option,
        trendDateBuckets.value.map((bucket) => bucket[option.key]),
        chartPalette.value,
        index === 0,
      ),
    ),
  }
})

const trendChartOptions = computed(() =>
  createSellerLineChartOptions({
    palette: chartPalette.value,
    formatTooltipTitle: formatFullDateLabel,
    formatValue(value, label) {
      return ['銷售額', '平均客單價'].includes(label)
        ? formatCurrency(value)
        : value.toLocaleString('zh-TW')
    },
  }),
)

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

function initializeTrendDateRange() {
  const today = new Date()
  const start = new Date(today)
  start.setDate(today.getDate() - 6)
  trendStartDate.value = toDateKey(start)
  trendEndDate.value = toDateKey(today)
}

async function applyTrendDateRange() {
  if (!trendStartDate.value || !trendEndDate.value) return
  if (trendEndDate.value < trendStartDate.value) {
    dashboardError.value = '結束日期不可早於開始日期。'
    return
  }

  try {
    dashboardError.value = ''
    await loadTrendData()
  } catch {
    dashboardError.value = '無法載入關鍵指標分析資料，請稍後再試。'
    trendWalletTransactions.value = []
    trendSellerOrders.value = []
  }
}

async function selectTrendPreset(preset) {
  const end = new Date()
  const start = new Date(end)
  if (preset === '7') start.setDate(end.getDate() - 6)
  if (preset === '30') start.setDate(end.getDate() - 29)
  if (preset === 'month') start.setDate(1)
  trendStartDate.value = toDateKey(start)
  trendEndDate.value = toDateKey(end)
  trendStartPicker?.setDate(trendStartDate.value, false)
  trendEndPicker?.setDate(trendEndDate.value, false)
  trendEndPicker?.set('minDate', trendStartDate.value)
  trendStartPicker?.set('maxDate', trendEndDate.value)
  await applyTrendDateRange()
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
  chartPalette.value = createSellerChartPalette()
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

function createTrendDatePicker(input, field, defaultDate) {
  return flatpickr(input, {
    altInput: true,
    altInputClass: 'seller-flatpickr-input coupon-date-input',
    altFormat: 'Y/m/d',
    allowInput: false,
    dateFormat: 'Y-m-d',
    defaultDate,
    disableMobile: true,
    locale: MandarinTraditional,
    monthSelectorType: 'dropdown',
    onChange: (_, dateString) => {
      if (field === 'start') {
        trendStartDate.value = dateString
        trendEndPicker?.set('minDate', dateString)
      }
      if (field === 'end') {
        trendEndDate.value = dateString
        trendStartPicker?.set('maxDate', dateString)
      }
      void applyTrendDateRange()
    },
    onReady: (_, __, instance) => {
      instance.calendarContainer.classList.add('seller-module-flatpickr')
    },
  })
}

function initializeTrendDatePickers() {
  trendStartPicker?.destroy()
  trendEndPicker?.destroy()
  trendStartPicker = trendStartInput.value
    ? createTrendDatePicker(trendStartInput.value, 'start', trendStartDate.value)
    : null
  trendEndPicker = trendEndInput.value
    ? createTrendDatePicker(trendEndInput.value, 'end', trendEndDate.value)
    : null
  trendEndPicker?.set('minDate', trendStartDate.value)
  trendStartPicker?.set('maxDate', trendEndDate.value)
}

onMounted(() => {
  loadChartPalette()
  initializeTrendDateRange()
  initializeDashboardDatePicker()
  void nextTick(initializeTrendDatePickers)
  loadDashboardData()
  void applyTrendDateRange()
  dashboardRefreshTimer = window.setInterval(
    () => {
      void loadDashboardData()
      void applyTrendDateRange()
    },
    5 * 60 * 1000,
  )
})

onUnmounted(() => {
  if (dashboardRefreshTimer) {
    window.clearInterval(dashboardRefreshTimer)
  }
  dashboardDatePicker?.destroy()
  trendStartPicker?.destroy()
  trendEndPicker?.destroy()
  dashboardDatePicker = null
  trendStartPicker = null
  trendEndPicker = null
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

    <section class="performance-panel dashboard-performance-panel">
      <div class="section-heading compact">
        <h2>關鍵指標分析</h2>
        <span>{{ trendRangeLabel }}</span>
        <span class="trend-count"
          >已選擇 {{ selectedTrendKeys.length }} / {{ trendMetricOptions.length }}</span
        >
      </div>

      <div class="performance-filter" aria-label="關鍵指標分析日期篩選">
        <div class="performance-presets">
          <button type="button" @click="selectTrendPreset('7')">近 7 天</button>
          <button type="button" @click="selectTrendPreset('30')">近 30 天</button>
          <button type="button" @click="selectTrendPreset('month')">本月</button>
        </div>
        <div class="performance-dates">
          <label>
            開始日期
            <input ref="trendStartInput" type="text" />
          </label>
          <span aria-hidden="true">至</span>
          <label>
            結束日期
            <input ref="trendEndInput" type="text" />
          </label>
        </div>
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

      <div class="performance-chart" aria-label="關鍵指標日期趨勢">
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
.product-performance-panel {
  padding: var(--space-5);
}

.performance-panel {
  display: grid;
  gap: 14px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 18px 20px;
  background: var(--color-surface);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
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
.section-heading span,
.metric-label,
.metric-compare span {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.section-heading.compact {
  align-items: center;
  justify-content: flex-start;
}

.section-heading h2 {
  margin: 0;
  color: var(--color-text-900);
  font-size: 16px;
  font-weight: 600;
}

.trend-count {
  margin-left: auto;
}

.performance-filter {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding: 2px 0;
}

.performance-presets,
.performance-dates {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.performance-presets button {
  min-height: 32px;
  border: 1px solid var(--color-border);
  border-radius: 3px;
  padding: 0 12px;
  background: var(--color-surface);
  color: var(--color-text-800);
  font: inherit;
  cursor: pointer;
}

.performance-presets button:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}

.performance-dates label {
  display: grid;
  gap: 4px;
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 600;
}

.performance-dates input {
  width: 132px;
  min-height: 32px;
  border: 1px solid var(--color-border);
  border-radius: 3px;
  padding: 0 8px;
  font: inherit;
}

.performance-dates > span {
  padding-bottom: 8px;
  color: var(--color-text-muted);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 190px), 1fr));
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.metric-card {
  position: relative;
  display: grid;
  min-width: 0;
  min-height: 116px;
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
  color: var(--color-text-900);
  font-size: clamp(1.25rem, 1rem + 1.1vw, 1.75rem);
  font-variant-numeric: tabular-nums;
  line-height: 1;
  white-space: nowrap;
}

.metric-compare {
  display: grid;
  grid-template-columns: minmax(0, 1fr) max-content;
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

.trend-selector {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
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

.performance-chart {
  height: 220px;
  min-height: 220px;
  border-top: 1px solid var(--color-border);
  padding-top: 14px;
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
    grid-template-columns: repeat(auto-fit, minmax(min(100%, 210px), 1fr));
  }
}

@media (max-width: 900px) {
  .performance-filter {
    align-items: stretch;
    flex-direction: column;
  }

  .performance-dates {
    justify-content: space-between;
  }

  .trend-selector {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .filter-bar {
    overflow-x: auto;
  }

  .trend-selector {
    grid-template-columns: 1fr;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .panel-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .product-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
